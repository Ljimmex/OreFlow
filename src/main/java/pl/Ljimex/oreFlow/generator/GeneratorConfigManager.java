package pl.Ljimex.oreFlow.generator;

import pl.Ljimex.oreFlow.OreFlow;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Ładuje i cache'uje wszystkie generatory z generators.yml jako type-safe GeneratorConfig.
 */
public class GeneratorConfigManager {

    private final OreFlow plugin;
    private final Map<String, GeneratorConfig> generators = new LinkedHashMap<>();

    public GeneratorConfigManager(OreFlow plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        generators.clear();
        ConfigurationSection generatorsSection = plugin.getConfigManager().getGenerators()
                .getConfigurationSection("generators");
        if (generatorsSection == null) {
            plugin.getLogger().warning("generators.yml section 'generators' is null!");
            return;
        }

        for (String key : generatorsSection.getKeys(false)) {
            ConfigurationSection section = generatorsSection.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                GeneratorConfig generatorConfig = new GeneratorConfig(key, section);
                generators.put(key.toLowerCase(), generatorConfig);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load generator '" + key + "'", e);
            }
        }

        plugin.getLogger().info("Loaded " + generators.size() + " generators");
    }

    public GeneratorConfig getGenerator(String key) {
        return generators.get(key.toLowerCase());
    }

    public boolean hasGenerator(String key) {
        return generators.containsKey(key.toLowerCase());
    }

    public Collection<GeneratorConfig> getGenerators() {
        return Collections.unmodifiableCollection(generators.values());
    }

    public GeneratorConfig getFirstEnabledGenerator() {
        for (GeneratorConfig generator : generators.values()) {
            if (generator.isEnabled()) {
                return generator;
            }
        }
        return null;
    }

    public int getGeneratorCount() {
        return generators.size();
    }
}
