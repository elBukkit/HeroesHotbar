package com.elmakers.mine.bukkit.heroes;

import com.elmakers.mine.bukkit.utility.InventoryUtils;
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
        if (InventoryUtils.getMetaBoolean(clickedItem, "unavailable", false)) {
            controller.getLogger().info("UNVAILABLE");
            event.setCancelled(true);
            return;
        }

        boolean isSkill = clickedItem != null && controller.isSkill(clickedItem);
        HumanEntity player = event.getWhoClicked();
        InventoryAction action = event.getAction();

        // Check for right-click-to-use
        boolean isRightClick = action == InventoryAction.PICKUP_HALF;
        if (isSkill && isRightClick) {
            if (player instanceof Player) {
                controller.useSkill((Player) player, clickedItem);
            }
            player.closeInventory();
            event.setCancelled(true);

            controller.getLogger().info("QUICK");
            return;
        }

        // Check for wearing skills, do not allow
        ItemStack heldItem = event.getCursor();
        boolean heldSkill = controller.isSkill(heldItem);
        if (heldSkill && event.getSlotType() == InventoryType.SlotType.ARMOR) {
            event.setCancelled(true);
            controller.getLogger().info("ARMOR");
            return;
        }

        boolean isHotbar = event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD;
        if (isHotbar && event.getSlotType() == InventoryType.SlotType.ARMOR) {
            int slot = event.getHotbarButton();
            ItemStack item = player.getInventory().getItem(slot);
            if (item != null && controller.isSkill(item)) {
                event.setCancelled(true);
                controller.getLogger().info("ARMOR2");
                return;
            }
        }

        // Delegate to skill selector
        SkillSelector skillSelector = controller.getActiveSkillSelector(event.getWhoClicked());
        if (skillSelector != null) {
            skillSelector.onClick(event);
            return;
        }

        // Preventing putting skills in containers
        InventoryType inventoryType = event.getInventory().getType();
        boolean isPlayerInventory = inventoryType == InventoryType.CRAFTING || inventoryType == InventoryType.PLAYER;
        boolean isDrop = event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP;
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
