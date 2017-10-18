package com.elmakers.mine.bukkit.heroes;

import com.elmakers.mine.bukkit.utility.CompatibilityUtils;
import com.elmakers.mine.bukkit.utility.InventoryUtils;
import com.elmakers.mine.bukkit.utility.NMSUtils;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.CharacterManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class manages the centralized hotbar functionality.
 */
public class HotbarController {
    private final static List<String> emptySkillList = new ArrayList<>();
    private static final int MAX_LORE_LENGTH = 24;
    private static final DecimalFormat SECONDS_FORMATTER = new DecimalFormat("0.##");

    private final Plugin plugin;
    private final CharacterManager characters;
    private final SkillManager skills;

    private String skillKey;
    private int skillInventoryRows;
    private MaterialAndData defaultSkillIcon;

    public HotbarController(Plugin owningPlugin, Heroes heroesPlugin) {
        this.plugin = owningPlugin;
        characters = heroesPlugin.getCharacterManager();
        skills = heroesPlugin.getSkillManager();
    }

    public void initialize() {
        Configuration config = plugin.getConfig();
        skillKey = config.getString("nbt_key");
        if (skillKey == null || skillKey.isEmpty()) {
            skillKey = "heroesskill";
        }

        skillInventoryRows = config.getInt("skill_inventory_max_rows", 6);
        try {
            defaultSkillIcon = new MaterialAndData(config.getString("default_skill_icon", "stick"));
        } catch (Exception ex) {
            plugin.getLogger().warning("Invalid icon in config: " + config.getString("default_skill_icon"));
            defaultSkillIcon = new MaterialAndData(Material.STICK);
        }
    }

    public void clear() {

    }

    public String getMessage(String key) {
        return plugin.getConfig().getString(key, "");
    }

    public String getMessage(String key, String defaultValue) {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(key, defaultValue));
    }

    public ItemStack createSkillItem(SkillDescription skill, Player player) {
        ItemStack item = null;
        MaterialAndData icon = skill.getIcon();
        String iconURL = skill.getIconURL();
        String nameTemplate;
        if (icon == null) {
            icon = defaultSkillIcon;
        }

        boolean unavailable = !canUseSkill(player, skill.getName());
        if (unavailable)
        {
            nameTemplate = getMessage("skills.item_name_unavailable", "$skill");

            MaterialAndData disabledIcon = skill.getDisabledIcon();
            if (disabledIcon != null) {
                icon = disabledIcon;
            }
            String disabledIconURL = skill.getDisabledIconURL();
            if (disabledIconURL != null) {
                iconURL = disabledIconURL;
            }
        } else {
            nameTemplate = getMessage("skills.item_name", "$skill");
        }

        if (iconURL != null && !iconURL.isEmpty()) {
            item = InventoryUtils.getURLSkull(iconURL);
        } else {
            item = icon.createItemStack();
        }
        item = NMSUtils.makeReal(item);
        if (item == null) {
            plugin.getLogger().warning("Unable to create item stack for skill: " + skill.getName());
            return null;
        }

        // Set flags and NBT data
        NMSUtils.setMeta(item, skillKey, skill.getName());
        NMSUtils.makeUnbreakable(item);
        InventoryUtils.hideFlags(item, (byte)63);

        if (unavailable) {
            InventoryUtils.setMetaBoolean(item, "unavailable", true);
        }

        // Set display name
        CompatibilityUtils.setDisplayName(item, nameTemplate.replace("$skill", skill.getName()));

        // Set lore
        List<String> lore = new ArrayList<>();
        addSkillLore(skill, lore, player);
        CompatibilityUtils.setLore(item, lore);

        return item;
    }

    protected Skill getSkill(String key) {
        if (skills == null) return null;
        return skills.getSkill(key);
    }

    private String getTimeDescription(int time) {
        if (time > 0) {
            int timeInSeconds = time / 1000;
            if (timeInSeconds > 60 * 60 ) {
                int hours = timeInSeconds / (60 * 60);
                if (hours == 1) {
                    return getMessage("cooldown.description_hour");
                }
                return getMessage("cooldown.description_hours").replace("$hours", Integer.toString(hours));
            } else if (timeInSeconds > 60) {
                int minutes = timeInSeconds / 60;
                if (minutes == 1) {
                    return getMessage("cooldown.description_minute");
                }
                return getMessage("cooldown.description_minutes").replace("$minutes", Integer.toString(minutes));
            } else if (timeInSeconds > 1) {
                return getMessage("cooldown.description_seconds").replace("$seconds", Integer.toString(timeInSeconds));
            } else if (timeInSeconds == 1) {
                return getMessage("cooldown.description_second");
            } else {
                String timeDescription = getMessage("cooldown.description_moment");
                if (timeDescription.contains("$seconds")) {
                    timeDescription = timeDescription.replace("$seconds", SECONDS_FORMATTER.format(time / 1000.0D));
                }
                return timeDescription;
            }
        }
        return null;
    }

    public void addSkillLore(SkillDescription skillDescription, List<String> lore, Player player) {
        Hero hero = getHero(player);
        if (hero == null) return;
        Skill skill = skillDescription.getSkill();

        if (skill instanceof PassiveSkill)
        {
            lore.add(getMessage("skills.passive_description", "Passive"));
        }

        int level = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.LEVEL, 1, true);

        String levelDescription = getMessage("skills.level_description", "").replace("$level", Integer.toString(level));
        if (levelDescription != null && levelDescription.length() > 0) {
            lore.add(ChatColor.GOLD + levelDescription);
        }
        String description = skill.getDescription(hero);
        if (description != null && description.length() > 0) {
            description = getMessage("skills.description", "$description").replace("$description", description);
            InventoryUtils.wrapText(description, MAX_LORE_LENGTH, lore);
        }

        description = skillDescription.getDescription();
        if (description != null && description.length() > 0) {
            description = getMessage("skills.description_extra", "$description").replace("$description", description);
            InventoryUtils.wrapText(description, MAX_LORE_LENGTH, lore);
        }

        int cooldown = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.COOLDOWN, 0, true);
        if (cooldown > 0)
        {
            String cooldownDescription = getTimeDescription(cooldown);
            if (cooldownDescription != null && !cooldownDescription.isEmpty()) {
                lore.add(getMessage("cooldown.description", "$time").replace("$time", cooldownDescription));
            }
        }

        int mana = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.MANA, 0, true);
        if (mana > 0)
        {
            String manaDescription = getMessage("costs.heroes_mana").replace("$amount", Integer.toString(mana));
            lore.add(ChatColor.YELLOW + getMessage("skills.costs_description").replace("$description", manaDescription));
        }
    }

    public int getSkillLevel(Player player, String skillName) {
        Skill skill = skills.getSkill(skillName);
        if (skill == null) return 0;
        Hero hero = getHero(player);
        if (hero == null) return 0;
        return SkillConfigManager.getUseSetting(hero, skill, SkillSetting.LEVEL, 1, true);
    }

    protected Hero getHero(Player player) {
        if (characters == null) return null;
        return characters.getHero(player);
    }

    public String getClassName(Player player) {
        Hero hero = getHero(player);
        if (hero == null) return "";
        HeroClass heroClass = hero.getHeroClass();
        if (heroClass == null) return "";
        return heroClass.getName();
    }

    public String getSecondaryClassName(Player player) {
        Hero hero = getHero(player);
        if (hero == null) return "";
        HeroClass heroClass = hero.getSecondClass();
        if (heroClass == null) return "";
        return heroClass.getName();
    }

    private Multimap<Integer, Skill> mapSkillsByLevel(Hero hero, Collection<String> skillNames) {

        Multimap<Integer, Skill> skillMap = TreeMultimap.create(Ordering.natural(), new Comparator<Skill>() {
            @Override
            public int compare(Skill skill1, Skill skill2) {
                return skill1.getName().compareTo(skill2.getName());
            }
        });
        for (String skillName : skillNames)
        {
            Skill skill = skills.getSkill(skillName);
            if (skill == null) continue;
            int level = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.LEVEL, 1, true);
            skillMap.put(level, skill);
        }
        return skillMap;
    }

    private void addSkills(Hero hero, HeroClass heroClass, Collection<String> skillSet, boolean showUnuseable, boolean showPassive)
    {
        if (heroClass != null)
        {
            Set<String> classSkills = heroClass.getSkillNames();
            for (String classSkill : classSkills)
            {
                Skill skill = skills.getSkill(classSkill);
                if (!showUnuseable && !hero.canUseSkill(skill)) continue;
                if (!showPassive && !(skill instanceof ActiveSkill)) continue;
                // getRaw's boolean default value is ignored! :(
                if (SkillConfigManager.getRaw(skill, "wand", "true").equalsIgnoreCase("true"))
                {
                    skillSet.add(classSkill);
                }
            }
        }
    }

    public List<String> getSkillList(Player player, boolean showUnuseable, boolean showPassive)
    {
        if (skills == null) return emptySkillList;
        Hero hero = getHero(player);
        if (hero == null) return emptySkillList;

        HeroClass heroClass = hero.getHeroClass();
        HeroClass secondClass = hero.getSecondClass();
        Set<String> primarySkills = new HashSet<>();
        Set<String> secondarySkills = new HashSet<>();
        addSkills(hero, heroClass, primarySkills, showUnuseable, showPassive);
        addSkills(hero, secondClass, secondarySkills, showUnuseable, showPassive);
        secondarySkills.removeAll(primarySkills);

        Multimap<Integer, Skill> primaryMap = mapSkillsByLevel(hero, primarySkills);
        Multimap<Integer, Skill> secondaryMap = mapSkillsByLevel(hero, secondarySkills);
        List<String> skillNames = new ArrayList<>();
        for (Skill skill : primaryMap.values())
        {
            skillNames.add(skill.getName());
        }
        for (Skill skill : secondaryMap.values())
        {
            skillNames.add(skill.getName());
        }
        return skillNames;
    }

    public int getSkillInventoryRows() {
        return skillInventoryRows;
    }

    public boolean canUseSkill(Player player, String skillName) {
        Hero hero = getHero(player);
        if (hero == null) return false;
        return hero.canUseSkill(skillName);
    }
}
