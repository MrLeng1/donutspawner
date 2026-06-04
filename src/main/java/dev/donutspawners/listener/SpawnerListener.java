package dev.donutspawners.listener;

import dev.donutspawners.DonutSpawners;
import dev.donutspawners.gui.SpawnerGUI;
import dev.donutspawners.gui.SpawnerItem;
import dev.donutspawners.model.SpawnerData;
import dev.donutspawners.manager.SpawnerManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpawnerListener implements Listener {

    private final DonutSpawners plugin;
    private final SpawnerManager manager;

    // Track which spawner location each player has open: playerUUID -> locKey
    private final Map<UUID, String> openGUI = new HashMap<>();
    // Track current page per player
    private final Map<UUID, Integer> openPage = new HashMap<>();

    public SpawnerListener(DonutSpawners plugin) {
        this.plugin = plugin;
        this.manager = plugin.getSpawnerManager();
    }

    // ─── Place spawner ────────────────────────────────────────────────────────
    // SHIFT + place → place entire stack count (all items in hand)
    // Normal place  → place 1 spawner only

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!SpawnerItem.isSpawnerItem(item)) return;

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        Location loc = block.getLocation();
        EntityType type = SpawnerItem.getEntityType(item);
        int perItem = SpawnerItem.getStack(item); // stack value stored in NBT (e.g. 1x spawner = 1)

        boolean shifting = player.isSneaking();

        // How many physical items to consume
        int itemsToConsume = shifting ? item.getAmount() : 1;
        int addedStack = perItem * itemsToConsume;

        // Check if there's already a DonutSpawner here (stacking)
        if (manager.isSpawner(loc)) {
            SpawnerData existing = manager.getSpawner(loc);
            if (existing.getEntityType() == type) {
                existing.setStackSize(existing.getStackSize() + addedStack);
                item.setAmount(item.getAmount() - itemsToConsume);
                event.setCancelled(true);
                player.sendMessage(Component.text("Stacked! Now " + existing.getStackSize() + "x " + type.name() + " Spawner")
                        .color(NamedTextColor.GREEN));
                return;
            }
        }

        // New spawner placement
        SpawnerData data = new SpawnerData(type, addedStack);
        manager.registerSpawner(loc, data);

        // Consume extra items if shift-placing
        if (shifting && itemsToConsume > 1) {
            item.setAmount(item.getAmount() - (itemsToConsume - 1)); // BlockPlaceEvent already consumed 1
        }

        // Set the actual block spawner type (cosmetic)
        if (block.getState() instanceof CreatureSpawner cs) {
            cs.setSpawnedType(type);
            cs.update();
        }

        player.sendMessage(Component.text("Placed " + addedStack + "x " + type.name() + " Spawner!")
                .color(NamedTextColor.GREEN));
    }

    // ─── Break spawner (silk touch only) ─────────────────────────────────────
    // SHIFT + break → drops entire stack as one item
    // Normal break  → drops only 1, reduces stack by 1

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) return;
        if (!manager.isSpawner(block.getLocation())) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        boolean hasSilk = tool.containsEnchantment(Enchantment.SILK_TOUCH);

        if (!hasSilk && !player.hasPermission("donutspawners.silktouch.bypass")) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You need a Silk Touch tool to break this spawner!")
                    .color(NamedTextColor.RED));
            return;
        }

        SpawnerData data = manager.getSpawner(block.getLocation());
        event.setDropItems(false);

        boolean shifting = player.isSneaking();

        if (shifting || data.getStackSize() <= 1) {
            // SHIFT break (or last spawner) → drop everything, remove block
            ItemStack drop = SpawnerItem.create(data.getEntityType(), data.getStackSize());
            block.getWorld().dropItemNaturally(block.getLocation(), drop);

            // Drop stored loot
            for (ItemStack stored : data.getStorage()) {
                if (stored != null) block.getWorld().dropItemNaturally(block.getLocation(), stored);
            }

            manager.removeSpawner(block.getLocation());
            player.sendMessage(Component.text("Removed all " + data.getStackSize() + "x " + data.getEntityType().name() + " Spawners!")
                    .color(NamedTextColor.GREEN));

        } else {
            // Normal break → remove 1 from stack, keep block
            event.setCancelled(true);
            int newStack = data.getStackSize() - 1;
            data.setStackSize(newStack);
            manager.save();

            ItemStack drop = SpawnerItem.create(data.getEntityType(), 1);
            block.getWorld().dropItemNaturally(block.getLocation(), drop);

            player.sendMessage(Component.text("Removed 1 spawner. " + newStack + "x " + data.getEntityType().name() + " remaining.")
                    .color(NamedTextColor.YELLOW));
        }
    }

    // ─── Right-click spawner → open GUI ──────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.SPAWNER) return;
        if (!manager.isSpawner(block.getLocation())) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("donutspawners.use")) {
            player.sendMessage(Component.text("You don't have permission to use spawners!")
                    .color(NamedTextColor.RED));
            return;
        }

        event.setCancelled(true);
        SpawnerData data = manager.getSpawner(block.getLocation());

        openGUI.put(player.getUniqueId(), manager.locKey(block.getLocation()));
        openPage.put(player.getUniqueId(), 0);

        Inventory inv = SpawnerGUI.build(data, 0);
        player.openInventory(inv);
    }

    // ─── GUI click handler ────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        UUID uuid = player.getUniqueId();
        if (!openGUI.containsKey(uuid)) return;

        Inventory inv = event.getInventory();
        int slot = event.getRawSlot();

        // Prevent taking items from bottom bar
        if (slot >= 0 && slot < 54 && SpawnerGUI.isBottomBar(slot)) {
            event.setCancelled(true);
        }

        String locKey = openGUI.get(uuid);
        SpawnerData data = getDataByKey(locKey);
        if (data == null) return;

        int page = openPage.getOrDefault(uuid, 0);

        if (slot == SpawnerGUI.SLOT_SELL) {
            event.setCancelled(true);
            handleSell(player, data, locKey, page, inv);
        } else if (slot == SpawnerGUI.SLOT_DROP) {
            event.setCancelled(true);
            handleDrop(player, data, locKey, page, inv);
        } else if (slot == SpawnerGUI.SLOT_INFO) {
            event.setCancelled(true);
        }
    }

    // ─── GUI close → sync storage ─────────────────────────────────────────────

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!openGUI.containsKey(uuid)) return;

        String locKey = openGUI.get(uuid);
        SpawnerData data = getDataByKey(locKey);
        int page = openPage.getOrDefault(uuid, 0);

        if (data != null) {
            // Sync items player may have put in storage slots back to data
            syncPageToData(event.getInventory(), data, page);
            manager.save();
        }

        openGUI.remove(uuid);
        openPage.remove(uuid);
    }

    // ─── Mob death → add loot to nearest DonutSpawner ────────────────────────

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Location loc = event.getEntity().getLocation();

        // Find nearest spawner of matching type within 16 blocks
        SpawnerData nearest = null;
        String nearestKey = null;
        double nearestDist = 16.0;

        for (Map.Entry<String, SpawnerData> entry : manager.getAllSpawners().entrySet()) {
            if (entry.getValue().getEntityType() != event.getEntityType()) continue;
            Location sLoc = keyToLocation(entry.getKey());
            if (sLoc == null || !sLoc.getWorld().equals(loc.getWorld())) continue;
            double dist = sLoc.distance(loc);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = entry.getValue();
                nearestKey = entry.getKey();
            }
        }

        if (nearest == null) return;

        // Absorb drops into spawner storage
        for (ItemStack drop : event.getDrops()) {
            if (drop != null) nearest.addItem(drop);
        }
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    // ─── Sell handler ─────────────────────────────────────────────────────────

    private void handleSell(Player player, SpawnerData data, String locKey, int page, Inventory inv) {
        // Run /sell command as player (requires a sell plugin like EssentialsX)
        List<ItemStack> items = data.drainStorage();
        if (items.isEmpty()) {
            player.sendMessage(Component.text("No items to sell!").color(NamedTextColor.RED));
            return;
        }

        // Give items to player temporarily, then run /sell
        // Simpler approach: drop into player inventory and run sell
        for (ItemStack item : items) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            // Drop leftover at player's feet
            for (ItemStack lr : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), lr);
            }
        }

        // Execute sell command
        Bukkit.dispatchCommand(player, "sell all");

        player.sendMessage(Component.text("Selling all spawner loot!")
                .color(NamedTextColor.GREEN));

        // Refresh GUI
        refreshGUI(player, data, page, inv);
    }

    // ─── Drop handler ─────────────────────────────────────────────────────────

    private void handleDrop(Player player, SpawnerData data, String locKey, int page, Inventory inv) {
        List<ItemStack> items = data.drainPage(page, SpawnerGUI.PAGE_SIZE);
        if (items.isEmpty()) {
            player.sendMessage(Component.text("No items to drop on this page!")
                    .color(NamedTextColor.RED));
            return;
        }

        Location dropLoc = player.getLocation();
        for (ItemStack item : items) {
            if (item != null) dropLoc.getWorld().dropItemNaturally(dropLoc, item);
        }

        player.sendMessage(Component.text("Dropped " + items.size() + " item stacks!")
                .color(NamedTextColor.GREEN));

        manager.save();
        refreshGUI(player, data, page, inv);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void refreshGUI(Player player, SpawnerData data, int page, Inventory inv) {
        Inventory fresh = SpawnerGUI.build(data, page);
        // Copy contents into existing open inventory to avoid flicker
        inv.setContents(fresh.getContents());
    }

    private void syncPageToData(Inventory inv, SpawnerData data, int page) {
        int start = page * SpawnerGUI.PAGE_SIZE;
        // Resize data storage list if needed
        while (data.getStorage().size() < start) data.getStorage().add(null);

        for (int i = 0; i < SpawnerGUI.PAGE_SIZE; i++) {
            int idx = start + i;
            ItemStack item = inv.getItem(i);
            if (idx < data.getStorage().size()) {
                data.getStorage().set(idx, item);
            } else if (item != null) {
                data.getStorage().add(item);
            }
        }
        // Remove nulls at end
        data.getStorage().removeIf(item -> item == null);
    }

    private SpawnerData getDataByKey(String locKey) {
        return manager.getAllSpawners().get(locKey);
    }

    private Location keyToLocation(String key) {
        try {
            String[] parts = key.split(",");
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (Exception e) {
            return null;
        }
    }

    // Make PAGE_SIZE accessible
    static final int PAGE_SIZE = 45;
}
