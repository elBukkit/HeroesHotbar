package com.elmakers.mine.bukkit.heroes;

import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;

public class HotbarUpdateTask implements Runnable {
    private final HotbarController controller;

    public HotbarUpdateTask(HotbarController controller) {
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            for (Player player : controller.getServer().getOnlinePlayers()) {
                controller.getLogger().info("Updating Hotbar");
                updateHotbar(player);
            }
        } catch (Exception ex) {
            controller.getLogger().log(Level.WARNING, "Error updating hotbar", ex);
        }
    }

    private long getRemainingCooldown(Player player, String skillKey) {
        if (player == null) return 0;
        Hero hero = controller.getHero(player);
        if (hero == null) return 0;
        Long cooldown = hero.getCooldown(skillKey);
        if (cooldown == null) return 0;
        long now = System.currentTimeMillis();
        return Math.max(0, cooldown - now);
    }

    public int getRequiredMana(Player player, String skillKey) {
        if (player == null) return 0;
        Hero hero = controller.getHero(player);
        if (hero == null) return 0;
        Skill skill = controller.getSkill(skillKey);
        if (skill == null) return 0;
        return SkillConfigManager.getUseSetting(hero, skill, SkillSetting.MANA, 0, true);
    }

    private void updateHotbar(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack skillItem = player.getInventory().getItem(i);
            String skillKey = controller.getSkillKey(skillItem);
            if (skillKey == null || skillKey.isEmpty()) continue;

            int targetAmount = 1;
            long remainingCooldown = getRemainingCooldown(player, skillKey);
            int requiredMana = getRequiredMana(player, skillKey);
            boolean canUse = controller.canUseSkill(player, skillKey);

            if (canUse && remainingCooldown == 0 && requiredMana == 0) {
                targetAmount = 1;
            } else if (!canUse) {
                targetAmount = 99;
            } else {
                canUse = remainingCooldown == 0;
                targetAmount = (int)Math.min(Math.ceil((double)remainingCooldown / 1000), 99);
                if (requiredMana > 0) {
                    Hero hero = controller.getHero(player);
                    if (requiredMana <= hero.getMaxMana()) {
                        float remainingMana = requiredMana - hero.getMana();
                        canUse = canUse && remainingMana <= 0;
                        int targetManaTime = (int)Math.min(Math.ceil(remainingMana / hero.getManaRegen()), 99);
                        targetAmount = Math.max(targetManaTime, targetAmount);
                    } else {
                        targetAmount = 99;
                        canUse = false;
                    }
                }
            }

            if (targetAmount == 0) targetAmount = 1;
            boolean setAmount = false;

            SkillDescription skillDescription = new SkillDescription(controller, player, skillKey);

            MaterialAndData disabledIcon = skillDescription.getDisabledIcon();
            MaterialAndData spellIcon = skillDescription.getIcon();
            if (disabledIcon != null && spellIcon != null) {
                if (!canUse) {
                    if (disabledIcon.getMaterial() != skillItem.getType() || disabledIcon.getData() != skillItem.getDurability()) {
                        disabledIcon.applyToItem(skillItem);
                    }
                    if (targetAmount == 99) {
                        if (skillItem.getAmount() != 1) {
                            skillItem.setAmount(1);
                        }
                        setAmount = true;
                    }
                } else {
                    if (spellIcon.getMaterial() != skillItem.getType() || spellIcon.getData() != skillItem.getDurability()) {
                        spellIcon.applyToItem(skillItem);
                    }
                }
            }

            if (!setAmount && skillItem.getAmount() != targetAmount) {
                skillItem.setAmount(targetAmount);
            }
        }
    }
}
