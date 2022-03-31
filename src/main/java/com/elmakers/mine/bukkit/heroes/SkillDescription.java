package com.elmakers.mine.bukkit.heroes;

import com.elmakers.mine.bukkit.heroes.utilities.CompatibilityUtils;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.OptionalInt;

public class SkillDescription implements Comparable<SkillDescription> {
    private final String skillKey;
    private final String name;
    private final String description;
    private final Skill skill;
    private final int skillLevel;
    private final String iconURL;
    private final String disabledIconURL;
    private final ItemStack icon;

    public SkillDescription(HotbarController controller, Player player, String skillKey) {
        this.skill = controller.getSkill(skillKey);
        this.skillKey = skillKey;
        this.skillLevel = controller.getSkillLevel(player, skillKey);

        this.iconURL = skill == null ? null : SkillConfigManager.getRaw(skill, "icon-url", SkillConfigManager.getRaw(skill, "icon_url", null));


        String iconDisabledURL = skill == null ? null : SkillConfigManager.getRaw(skill, "icon-disabled-url", SkillConfigManager.getRaw(skill, "icon_disabled_url", null));

        if (iconDisabledURL == null) {
            iconDisabledURL = controller.getDefaultDisabledIconURL();
        }

        this.disabledIconURL = iconDisabledURL;

        String skillDisplayName = skill == null ? null : SkillConfigManager.getRaw(skill, "name", skill.getName());
        this.name = skillDisplayName == null || skillDisplayName.isEmpty() ? skillKey : skillDisplayName;
        this.description = skill == null ? null : SkillConfigManager.getRaw(skill, "description", "");

        this.icon = controller.getSkillItem(this, player);
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

    public ItemStack updateIcon(HotbarController controller, Player player) {
        //fixme: Technically speaking, the code to generate the skull item SHOULD be here not in controller. But anyway
        controller.updateSkillItem(icon, this, player);
        return icon;
    }

    public String getIconURL() {
        return iconURL;
    }

    public String getDisabledIconURL() {
        return disabledIconURL;
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