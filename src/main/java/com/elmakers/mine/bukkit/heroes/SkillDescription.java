package com.elmakers.mine.bukkit.heroes;

import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import org.bukkit.entity.Player;

public class SkillDescription implements Comparable<SkillDescription> {
    private final String skillKey;
    private final String name;
    private final String description;
    private final Skill skill;
    private final int skillLevel;
    private final MaterialAndData icon;
    private final MaterialAndData disabledIcon;
    private final String iconURL;
    private final String disabledIconURL;

    public SkillDescription(HotbarController controller, Player player, String skillKey) {
        this.skill = controller.getSkill(skillKey);
        this.skillKey = skillKey;
        this.skillLevel = controller.getSkillLevel(player, skillKey);

        String icon = skill == null ? null : SkillConfigManager.getRaw(skill, "icon", null);
        String iconURL = skill == null ? null : SkillConfigManager.getRaw(skill, "icon-url", SkillConfigManager.getRaw(skill, "icon_url", null));
        if (icon != null && icon.startsWith("http://")) {
            iconURL = icon;
            icon = null;
            //controller.getLogger().warning("Skull icons are no longer supported");
        }
        this.iconURL = iconURL;
        this.icon = icon == null || icon.isEmpty() ? null : new MaterialAndData(icon);

        String iconDisabledURL = skill == null ? null : SkillConfigManager.getRaw(skill, "icon-disabled-url", SkillConfigManager.getRaw(skill, "icon_disabled_url", null));
        String iconDisabled = skill == null ? null : SkillConfigManager.getRaw(skill, "icon-disabled", SkillConfigManager.getRaw(skill, "icon_disabled", null));
        if (iconDisabled != null && iconDisabled.startsWith("http://")) {
            iconDisabled = null;
            iconDisabledURL = icon;
        }

        this.disabledIcon = iconDisabled == null || iconDisabled.isEmpty() ? null : new MaterialAndData(iconDisabled);

        if (iconDisabledURL == null) {
            iconDisabledURL = controller.getDefaultDisabledIconURL();
        }

        this.disabledIconURL = iconDisabledURL;

        String skillDisplayName = skill == null ? null : SkillConfigManager.getRaw(skill, "name", skill.getName());
        this.name = skillDisplayName == null || skillDisplayName.isEmpty() ? skillKey : skillDisplayName;
        this.description = skill == null ? null : SkillConfigManager.getRaw(skill, "description", "");
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

    public MaterialAndData getIcon() {
        return icon;
    }

    public String getIconURL() {
        return iconURL;
    }

    public String getDisabledIconURL() {
        return disabledIconURL;
    }

    public MaterialAndData getDisabledIcon() {
        return disabledIcon;
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