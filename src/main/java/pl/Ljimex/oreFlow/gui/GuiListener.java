package pl.Ljimex.oreFlow.gui;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import pl.Ljimex.oreFlow.OreFlow;
import pl.Ljimex.oreFlow.config.GuiConfigManager.ButtonConfig;
import pl.Ljimex.oreFlow.generator.StoneGeneratorManager;

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

        if (title.equals(plainSerializer.serialize(plugin.getGuiConfigManager().getMainMenuTitleComponent()))) {
            event.setCancelled(true);
            handleMainMenuClick(player, event);
        } else if (title.equals(plainSerializer.serialize(plugin.getGuiConfigManager().getGuiTitleComponent()))) {
            event.setCancelled(true);
            handleDropGuiClick(player, event);
        } else if (title.equals(plainSerializer.serialize(plugin.getGuiManager().getAdminGuiTitleComponent()))) {
            event.setCancelled(true);
            handleAdminGuiClick(player, event);
        } else if (title.equals(plainSerializer.serialize(plugin.getGuiConfigManager().getStoneGeneratorTitleComponent()))) {
            event.setCancelled(true);
            handleStoneGeneratorGuiClick(player, event);
        }
    }

    private void handleMainMenuClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || clicked.getItemMeta() == null) {
            return;
        }

        ButtonConfig dropsButton = plugin.getGuiConfigManager().getMainMenuButton("open_drops");
        if (dropsButton != null && slot == dropsButton.slot) {
            setPlayerPage(player, 0);
            player.openInventory(plugin.getGuiManager().createDropGui(player, 0));
            return;
        }

        ButtonConfig generatorButton = plugin.getGuiConfigManager().getMainMenuButton("open_stone_generator");
        if (generatorButton != null && slot == generatorButton.slot) {
            player.openInventory(plugin.getGuiManager().createStoneGeneratorGui(player));
            return;
        }

        ButtonConfig cobblexButton = plugin.getGuiConfigManager().getMainMenuButton("open_cobblex");
        if (cobblexButton != null && slot == cobblexButton.slot) {
            if (!player.hasPermission("cobblex.use")) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission",
                        "permission", "cobblex.use"));
                return;
            }
            craftCobbleX(player);
            player.closeInventory();
            return;
        }

        ButtonConfig closeButton = plugin.getGuiConfigManager().getMainMenuButton("close");
        if (closeButton != null && slot == closeButton.slot) {
            player.closeInventory();
        }
    }

    private void handleDropGuiClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        int currentPage = playerPage.getOrDefault(player.getUniqueId(), 0);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || clicked.getItemMeta() == null) {
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
                    ? plugin.getMessageManager().getMessage("commands.drop-destination-inventory")
                    : plugin.getMessageManager().getMessage("commands.drop-destination-ground"));
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

    private void handleStoneGeneratorGuiClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();

        // Back button - return to main menu
        ButtonConfig backButton = plugin.getGuiConfigManager().getStoneGeneratorButton("back");
        if (backButton != null && slot == backButton.slot) {
            player.openInventory(plugin.getGuiManager().createMainMenu(player));
            return;
        }

        // Exit button - close the GUI
        ButtonConfig exitButton = plugin.getGuiConfigManager().getStoneGeneratorButton("exit");
        if (exitButton != null && slot == exitButton.slot) {
            player.closeInventory();
            return;
        }

        // Craft by clicking the result generator item
        int resultSlot = plugin.getGuiConfigManager().getStoneGeneratorResultSlot();
        if (slot != resultSlot) {
            return;
        }

        if (!player.hasPermission("oreflow.generator.place")) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission",
                    "permission", "oreflow.generator.place"));
            return;
        }

        craftStoneGenerator(player);
    }

    private void craftStoneGenerator(Player player) {
        StoneGeneratorManager manager = plugin.getStoneGeneratorManager();
        ConfigurationSection generatorsSection = plugin.getConfigManager().getGenerators()
                .getConfigurationSection("generators");
        if (generatorsSection == null) {
            return;
        }

        ConfigurationSection generatorSection = null;
        for (String key : generatorsSection.getKeys(false)) {
            ConfigurationSection section = generatorsSection.getConfigurationSection(key);
            if (section != null && section.getBoolean("enabled", true)) {
                generatorSection = section;
                break;
            }
        }

        if (generatorSection == null) {
            return;
        }

        ConfigurationSection craftingSection = generatorSection.getConfigurationSection("crafting");
        if (craftingSection == null) {
            return;
        }

        List<String> shape = craftingSection.getStringList("shape");
        ConfigurationSection ingredientsSection = craftingSection.getConfigurationSection("ingredients");
        if (ingredientsSection == null) {
            return;
        }

        // Build required ingredients map
        Map<Material, Integer> required = new HashMap<>();
        for (String row : shape) {
            for (char c : row.toCharArray()) {
                String key = String.valueOf(c);
                if (key.isBlank()) {
                    continue;
                }
                Material material = Material.matchMaterial(ingredientsSection.getString(key, "STONE"));
                if (material != null) {
                    required.merge(material, 1, Integer::sum);
                }
            }
        }

        // Check inventory
        Map<Material, Integer> available = new HashMap<>();
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() != Material.AIR) {
                available.merge(item.getType(), item.getAmount(), Integer::sum);
            }
        }

        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            int have = available.getOrDefault(entry.getKey(), 0);
            if (have < entry.getValue()) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.generator-insufficient-items",
                        "material", entry.getKey().name(),
                        "required", String.valueOf(entry.getValue()),
                        "have", String.valueOf(have)));
                return;
            }
        }

        // Remove ingredients
        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            removeItems(player, entry.getKey(), entry.getValue());
        }

        // Give generator item
        ItemStack generator = manager.createGeneratorItem(generatorSection);
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), generator);
            player.sendMessage(plugin.getMessageManager().getMessage("commands.generator-crafted-ground"));
        } else {
            player.getInventory().addItem(generator);
            player.sendMessage(plugin.getMessageManager().getMessage("commands.generator-crafted"));
        }

        player.closeInventory();
    }

    private void craftCobbleX(Player player) {
        var cobbleXManager = new pl.Ljimex.oreFlow.cobblex.CobbleXManager(plugin);
        int cost = cobbleXManager.getCraftingCost();
        int cobbleCount = countMaterial(player, Material.COBBLESTONE);

        if (cobbleCount < cost) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.cobblex-insufficient",
                    "required", String.valueOf(cost),
                    "have", String.valueOf(cobbleCount)));
            return;
        }

        removeItems(player, Material.COBBLESTONE, cost);

        ItemStack cobbleX = cobbleXManager.createCobbleX(1);
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), cobbleX);
            player.sendMessage(plugin.getMessageManager().getMessage("commands.cobblex-crafted-ground"));
        } else {
            player.getInventory().addItem(cobbleX);
            player.sendMessage(plugin.getMessageManager().getMessage("commands.cobblex-crafted"));
        }
    }

    private int countMaterial(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == material) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    remaining -= itemAmount;
                    item.setAmount(0);
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
                if (remaining <= 0) {
                    break;
                }
            }
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
