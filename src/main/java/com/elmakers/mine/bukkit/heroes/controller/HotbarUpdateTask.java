package com.elmakers.mine.bukkit.heroes.controller;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
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
        for (Player player : controller.getServer().getOnlinePlayers()) {
            updateHotbar(player);
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

    public int getRequiredStamina(Player player, String skillKey) {
        if (player == null) return 0;
        Hero hero = controller.getHero(player);
        if (hero == null) return 0;
        Skill skill = controller.getSkill(skillKey);
        if (skill == null) return 0;
        return SkillConfigManager.getUseSetting(hero, skill, SkillSetting.STAMINA, 0, true);
    }

    private void updateHotbar(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack skillItem = inv.getItem(i);
            if(skillItem == null || skillItem.getType().isAir()) continue;
            String skillKey = controller.getSkillKey(skillItem);
            if (skillKey == null || skillKey.isEmpty()) continue;

            int targetAmount = 1;
            long remainingCooldown = getRemainingCooldown(player, skillKey);
            int requiredMana = getRequiredMana(player, skillKey);
            int requiredStamina = getRequiredStamina(player, skillKey);
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
                        int targetManaTime = (int)Math.min(Math.ceil(remainingMana / hero.getManaRegen()), 99); //fixme pretty sure this does jack shit
                        targetAmount = Math.max(targetManaTime, targetAmount);
                    } else {
                        targetAmount = 99;
                        canUse = false;
                    }
                }
                if(canUse && requiredStamina > 0) { //Only bother checking if has enough mana.
                    Hero hero = controller.getHero(player);
                    if (requiredStamina <= hero.getMaxStamina()) {
                        float remainingStamina = requiredStamina - hero.getStamina();
                        canUse = remainingStamina <= 0;
                        int targetStaminaTime = (int)Math.min(Math.ceil(remainingStamina / hero.getStaminaRegen()), 99); //fixme pretty sure this does jack shit
                        targetAmount = Math.max(targetStaminaTime, targetAmount);
                    } else {
                        targetAmount = 99;
                        canUse = false;
                    }
                }
            }

            if (targetAmount == 0) targetAmount = 1;
            boolean setAmount = false;

            SkillDescription skillDescription = controller.getSkillDescription(player, skillKey);
            if(skillDescription != null) {
                if (!canUse) {
                    if (targetAmount == 99) {
                        if (skillItem.getAmount() != 1) {
                            skillItem.setAmount(1);
                        }
                        setAmount = true;
                    }
                }

                if (!setAmount && skillItem.getAmount() != targetAmount) {
                    skillItem.setAmount(targetAmount);
                }
                //skillDescription.setProfileState(skillItem, canUse);
            }
        }
    }
}
