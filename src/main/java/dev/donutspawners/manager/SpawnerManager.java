package dev.donutspawners.manager;

import dev.donutspawners.DonutSpawners;
import dev.donutspawners.model.SpawnerData;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SpawnerManager {

    private final DonutSpawners plugin;
    private final Map<String, SpawnerData> spawners = new HashMap<>();
    private final File dataFile;
    private FileConfiguration dataConfig;

    public SpawnerManager(DonutSpawners plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "spawners.yml");
        load();
    }

    public String locKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public void registerSpawner(Location loc, SpawnerData data) {
        spawners.put(locKey(loc), data);
    }

    public SpawnerData getSpawner(Location loc) {
        return spawners.get(locKey(loc));
    }

    public boolean isSpawner(Location loc) {
        return spawners.containsKey(locKey(loc));
    }

    public void removeSpawner(Location loc) {
        spawners.remove(locKey(loc));
        save();
    }

    // ─── Persistence ────────────────────────────────────────────────────────

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, SpawnerData> entry : spawners.entrySet()) {
            String key = entry.getKey().replace(",", "_").replace(".", "_");
            SpawnerData data = entry.getValue();
            String base = "spawners." + key;
            cfg.set(base + ".loc", entry.getKey());
            cfg.set(base + ".type", data.getEntityType().name());
            cfg.set(base + ".stack", data.getStackSize());
            for (int i = 0; i < data.getStorage().size(); i++) {
                cfg.set(base + ".storage." + i, data.getStorage().get(i));
            }
        }
        try {
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save spawner data: " + e.getMessage());
        }
    }

    public void load() {
        if (!dataFile.exists()) return;
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (!dataConfig.contains("spawners")) return;

        for (String key : dataConfig.getConfigurationSection("spawners").getKeys(false)) {
            String base = "spawners." + key;
            String loc = dataConfig.getString(base + ".loc");
            String typeStr = dataConfig.getString(base + ".type");
            int stack = dataConfig.getInt(base + ".stack", 1);

            EntityType type;
            try {
                type = EntityType.valueOf(typeStr);
            } catch (Exception e) {
                continue;
            }

            SpawnerData data = new SpawnerData(type, stack);

            if (dataConfig.contains(base + ".storage")) {
                for (String idx : dataConfig.getConfigurationSection(base + ".storage").getKeys(false)) {
                    ItemStack item = dataConfig.getItemStack(base + ".storage." + idx);
                    if (item != null) data.getStorage().add(item);
                }
            }

            spawners.put(loc, data);
        }
    }

    public Map<String, SpawnerData> getAllSpawners() {
        return spawners;
    }
}
