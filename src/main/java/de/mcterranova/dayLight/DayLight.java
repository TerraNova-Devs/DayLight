package de.mcterranova.dayLight;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class DayLight extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {

    private long dayLengthTicks;
    private long nightLengthTicks;
    private double timeIncrement;
    private double sleepRatio;
    private World world;
    private Random random;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        world = Bukkit.getWorlds().get(0);
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doWeatherCycle", "false");

        random = new Random();

        getCommand("daynightreload").setExecutor(this);
        getCommand("daynightreload").setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, this);

        startDayNightCycleTask();
        startWeatherCycleTask();
    }

    private void loadConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();
        dayLengthTicks = config.getLong("day-length-minutes", 120) * 60 * 20;
        nightLengthTicks = config.getLong("night-length-minutes", 60) * 60 * 20;
        sleepRatio = config.getDouble("sleep-ratio", 0.5);
    }

    private void startDayNightCycleTask() {
        new BukkitRunnable() {
            private double customTime = world.getTime();

            @Override
            public void run() {
                boolean isDay = customTime < 12000;
                long currentPeriodLength = isDay ? dayLengthTicks : nightLengthTicks;
                timeIncrement = 12000.0 / currentPeriodLength;

                customTime = (customTime + timeIncrement) % 24000;
                world.setTime((long) customTime);
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    // Weather cycle logic
    private void startWeatherCycleTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean currentlyStorming = world.hasStorm();
                int nextWeatherDuration = random.nextInt(10) + 5; // weather changes every 5-15 minutes

                world.setStorm(!currentlyStorming);
                world.setWeatherDuration(nextWeatherDuration * 60 * 20);
            }
        }.runTaskTimer(this, 0L, 20L * 60L * 10L); // checks every 10 real-time minutes
    }

    // Allow players to skip nights
    @EventHandler
    public void onPlayerSleep(PlayerBedEnterEvent event) {
        World world = event.getPlayer().getWorld();
        long sleeping = world.getPlayers().stream().filter(LivingEntity::isSleeping).count();
        long needed = (long) Math.ceil(world.getPlayers().size() * sleepRatio);

        if (sleeping >= needed) {
            world.setTime(0);
            world.setStorm(false);
            world.setThundering(false);
        }
    }

    // Allow manual weather changes
    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        event.setCancelled(false);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("daynightreload")) {
            if (!sender.hasPermission("daylight.reload")) {
                sender.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }
            loadConfig();
            sender.sendMessage("§aDay/Night timer configuration reloaded.");
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
