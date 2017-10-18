package com.elmakers.mine.bukkit.heroes;

import com.elmakers.mine.bukkit.utility.NMSUtils;
import com.herocraftonline.heroes.Heroes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This is the main Plugin class for the Heroes Hotbar plugin.
 */
public class HotbarPlugin extends JavaPlugin {
    private HotbarController controller;
    private Plugin heroesPlugin;

    /*
     * Plugin interface
     */
    @Override
    public void onLoad() {
    }

    @Override
    public void onEnable() {
        try {
            heroesPlugin = getServer().getPluginManager().getPlugin("Heroes");
            if (heroesPlugin == null || !(heroesPlugin instanceof Heroes)) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[HeroesHotbar] Heroes could not be found, HeroesHotbar plugin will not load.");
                return;
            }
        } catch (Throwable ex) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[HeroesHotbar] There was an error finding the Heroes plugin, HeroesHotbar plugin will not load.");
            getLogger().warning(ex.getMessage());
            return;
        }

        if (NMSUtils.getFailed()) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[HeroesHotbar] Something went wrong with some Deep Magic, plugin will not load.");
            Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED + "[HeroesHotbar] Please make sure you are running a compatible version of " + ChatColor.RED + "Spigot (1.9 or Higher)!");
        } else {
            if (NMSUtils.isLegacy()) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[HeroesHotbar] Using backwards-compatibility layer. It is highly recommended that you update to the latest Spigot version and/or the latest plugin version.");
            }
            initialize();
        }
    }

    @Override
    public void onDisable() {
        if (controller != null) {
            controller.clear();
        }
    }

    /**
     * Initialization, set up commands, listeners and controller
     */
    protected void initialize() {
        saveDefaultConfig();

        // Set up controller
        if (controller == null) {
            controller = new HotbarController(this, (Heroes) heroesPlugin);
        }

        controller.initialize();

        // Set up command executors
        CommandExecutor skillsMenuCommand = new SkillsMenuCommandExecutor(controller);
        getCommand("skillmenu").setExecutor(skillsMenuCommand);

        // Set up listeners
        InventoryListener inventoryListener = new InventoryListener(controller);
        getServer().getPluginManager().registerEvents(inventoryListener, this);
        PlayerListener playerListener = new PlayerListener(controller);
        getServer().getPluginManager().registerEvents(playerListener, this);
    }
}
