package dev.donutspawners.gui;

import dev.donutspawners.model.SpawnerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class SpawnerGUI {

    // Row 6 bottom action bar slot indices (0-based)
    public static final int SLOT_SELL   = 48; // row6, col4 (0-based col 3)
    public static final int SLOT_INFO   = 49; // row6, col5 (0-based col 4)
    public static final int SLOT_DROP   = 50; // row6, col6 (0-based col 5)

    // Filler slot for bottom bar
    public static final int SLOT_FILL_1 = 45;
    public static final int SLOT_FILL_2 = 46;
    public static final int SLOT_FILL_3 = 47;
    public static final int SLOT_FILL_4 = 51;
    public static final int SLOT_FILL_5 = 52;
    public static final int SLOT_FILL_6 = 53;

    private static final int PAGE_SIZE = 45; // slots 0-44 for items

    public static Inventory build(SpawnerData data, int page) {
        String typeName = capitalize(data.getEntityType().name().replace("_", " "));
        int totalPages = Math.max(1, (int) Math.ceil(data.getStorage().size() / (double) PAGE_SIZE));

        Component title = Component.text(data.getStackSize() + "x " + typeName + " Spawner")
                .color(NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false);

        Inventory inv = Bukkit.createInventory(null, 54, title);

        // ── Storage items (slots 0-44) ──────────────────────────────────────
        List<ItemStack> storage = data.getStorage();
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx < storage.size()) {
                inv.setItem(i, storage.get(idx));
            }
        }

        // ── Bottom bar filler (black glass) ────────────────────────────────
        ItemStack filler = buildFiller();
        for (int s : new int[]{SLOT_FILL_1, SLOT_FILL_2, SLOT_FILL_3, SLOT_FILL_4, SLOT_FILL_5, SLOT_FILL_6}) {
            inv.setItem(s, filler);
        }

        // ── Slot 4 (index 48): Emerald - SELL ALL ──────────────────────────
        inv.setItem(SLOT_SELL, buildSellButton());

        // ── Slot 5 (index 49): Skull - INFO ────────────────────────────────
        inv.setItem(SLOT_INFO, buildInfoButton(data, page, totalPages));

        // ── Slot 6 (index 50): Dropper - DROP ALL ──────────────────────────
        inv.setItem(SLOT_DROP, buildDropButton());

        return inv;
    }

    // ─── Button builders ─────────────────────────────────────────────────────

    private static ItemStack buildSellButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("SELL ALL")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click to sell all mob drops!")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildInfoButton(SpawnerData data, int page, int totalPages) {
        ItemStack item = new ItemStack(Material.SKELETON_SKULL);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        int fillPct = (int) data.getFillPercent();
        String typeName = capitalize(data.getEntityType().name().replace("_", " "));

        meta.displayName(Component.text(data.getStackSize() + " " + typeName.toUpperCase() + " SPAWNER")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("(" + fillPct + "% filled)")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Page: " + (page + 1) + "/" + totalPages)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Total items: " + data.getTotalItems())
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildDropButton() {
        ItemStack item = new ItemStack(Material.DROPPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("DROP LOOT")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click to drop all loot on the page")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

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

    public static boolean isActionSlot(int slot) {
        return slot == SLOT_SELL || slot == SLOT_INFO || slot == SLOT_DROP
                || slot == SLOT_FILL_1 || slot == SLOT_FILL_2 || slot == SLOT_FILL_3
                || slot == SLOT_FILL_4 || slot == SLOT_FILL_5 || slot == SLOT_FILL_6;
    }

    public static boolean isBottomBar(int slot) {
        return slot >= 45;
    }
}
