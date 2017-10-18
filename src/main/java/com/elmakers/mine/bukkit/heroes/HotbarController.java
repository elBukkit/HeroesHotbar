package com.elmakers.mine.bukkit.heroes;

import org.bukkit.plugin.Plugin;

/**
 * This class manages the centralized hotbar functionality.
 */
public class HotbarController {
    private final Plugin owningPlugin;
    private final Plugin heroesPlugin;

    public HotbarController(Plugin owningPlugin, Plugin heroesPlugin) {
        this.owningPlugin = owningPlugin;
        this.heroesPlugin = heroesPlugin;
    }

    public void initialize() {

    }

    public void clear() {

    }

}
