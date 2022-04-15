package com.elmakers.mine.bukkit.heroes.utilities;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import com.elmakers.mine.bukkit.heroes.HotbarPlugin;
import com.elmakers.mine.bukkit.heroes.controller.HotbarController;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

public class CompatibilityUtils {
    private static Plugin plugin;

    public static void initialize(Plugin owningPlugin) {
        plugin = owningPlugin;
    }

    public static Inventory createInventory(InventoryHolder holder, int size, final String name) {
        size = (int) (Math.ceil((double) size / 9) * 9);
        size = Math.min(size, 54);
        String translatedName = translateColors(name);
        return Bukkit.createInventory(holder, size, translatedName);
    }

    public static String translateColors(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void setDisplayName(ItemStack itemStack, String name) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(name);
        itemStack.setItemMeta(meta);
    }

    public static void setLore(ItemStack itemStack, List<String> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
    }

    private static NamespacedKey getKey(String key) {
        return new NamespacedKey(plugin, key);
    }

    public static boolean getMetaBoolean(ItemStack itemStack, String key) {
        return getMetaBoolean(itemStack, key, false);
    }

    public static boolean getMetaBoolean(ItemStack itemStack, String key, boolean defaultValue) {
        if (itemStack == null) return false;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;
        Byte data = meta.getPersistentDataContainer().get(getKey(key), PersistentDataType.BYTE);
        if (data == null) {
            return defaultValue;
        }
        return data != 0;
    }

    public static void setMetaBoolean(ItemStack itemStack, String key, boolean value) {
        if (itemStack == null) return;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(getKey(key), PersistentDataType.BYTE, (byte)(value ? 1 : 0));
        itemStack.setItemMeta(meta);
    }

    public static void setMeta(ItemStack itemStack, String key, String value) {
        if (itemStack == null) return;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(getKey(key), PersistentDataType.STRING, value);
        itemStack.setItemMeta(meta);
    }

    public static boolean hasMeta(ItemStack itemStack, String key) {
        if (itemStack == null) return false;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(getKey(key), PersistentDataType.STRING);
    }

    public static String getMetaString(ItemStack itemStack, String key) {
        if (itemStack == null) return null;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(getKey(key), PersistentDataType.STRING);
    }

    public static void makeUnbreakable(ItemStack itemStack) {
        if (itemStack == null) return;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;
        meta.setUnbreakable(true);
        itemStack.setItemMeta(meta);
    }

    public static void hideFlags(ItemStack itemStack) {
        if (itemStack == null) return;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        itemStack.setItemMeta(meta);
    }

    public static void wrapText(String text, int maxLength, Collection<String> list) {
        wrapText(text, "", maxLength, list);
    }

    public static void wrapText(String text, String prefix, int maxLength, Collection<String> list) {
        String colorPrefix = "";
        String[] lines = StringUtils.split(text, "\n\r");
        for (String line : lines) {
            line = prefix + line;
            while (line.length() > maxLength)
            {
                int spaceIndex = line.lastIndexOf(' ', maxLength);
                if (spaceIndex <= 0) {
                    list.add(colorPrefix + line);
                    return;
                }
                String colorText = colorPrefix + line.substring(0, spaceIndex);
                colorPrefix = ChatColor.getLastColors(colorText);
                list.add(colorText);
                line = line.substring(spaceIndex);
            }

            list.add(colorPrefix + line);
        }
    }
    public static void getUnknownIcon(HotbarController controller, UUID uuid) {
        PlayerProfile profile = plugin.getServer().createPlayerProfile(uuid);
        profile.update().thenAcceptAsync(controller::setUnknownIcon, runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
    }

    public static void setSkullProfile(ItemStack skull, PlayerProfile profile) {
        try {
            ItemMeta itemMeta = skull.getItemMeta();
            SkullMeta meta = (SkullMeta)itemMeta;
            meta.setOwnerProfile(profile);
            skull.setItemMeta(meta);
        }
        catch(ClassCastException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to cast item meta for skill icon! " + e.toString());
        }
    }

    public static PlayerProfile getPlayerProfile(String name, String url) {
        PlayerProfile profile = plugin.getServer().createPlayerProfile(UUID.randomUUID(), name);
        PlayerTextures texture = profile.getTextures();
        try {
            texture.setSkin(new URL(url));
        }
        catch(MalformedURLException e) {
            plugin.getLogger().log(Level.WARNING, () -> "Url was malformed for skill icon " + name);
            return null;
        }
        profile.setTextures(texture);
        return profile;
    }
}
