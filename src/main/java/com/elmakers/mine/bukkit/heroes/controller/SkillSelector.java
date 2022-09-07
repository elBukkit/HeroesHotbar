package com.elmakers.mine.bukkit.heroes.controller;

import java.util.*;
import java.util.logging.Level;

import com.elmakers.mine.bukkit.heroes.controller.HotbarController;
import com.elmakers.mine.bukkit.heroes.controller.SkillDescription;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import com.elmakers.mine.bukkit.heroes.utilities.CompatibilityUtils;

import javax.annotation.Nullable;

public class SkillSelector {
    private final HotbarController controller;
    private final Player player;

    private int page;
    private Map<String, SkillDescription> allSkills;
    private String inventoryTitle;
    private boolean guiOpen;

    public SkillSelector(HotbarController controller, Player player) {
        this.controller = controller;
        this.player = player;
        guiOpen = false;

        updateSkills();
    }

    public void updateSkills() {
        this.allSkills = new LinkedHashMap<>();
        getAllSkills().forEach(skill -> allSkills.put(skill.getKey(), skill));
    }

    public void updateSkillsForLevelUp() { //Need to update lore, and availability as well, updateSkills is used initially
        this.allSkills = new LinkedHashMap<>();
        getAllSkills().forEach(skill -> {
            controller.updateSkillItem(skill.getIcon(), skill, player);
            List<String> lore = new ArrayList<>();
            controller.addSkillLore(skill, lore, player);
            allSkills.put(skill.getKey(), skill);
        });
    }

    //Resets all skills and recalculates them. This should only ever be called on a class change.
    public void refreshAllSkills() {
        this.allSkills = new LinkedHashMap<>();
        getAllSkills().forEach(skill -> allSkills.put(skill.getKey(), skill));
    }

    public List<SkillDescription> getAllSkills() {
        String classString = controller.getClassName(player);
        String class2String = controller.getSecondaryClassName(player);
        String messageKey = !class2String.isEmpty() ? "skills.inventory_title_secondary" : "skills.inventory_title";
        inventoryTitle = controller.getMessage(messageKey, "Skills ($page/$pages)");
        inventoryTitle = inventoryTitle
                .replace("$class2", class2String)
                .replace("$class", classString);

        List<String> heroesSkills = controller.getSkillList(player, true, true);
        List<SkillDescription> descriptions = new LinkedList<>();
        for (String heroesSkill : heroesSkills) {
            descriptions.add(new SkillDescription(controller, player, heroesSkill));
        }

        if (descriptions.size() == 0) {
            player.sendMessage(controller.getMessage("skills.none", "You have no skills"));
        }

        try {
            Collections.sort(descriptions);
        }
        catch(ClassCastException e) {
            controller.getLogger().log(Level.WARNING, "Could not sort skill descriptions!");
        }
        return descriptions;
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
        Iterator<SkillDescription> iterator = allSkills.values().iterator();
        int i = startIndex;
        while(iterator.hasNext() && i <= maxIndex && i < allSkills.size()) {
            skills.add(iterator.next());
            i++;
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
            displayInventory.addItem(skill.updateIcon(controller, player));
        }

        player.closeInventory();
        player.openInventory(displayInventory);
        guiOpen = true;
    }

    public void setGuiState(boolean open) {
        this.guiOpen = open;
    }

    public boolean isGuiOpen() {
        return guiOpen;
    }

    @Nullable
    public SkillDescription getSkill(String skillName) {
        return this.allSkills.get(skillName);
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
