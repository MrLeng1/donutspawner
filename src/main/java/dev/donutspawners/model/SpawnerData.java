package dev.donutspawners.model;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SpawnerData {

    private final EntityType entityType;
    private int stackSize;
    private final List<ItemStack> storage;
    private int maxStorage;

    public SpawnerData(EntityType entityType, int stackSize) {
        this.entityType = entityType;
        this.stackSize = stackSize;
        this.storage = new ArrayList<>();
        this.maxStorage = 54 * stackSize; // scales with stack
    }

    public EntityType getEntityType() { return entityType; }
    public int getStackSize() { return stackSize; }
    public void setStackSize(int stackSize) {
        this.stackSize = stackSize;
        this.maxStorage = 54 * stackSize;
    }

    public List<ItemStack> getStorage() { return storage; }

    public int getMaxStorage() { return maxStorage; }

    public int getTotalItems() {
        int total = 0;
        for (ItemStack item : storage) {
            if (item != null) total += item.getAmount();
        }
        return total;
    }

    public double getFillPercent() {
        if (maxStorage == 0) return 0;
        return (getTotalItems() / (double) maxStorage) * 100.0;
    }

    public void addItem(ItemStack item) {
        // Try to merge with existing stacks first
        for (ItemStack stored : storage) {
            if (stored != null && stored.isSimilar(item) && stored.getAmount() < stored.getMaxStackSize()) {
                int space = stored.getMaxStackSize() - stored.getAmount();
                int take = Math.min(space, item.getAmount());
                stored.setAmount(stored.getAmount() + take);
                item.setAmount(item.getAmount() - take);
                if (item.getAmount() <= 0) return;
            }
        }
        // Add as new entry
        storage.add(item.clone());
    }

    public List<ItemStack> drainStorage() {
        List<ItemStack> items = new ArrayList<>(storage);
        storage.clear();
        return items;
    }

    public List<ItemStack> drainPage(int page, int pageSize) {
        int start = page * pageSize;
        int end = Math.min(start + pageSize, storage.size());
        if (start >= storage.size()) return new ArrayList<>();
        List<ItemStack> page_items = new ArrayList<>(storage.subList(start, end));
        storage.subList(start, end).clear();
        return page_items;
    }
}
