package pl.Ljimex.oreFlow.config;

import pl.Ljimex.oreFlow.OreFlow;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

public class ConfigManager {

    private final OreFlow plugin;
    private final ConfigMigration migration;

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
        this.migration = new ConfigMigration(plugin);
    }

    /**
     * Pierwsze ładowanie configów przy starcie pluginu.
     */
    public void loadConfigs() {
        plugin.saveDefaultConfig();

        configFile = new File(plugin.getDataFolder(), "config.yml");
        dropsFile = new File(plugin.getDataFolder(), "drops.yml");
        generatorsFile = new File(plugin.getDataFolder(), "generators.yml");

        createDefaultFile(configFile, "config.yml");
        createDefaultFile(dropsFile, "drops.yml");
        createDefaultFile(generatorsFile, "generators.yml");
        createDefaultLangFiles();

        reloadConfigs();
    }

    /**
     * Przeładowanie configów z dysku. Wykonuje migracje i walidację.
     */
    public void reloadConfigs() {
        config = migration.loadAndMigrate(configFile, "config.yml");
        drops = migration.loadAndMigrate(dropsFile, "drops.yml");
        generators = migration.loadAndMigrate(generatorsFile, "generators.yml");

        String language = config.getString("settings.language", "pl");
        File localizedLangFile = new File(new File(plugin.getDataFolder(), "lang"), language + ".yml");
        if (!localizedLangFile.exists()) {
            plugin.getLogger().warning("Language file " + language + ".yml not found, falling back to pl.yml");
            localizedLangFile = new File(new File(plugin.getDataFolder(), "lang"), "pl.yml");
        }
        langFile = localizedLangFile;
        lang = migration.loadAndMigrate(langFile, "lang/" + language + ".yml");

        validate();
    }

    /**
     * Zapisuje wszystkie załadowane configi na dysk.
     */
    public void saveConfigs() {
        saveConfig(config, configFile);
        saveConfig(drops, dropsFile);
        saveConfig(generators, generatorsFile);
        saveConfig(lang, langFile);
    }

    /**
     * Zapisuje konkretny config.
     */
    public void saveConfig(FileConfiguration configuration, File file) {
        if (configuration == null || file == null) {
            return;
        }
        try {
            configuration.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config file: " + file.getName(), e);
        }
    }

    /**
     * Walidacja najważniejszych wartości w configach.
     */
    public void validate() {
        // Walidacja materiałów w mineable-blocks
        List<String> mineableBlocks = config.getStringList("settings.mineable-blocks");
        for (String materialName : mineableBlocks) {
            validateMaterial("settings.mineable-blocks", materialName);
        }

        // Walidacja max-drops-per-block
        int maxDrops = config.getInt("settings.max-drops-per-block", 0);
        if (maxDrops < 0) {
            plugin.getLogger().warning("settings.max-drops-per-block cannot be negative, using 0");
            config.set("settings.max-drops-per-block", 0);
        }

        // Walidacja exp-multiplier
        double expMultiplier = config.getDouble("settings.exp-multiplier", 1.0);
        if (expMultiplier < 0) {
            plugin.getLogger().warning("settings.exp-multiplier cannot be negative, using 1.0");
            config.set("settings.exp-multiplier", 1.0);
        }

        // Walidacja base-exp
        int baseExp = config.getInt("settings.base-exp", 1);
        if (baseExp < 0) {
            plugin.getLogger().warning("settings.base-exp cannot be negative, using 0");
            config.set("settings.base-exp", 0);
        }

        // Walidacja exp-mode
        String expMode = config.getString("settings.exp-mode", "direct-give");
        if (!"direct-give".equalsIgnoreCase(expMode) && !"orb-spawn".equalsIgnoreCase(expMode)) {
            plugin.getLogger().warning("settings.exp-mode must be 'direct-give' or 'orb-spawn', using 'direct-give'");
            config.set("settings.exp-mode", "direct-give");
        }

        // Walidacja języka
        String language = config.getString("settings.language", "pl");
        if (!List.of("pl", "en", "de").contains(language.toLowerCase())) {
            plugin.getLogger().warning("Invalid language '" + language + "', falling back to pl");
            config.set("settings.language", "pl");
        }
    }

    /**
     * Sprawdza czy podana nazwa materiału jest poprawna.
     */
    public boolean validateMaterial(String path, String materialName) {
        if (materialName == null || materialName.isBlank()) {
            plugin.getLogger().warning("Empty material name at " + path);
            return false;
        }
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            plugin.getLogger().warning("Unknown material '" + materialName + "' at " + path);
            return false;
        }
        return true;
    }

    private void createDefaultFile(File file, String resourceName) {
        if (!file.exists()) {
            plugin.saveResource(resourceName, false);
        }
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

    public File getConfigFile() {
        return configFile;
    }

    public ConfigMigration getMigration() {
        return migration;
    }
}
