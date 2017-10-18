package com.elmakers.mine.bukkit.heroes;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class MaterialAndData {
    private final Material material;
    private final short data;

    public MaterialAndData(Material material, short data) {
        this.material = material;
        this.data = data;
    }

    public MaterialAndData(Material material) {
        this.material = material;
        this.data = 0;
    }

    public MaterialAndData(String key) {
        String[] pieces = StringUtils.split(key, ':');
        this.material = Material.valueOf(pieces[0].toUpperCase());
        short data = 0;
        if (pieces.length > 1) {
            data = Short.parseShort(pieces[1]);
        }
        this.data = data;
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
    }

    public ItemStack createItemStack() {
        return new ItemStack(material, 1, data);
    }
}
