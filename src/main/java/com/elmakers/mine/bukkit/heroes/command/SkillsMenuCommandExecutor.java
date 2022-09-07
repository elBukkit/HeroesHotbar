package com.elmakers.mine.bukkit.heroes.command;

import com.elmakers.mine.bukkit.heroes.controller.SkillSelector;
import com.elmakers.mine.bukkit.heroes.controller.HotbarController;
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

        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (Exception ex) {
                sender.sendMessage(ChatColor.RED + "Expected page number, got " + args[0]);
                return true;
            }
        }

        showSkillsMenu(player, page);
        return true;
    }

    private void showSkillsMenu(Player player, int page) {
        SkillSelector selector = controller.getActiveSkillSelector(player);
        selector.setPage(page);
        selector.openInventory();
    }
}
