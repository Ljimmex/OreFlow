package pl.Ljimex.oreFlow.config;

import pl.Ljimex.oreFlow.OreFlow;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerSettingsManager {

    private final OreFlow plugin;
    private final File settingsFile;
    private FileConfiguration settings;

    // Cache ustawien w pamieci
    private final Map<UUID, PlayerSettings> cache = new HashMap<>();

    private boolean defaultCobble;
    private boolean defaultDropToInventory;
    private boolean defaultExp;

    public PlayerSettingsManager(OreFlow plugin) {
        this.plugin = plugin;
        this.settingsFile = new File(plugin.getDataFolder(), "players.yml");
    }

    public void load() {
        if (!settingsFile.exists()) {
            try {
                settingsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create players.yml", e);
            }
        }
        settings = YamlConfiguration.loadConfiguration(settingsFile);
        reloadDefaults();
    }

    /**
     * Przeładowuje domyślne wartości z configu bez utraty cache graczy.
     */
    public void reload() {
        load();
    }

    private void reloadDefaults() {
        this.defaultCobble = plugin.getConfigManager().getConfig()
                .getBoolean("settings.cobblestone-enabled", false);
        this.defaultDropToInventory = plugin.getConfigManager().getConfig()
                .getBoolean("settings.drop-to-inventory", true);
        this.defaultExp = plugin.getConfigManager().getConfig()
                .getBoolean("settings.exp-enabled", true);
    }

    public void save() {
        if (settings == null) {
            return;
        }

        // Zapisz wszystkie ustawienia z cache do pliku
        for (Map.Entry<UUID, PlayerSettings> entry : cache.entrySet()) {
            saveToConfig(entry.getKey(), entry.getValue());
        }

        try {
            settings.save(settingsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save players.yml", e);
        }
    }

    /**
     * Zapisuje pojedynczego gracza i usuwa go z cache.
     */
    public void saveAndEvict(UUID uuid) {
        PlayerSettings playerSettings = cache.get(uuid);
        if (playerSettings == null) {
            return;
        }
        saveToConfig(uuid, playerSettings);
        cache.remove(uuid);
        flush();
    }

    /**
     * Zapisuje pojedynczego gracza, ale pozostawia w cache.
     */
    public void save(UUID uuid) {
        PlayerSettings playerSettings = cache.get(uuid);
        if (playerSettings == null) {
            return;
        }
        saveToConfig(uuid, playerSettings);
        flush();
    }

    private void saveToConfig(UUID uuid, PlayerSettings playerSettings) {
        String path = uuid.toString();
        settings.set(path + ".cobble-enabled", playerSettings.isCobbleEnabled());
        settings.set(path + ".drop-to-inventory", playerSettings.isDropToInventory());
        settings.set(path + ".exp-enabled", playerSettings.isExpEnabled());
        settings.set(path + ".disabled-drops", playerSettings.getDisabledDrops().isEmpty()
                ? null : playerSettings.getDisabledDrops().stream().toList());
        settings.set(path + ".disabled-drop-messages", playerSettings.getDisabledDropMessages().isEmpty()
                ? null : playerSettings.getDisabledDropMessages().stream().toList());
    }

    private void flush() {
        try {
            settings.save(settingsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save players.yml", e);
        }
    }

    // Cobblestone
    public boolean isCobbleEnabled(UUID uuid) {
        return getSettings(uuid).isCobbleEnabled();
    }

    public void setCobbleEnabled(UUID uuid, boolean enabled) {
        getSettings(uuid).setCobbleEnabled(enabled);
        save(uuid);
    }

    public boolean toggleCobble(UUID uuid) {
        PlayerSettings playerSettings = getSettings(uuid);
        boolean newValue = !playerSettings.isCobbleEnabled();
        playerSettings.setCobbleEnabled(newValue);
        save(uuid);
        return newValue;
    }

    // Drop destination
    public boolean isDropToInventory(UUID uuid) {
        return getSettings(uuid).isDropToInventory();
    }

    public void setDropToInventory(UUID uuid, boolean enabled) {
        getSettings(uuid).setDropToInventory(enabled);
        save(uuid);
    }

    public boolean toggleDropToInventory(UUID uuid) {
        PlayerSettings playerSettings = getSettings(uuid);
        boolean newValue = !playerSettings.isDropToInventory();
        playerSettings.setDropToInventory(newValue);
        save(uuid);
        return newValue;
    }

    // EXP
    public boolean isExpEnabled(UUID uuid) {
        return getSettings(uuid).isExpEnabled();
    }

    public void setExpEnabled(UUID uuid, boolean enabled) {
        getSettings(uuid).setExpEnabled(enabled);
        save(uuid);
    }

    public boolean toggleExpEnabled(UUID uuid) {
        PlayerSettings playerSettings = getSettings(uuid);
        boolean newValue = !playerSettings.isExpEnabled();
        playerSettings.setExpEnabled(newValue);
        save(uuid);
        return newValue;
    }

    // Per-drop toggles
    public boolean isDropEnabled(UUID uuid, String dropKey) {
        return getSettings(uuid).isDropEnabled(dropKey);
    }

    public boolean toggleDrop(UUID uuid, String dropKey) {
        boolean result = getSettings(uuid).toggleDrop(dropKey);
        save(uuid);
        return result;
    }

    // Per-drop message toggles
    public boolean isDropMessageEnabled(UUID uuid, String dropKey) {
        return getSettings(uuid).isDropMessageEnabled(dropKey);
    }

    public boolean toggleDropMessage(UUID uuid, String dropKey) {
        boolean result = getSettings(uuid).toggleDropMessage(dropKey);
        save(uuid);
        return result;
    }

    public void evict(UUID uuid) {
        cache.remove(uuid);
    }

    private PlayerSettings getSettings(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadSettings);
    }

    private PlayerSettings loadSettings(UUID uuid) {
        PlayerSettings playerSettings = new PlayerSettings(defaultCobble, defaultDropToInventory, defaultExp);

        if (settings != null && settings.contains(uuid.toString())) {
            String path = uuid.toString();
            playerSettings.setCobbleEnabled(settings.getBoolean(path + ".cobble-enabled", defaultCobble));
            playerSettings.setDropToInventory(settings.getBoolean(path + ".drop-to-inventory", defaultDropToInventory));
            playerSettings.setExpEnabled(settings.getBoolean(path + ".exp-enabled", defaultExp));

            if (settings.contains(path + ".disabled-drops")) {
                playerSettings.getDisabledDrops().addAll(settings.getStringList(path + ".disabled-drops"));
            }
            if (settings.contains(path + ".disabled-drop-messages")) {
                playerSettings.getDisabledDropMessages().addAll(settings.getStringList(path + ".disabled-drop-messages"));
            }
        }

        return playerSettings;
    }

    private static class PlayerSettings {
        private boolean cobbleEnabled;
        private boolean dropToInventory;
        private boolean expEnabled;
        private final Set<String> disabledDrops = new HashSet<>();
        private final Set<String> disabledDropMessages = new HashSet<>();

        public PlayerSettings(boolean cobbleEnabled, boolean dropToInventory, boolean expEnabled) {
            this.cobbleEnabled = cobbleEnabled;
            this.dropToInventory = dropToInventory;
            this.expEnabled = expEnabled;
        }

        public boolean isCobbleEnabled() {
            return cobbleEnabled;
        }

        public void setCobbleEnabled(boolean cobbleEnabled) {
            this.cobbleEnabled = cobbleEnabled;
        }

        public boolean isDropToInventory() {
            return dropToInventory;
        }

        public void setDropToInventory(boolean dropToInventory) {
            this.dropToInventory = dropToInventory;
        }

        public boolean isExpEnabled() {
            return expEnabled;
        }

        public void setExpEnabled(boolean expEnabled) {
            this.expEnabled = expEnabled;
        }

        public Set<String> getDisabledDrops() {
            return disabledDrops;
        }

        public boolean isDropEnabled(String dropKey) {
            return !disabledDrops.contains(dropKey.toLowerCase());
        }

        public boolean toggleDrop(String dropKey) {
            String key = dropKey.toLowerCase();
            if (disabledDrops.contains(key)) {
                disabledDrops.remove(key);
                return true;
            } else {
                disabledDrops.add(key);
                return false;
            }
        }

        public Set<String> getDisabledDropMessages() {
            return disabledDropMessages;
        }

        public boolean isDropMessageEnabled(String dropKey) {
            return !disabledDropMessages.contains(dropKey.toLowerCase());
        }

        public boolean toggleDropMessage(String dropKey) {
            String key = dropKey.toLowerCase();
            if (disabledDropMessages.contains(key)) {
                disabledDropMessages.remove(key);
                return true;
            } else {
                disabledDropMessages.add(key);
                return false;
            }
        }
    }
}
