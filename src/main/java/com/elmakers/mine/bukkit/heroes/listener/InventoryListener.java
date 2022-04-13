package com.elmakers.mine.bukkit.heroes.listener;

import com.elmakers.mine.bukkit.heroes.controller.HotbarController;
import com.elmakers.mine.bukkit.heroes.utilities.CompatibilityUtils;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
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
        boolean isGuiOpen = controller.isGuiOpen((Player) player);

        // Check for right-click-to-prepare
        boolean isRightClick = event.getClick() == ClickType.RIGHT;
        if (isSkill && isRightClick) {
            controller.prepareSkill((Player) player, clickedItem);
            event.setCancelled(true);
            return;
        }

        // Drop key unprepares
        if (isSkill && isDrop) {
            controller.unprepareSkill((Player) player, clickedItem);
            player.getInventory().setItem(event.getSlot(), null);
            // Only cancel event if in the skill selector
            if (isGuiOpen) {
                event.setCancelled(true);
                return;
            }
        }

        // Check for wearing skills, do not allow
        ItemStack heldItem = event.getCursor();
        boolean heldSkill = controller.isSkill(heldItem);
        boolean isMove = event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY;
        if (heldSkill && event.getSlotType() == InventoryType.SlotType.ARMOR) {
            event.setCancelled(true);
            return;
        }
        //Shift clicking into inventory
        if(isSkill && isMove && (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT)) {
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


        if (event.getAction() == InventoryAction.PICKUP_ALL || isHotbar || isMove) {
            if (!controller.prepareSkill((Player) player, clickedItem)) {
                event.setCancelled(true);
            } else if (isMove) {
                // This is needed for the item name and lore to update when shift+clicking
                controller.delayedInventoryUpdate((Player)player);
            }

            // Just prepare but don't grab, if the skill inventory is open and we already have this skill
            if (!event.isCancelled() && isGuiOpen && controller.hasSkillItem((Player)player, controller.getSkillKey(clickedItem))) {
                event.setCancelled(true);
                return;
            }
        }

        // Delegate to skill selector
        if (isGuiOpen) {
            controller.getActiveSkillSelector(player).onClick(event);
            return;
        }

        // Preventing putting skills in containers
        if(event.getClickedInventory() == null) {
            return;
        }
        InventoryType inventoryType = event.getClickedInventory().getType();
        boolean isPlayerInventory = inventoryType == InventoryType.PLAYER;
        heldSkill = heldSkill || controller.isLegacySkill(clickedItem);
        if (heldSkill && !isPlayerInventory) {
            ClickType click = event.getClick();
            if (click == ClickType.LEFT || click == ClickType.RIGHT) {
                event.setCancelled(true);
            }
        }
    }
}
