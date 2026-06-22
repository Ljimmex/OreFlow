package pl.Ljimex.oreFlow.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import pl.Ljimex.oreFlow.OreFlow;

import java.util.HashMap;
import java.util.Map;

public class MessageManager {

    private final OreFlow plugin;

    public MessageManager(OreFlow plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration getLang() {
        return plugin.getConfigManager().getLang();
    }

    /**
     * Zwraca sformatowaną wiadomość z lang.yml i podstawia zmienne w nawiasach klamrowych.
     * Obsługuje legacy kolory (&).
     */
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getLang().getString(path, path);
        return replacePlaceholders(colorize(message), placeholders);
    }

    public String getMessage(String path) {
        return getMessage(path, new HashMap<>());
    }

    public String getMessage(String path, String... placeholders) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            map.put(placeholders[i], placeholders[i + 1]);
        }
        return getMessage(path, map);
    }

    /**
     * Zwraca wiadomość Action Bar z lang.yml (bez colorize, MiniMessage obsługuje to w ActionBarUtil).
     */
    public String getActionBarMessage(String key, Map<String, String> placeholders) {
        String path = "actionbar." + key + ".message";
        String message = getLang().getString(path, path);
        return replacePlaceholders(message, placeholders);
    }

    public String getActionBarMessage(String key, String... placeholders) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            map.put(placeholders[i], placeholders[i + 1]);
        }
        return getActionBarMessage(key, map);
    }

    public boolean isActionBarEnabled(String key) {
        return getLang().getBoolean("actionbar." + key + ".enabled", true);
    }

    public int getActionBarDuration(String key) {
        return getLang().getInt("actionbar." + key + ".duration", 40);
    }

    /**
     * Pobiera dowolną wartość tekstową z lang.yml z podstawionymi zmiennymi.
     */
    public String getRaw(String path, Map<String, String> placeholders) {
        String message = getLang().getString(path, path);
        return replacePlaceholders(message, placeholders);
    }

    private String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (message == null) {
            return "";
        }
        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    private String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }
}
