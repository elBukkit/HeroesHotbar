package com.elmakers.mine.bukkit.heroes;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command executor responsible for handling the /skillmenu command.
 */
public class SkillsMenuCommandExecutor implements CommandExecutor {
    private final HotbarController controller;

    public SkillsMenuCommandExecutor(HotbarController controller) {
        this.controller = controller;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command may only be used in-game");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("Heroes.commands.skillmenu")) {
            player.sendMessage(ChatColor.RED + "You don't have permission for this command");
            return true;
        }
        // TODO
        return true;
    }
}
