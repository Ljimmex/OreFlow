package pl.Ljimex.oreFlow.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.configuration.file.FileConfiguration;

import pl.Ljimex.oreFlow.OreFlow;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager wiadomości. Wszystkie teksty są parsowane jako MiniMessage.
 * Zwraca gotowe Adventure Component.
 */
public class MessageManager {

    private final OreFlow plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MessageManager(OreFlow plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        // ConfigManager przeładowuje lang; ten manager nie cache'uje,
        // więc wystarczy, że odświeżymy referencję.
    }

    private FileConfiguration getLang() {
        return plugin.getConfigManager().getLang();
    }

    /**
     * Zwraca wiadomość z lang.yml z podstawionymi zmiennymi jako Component.
     */
    public Component getMessage(String path, Map<String, String> placeholders) {
        String message = getLang().getString(path, path);
        if (message == null) {
            return Component.text(path);
        }
        return deserialize(replacePlaceholders(message, placeholders));
    }

    public Component getMessage(String path) {
        return getMessage(path, new HashMap<>());
    }

    public Component getMessage(String path, String... placeholders) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            map.put(placeholders[i], placeholders[i + 1]);
        }
        return getMessage(path, map);
    }

    /**
     * Zwraca wiadomość Action Bar z lang.yml.
     */
    public Component getActionBarMessage(String key, Map<String, String> placeholders) {
        String path = "actionbar." + key + ".message";
        String message = getLang().getString(path, path);
        if (message == null) {
            return Component.text(path);
        }
        return deserialize(replacePlaceholders(message, placeholders));
    }

    public Component getActionBarMessage(String key, String... placeholders) {
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
     * Pobiera dowolną wartość tekstową z lang.yml z podstawionymi zmiennymi jako Component.
     */
    public Component getRaw(String path, Map<String, String> placeholders) {
        return deserialize(getRawString(path, placeholders));
    }

    /**
     * Pobiera surowy tekst z lang.yml (bez parsowania MiniMessage).
     */
    public String getRawString(String path) {
        return getLang().getString(path, path);
    }

    /**
     * Pobiera surowy tekst z lang.yml z podstawionymi zmiennymi (bez parsowania MiniMessage).
     */
    public String getRawString(String path, Map<String, String> placeholders) {
        String message = getLang().getString(path, path);
        if (message == null) {
            return path;
        }
        return replacePlaceholders(message, placeholders);
    }

    /**
     * Parsuje tekst MiniMessage na Component.
     */
    public Component deserialize(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return miniMessage.deserialize(text);
    }

    /**
     * Parsuje tekst MiniMessage z jednym placeholderem.
     */
    public Component deserialize(String text, String placeholder, String value) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return miniMessage.deserialize(text, Placeholder.unparsed(placeholder, value != null ? value : ""));
    }

    /**
     * Zamienia proste zmienne {key} na wartości.
     */
    private String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (message == null) {
            return "";
        }
        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace("{" + entry.getKey() + "}", value);
        }
        return result;
    }

    /**
     * Wysyla wiadomosc do gracza (lub CommandSender) bezposrednio.
     */
    public void send(org.bukkit.command.CommandSender sender, String path, Map<String, String> placeholders) {
        if (sender == null) {
            return;
        }
        sender.sendMessage(getMessage(path, placeholders));
    }

    public void send(org.bukkit.command.CommandSender sender, String path) {
        send(sender, path, new HashMap<>());
    }

    public void send(org.bukkit.command.CommandSender sender, String path, String... placeholders) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            map.put(placeholders[i], placeholders[i + 1]);
        }
        send(sender, path, map);
    }

    /**
     * Konwertuje mapę placeholderów na TagResolver[] dla MiniMessage.
     */
    public TagResolver[] toResolvers(Map<String, String> placeholders) {
        TagResolver[] resolvers = new TagResolver[placeholders.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolvers[i++] = Placeholder.unparsed(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
        }
        return resolvers;
    }
}
