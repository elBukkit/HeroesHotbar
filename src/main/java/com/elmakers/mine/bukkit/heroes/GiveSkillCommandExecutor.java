package com.elmakers.mine.bukkit.heroes;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Command executor responsible for handling the /giveskill command.
 */
public class GiveSkillCommandExecutor implements CommandExecutor {
    private final HotbarController controller;

    public GiveSkillCommandExecutor(HotbarController controller) {
        this.controller = controller;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Heroes.commands.skillmenu")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission for this command");
            return true;
        }
        if (!(sender instanceof Player) && args.length <= 1) {
            sender.sendMessage(ChatColor.RED + "Console Usage: giveskill <player> <skill>");
            return true;
        }
        if (args.length <= 0) {
            sender.sendMessage(ChatColor.RED + "Usage: giveskill [player] <skill>");
            return true;
        }

        Player player = sender instanceof Player ? (Player)sender : null;
        if (args.length >= 2) {
            player = Bukkit.getPlayer(args[0]);
        }
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Unknown player: " + args[0]);
            return true;
        }

        String skillName = args.length > 1 ? args[1] : args[0];
        SkillDescription skillDescription = new SkillDescription(controller, player, skillName);
        if (!skillDescription.isValid()) {
            sender.sendMessage(ChatColor.RED + "Unknown skill: " + skillName);
            return true;
        }

        ItemStack item = controller.createSkillItem(skillDescription, player);
        player.getInventory().addItem(item);
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Gave skill " + skillName + " to " + player.getName());
        return true;
    }
}
