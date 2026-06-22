package pl.Ljimex.oreFlow.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import pl.Ljimex.oreFlow.OreFlow;

public class GuiListener implements Listener {

    private final OreFlow plugin;

    public GuiListener(OreFlow plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();
        if (!title.equals(colorize("&8&lZarzadzaj dropami ze stone"))) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        String displayName = clicked.getItemMeta().getDisplayName();

        // Zamkniecie GUI
        if (displayName.equals(colorize("&cZamknij"))) {
            player.closeInventory();
            return;
        }

        // W przyszlej wersji: LPM = szczegoly, PPM = wlacz/wylacz
        // Na razie tylko informacja
        player.sendMessage(colorize("&eSzczegoly dropu: " + displayName));
        player.sendMessage(colorize("&7Funkcja wlaczania/wylaczania dropow bedzie dostepna w przyszlej aktualizacji."));
    }

    private String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }
}
