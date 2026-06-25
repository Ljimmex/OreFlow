package pl.Ljimex.oreFlow.gui;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import pl.Ljimex.oreFlow.OreFlow;
import pl.Ljimex.oreFlow.config.GuiConfigManager.ButtonConfig;
import pl.Ljimex.oreFlow.generator.GeneratorConfig;
import pl.Ljimex.oreFlow.generator.StoneGeneratorManager;

public class GuiListener implements Listener {

    private final OreFlow plugin;
    private final NamespacedKey dropKey;
    private final NamespacedKey adminDropKey;
    private final PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
    private final ConcurrentHashMap<UUID, Integer> playerPage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> adminPage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> cobblexDropPage = new ConcurrentHashMap<>();

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
        } else if (title.equals(plainSerializer.serialize(plugin.getGuiConfigManager().getCobbleXMenuTitleComponent()))) {
            event.setCancelled(true);
            handleCobbleXMenuClick(player, event);
        } else if (title.equals(plainSerializer.serialize(plugin.getGuiConfigManager().getCobbleXCraftTitleComponent()))) {
            event.setCancelled(true);
            handleCobbleXCraftGuiClick(player, event);
        } else if (title.equals(plainSerializer.serialize(plugin.getGuiConfigManager().getCobbleXDropTitleComponent()))) {
            event.setCancelled(true);
            handleCobbleXDropGuiClick(player, event);
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
            player.openInventory(plugin.getGuiManager().createCobbleXMenu(player));
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

        ButtonConfig backButton = plugin.getGuiConfigManager().getButton("back");
        if (backButton != null && slot == backButton.slot) {
            player.openInventory(plugin.getGuiManager().createMainMenu(player));
            return;
        }

        ButtonConfig closeButton = plugin.getGuiConfigManager().getButton("close");
        if (closeButton != null && slot == closeButton.slot) {
            player.closeInventory();
            return;
        }

        // Drops
        String dropKeyValue = clicked.getItemMeta().getPersistentDataContainer().get(this.dropKey, PersistentDataType.STRING);
        if (dropKeyValue != null && plugin.getDropConfigManager().hasDrop(dropKeyValue)) {
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
        if (dropKeyValue != null && plugin.getDropConfigManager().hasDrop(dropKeyValue)) {
            var drops = plugin.getConfigManager().getDrops();
            boolean enabled = drops.getBoolean(dropKeyValue + ".enabled", true);
            drops.set(dropKeyValue + ".enabled", !enabled);
            plugin.getConfigManager().saveConfigs();
            plugin.getDropConfigManager().reload();
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

        ClickType clickType = event.getClick();
        if (clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT) {
            craftStoneGenerator(player, 1);
        } else if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
            craftStoneGenerator(player, -1);
        }
    }

    private void craftStoneGenerator(Player player, int requestedAmount) {
        StoneGeneratorManager manager = plugin.getStoneGeneratorManager();
        GeneratorConfig generatorConfig = plugin.getGeneratorConfigManager().getFirstEnabledGenerator();
        if (generatorConfig == null) {
            return;
        }

        Map<Material, Integer> required = new HashMap<>();
        for (String row : generatorConfig.getCraftingShape()) {
            for (char c : row.toCharArray()) {
                if (c == ' ') {
                    continue;
                }
                Material material = generatorConfig.getIngredients().get(c);
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

        // Calculate maximum craftable amount
        int maxPossible = Integer.MAX_VALUE;
        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            int have = available.getOrDefault(entry.getKey(), 0);
            int perGenerator = entry.getValue();
            if (perGenerator > 0) {
                maxPossible = Math.min(maxPossible, have / perGenerator);
            }
        }

        int amount = requestedAmount <= 0 ? maxPossible : Math.min(requestedAmount, maxPossible);

        if (amount <= 0) {
            Map.Entry<Material, Integer> firstRequired = required.entrySet().iterator().next();
            int have = available.getOrDefault(firstRequired.getKey(), 0);
            player.sendMessage(plugin.getMessageManager().getMessage("commands.generator-insufficient-items",
                    "material", firstRequired.getKey().name(),
                    "required", String.valueOf(firstRequired.getValue()),
                    "have", String.valueOf(have)));
            return;
        }

        // Remove ingredients
        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            removeItems(player, entry.getKey(), entry.getValue() * amount);
        }

        // Give generator items
        ItemStack generator = manager.createGeneratorItem(generatorConfig);
        generator.setAmount(amount);
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), generator);
            player.sendMessage(plugin.getMessageManager().getMessage("commands.generator-crafted-ground",
                    "amount", String.valueOf(amount)));
        } else {
            player.getInventory().addItem(generator);
            player.sendMessage(plugin.getMessageManager().getMessage("commands.generator-crafted",
                    "amount", String.valueOf(amount)));
        }

        player.closeInventory();
    }

    private void handleCobbleXMenuClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();

        ButtonConfig craftButton = plugin.getGuiConfigManager().getCobbleXMenuButton("open_cobblex_craft");
        if (craftButton != null && slot == craftButton.slot) {
            player.openInventory(plugin.getGuiManager().createCobbleXCraftGui(player));
            return;
        }

        ButtonConfig dropsButton = plugin.getGuiConfigManager().getCobbleXMenuButton("open_cobblex_drops");
        if (dropsButton != null && slot == dropsButton.slot) {
            cobblexDropPage.put(player.getUniqueId(), 0);
            player.openInventory(plugin.getGuiManager().createCobbleXDropGui(player, 0));
            return;
        }

        ButtonConfig backButton = plugin.getGuiConfigManager().getCobbleXMenuButton("back");
        if (backButton != null && slot == backButton.slot) {
            player.openInventory(plugin.getGuiManager().createMainMenu(player));
            return;
        }

        ButtonConfig exitButton = plugin.getGuiConfigManager().getCobbleXMenuButton("exit");
        if (exitButton != null && slot == exitButton.slot) {
            player.closeInventory();
        }
    }

    private void handleCobbleXCraftGuiClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();

        ButtonConfig backButton = plugin.getGuiConfigManager().getCobbleXCraftButton("back");
        if (backButton != null && slot == backButton.slot) {
            player.openInventory(plugin.getGuiManager().createCobbleXMenu(player));
            return;
        }

        ButtonConfig exitButton = plugin.getGuiConfigManager().getCobbleXCraftButton("exit");
        if (exitButton != null && slot == exitButton.slot) {
            player.closeInventory();
            return;
        }

        int resultSlot = plugin.getGuiConfigManager().getCobbleXCraftResultSlot();
        if (slot != resultSlot) {
            return;
        }

        if (!player.hasPermission("cobblex.use")) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission",
                    "permission", "cobblex.use"));
            return;
        }

        ClickType clickType = event.getClick();
        if (clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT) {
            craftCobbleX(player, 1);
        } else if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
            craftCobbleX(player, -1);
        }
    }

    private void handleCobbleXDropGuiClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        int currentPage = cobblexDropPage.getOrDefault(player.getUniqueId(), 0);

        ButtonConfig prevButton = plugin.getGuiConfigManager().getCobbleXDropButton("previous_page");
        if (prevButton != null && slot == prevButton.slot) {
            if (currentPage > 0) {
                cobblexDropPage.put(player.getUniqueId(), currentPage - 1);
                refreshCobbleXDropGui(player, currentPage - 1);
            }
            return;
        }

        ButtonConfig nextButton = plugin.getGuiConfigManager().getCobbleXDropButton("next_page");
        if (nextButton != null && slot == nextButton.slot) {
            int totalPages = plugin.getGuiManager().getCobbleXDropTotalPages();
            if (currentPage < totalPages - 1) {
                cobblexDropPage.put(player.getUniqueId(), currentPage + 1);
                refreshCobbleXDropGui(player, currentPage + 1);
            }
            return;
        }

        ButtonConfig backButton = plugin.getGuiConfigManager().getCobbleXDropButton("back");
        if (backButton != null && slot == backButton.slot) {
            player.openInventory(plugin.getGuiManager().createCobbleXMenu(player));
            return;
        }

        ButtonConfig closeButton = plugin.getGuiConfigManager().getCobbleXDropButton("close");
        if (closeButton != null && slot == closeButton.slot) {
            player.closeInventory();
        }
    }

    private void refreshCobbleXDropGui(Player player, int page) {
        player.openInventory(plugin.getGuiManager().createCobbleXDropGui(player, page));
    }

    private void craftCobbleX(Player player, int requestedAmount) {
        var cobbleXManager = plugin.getCobbleXManager();
        int costPerItem = cobbleXManager.getCraftingCost();
        int cobbleCount = countMaterial(player, Material.COBBLESTONE);

        int maxPossible = cobbleCount / costPerItem;
        int amount = requestedAmount <= 0 ? maxPossible : Math.min(requestedAmount, maxPossible);

        if (amount <= 0) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.cobblex-insufficient",
                    "required", String.valueOf(costPerItem),
                    "have", String.valueOf(cobbleCount)));
            return;
        }

        removeItems(player, Material.COBBLESTONE, costPerItem * amount);

        ItemStack cobbleX = cobbleXManager.createCobbleX(amount);
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), cobbleX);
            player.sendMessage(plugin.getMessageManager().getMessage("commands.cobblex-crafted-ground",
                    "amount", String.valueOf(amount)));
        } else {
            player.getInventory().addItem(cobbleX);
            player.sendMessage(plugin.getMessageManager().getMessage("commands.cobblex-crafted",
                    "amount", String.valueOf(amount)));
        }

        player.closeInventory();
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
