package com.elmakers.mine.bukkit.heroes;

import java.io.StringReader;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

@SuppressWarnings("deprecation")
public class MaterialAndData {
    protected static Gson gson;
    private final Material material;
    private final short data;
    private final int customData;

    public MaterialAndData(Material material, short data) {
        this.material = material;
        this.data = data;
        this.customData = 0;
    }

    public MaterialAndData(Material material) {
        this.material = material;
        this.data = 0;
        this.customData = 0;
    }

    public MaterialAndData(String key) {
        String materialKey = key;
        int customData = 0;
        short data = 0;
        int jsonStart = materialKey.indexOf('{');
        if (jsonStart > 0) {
            String fullKey = materialKey;
            materialKey = fullKey.substring(0, jsonStart);
            String json = fullKey.substring(jsonStart);
            int jsonEnd = json.lastIndexOf('}');
            if (jsonEnd != json.length() - 1) {
                materialKey += json.substring(jsonEnd + 1);
                json = json.substring(0, jsonEnd + 1);
            }
            if (!json.contains(":")) {
                try {
                    customData = Integer.parseInt(json.substring(1, json.length() - 1));
                } catch (Exception ex) {
                    Bukkit.getLogger().warning("[HeroesHotbar] Error parsing item custom model data: " + json + " : " + ex.getMessage());
                }
            } else {
                try {
                    JsonReader reader = new JsonReader(new StringReader(json));
                    reader.setLenient(true);
                    Map<String, Object> tags = getGson().fromJson(reader, Map.class);
                    Object modelTag = tags.get("CustomModelData");
                    if (modelTag != null) {
                        if (modelTag instanceof Integer) {
                            customData = (int)(Integer)modelTag;
                        } else if (modelTag instanceof Double) {
                            customData = (int)(double)(Double)modelTag;
                        } else if (modelTag instanceof String) {
                            customData = Integer.parseInt((String)modelTag);
                        }
                    }
                } catch (Throwable ex) {
                    Bukkit.getLogger().warning("[HeroesHotbar] Error parsing item json: " + json + " : " + ex.getMessage());
                }
            }
        } else {
            String[] pieces = StringUtils.split(key, ':');
            materialKey = pieces[0];
            if (pieces.length > 1) {
                data = Short.parseShort(pieces[1]);
            }
        }

        this.material = Material.valueOf(materialKey.toUpperCase());
        this.data = data;
        this.customData = customData;
    }

    public Material getMaterial() {
        return material;
    }

    public short getData() {
        return data;
    }

    public void applyToItem(ItemStack item) {
        item.setType(material);
        item.setDurability(data);
        if (customData > 0) {
            ItemMeta meta = item.getItemMeta();
            meta.setCustomModelData(customData);
            item.setItemMeta(meta);
        }
    }

    public ItemStack createItemStack() {
        ItemStack itemStack = new ItemStack(material, 1, data);
        applyToItem(itemStack);
        return itemStack;
    }

    private static Gson getGson() {
        if (gson == null) {
            gson = new Gson();
        }
        return gson;
    }
}
