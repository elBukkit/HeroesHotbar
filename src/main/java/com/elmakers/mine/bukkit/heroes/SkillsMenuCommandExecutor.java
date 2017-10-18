package com.elmakers.mine.bukkit.heroes;

import com.elmakers.mine.bukkit.utility.CompatibilityUtils;
import com.elmakers.mine.bukkit.utility.InventoryUtils;
import com.herocraftonline.heroes.characters.Hero;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        List<SkillDescription> allSkills = new ArrayList<>();
        String classString = controller.getClassName(player);
        String class2String = controller.getSecondaryClassName(player);
        String messageKey = !class2String.isEmpty() ? "skills.inventory_title_secondary" : "skills.inventory_title";
        String inventoryTitle = controller.getMessage(messageKey, "Skills ($page/$pages)");
        inventoryTitle = inventoryTitle
                .replace("$class2", class2String)
                .replace("$class", classString);

        List<String> heroesSkills = controller.getSkillList(player, true, true);
        for (String heroesSkill : heroesSkills) {
            allSkills.add(new SkillDescription(controller, player, heroesSkill));
        }

        if (allSkills.size() == 0) {
            player.sendMessage(controller.getMessage("skills.none", "You have no skills"));
            return;
        }

        Collections.sort(allSkills);

        int inventorySize = 9 * controller.getSkillInventoryRows();
        int numPages = (int)Math.ceil((float)allSkills.size() / inventorySize);
        if (page < 1) page = numPages;
        else if (page > numPages) page = 1;

        int pageIndex = page - 1;
        int startIndex = pageIndex * inventorySize;
        int maxIndex = (pageIndex + 1) * inventorySize - 1;

        List<SkillDescription> skills = new ArrayList<>();
        for (int i = startIndex; i <= maxIndex && i < allSkills.size(); i++) {
            skills.add(allSkills.get(i));
        }
        if (skills.size() == 0)
        {
            String messageTemplate = controller.getMessage("skills.none_on_page", "No skills on page $page");
            player.sendMessage(messageTemplate.replace("$page", Integer.toString(page)));
            return;
        }

        int invSize = (int)Math.ceil(skills.size() / 9.0f) * 9;
        String title = inventoryTitle;
        title = title
                .replace("$pages", Integer.toString(numPages))
                .replace("$page", Integer.toString(page));
        Inventory displayInventory = CompatibilityUtils.createInventory(null, invSize, title);
        for (SkillDescription skill : skills)
        {
            ItemStack skillItem = controller.createSkillItem(skill, player);
            if (skillItem == null) continue;
            displayInventory.addItem(skillItem);
        }

        player.closeInventory();
        player.openInventory(displayInventory);
    }

}
