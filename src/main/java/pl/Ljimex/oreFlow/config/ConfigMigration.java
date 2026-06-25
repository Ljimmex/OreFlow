package pl.Ljimex.oreFlow.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import pl.Ljimex.oreFlow.OreFlow;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Centralny system migracji configów.
 * Rejestruje migracje per plik i wykonuje je krokowo od obecnej wersji do docelowej.
 */
public class ConfigMigration {

    private final OreFlow plugin;
    private final Map<String, Integer> targetVersions = new HashMap<>();
    private final Map<String, Map<Integer, Consumer<FileConfiguration>>> migrations = new HashMap<>();

    public ConfigMigration(OreFlow plugin) {
        this.plugin = plugin;
        registerDefaultMigrations();
    }

    /**
     * Zwraca docelową wersję dla pliku z JAR.
     */
    public int getTargetVersion(String resourceName) {
        return targetVersions.getOrDefault(resourceName, 1);
    }

    /**
     * Wykonuje migrację configu w pamięci. Nie zapisuje pliku.
     *
     * @param config         załadowany config
     * @param resourceName   nazwa zasobu w JAR (np. "config.yml")
     * @return true jeśli config wymaga zapisu (była migracja lub dodano brakujące klucze)
     */
    public boolean migrate(FileConfiguration config, String resourceName) {
        int currentVersion = config.getInt("config-version", 0);
        int targetVersion = getTargetVersion(resourceName);

        if (currentVersion >= targetVersion) {
            return false;
        }

        plugin.getLogger().info("Migrating " + resourceName + " from version " + currentVersion + " to " + targetVersion);

        Map<Integer, Consumer<FileConfiguration>> fileMigrations = migrations.getOrDefault(resourceName, new HashMap<>());
        for (int version = currentVersion + 1; version <= targetVersion; version++) {
            Consumer<FileConfiguration> migration = fileMigrations.get(version);
            if (migration != null) {
                try {
                    migration.accept(config);
                    plugin.getLogger().info("Applied migration " + resourceName + " v" + version);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to apply migration " + resourceName + " v" + version, e);
                }
            }
        }

        config.set("config-version", targetVersion);
        return true;
    }

    /**
     * Ładuje config z pliku, wykonuje migrację i zapisuje jeśli była potrzebna.
     */
    public FileConfiguration loadAndMigrate(File file, String resourceName) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        boolean migrated = migrate(config, resourceName);

        // Ustawienie wartości domyślnych z JAR
        loadDefaults(config, resourceName);

        if (migrated) {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save migrated config: " + file.getName(), e);
            }
        }

        return config;
    }

    /**
     * Ładuje wartości domyślne z zasobu JAR i ustawia je jako defaults dla configu.
     */
    private void loadDefaults(FileConfiguration config, String resourceName) {
        try (InputStream defaultStream = plugin.getResource(resourceName)) {
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                config.setDefaults(defaultConfig);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load default config resource: " + resourceName, e);
        }
    }

    /**
     * Rejestruje migrację dla konkretnego pliku i wersji docelowej.
     */
    private void registerMigration(String resourceName, int targetVersion, Consumer<FileConfiguration> migration) {
        targetVersions.put(resourceName, targetVersion);
        migrations.computeIfAbsent(resourceName, k -> new HashMap<>()).put(targetVersion, migration);
    }

    private void copyMissingSectionsFromResource(String resourceName, FileConfiguration config, String... paths) {
        try (InputStream defaultStream = plugin.getResource(resourceName)) {
            if (defaultStream == null) {
                return;
            }
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            for (String path : paths) {
                if (!config.contains(path) && defaultConfig.contains(path)) {
                    config.set(path, defaultConfig.get(path));
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not copy missing sections from " + resourceName, e);
        }
    }

    private void registerDefaultMigrations() {
        // config.yml v1: dodanie config-version i nowych kluczy
        registerMigration("config.yml", 1, config -> {
            config.set("config-version", 1);
            if (!config.contains("settings.base-exp")) {
                config.set("settings.base-exp", 1);
            }
            if (!config.contains("settings.exp-multiplier")) {
                config.set("settings.exp-multiplier", 1.0);
            }
            if (!config.contains("settings.exp-mode")) {
                config.set("settings.exp-mode", "direct-give");
            }
        });

        // drops.yml v1: dodanie config-version
        registerMigration("drops.yml", 1, config -> {
            config.set("config-version", 1);
        });

        // generators.yml v1: dodanie config-version
        registerMigration("generators.yml", 1, config -> {
            config.set("config-version", 1);
        });

        // gui.yml v1: dodanie config-version
        registerMigration("gui.yml", 1, config -> {
            config.set("config-version", 1);
        });

        // gui.yml v2: dodanie sekcji CobbleX GUI
        registerMigration("gui.yml", 2, config -> {
            config.set("config-version", 2);
            copyMissingSectionsFromResource("gui.yml", config,
                    "cobblex-menu", "cobblex-craft", "cobblex-drop");
        });

        // lang files v1: dodanie config-version
        registerMigration("lang/pl.yml", 1, config -> config.set("config-version", 1));
        registerMigration("lang/en.yml", 1, config -> config.set("config-version", 1));
        registerMigration("lang/de.yml", 1, config -> config.set("config-version", 1));
    }
}
