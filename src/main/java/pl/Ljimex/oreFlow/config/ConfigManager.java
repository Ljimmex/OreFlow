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
        langFile = new File(new File(plugin.getDataFolder(), "lang"), "pl.yml");

        createDefaultFile(configFile, "config.yml");
        createDefaultFile(dropsFile, "drops.yml");
        createDefaultFile(generatorsFile, "generators.yml");
        createDefaultLangFiles();

        reloadConfigs();
    }

    private void createDefaultLangFiles() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        createDefaultFile(new File(langDir, "pl.yml"), "lang/pl.yml");
        createDefaultFile(new File(langDir, "en.yml"), "lang/en.yml");
        createDefaultFile(new File(langDir, "de.yml"), "lang/de.yml");
    }

    public void reloadConfigs() {
        config = YamlConfiguration.loadConfiguration(configFile);
        drops = YamlConfiguration.loadConfiguration(dropsFile);
        generators = YamlConfiguration.loadConfiguration(generatorsFile);

        loadDefaultsFromResources(config, configFile, "config.yml");
        loadDefaultsFromResources(drops, dropsFile, "drops.yml");
        loadDefaultsFromResources(generators, generatorsFile, "generators.yml");

        // Language-specific lang file from lang/ folder
        String language = config.getString("settings.language", "pl");
        File localizedLangFile = new File(new File(plugin.getDataFolder(), "lang"), language + ".yml");
        if (localizedLangFile.exists()) {
            langFile = localizedLangFile;
        } else {
            langFile = new File(new File(plugin.getDataFolder(), "lang"), "pl.yml");
        }
        lang = YamlConfiguration.loadConfiguration(langFile);
        loadDefaultsFromResources(lang, langFile, "lang/" + language + ".yml");
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
