package dev.donutspawners;

import dev.donutspawners.listener.SpawnerCommand;
import dev.donutspawners.listener.SpawnerListener;
import dev.donutspawners.manager.SpawnerManager;
import org.bukkit.plugin.java.JavaPlugin;

public class DonutSpawners extends JavaPlugin {

    private static DonutSpawners instance;
    private SpawnerManager spawnerManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        spawnerManager = new SpawnerManager(this);

        getServer().getPluginManager().registerEvents(new SpawnerListener(this), this);

        SpawnerCommand cmd = new SpawnerCommand(this);
        getCommand("donutspawner").setExecutor(cmd);
        getCommand("donutspawner").setTabCompleter(cmd);

        // Auto-save task
        long saveInterval = getConfig().getLong("auto_save_interval", 6000L);
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> spawnerManager.save(), saveInterval, saveInterval);

        getLogger().info("DonutSpawners v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (spawnerManager != null) spawnerManager.save();
        getLogger().info("DonutSpawners disabled. Data saved.");
    }

    public static DonutSpawners getInstance() { return instance; }
    public SpawnerManager getSpawnerManager() { return spawnerManager; }
}
