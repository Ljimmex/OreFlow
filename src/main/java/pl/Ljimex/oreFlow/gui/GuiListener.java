package pl.Ljimex.oreFlow.gui;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import pl.Ljimex.oreFlow.OreFlow;
import pl.Ljimex.oreFlow.config.GuiConfigManager.ButtonConfig;

public class GuiListener implements Listener {

    private final OreFlow plugin;
    private final NamespacedKey dropKey;
    private final NamespacedKey adminDropKey;
    private final PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
    private final ConcurrentHashMap<UUID, Integer> playerPage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> adminPage = new ConcurrentHashMap<>();

    public GuiListener(OreFlow plugin) {
        this.plugin = plugin;
        this.dropKey = new NamespacedKey(plugin, "drop_key");
        this.adminDropKey = new NamespacedKey(plugin, "admin_drop_key");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = plainSerializer.serialize(event.getView().title());
        String dropTitle = plainSerializer.serialize(plugin.getGuiConfigManager().getGuiTitleComponent());
        String adminTitle = plainSerializer.serialize(plugin.getGuiManager().getAdminGuiTitleComponent());

        if (title.equals(dropTitle)) {
            event.setCancelled(true);
            handleDropGuiClick(player, event);
        } else if (title.equals(adminTitle)) {
            event.setCancelled(true);
            handleAdminGuiClick(player, event);
        }
    }

    private void handleDropGuiClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        int currentPage = playerPage.getOrDefault(player.getUniqueId(), 0);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || clicked.getItemMeta() == null) {
            // Still allow page buttons even if item is null? No, page buttons are items.
            return;
        }

        ClickType clickType = event.getClick();

        // Page buttons
        ButtonConfig prevButton = plugin.getGuiConfigManager().getButton("previous_page");
        if (prevButton != null && slot == prevButton.slot) {
            if (currentPage > 0) {
                playerPage.put(player.getUniqueId(), currentPage - 1);
                refreshDropGui(player, currentPage - 1);
            }
            return;
        }

        ButtonConfig nextButton = plugin.getGuiConfigManager().getButton("next_page");
        if (nextButton != null && slot == nextButton.slot) {
            int totalPages = plugin.getGuiManager().getTotalPages();
            if (currentPage < totalPages - 1) {
                playerPage.put(player.getUniqueId(), currentPage + 1);
                refreshDropGui(player, currentPage + 1);
            }
            return;
        }

        // Buttons
        ButtonConfig cobbleButton = plugin.getGuiConfigManager().getButton("cobble");
        if (cobbleButton != null && slot == cobbleButton.slot) {
            boolean newState = plugin.getPlayerSettingsManager().toggleCobble(player.getUniqueId());
            player.sendMessage(newState
                    ? plugin.getMessageManager().getMessage("commands.cobble-enabled")
                    : plugin.getMessageManager().getMessage("commands.cobble-disabled"));
            refreshDropGui(player, currentPage);
            return;
        }

        ButtonConfig destinationButton = plugin.getGuiConfigManager().getButton("drop_destination");
        if (destinationButton != null && slot == destinationButton.slot) {
            boolean newState = plugin.getPlayerSettingsManager().toggleDropToInventory(player.getUniqueId());
            player.sendMessage(newState
                    ? plugin.getMessageManager().getMessage("commands.drop-destination-inventory",
                            "status", plugin.getMessageManager().getMessage("status.inventory"))
                    : plugin.getMessageManager().getMessage("commands.drop-destination-ground",
                            "status", plugin.getMessageManager().getMessage("status.ground")));
            refreshDropGui(player, currentPage);
            return;
        }

        ButtonConfig expButton = plugin.getGuiConfigManager().getButton("exp");
        if (expButton != null && slot == expButton.slot) {
            boolean newState = plugin.getPlayerSettingsManager().toggleExpEnabled(player.getUniqueId());
            player.sendMessage(newState
                    ? plugin.getMessageManager().getMessage("commands.exp-enabled")
                    : plugin.getMessageManager().getMessage("commands.exp-disabled"));
            refreshDropGui(player, currentPage);
            return;
        }

        ButtonConfig closeButton = plugin.getGuiConfigManager().getButton("close");
        if (closeButton != null && slot == closeButton.slot) {
            player.closeInventory();
            return;
        }

        // Drops
        String dropKeyValue = clicked.getItemMeta().getPersistentDataContainer().get(this.dropKey, PersistentDataType.STRING);
        if (dropKeyValue != null && plugin.getConfigManager().getDrops().contains(dropKeyValue)) {
            if (clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT) {
                boolean enabled = plugin.getPlayerSettingsManager().toggleDropMessage(player.getUniqueId(), dropKeyValue);
                player.sendMessage(enabled
                        ? plugin.getMessageManager().getMessage("gui-messages.drop-message-enabled", "drop", dropKeyValue)
                        : plugin.getMessageManager().getMessage("gui-messages.drop-message-disabled", "drop", dropKeyValue));
            } else {
                boolean enabled = plugin.getPlayerSettingsManager().toggleDrop(player.getUniqueId(), dropKeyValue);
                player.sendMessage(enabled
                        ? plugin.getMessageManager().getMessage("commands.drop-toggled-on", "drop", dropKeyValue)
                        : plugin.getMessageManager().getMessage("commands.drop-toggled-off", "drop", dropKeyValue));
            }
            refreshDropGui(player, currentPage);
        }
    }

    private void handleAdminGuiClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        int currentPage = adminPage.getOrDefault(player.getUniqueId(), 0);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || clicked.getItemMeta() == null) {
            return;
        }

        // Admin page buttons (reuse same config slots)
        ButtonConfig prevButton = plugin.getGuiConfigManager().getButton("previous_page");
        if (prevButton != null && slot == prevButton.slot) {
            if (currentPage > 0) {
                adminPage.put(player.getUniqueId(), currentPage - 1);
                refreshAdminGui(player, currentPage - 1);
            }
            return;
        }

        ButtonConfig nextButton = plugin.getGuiConfigManager().getButton("next_page");
        if (nextButton != null && slot == nextButton.slot) {
            int totalPages = plugin.getGuiManager().getAdminTotalPages();
            if (currentPage < totalPages - 1) {
                adminPage.put(player.getUniqueId(), currentPage + 1);
                refreshAdminGui(player, currentPage + 1);
            }
            return;
        }

        ButtonConfig closeButton = plugin.getGuiConfigManager().getButton("close");
        if (closeButton != null && slot == closeButton.slot) {
            player.closeInventory();
            return;
        }

        // Drops - toggle global enabled in drops.yml
        String dropKeyValue = clicked.getItemMeta().getPersistentDataContainer().get(this.adminDropKey, PersistentDataType.STRING);
        if (dropKeyValue != null && plugin.getConfigManager().getDrops().contains(dropKeyValue)) {
            var drops = plugin.getConfigManager().getDrops();
            boolean enabled = drops.getBoolean(dropKeyValue + ".enabled", true);
            drops.set(dropKeyValue + ".enabled", !enabled);
            plugin.getConfigManager().saveConfigs();
            player.sendMessage(enabled
                    ? plugin.getMessageManager().getMessage("commands.drop-toggled-off", "drop", dropKeyValue)
                    : plugin.getMessageManager().getMessage("commands.drop-toggled-on", "drop", dropKeyValue));
            refreshAdminGui(player, currentPage);
        }
    }

    private void refreshDropGui(Player player, int page) {
        player.openInventory(plugin.getGuiManager().createDropGui(player, page));
    }

    private void refreshAdminGui(Player player, int page) {
        player.openInventory(plugin.getGuiManager().createAdminGui(player, page));
    }

    public void setPlayerPage(Player player, int page) {
        playerPage.put(player.getUniqueId(), page);
    }

    public void setAdminPage(Player player, int page) {
        adminPage.put(player.getUniqueId(), page);
    }
}
