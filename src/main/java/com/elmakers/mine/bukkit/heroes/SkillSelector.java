package com.elmakers.mine.bukkit.heroes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.elmakers.mine.bukkit.heroes.utilities.CompatibilityUtils;

public class SkillSelector {
    private final HotbarController controller;
    private final Player player;

    private int page;
    private List<SkillDescription> allSkills = new ArrayList<>();
    private String inventoryTitle;

    public SkillSelector(HotbarController controller, Player player) {
        this.controller = controller;
        this.player = player;

        String classString = controller.getClassName(player);
        String class2String = controller.getSecondaryClassName(player);
        String messageKey = !class2String.isEmpty() ? "skills.inventory_title_secondary" : "skills.inventory_title";
        inventoryTitle = controller.getMessage(messageKey, "Skills ($page/$pages)");
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
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void openInventory() {
        int inventorySize = 9 * controller.getSkillInventoryRows();
        int numPages = (int) Math.ceil((float) allSkills.size() / inventorySize);
        if (page < 1) page = numPages;
        else if (page > numPages) page = 1;

        int pageIndex = page - 1;
        int startIndex = pageIndex * inventorySize;
        int maxIndex = (pageIndex + 1) * inventorySize - 1;

        List<SkillDescription> skills = new ArrayList<>();
        for (int i = startIndex; i <= maxIndex && i < allSkills.size(); i++) {
            skills.add(allSkills.get(i));
        }
        if (skills.size() == 0) {
            String messageTemplate = controller.getMessage("skills.none_on_page", "No skills on page $page");
            player.sendMessage(messageTemplate.replace("$page", Integer.toString(page)));
            return;
        }

        int invSize = (int) Math.ceil(skills.size() / 9.0f) * 9;
        String title = inventoryTitle;
        title = title
                .replace("$pages", Integer.toString(numPages))
                .replace("$page", Integer.toString(page));
        Inventory displayInventory = CompatibilityUtils.createInventory(null, invSize, title);
        for (SkillDescription skill : skills) {
            ItemStack skillItem = controller.createSkillItem(skill, player);
            if (skillItem == null) continue;
            displayInventory.addItem(skillItem);
        }

        player.closeInventory();
        player.openInventory(displayInventory);
        controller.setActiveSkillSelector(player, this);
    }

    public void onClick(InventoryClickEvent event) {
        // Cycle inventory pages
        InventoryAction action = event.getAction();
        if (action == InventoryAction.NOTHING) {
            int direction = event.getClick() == ClickType.LEFT ? 1 : -1;
            page = page + direction;
            openInventory();
            event.setCancelled(true);
        }
    }
}
