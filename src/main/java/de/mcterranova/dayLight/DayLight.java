package de.mcterranova.dayLight;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.List;

public final class DayLight extends JavaPlugin implements CommandExecutor, TabCompleter {

    private long dayLengthTicks;
    private long nightLengthTicks;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        getCommand("daynightreload").setExecutor(this);
        getCommand("daynightreload").setTabCompleter(this);

        startDayNightCycleTask();
    }

    private void loadConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();
        dayLengthTicks = config.getLong("day-length-minutes", 60) * 60 * 20;
        nightLengthTicks = config.getLong("night-length-minutes", 15) * 60 * 20;
    }

    private void startDayNightCycleTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorlds().get(0);
                long time = world.getTime();

                if (time >= 0 && time < 12000) {
                    world.setTime((time + (12000 / (dayLengthTicks / 20))) % 24000);
                } else {
                    world.setTime(12000 + (time - 12000 + (12000 / (nightLengthTicks / 20))) % 12000);
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("daynightreload")) {
            if(!sender.hasPermission("daylight.reload")) {
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
