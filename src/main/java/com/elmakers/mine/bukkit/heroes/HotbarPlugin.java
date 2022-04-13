package com.elmakers.mine.bukkit.heroes;

import com.elmakers.mine.bukkit.heroes.command.GiveSkillCommandExecutor;
import com.elmakers.mine.bukkit.heroes.command.SkillsMenuCommandExecutor;
import com.elmakers.mine.bukkit.heroes.controller.HotbarController;
import com.elmakers.mine.bukkit.heroes.listener.InventoryListener;
import com.elmakers.mine.bukkit.heroes.listener.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.elmakers.mine.bukkit.heroes.utilities.CompatibilityUtils;
import com.herocraftonline.heroes.Heroes;

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
        initialize();
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
        CompatibilityUtils.initialize(this);
        saveDefaultConfig();

        // Set up controller
        if (controller == null) {
            controller = new HotbarController(this, (Heroes) heroesPlugin);
        }

        controller.initialize();

        // Set up command executors
        CommandExecutor skillsMenuCommand = new SkillsMenuCommandExecutor(controller);
        getCommand("skillmenu").setExecutor(skillsMenuCommand);
        CommandExecutor giveSkillCommand = new GiveSkillCommandExecutor(controller);
        getCommand("giveskill").setExecutor(giveSkillCommand);

        // Set up listeners
        InventoryListener inventoryListener = new InventoryListener(controller);
        getServer().getPluginManager().registerEvents(inventoryListener, this);
        PlayerListener playerListener = new PlayerListener(controller);
        getServer().getPluginManager().registerEvents(playerListener, this);
    }
}
