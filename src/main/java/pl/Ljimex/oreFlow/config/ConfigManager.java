package pl.Ljimex.oreFlow.config;

import pl.Ljimex.oreFlow.OreFlow;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class ConfigManager {

    private final OreFlow plugin;

    private File configFile;
    private File dropsFile;
    private File generatorsFile;
    private File langFile;

    private FileConfiguration config;
    private FileConfiguration drops;
    private FileConfiguration generators;
    private FileConfiguration lang;

    public ConfigManager(OreFlow plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        plugin.saveDefaultConfig();

        configFile = new File(plugin.getDataFolder(), "config.yml");
        dropsFile = new File(plugin.getDataFolder(), "drops.yml");
        generatorsFile = new File(plugin.getDataFolder(), "generators.yml");
        langFile = new File(plugin.getDataFolder(), "lang.yml");

        createDefaultFile(configFile, "config.yml");
        createDefaultFile(dropsFile, "drops.yml");
        createDefaultFile(generatorsFile, "generators.yml");
        createDefaultFile(langFile, "lang.yml");

        reloadConfigs();
    }

    public void reloadConfigs() {
        config = YamlConfiguration.loadConfiguration(configFile);
        drops = YamlConfiguration.loadConfiguration(dropsFile);
        generators = YamlConfiguration.loadConfiguration(generatorsFile);
        lang = YamlConfiguration.loadConfiguration(langFile);

        loadDefaultsFromResources(config, configFile, "config.yml");
        loadDefaultsFromResources(drops, dropsFile, "drops.yml");
        loadDefaultsFromResources(generators, generatorsFile, "generators.yml");
        loadDefaultsFromResources(lang, langFile, "lang.yml");
    }

    public void saveConfigs() {
        saveConfig(config, configFile);
        saveConfig(drops, dropsFile);
        saveConfig(generators, generatorsFile);
        saveConfig(lang, langFile);
    }

    private void createDefaultFile(File file, String resourceName) {
        if (!file.exists()) {
            plugin.saveResource(resourceName, false);
        }
    }

    private void loadDefaultsFromResources(FileConfiguration configuration, File file, String resourceName) {
        InputStream defaultStream = plugin.getResource(resourceName);
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            configuration.setDefaults(defaultConfig);
        }
    }

    private void saveConfig(FileConfiguration configuration, File file) {
        try {
            configuration.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config file: " + file.getName(), e);
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getDrops() {
        return drops;
    }

    public FileConfiguration getGenerators() {
        return generators;
    }

    public FileConfiguration getLang() {
        return lang;
    }
}
