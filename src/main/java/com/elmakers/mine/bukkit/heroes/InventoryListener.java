package com.elmakers.mine.bukkit.heroes;

import com.elmakers.mine.bukkit.heroes.utilities.CompatibilityUtils;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class InventoryListener implements Listener {
    private final HotbarController controller;

    public InventoryListener(HotbarController controller) {
        this.controller = controller;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        HumanEntity player = event.getWhoClicked();

        if (CompatibilityUtils.getMetaBoolean(clickedItem, "unavailable", false)) {
            event.setCancelled(true);
            player.sendMessage(controller.getMessage("skills.unlearned").replace("$skill", controller.getSkillKey(clickedItem)));
            return;
        }
        if (CompatibilityUtils.getMetaBoolean(clickedItem, "passive", false)) {
            event.setCancelled(true);
            return;
        }

        boolean isSkill = clickedItem != null && controller.isSkill(clickedItem);
        boolean isDrop = event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP;
        InventoryAction action = event.getAction();
        SkillSelector skillSelector = controller.getActiveSkillSelector(event.getWhoClicked());

        // Check for right-click-to-prepare
        boolean isRightClick = action == InventoryAction.PICKUP_HALF;
        if (isSkill && isRightClick) {
            if (player instanceof Player) {
                controller.unprepareSkill((Player) player, clickedItem);
            }
            event.setCancelled(true);
            return;
        }

        // Drop key unprepares
        if (isSkill && isDrop) {
            if (player instanceof Player) {
                controller.unprepareSkill((Player) player, clickedItem);
            }

            // Only cancel event if in the skill selector
            if (skillSelector != null) {
                event.setCancelled(true);
            }
            return;
        }

        // Check for wearing skills, do not allow
        ItemStack heldItem = event.getCursor();
        boolean heldSkill = controller.isSkill(heldItem);
        if (heldSkill && event.getSlotType() == InventoryType.SlotType.ARMOR) {
            event.setCancelled(true);
            return;
        }

        boolean isHotbar = event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD;
        if (isHotbar && event.getSlotType() == InventoryType.SlotType.ARMOR) {
            int slot = event.getHotbarButton();
            ItemStack item = player.getInventory().getItem(slot);
            if (item != null && controller.isSkill(item)) {
                event.setCancelled(true);
                return;
            }
        }

        // Clicking a skill prepares it

        boolean isMove = event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY;
        if (event.getAction() == InventoryAction.PICKUP_ALL || isHotbar || isMove) {
            if (player instanceof Player) {
                if (!controller.prepareSkill((Player) player, clickedItem)) {
                    event.setCancelled(true);
                } else if (isMove) {
                    // This is needed for the item name and lore to update when shift+clicking
                    controller.delayedInventoryUpdate((Player)player);
                }

                // Just prepare but don't grab, if the skill inventory is open and we already have this skill
                if (!event.isCancelled() && skillSelector != null && controller.hasSkillItem((Player)player, controller.getSkillKey(clickedItem))) {
                    event.setCancelled(true);
                }
            }

            return;
        }

        // Delegate to skill selector
        if (skillSelector != null) {
            skillSelector.onClick(event);
            return;
        }

        // Preventing putting skills in containers
        InventoryType inventoryType = event.getInventory().getType();
        boolean isPlayerInventory = inventoryType == InventoryType.CRAFTING || inventoryType == InventoryType.PLAYER;
        isSkill = isSkill || controller.isLegacySkill(clickedItem);
        if (isSkill && !isPlayerInventory) {
            if (!isDrop) {
                event.setCancelled(true);
            }
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        controller.clearActiveSkillSelector(event.getPlayer());
    }
}
