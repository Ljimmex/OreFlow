package pl.Ljimex.oreFlow.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import pl.Ljimex.oreFlow.OreFlow;

public class ActionBarUtil {

    private final OreFlow plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ActionBarUtil(OreFlow plugin) {
        this.plugin = plugin;
    }

    /**
     * Wysyla wiadomosc na Action Bar przy uzyciu MiniMessage formatu.
     * Wspiera gradienty, kolory hex, bold, italic itp.
     *
     * Przyklady:
     * - "<#FF5555>Czerwony tekst"
     * - "<gradient:#00FF88:#55FFFF>Tekst gradientowy</gradient>"
     * - "<bold><#FFAA00>Pogrubiony</#FFAA00></bold>"
     */
    public void send(Player player, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        try {
            Component component = miniMessage.deserialize(message);
            player.sendActionBar(component);
        } catch (Exception e) {
            // Fallback na zwykly tekst jesli MiniMessage nie moze sparsowac
            player.sendActionBar(Component.text(org.bukkit.ChatColor.translateAlternateColorCodes('&', message)));
        }
    }

    /**
     * Wysyla wiadomosc na Action Bar w formacie legacy (&kolor).
     * Uzyj send() jesli chcesz gradienty i efekty MiniMessage.
     */
    public void sendLegacy(Player player, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        String colored = org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
        player.sendActionBar(Component.text(colored));
    }
}
