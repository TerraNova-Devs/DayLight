package de.mcterranova.dayLight;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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
    private double customTime;
    private long sleeping;
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
        getCommand("addtime").setExecutor(this);
        getCommand("addtime").setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, this);

        startDayNightCycleTask(world);
    }

    private void loadConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();
        dayLengthTicks = config.getLong("day-length-minutes", 10) * 60 * 20;
        nightLengthTicks = config.getLong("night-length-minutes", 10) * 60 * 20;
        sleepRatio = config.getDouble("sleep-ratio", 0.5);
    }

    private void startDayNightCycleTask(World world) {
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean isDay = customTime < 12000;
                long ticksPerCycle = isDay ? dayLengthTicks : nightLengthTicks;
                double increment = 12000.0 / ticksPerCycle;

                customTime = (customTime + increment) % 24000;
                world.setTime((long) customTime);
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    // Allow players to skip nights
    @EventHandler
    public void onPlayerSleep(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        long needed = (long) Math.ceil(world.getPlayers().size() * sleepRatio);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            sleeping = world.getPlayers().stream().filter(p -> p.getSleepTicks() > 0).count();
            if (sleeping >= needed) {
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    customTime = 0;
                    world.setTime(0);
                    world.setStorm(false);
                    world.setThundering(false);
                }, 20L);
                world.sendMessage(Component.text("§aDie Nacht wird übersprungen"));
            } else if (player.getSleepTicks() != 0){
                player.sendMessage(Component.text("§cEs schlafen nicht genügend Spieler: " + sleeping + "/" + needed));
            }
        }, 2L);
    }

    // Allow manual weather changes
    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (event.getCause().equals(WeatherChangeEvent.Cause.COMMAND)) {
            return;
        }
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

        if (command.getName().equalsIgnoreCase("addtime")) {
            if(!sender.hasPermission("daylight.addtime")) {
                sender.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }
            if (args.length == 1) {
                try {
                    long addedTicks = Long.parseLong(args[0]);
                    customTime = (customTime + addedTicks) % 24000;
                    sender.sendMessage("§aAdded " + addedTicks + " ticks to the current time.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cPlease provide a valid number of ticks.");
                }
            } else {
                sender.sendMessage("§cUsage: /addtime <ticks>");
            }
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
