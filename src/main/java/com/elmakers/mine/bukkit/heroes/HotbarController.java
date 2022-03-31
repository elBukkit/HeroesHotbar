package com.elmakers.mine.bukkit.heroes;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import com.elmakers.mine.bukkit.heroes.utilities.CompatibilityUtils;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.CharacterManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.OutsourcedSkill;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;

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

    private String skillNBTKey;
    private String legacyNBTKey;
    private int skillInventoryRows;
    private MaterialAndData defaultSkillIcon;
    private String defaultDisabledIconURL;

    private Map<UUID, SkillSelector> selectors = new HashMap<>();

    public HotbarController(Plugin owningPlugin, Heroes heroesPlugin) {
        this.plugin = owningPlugin;
        characters = heroesPlugin.getCharacterManager();
        skills = heroesPlugin.getSkillManager();
    }

    public void initialize() {
        Configuration config = plugin.getConfig();
        skillNBTKey = config.getString("nbt_key");
        if (skillNBTKey == null || skillNBTKey.isEmpty()) {
            skillNBTKey = "heroesskill";
        }
        legacyNBTKey = config.getString("legacy_nbt_key");
        defaultDisabledIconURL = config.getString("disabled_icon_url");

        skillInventoryRows = config.getInt("skill_inventory_max_rows", 6);
        try {
            defaultSkillIcon = new MaterialAndData(config.getString("default_skill_icon", "stick"));
        } catch (Exception ex) {
            plugin.getLogger().warning("Invalid icon in config: " + config.getString("default_skill_icon"));
            defaultSkillIcon = new MaterialAndData(Material.STICK);
        }

        int hotbarUpdateInterval = config.getInt("update_interval");
        if (hotbarUpdateInterval > 0) {
            final HotbarUpdateTask updateTask = new HotbarUpdateTask(this);
            plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, updateTask, 0, hotbarUpdateInterval);
        }
    }

    public void clear() {

    }

    public String getMessage(String key) {
        return getMessage(key, "");
    }

    public String getMessage(String key, String defaultValue) {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(key, defaultValue));
    }

    public String getSkillTitle(Player player, String skillName) {
        String nameTemplate;

        boolean unavailable = !canUseSkill(player, skillName);
        boolean unprepared = !isPrepared(player, skillName);
        if (unavailable) {
            nameTemplate = getMessage("skills.item_name_unavailable", "$skill");
        } else if (unprepared) {
            nameTemplate = getMessage("skills.item_name_unprepared", "$skill");
        } else {
            nameTemplate = getMessage("skills.item_name", "$skill");
        }

        return nameTemplate.replace("$skill", skillName);
    }

    public ItemStack createSkillItem(SkillDescription skill, Player player) {
        ItemStack item = null;
        MaterialAndData icon = skill.getIcon();
        String iconURL = skill.getIconURL();
        if (icon == null) {
            icon = defaultSkillIcon;
        }

        boolean unavailable = !canUseSkill(player, skill.getKey());
        if (unavailable) {
            MaterialAndData disabledIcon = skill.getDisabledIcon();
            if (disabledIcon != null) {
                icon = disabledIcon;
            }
            String disabledIconURL = skill.getDisabledIconURL();
            if (disabledIconURL != null && !disabledIconURL.isEmpty()) {
                iconURL = disabledIconURL;
            }
        }

        if (iconURL != null && !iconURL.isEmpty()) {
            item = CompatibilityUtils.getSkullIcon(skill.getKey(), iconURL);
        } else {
            item = icon.createItemStack();
        }
        if (item == null) {
            plugin.getLogger().warning("Unable to create item stack for skill: " + skill.getName());
            return null;
        }

        // Set flags and NBT data
        CompatibilityUtils.setMeta(item, skillNBTKey, skill.getKey());
        CompatibilityUtils.makeUnbreakable(item);
        CompatibilityUtils.hideFlags(item);

        boolean passive = skill.getSkill() instanceof PassiveSkill || skill.getSkill() instanceof OutsourcedSkill;
        if (unavailable) {
            CompatibilityUtils.setMetaBoolean(item, "unavailable", true);
        }
        if (passive) {
            CompatibilityUtils.setMetaBoolean(item, "passive", true);
        }

        // Set display name
        CompatibilityUtils.setDisplayName(item, getSkillTitle(player, skill.getName()));

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
            if (timeInSeconds > 60 * 60) {
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

        OptionalInt preparedPoints = hero.getSkillPrepareCost(skill);
        if (skill instanceof PassiveSkill) {
            lore.add(getMessage("skills.passive_description", "Passive"));
        } else {
            if (preparedPoints.isPresent()) {
                String costTemplate = getMessage("skills.prepared_lore", "Prepared cost: $points");
                lore.add(costTemplate.replace("$points", Integer.toString(preparedPoints.getAsInt())));
            }
        }

        int level = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.LEVEL, 1, true);

        String levelDescription = getMessage("skills.level_description", "").replace("$level", Integer.toString(level));
        if (levelDescription != null && levelDescription.length() > 0) {
            lore.add(levelDescription);
        }
        String description = skill.getDescription(hero);
        if (description != null && description.length() > 0) {
            description = getMessage("skills.description", "$description").replace("$description", description);
            CompatibilityUtils.wrapText(description, MAX_LORE_LENGTH, lore);
        }

        /*
        // This looks like it just generates redundant lore?
        description = skillDescription.getDescription();
        if (description != null && description.length() > 0) {
            description = getMessage("skills.description_extra", "$description").replace("$description", description);
            InventoryUtils.wrapText(description, MAX_LORE_LENGTH, lore);
        }
        */

        int cooldown = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.COOLDOWN, 0, true);
        if (cooldown > 0) {
            String cooldownDescription = getTimeDescription(cooldown);
            if (cooldownDescription != null && !cooldownDescription.isEmpty()) {
                lore.add(getMessage("cooldown.description", "$time").replace("$time", cooldownDescription));
            }
        }

        int mana = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.MANA, 0, true);
        if (mana > 0) {
            String manaDescription = getMessage("costs.heroes_mana").replace("$amount", Integer.toString(mana));
            lore.add(getMessage("skills.costs_description").replace("$description", manaDescription));
        }

        if (preparedPoints.isPresent() && isPrepared(player, skill.getName())) {
            lore.add(getMessage("skills.unprepare_lore"));
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
        HeroClass heroClass = hero.getSecondaryClass();
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
        for (String skillName : skillNames) {
            Skill skill = skills.getSkill(skillName);
            if (skill == null) continue;
            int level = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.LEVEL, 1, true);
            skillMap.put(level, skill);
        }
        return skillMap;
    }

    private void addSkills(Hero hero, HeroClass heroClass, Collection<String> skillSet, boolean showUnuseable, boolean showPassive) {
        if (heroClass != null) {
            Set<String> classSkills = heroClass.getSkillNames();
            for (String classSkill : classSkills) {
                Skill skill = skills.getSkill(classSkill);
                if (!showUnuseable && !hero.canUseSkill(skill)) continue;
                if (!showPassive && !(skill instanceof ActiveSkill)) continue;
                // getRaw's boolean default value is ignored! :(
                if (SkillConfigManager.getRaw(skill, "wand", "true").equalsIgnoreCase("true")) {
                    skillSet.add(classSkill);
                }
            }
        }
    }

    public List<String> getSkillList(Player player, boolean showUnuseable, boolean showPassive) {
        if (skills == null) return emptySkillList;
        Hero hero = getHero(player);
        if (hero == null) return emptySkillList;

        HeroClass heroClass = hero.getHeroClass();
        HeroClass secondClass = hero.getSecondaryClass();
        HeroClass raceClass = hero.getRaceClass();
        Set<String> primarySkills = new HashSet<>();
        Set<String> secondarySkills = new HashSet<>();
        Set<String> raceSkills = new HashSet<>();
        addSkills(hero, heroClass, primarySkills, showUnuseable, showPassive);
        addSkills(hero, secondClass, secondarySkills, showUnuseable, showPassive);
        addSkills(hero, raceClass, raceSkills, showUnuseable, showPassive);
        secondarySkills.removeAll(primarySkills);
        raceSkills.removeAll(primarySkills);

        Multimap<Integer, Skill> primaryMap = mapSkillsByLevel(hero, primarySkills);
        Multimap<Integer, Skill> secondaryMap = mapSkillsByLevel(hero, secondarySkills);
        Multimap<Integer, Skill> raceMap = mapSkillsByLevel(hero, raceSkills);
        List<String> skillNames = new ArrayList<>();
        for (Skill skill : primaryMap.values()) {
            skillNames.add(skill.getName());
        }
        for (Skill skill : secondaryMap.values()) {
            skillNames.add(skill.getName());
        }
        for (Skill skill : raceMap.values()) {
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
        return hero.canUseSkill(skillName, true);
    }

    public boolean isPrepared(Player player, String skillName) {
        Hero hero = getHero(player);
        if (hero == null) return false;
        return hero.isSkillPrepared(skillName) || !hero.getSkillPrepareCost(skillName).isPresent();
    }

    public SkillSelector getActiveSkillSelector(HumanEntity player) {
        return selectors.get(player.getUniqueId());
    }

    public void setActiveSkillSelector(HumanEntity player, SkillSelector selector) {
        selectors.put(player.getUniqueId(), selector);
    }

    public void clearActiveSkillSelector(HumanEntity player) {
        selectors.remove(player.getUniqueId());
    }

    public boolean isSkill(ItemStack item) {
        return CompatibilityUtils.hasMeta(item, skillNBTKey);
    }

    public boolean isLegacySkill(ItemStack item) {
        return CompatibilityUtils.hasMeta(item, legacyNBTKey);
    }

    public void useSkill(Player player, ItemStack item) {
        String skillKey = getSkillKey(item);
        if (skillKey != null && !skillKey.isEmpty()) {
            plugin.getServer().dispatchCommand(player, "skill " + skillKey);
        }
    }

    public void unprepareSkill(Player player, ItemStack item) {
        String skillKey = getSkillKey(item);
        if (skillKey != null && !skillKey.isEmpty()) {

            // Always take all of the items away here, players can use this to
            // "unprepare" skills that don't need preparing just to clean them out of their inventory
            // Only do this if the skill selector is active.
            SkillSelector activeSelector = getActiveSkillSelector(player);
            if (activeSelector != null) {
                Inventory inventory = player.getInventory();
                for (int i = 0; i < inventory.getSize(); i++) {
                    ItemStack slotItem = inventory.getItem(i);
                    String slotKey = getSkillKey(slotItem);
                    if (slotKey != null && slotKey.equals(skillKey)) {
                        inventory.setItem(i, null);
                    }
                }
            }

            // Make sure this skill can be unprepared
            Hero hero = getHero(player);
            Skill skill = getSkill(skillKey);
            OptionalInt preparedPoints = hero.getSkillPrepareCost(skill);
            if (preparedPoints.isPresent() && hero.isSkillPrepared(skillKey)) {
                // Unprepare it, update item name
                hero.unprepareSkill(skill);
                CompatibilityUtils.setDisplayName(item, getSkillTitle(player, skillKey));

                List<String> lore = new ArrayList<>();
                addSkillLore(new SkillDescription(this, player, skillKey), lore, player);
                CompatibilityUtils.setLore(item, lore);

                // Message the player
                int usedPoints = hero.getUsedSkillPreparePoints();
                int maxPoints = hero.getTotalSkillPreparePoints();
                int maxPrepared = hero.getPreparedSkillLimit();
                int currentPrepared = hero.getPreparedSkillCount();
                int remainingPoints = maxPoints - usedPoints;
                int remainingSlots = maxPrepared - currentPrepared;
                player.sendMessage(getMessage("skills.unprepared")
                    .replace("$skill", skillKey)
                    .replace("$points", Integer.toString(remainingPoints))
                    .replace("$slots", Integer.toString(remainingSlots)));
            }
        }
    }

    public boolean hasSkillItem(Player player, String skillKey) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack slotItem = inventory.getItem(i);
            String slotKey = getSkillKey(slotItem);
            if (slotKey != null && slotKey.equals(skillKey)) {
                return true;
            }
        }
        return false;
    }

    public boolean prepareSkill(Player player, ItemStack item) {
        String skillKey = getSkillKey(item);
        if (skillKey != null && !skillKey.isEmpty()) {
            Skill skill = getSkill(skillKey);
            Hero hero = getHero(player);
            OptionalInt preparedPoints = hero.getSkillPrepareCost(skill);
            if (preparedPoints.isPresent()) {
                if (!hero.isSkillPrepared(skillKey)) {
                    int usedPoints = hero.getUsedSkillPreparePoints();
                    int maxPoints = hero.getTotalSkillPreparePoints();
                    int maxPrepared = hero.getPreparedSkillLimit();
                    int currentPrepared = hero.getPreparedSkillCount();
                    if (currentPrepared + 1 > maxPrepared || usedPoints + preparedPoints.getAsInt() > maxPoints) {
                        player.sendMessage(getMessage("skills.prepare_limit"));
                        return false;
                    } else {
                        hero.prepareSkill(skillKey);
                        CompatibilityUtils.setDisplayName(item, getSkillTitle(player, skillKey));

                        int remainingPoints = maxPoints - usedPoints - preparedPoints.getAsInt();
                        int remainingSlots = maxPrepared - currentPrepared - 1;
                        player.sendMessage(getMessage("skills.prepared")
                            .replace("$skill", skillKey)
                            .replace("$points", Integer.toString(remainingPoints))
                            .replace("$slots", Integer.toString(remainingSlots)));

                        List<String> lore = new ArrayList<>();
                        addSkillLore(new SkillDescription(this, player, skillKey), lore, player);
                        CompatibilityUtils.setLore(item, lore);
                    }
                }
            }
        }
        return true;
    }

    public String getSkillKey(ItemStack item) {
        return CompatibilityUtils.getMetaString(item, skillNBTKey);
    }

    public Logger getLogger() {
        return plugin.getLogger();
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public Server getServer() {
        return plugin.getServer();
    }

    public String getDefaultDisabledIconURL() {
        return defaultDisabledIconURL;
    }

    public void delayedInventoryUpdate(final Player player) {
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @SuppressWarnings("deprecation")
            @Override
            public void run() {
                player.updateInventory();
            }
        }, 1);
    }
}
