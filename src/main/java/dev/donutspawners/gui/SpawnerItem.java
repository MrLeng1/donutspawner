package dev.donutspawners.gui;

import dev.donutspawners.DonutSpawners;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class SpawnerItem {

    public static final String NBT_TYPE_KEY = "spawner_type";
    public static final String NBT_STACK_KEY = "spawner_stack";

    public static ItemStack create(EntityType type, int stack) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();

        String typeName = capitalize(type.name().replace("_", " "));

        meta.displayName(Component.text(stack + "x " + typeName + " Spawner")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Type: " + typeName)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Stack: " + stack)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Place to activate. Requires Silk Touch to break.")
                .color(NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        // Store type and stack in PDC
        NamespacedKey typeKey = new NamespacedKey(DonutSpawners.getInstance(), NBT_TYPE_KEY);
        NamespacedKey stackKey = new NamespacedKey(DonutSpawners.getInstance(), NBT_STACK_KEY);
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.name());
        meta.getPersistentDataContainer().set(stackKey, PersistentDataType.INTEGER, stack);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isSpawnerItem(ItemStack item) {
        if (item == null || item.getType() != Material.SPAWNER) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        NamespacedKey key = new NamespacedKey(DonutSpawners.getInstance(), NBT_TYPE_KEY);
        return meta.getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    public static EntityType getEntityType(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(DonutSpawners.getInstance(), NBT_TYPE_KEY);
        String typeStr = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        try {
            return EntityType.valueOf(typeStr);
        } catch (Exception e) {
            return EntityType.PIG;
        }
    }

    public static int getStack(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(DonutSpawners.getInstance(), NBT_STACK_KEY);
        Integer stack = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        return stack != null ? stack : 1;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            sb.append(Character.toUpperCase(w.charAt(0)))
              .append(w.substring(1).toLowerCase())
              .append(" ");
        }
        return sb.toString().trim();
    }
}
