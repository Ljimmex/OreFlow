package pl.Ljimex.oreFlow.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import pl.Ljimex.oreFlow.OreFlow;

public class ActionBarUtil {

    private final OreFlow plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ActionBarUtil(OreFlow plugin) {
        this.plugin = plugin;
    }

    /**
     * Wysyla wiadomosc na Action Bar przy uzyciu MiniMessage formatu.
     * Domyslnie wyswietla sie przez standardowy czas (1 sekunde).
     */
    public void send(Player player, String message) {
        send(player, message, 20);
    }

    /**
     * Wysyla wiadomosc na Action Bar na okreslona liczbe tickow.
     * Wysyla co 20 tickow, aby utrzymac wiadomosc widoczna przez zadany czas.
     */
    public void send(Player player, String message, int durationTicks) {
        if (message == null || message.isEmpty()) {
            return;
        }

        Component component;
        try {
            component = miniMessage.deserialize(message);
        } catch (Exception e) {
            component = Component.text(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
        }

        final Component finalComponent = component;
        final int repeats = Math.max(1, (int) Math.ceil(durationTicks / 20.0));

        new BukkitRunnable() {
            private int count = 0;

            @Override
            public void run() {
                if (!player.isOnline() || count >= repeats) {
                    cancel();
                    return;
                }
                player.sendActionBar(finalComponent);
                count++;
            }
        }.runTaskTimer(plugin, 0L, 20L);
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
