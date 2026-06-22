package pl.Ljimex.oreFlow.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import pl.Ljimex.oreFlow.OreFlow;
import pl.Ljimex.oreFlow.config.GuiConfigManager;
import pl.Ljimex.oreFlow.config.GuiConfigManager.ButtonConfig;
import pl.Ljimex.oreFlow.config.GuiConfigManager.DecorationConfig;
import pl.Ljimex.oreFlow.config.GuiConfigManager.DropSectionConfig;
import pl.Ljimex.oreFlow.util.ItemColorUtil;

import java.util.ArrayList;
import java.util.List;

public class GuiManager {

    private final OreFlow plugin;
    private final GuiConfigManager guiConfig;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public GuiManager(OreFlow plugin) {
        this.plugin = plugin;
        this.guiConfig = plugin.getGuiConfigManager();
    }

    public int getTotalPages() {
        DropSectionConfig dropSection = guiConfig.getDropSection();
        ConfigurationSection dropsSection = plugin.getConfigManager().getDrops();
        int dropCount = dropsSection.getKeys(false).size();
        return Math.max(1, (int) Math.ceil((double) dropCount / dropSection.maxSlots));
    }

    public Inventory createDropGui(Player player, int page) {
        int size = guiConfig.getSize();
        Inventory inventory = Bukkit.createInventory(null, size, miniMessage.deserialize(guiConfig.getTitle()));

        // Decorations
        for (DecorationConfig decoration : guiConfig.getDecorations()) {
            if (decoration.slot < 0 || decoration.slot >= size) continue;
            ItemStack item = createSimpleItem(decoration.material, decoration.name, decoration.lore);
            if (item != null) {
                inventory.setItem(decoration.slot, item);
            }
        }

        // Drops
        DropSectionConfig dropSection = guiConfig.getDropSection();
        ConfigurationSection dropsSection = plugin.getConfigManager().getDrops();
        List<String> dropKeys = new ArrayList<>(dropsSection.getKeys(false));

        int dropsPerPage = dropSection.maxSlots;
        int totalPages = Math.max(1, (int) Math.ceil((double) dropKeys.size() / dropsPerPage));
        page = Math.max(0, Math.min(page, totalPages - 1));

        int startIndex = page * dropsPerPage;
        int slot = dropSection.startSlot;

        for (int i = 0; i < dropsPerPage && startIndex + i < dropKeys.size(); i++) {
            String dropKey = dropKeys.get(startIndex + i);
            ConfigurationSection drop = dropsSection.getConfigurationSection(dropKey);
            if (drop == null) {
                continue;
            }

            boolean enabled = plugin.getPlayerSettingsManager().isDropEnabled(player.getUniqueId(), dropKey);
            ItemStack item = createDropIcon(dropKey, drop, enabled, dropSection);
            if (item != null) {
                inventory.setItem(slot, item);
                slot++;
            }
        }

        // Buttons
        addButton(inventory, player, "cobble");
        addButton(inventory, player, "drop_destination");
        addButton(inventory, player, "exp");
        addPageButton(inventory, page, totalPages, "previous_page");
        addPageButton(inventory, page, totalPages, "next_page");
        addButton(inventory, player, "close");

        return inventory;
    }

    public Component getAdminGuiTitleComponent() {
        String title = plugin.getMessageManager().getRaw("gui.title-admin", new java.util.HashMap<>());
        return miniMessage.deserialize(title);
    }

    public int getAdminTotalPages() {
        return getTotalPages();
    }

    public Inventory createAdminGui(Player player, int page) {
        int size = guiConfig.getSize();
        Inventory inventory = Bukkit.createInventory(null, size, getAdminGuiTitleComponent());

        // Decorations
        for (DecorationConfig decoration : guiConfig.getDecorations()) {
            if (decoration.slot < 0 || decoration.slot >= size) continue;
            ItemStack item = createSimpleItem(decoration.material, decoration.name, decoration.lore);
            if (item != null) {
                inventory.setItem(decoration.slot, item);
            }
        }

        // Drops
        DropSectionConfig dropSection = guiConfig.getDropSection();
        ConfigurationSection dropsSection = plugin.getConfigManager().getDrops();
        List<String> dropKeys = new ArrayList<>(dropsSection.getKeys(false));

        int dropsPerPage = dropSection.maxSlots;
        int totalPages = Math.max(1, (int) Math.ceil((double) dropKeys.size() / dropsPerPage));
        page = Math.max(0, Math.min(page, totalPages - 1));

        int startIndex = page * dropsPerPage;
        int slot = dropSection.startSlot;

        for (int i = 0; i < dropsPerPage && startIndex + i < dropKeys.size(); i++) {
            String dropKey = dropKeys.get(startIndex + i);
            ConfigurationSection drop = dropsSection.getConfigurationSection(dropKey);
            if (drop == null) {
                continue;
            }

            boolean enabled = drop.getBoolean("enabled", true);
            ItemStack item = createAdminDropIcon(dropKey, drop, enabled, dropSection);
            if (item != null) {
                inventory.setItem(slot, item);
                slot++;
            }
        }

        // Buttons
        addPageButton(inventory, page, totalPages, "previous_page");
        addPageButton(inventory, page, totalPages, "next_page");
        addButton(inventory, player, "close");

        return inventory;
    }

    private void addButton(Inventory inventory, Player player, String type) {
        ButtonConfig button = guiConfig.getButton(type);
        if (button == null) {
            return;
        }

        int size = inventory.getSize();
        if (button.slot < 0 || button.slot >= size) {
            return;
        }

        String status = getButtonStatus(player, type);
        String name = button.name.replace("{status}", status);
        List<String> lore = new ArrayList<>();
        for (String line : button.lore) {
            lore.add(line.replace("{status}", status));
        }

        ItemStack item = createSimpleItem(button.material, name, lore);
        if (item != null) {
            inventory.setItem(button.slot, item);
        }
    }

    private void addPageButton(Inventory inventory, int currentPage, int totalPages, String type) {
        ButtonConfig button = guiConfig.getButton(type);
        if (button == null) {
            return;
        }

        int size = inventory.getSize();
        if (button.slot < 0 || button.slot >= size) {
            return;
        }

        boolean isNext = type.equalsIgnoreCase("next_page");
        boolean visible = isNext ? currentPage < totalPages - 1 : currentPage > 0;

        if (!visible) {
            return;
        }

        ItemStack item = createSimpleItem(button.material, button.name, button.lore);
        if (item != null) {
            inventory.setItem(button.slot, item);
        }
    }

    private String getButtonStatus(Player player, String type) {
        return switch (type.toLowerCase()) {
            case "cobble" -> plugin.getPlayerSettingsManager().isCobbleEnabled(player.getUniqueId())
                    ? "<green>✓ Enabled</green>" : "<red>✗ Disabled</red>";
            case "drop_destination" -> plugin.getPlayerSettingsManager().isDropToInventory(player.getUniqueId())
                    ? "<green>✓ Inventory</green>" : "<red>✗ Ground</red>";
            case "exp" -> plugin.getPlayerSettingsManager().isExpEnabled(player.getUniqueId())
                    ? "<green>✓ Enabled</green>" : "<red>✗ Disabled</red>";
            default -> "";
        };
    }

    private ItemStack createDropIcon(String dropKey, ConfigurationSection drop, boolean enabled, DropSectionConfig dropSection) {
        String materialName = drop.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String dropColor = ItemColorUtil.getColor(material);
            String status = enabled
                    ? "<green>✓ Enabled</green>"
                    : "<red>✗ Disabled</red>";

            String name = dropSection.name
                    .replace("{drop}", dropKey)
                    .replace("{capitalized_drop}", capitalize(dropKey))
                    .replace("{material}", materialName)
                    .replace("{color}", dropColor)
                    .replace("{status}", status);
            meta.displayName(miniMessage.deserialize(name));

            List<Component> loreComponents = new ArrayList<>();
            for (String line : dropSection.lore) {
                String replaced = replaceDropPlaceholders(line, drop, materialName, dropColor, status, enabled);
                if (replaced.contains("\n")) {
                    for (String subLine : replaced.split("\\n")) {
                        loreComponents.add(miniMessage.deserialize(subLine));
                    }
                } else {
                    loreComponents.add(miniMessage.deserialize(replaced));
                }
            }
            meta.lore(loreComponents);

            // Save original drop key for toggle
            NamespacedKey key = new NamespacedKey(plugin, "drop_key");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, dropKey);

            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createAdminDropIcon(String dropKey, ConfigurationSection drop, boolean enabled, DropSectionConfig dropSection) {
        String materialName = drop.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String dropColor = ItemColorUtil.getColor(material);
            String status = enabled
                    ? "<green>✓ Enabled</green>"
                    : "<red>✗ Disabled</red>";

            String name = dropSection.name
                    .replace("{drop}", dropKey)
                    .replace("{capitalized_drop}", capitalize(dropKey))
                    .replace("{material}", materialName)
                    .replace("{color}", dropColor)
                    .replace("{status}", status);
            meta.displayName(miniMessage.deserialize(name));

            List<String> lore = new ArrayList<>();
            lore.add("<dark_gray>Admin Panel</dark_gray>");
            lore.add("<gray>Global status: " + status + "</gray>");
            lore.add("<dark_gray>Click to toggle global drop</dark_gray>");
            meta.lore(lore.stream().map(miniMessage::deserialize).toList());

            NamespacedKey key = new NamespacedKey(plugin, "admin_drop_key");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, dropKey);

            item.setItemMeta(meta);
        }

        return item;
    }

    private String replaceDropPlaceholders(String line, ConfigurationSection drop, String materialName, String dropColor, String status, boolean enabled) {
        int minY = drop.getInt("min-y", Integer.MIN_VALUE);
        int maxY = drop.getInt("max-y", Integer.MAX_VALUE);
        String yLevel = (minY == Integer.MIN_VALUE && maxY == Integer.MAX_VALUE) ? "Any" : minY + " – " + maxY;

        String requiredTool = drop.getString("required-tool", "");
        String tool = requiredTool.isEmpty() ? "Any" : requiredTool;

        double baseChance = drop.getDouble("chance", 0.0);
        int minAmount = drop.getInt("min-amount", 1);
        int maxAmount = drop.getInt("max-amount", 1);

        boolean fortuneEnabled = isFortuneEnabled(drop);
        String fortuneEnabledText = fortuneEnabled ? "<green>Yes</green>" : "<red>No</red>";

        // Fortune level placeholders
        StringBuilder fortuneLines = new StringBuilder();
        ConfigurationSection levels = drop.getConfigurationSection("fortune.levels");
        if (levels != null) {
            for (int level = 1; level <= 3; level++) {
                ConfigurationSection levelSection = levels.getConfigurationSection(String.valueOf(level));
                if (levelSection != null) {
                    double chance = levelSection.getDouble("chance", 0.0);
                    int bonusMin = levelSection.getInt("bonus-min", 0);
                    int bonusMax = levelSection.getInt("bonus-max", 0);
                    String fortuneLine = " <dark_gray>▸</dark_gray> <yellow>F" + level + ":</yellow> <aqua>" + formatChance(chance)
                            + "%</aqua> <dark_gray>➜</dark_gray> <green>+" + bonusMin + "-" + bonusMax + "</green>";
                    if (!fortuneLines.isEmpty()) {
                        fortuneLines.append("\n");
                    }
                    fortuneLines.append(fortuneLine);
                }
            }
        }

        String toggleAction = enabled ? "<red>disable</red>" : "<green>enable</green>";

        return line
                .replace("{drop}", drop.getName())
                .replace("{capitalized_drop}", capitalize(drop.getName()))
                .replace("{material}", materialName)
                .replace("{color}", dropColor)
                .replace("{status}", status)
                .replace("{chance}", formatChance(baseChance))
                .replace("{min}", String.valueOf(minAmount))
                .replace("{max}", String.valueOf(maxAmount))
                .replace("{fortune_enabled}", fortuneEnabledText)
                .replace("{fortune1_chance}", getFortuneValue(drop, 1, "chance"))
                .replace("{fortune1_min}", getFortuneValue(drop, 1, "bonus-min"))
                .replace("{fortune1_max}", getFortuneValue(drop, 1, "bonus-max"))
                .replace("{fortune2_chance}", getFortuneValue(drop, 2, "chance"))
                .replace("{fortune2_min}", getFortuneValue(drop, 2, "bonus-min"))
                .replace("{fortune2_max}", getFortuneValue(drop, 2, "bonus-max"))
                .replace("{fortune3_chance}", getFortuneValue(drop, 3, "chance"))
                .replace("{fortune3_min}", getFortuneValue(drop, 3, "bonus-min"))
                .replace("{fortune3_max}", getFortuneValue(drop, 3, "bonus-max"))
                .replace("{tool}", tool)
                .replace("{y_level}", yLevel)
                .replace("{exp}", String.valueOf(drop.getInt("exp", 0)))
                .replace("{line}", "<dark_gray>" + "▬".repeat(24) + "</dark_gray>")
                .replace("{toggle_action}", toggleAction)
                .replace("{fortune_lines}", fortuneLines.toString());
    }

    private String getFortuneValue(ConfigurationSection drop, int level, String key) {
        ConfigurationSection levelSection = drop.getConfigurationSection("fortune.levels." + level);
        if (levelSection == null) {
            return "0";
        }
        if (key.equals("chance")) {
            return formatChance(levelSection.getDouble(key, 0.0));
        }
        return String.valueOf(levelSection.getInt(key, 0));
    }

    private ItemStack createSimpleItem(String materialName, String name, List<String> lore) {
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(miniMessage.deserialize(name));

            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(miniMessage.deserialize(line));
            }
            if (!loreComponents.isEmpty()) {
                meta.lore(loreComponents);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isFortuneEnabled(ConfigurationSection drop) {
        if (drop.contains("fortune.enabled")) {
            return drop.getBoolean("fortune.enabled", true);
        }
        return drop.getBoolean("fortune-multiplier", true);
    }

    private String formatChance(double chance) {
        return (chance == Math.floor(chance) ? (int) chance : chance) + "";
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String[] parts = text.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!result.isEmpty()) {
                result.append(" ");
            }
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }
}
