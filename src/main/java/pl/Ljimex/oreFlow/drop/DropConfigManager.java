package pl.Ljimex.oreFlow.drop;

import pl.Ljimex.oreFlow.OreFlow;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Ładuje i cache'uje wszystkie dropy z drops.yml jako type-safe DropConfig.
 */
public class DropConfigManager {

    private final OreFlow plugin;
    private final Map<String, DropConfig> drops = new HashMap<>();

    public DropConfigManager(OreFlow plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        drops.clear();
        ConfigurationSection dropsSection = plugin.getConfigManager().getDrops();
        if (dropsSection == null) {
            plugin.getLogger().warning("drops.yml section is null!");
            return;
        }

        for (String key : dropsSection.getKeys(false)) {
            if ("config-version".equals(key)) {
                continue;
            }
            ConfigurationSection section = dropsSection.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                DropConfig dropConfig = new DropConfig(key, section);
                drops.put(key.toLowerCase(), dropConfig);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load drop '" + key + "'", e);
            }
        }

        plugin.getLogger().info("Loaded " + drops.size() + " drops");
    }

    public DropConfig getDrop(String key) {
        return drops.get(key.toLowerCase());
    }

    public boolean hasDrop(String key) {
        return drops.containsKey(key.toLowerCase());
    }

    public Collection<DropConfig> getDrops() {
        return Collections.unmodifiableCollection(drops.values());
    }

    public int getDropCount() {
        return drops.size();
    }
}
