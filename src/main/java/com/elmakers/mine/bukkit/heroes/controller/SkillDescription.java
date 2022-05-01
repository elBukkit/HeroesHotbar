package com.elmakers.mine.bukkit.heroes.controller;

import com.elmakers.mine.bukkit.heroes.utilities.CompatibilityUtils;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.profile.PlayerProfile;

public class SkillDescription implements Comparable<SkillDescription> {
    private final String skillKey;
    private final String name;
    private final String description;
    private final Skill skill;
    private final int skillLevel;
    private final ItemStack icon;
    private final PlayerProfile iconProfile;
    private final PlayerProfile disabledProfile;

    public SkillDescription(HotbarController controller, Player player, String skillKey) {
        this.skill = controller.getSkill(skillKey);
        this.skillKey = skillKey;
        this.skillLevel = controller.getSkillLevel(player, skillKey);

        String skillDisplayName = skill == null ? null : SkillConfigManager.getRaw(skill, "name", skill.getName());
        this.name = skillDisplayName == null || skillDisplayName.isEmpty() ? skillKey : skillDisplayName;
        this.description = skill == null ? null : SkillConfigManager.getRaw(skill, "description", "");

        String iconURL = skill == null ? null : SkillConfigManager.getRaw(skill, "icon-url", SkillConfigManager.getRaw(skill, "icon_url", null));

        if(iconURL == null || iconURL.isEmpty()) {
            this.iconProfile = controller.getUnknownIcon();
        }
        else {
            this.iconProfile = CompatibilityUtils.getPlayerProfile(skillKey, iconURL);
        }

        String iconDisabledURL = skill == null ? null : SkillConfigManager.getRaw(skill, "icon-disabled-url", SkillConfigManager.getRaw(skill, "icon_disabled_url", null));

        if (iconDisabledURL == null || iconDisabledURL.isEmpty()) {
            this.disabledProfile = controller.getDefaultDisabledIcon();
        }
        else {
            this.disabledProfile = CompatibilityUtils.getPlayerProfile(skillKey, iconDisabledURL);
        }

        this.icon = new ItemStack(Material.PLAYER_HEAD, 1);
        controller.updateSkillItem(this, player);
    }

    public boolean isHeroes() {
        return skillKey != null;
    }

    @Override
    public int compareTo(SkillDescription other) {
        if (skillLevel != other.skillLevel) {
            return Integer.compare(skillLevel, other.skillLevel);
        }
        return getName().compareTo(other.getName());
    }

    /**
     * Gets icon associated with this skill description
     * @return A disabled icon if skill cannot be used or proper icon if it can be used
     */
    public ItemStack getIcon() {
        return icon;
    }

    public void setProfileState(ItemStack icon, boolean enabled) {
        if(icon.getType() == Material.PLAYER_HEAD) {
            CompatibilityUtils.setSkullProfile(icon, enabled ? iconProfile : disabledProfile);
        }
    }

    public ItemStack updateIcon(HotbarController controller, Player player) {
        //fixme: Technically speaking, the code to generate the skull item SHOULD be here not in controller. But anyway
        controller.updateSkillItem(icon, this, player);
        return icon;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return skillKey;
    }

    public Skill getSkill() {
        return skill;
    }

    public String getDescription() {
        return description;
    }

    public boolean isValid() {
        return skill != null;
    }
};