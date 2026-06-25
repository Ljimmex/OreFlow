package pl.Ljimex.oreFlow.listener;

import pl.Ljimex.oreFlow.OreFlow;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final OreFlow plugin;

    public PlayerListener(OreFlow plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getPlayerSettingsManager().saveAndEvict(event.getPlayer().getUniqueId());
    }
}
