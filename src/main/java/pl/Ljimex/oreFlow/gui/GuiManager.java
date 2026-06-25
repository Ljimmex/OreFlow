package pl.Ljimex.oreFlow.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import pl.Ljimex.oreFlow.OreFlow;
import pl.Ljimex.oreFlow.cobblex.CobbleXManager.CobbleXReward;
import pl.Ljimex.oreFlow.config.GuiConfigManager;
import pl.Ljimex.oreFlow.config.GuiConfigManager.ButtonConfig;
import pl.Ljimex.oreFlow.config.GuiConfigManager.DecorationConfig;
import pl.Ljimex.oreFlow.config.GuiConfigManager.DropSectionConfig;
import pl.Ljimex.oreFlow.drop.DropConfig;
import pl.Ljimex.oreFlow.generator.GeneratorConfig;
import pl.Ljimex.oreFlow.util.ItemColorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        int dropCount = plugin.getDropConfigManager().getDropCount();
        return Math.max(1, (int) Math.ceil((double) dropCount / dropSection.maxSlots));
    }

    public Inventory createDropGui(Player player, int page) {
        int size = guiConfig.getSize();
        Inventory inventory = Bukkit.createInventory(null, size, miniMessage.deserialize(guiConfig.getTitle()));

        // Decorations
        for (DecorationConfig decoration : guiConfig.getDecorations()) {
            if (decoration.slot < 0 || decoration.slot >= size) continue;
            ItemStack item = createDecorationItem(decoration);
            if (item != null) {
                inventory.setItem(decoration.slot, item);
            }
        }

        // Drops
        DropSectionConfig dropSection = guiConfig.getDropSection();
        List<pl.Ljimex.oreFlow.drop.DropConfig> drops = new ArrayList<>(plugin.getDropConfigManager().getDrops());

        int dropsPerPage = dropSection.maxSlots;
        int totalPages = Math.max(1, (int) Math.ceil((double) drops.size() / dropsPerPage));
        page = Math.max(0, Math.min(page, totalPages - 1));

        int startIndex = page * dropsPerPage;
        int slot = dropSection.startSlot;

        for (int i = 0; i < dropsPerPage && startIndex + i < drops.size(); i++) {
            pl.Ljimex.oreFlow.drop.DropConfig drop = drops.get(startIndex + i);
            boolean enabled = plugin.getPlayerSettingsManager().isDropEnabled(player.getUniqueId(), drop.getKey());
            ItemStack item = createDropIcon(drop, enabled, dropSection);
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
        String title = plugin.getMessageManager().getRawString("gui.title-admin", new java.util.HashMap<>());
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
            ItemStack item = createDecorationItem(decoration);
            if (item != null) {
                inventory.setItem(decoration.slot, item);
            }
        }

        // Drops
        DropSectionConfig dropSection = guiConfig.getDropSection();
        List<pl.Ljimex.oreFlow.drop.DropConfig> drops = new ArrayList<>(plugin.getDropConfigManager().getDrops());

        int dropsPerPage = dropSection.maxSlots;
        int totalPages = Math.max(1, (int) Math.ceil((double) drops.size() / dropsPerPage));
        page = Math.max(0, Math.min(page, totalPages - 1));

        int startIndex = page * dropsPerPage;
        int slot = dropSection.startSlot;

        for (int i = 0; i < dropsPerPage && startIndex + i < drops.size(); i++) {
            pl.Ljimex.oreFlow.drop.DropConfig drop = drops.get(startIndex + i);
            boolean enabled = plugin.getConfigManager().getDrops().getBoolean(drop.getKey() + ".enabled", true);
            ItemStack item = createAdminDropIcon(drop, enabled, dropSection);
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

    public Inventory createMainMenu(Player player) {
        int size = guiConfig.getMainMenuSize();
        Inventory inventory = Bukkit.createInventory(null, size, guiConfig.getMainMenuTitleComponent());

        for (DecorationConfig decoration : guiConfig.getMainMenuDecorations()) {
            if (decoration.slot < 0 || decoration.slot >= size) continue;
            ItemStack item = createDecorationItem(decoration);
            if (item != null) {
                inventory.setItem(decoration.slot, item);
            }
        }

        addMainMenuButton(inventory, "open_drops");
        addMainMenuButton(inventory, "open_stone_generator");
        addMainMenuButton(inventory, "open_cobblex");
        addMainMenuButton(inventory, "close");

        return inventory;
    }

    public Inventory createStoneGeneratorGui(Player player) {
        int size = guiConfig.getStoneGeneratorSize();
        Inventory inventory = Bukkit.createInventory(null, size, guiConfig.getStoneGeneratorTitleComponent());

        for (DecorationConfig decoration : guiConfig.getStoneGeneratorDecorations()) {
            if (decoration.slot < 0 || decoration.slot >= size) continue;
            ItemStack item = createDecorationItem(decoration);
            if (item != null) {
                inventory.setItem(decoration.slot, item);
            }
        }

        // Display recipe
        GeneratorConfig generatorConfig = plugin.getGeneratorConfigManager().getFirstEnabledGenerator();
        if (generatorConfig != null) {
            List<String> shape = generatorConfig.getCraftingShape();
            Map<Character, Material> ingredients = generatorConfig.getIngredients();
            List<Integer> recipeSlots = guiConfig.getStoneGeneratorRecipeSlots();

            int index = 0;
            for (String row : shape) {
                for (char c : row.toCharArray()) {
                    if (index >= recipeSlots.size()) break;
                    Material material = Material.AIR;
                    if (c != ' ') {
                        material = ingredients.getOrDefault(c, Material.AIR);
                    }
                    if (material != Material.AIR) {
                        inventory.setItem(recipeSlots.get(index), new ItemStack(material));
                    }
                    index++;
                }
            }

            // Result
            ItemStack result = plugin.getStoneGeneratorManager().createGeneratorItem(generatorConfig);
            inventory.setItem(guiConfig.getStoneGeneratorResultSlot(), result);
        }

        // Navigation buttons
        addStoneGeneratorButton(inventory, "back");
        addStoneGeneratorButton(inventory, "exit");

        return inventory;
    }

    public Inventory createCobbleXMenu(Player player) {
        int size = guiConfig.getCobbleXMenuSize();
        Inventory inventory = Bukkit.createInventory(null, size, guiConfig.getCobbleXMenuTitleComponent());

        for (DecorationConfig decoration : guiConfig.getCobbleXMenuDecorations()) {
            if (decoration.slot < 0 || decoration.slot >= size) continue;
            ItemStack item = createDecorationItem(decoration);
            if (item != null) {
                inventory.setItem(decoration.slot, item);
            }
        }

        addCobbleXMenuButton(inventory, "open_cobblex_craft");
        addCobbleXMenuButton(inventory, "open_cobblex_drops");
        addCobbleXMenuButton(inventory, "back");
        addCobbleXMenuButton(inventory, "exit");

        return inventory;
    }

    public Inventory createCobbleXCraftGui(Player player) {
        int size = guiConfig.getCobbleXCraftSize();
        Inventory inventory = Bukkit.createInventory(null, size, guiConfig.getCobbleXCraftTitleComponent());

        for (DecorationConfig decoration : guiConfig.getCobbleXCraftDecorations()) {
            if (decoration.slot < 0 || decoration.slot >= size) continue;
            ItemStack item = createDecorationItem(decoration);
            if (item != null) {
                inventory.setItem(decoration.slot, item);
            }
        }

        // Display recipe - 9 cobblestone
        List<Integer> recipeSlots = guiConfig.getCobbleXCraftRecipeSlots();
        for (int slot : recipeSlots) {
            if (slot >= 0 && slot < size) {
                inventory.setItem(slot, new ItemStack(Material.COBBLESTONE));
            }
        }

        // Result
        ItemStack result = plugin.getCobbleXManager().createCobbleX(1);
        inventory.setItem(guiConfig.getCobbleXCraftResultSlot(), result);

        // Navigation buttons
        addCobbleXCraftButton(inventory, "back");
        addCobbleXCraftButton(inventory, "exit");

        return inventory;
    }

    public int getCobbleXDropTotalPages() {
        DropSectionConfig dropSection = guiConfig.getCobbleXDropSection();
        int rewardCount = plugin.getCobbleXManager().getRewards().size();
        return Math.max(1, (int) Math.ceil((double) rewardCount / dropSection.maxSlots));
    }

    public Inventory createCobbleXDropGui(Player player, int page) {
        int size = guiConfig.getCobbleXDropSize();
        Inventory inventory = Bukkit.createInventory(null, size, guiConfig.getCobbleXDropTitleComponent());

        for (DecorationConfig decoration : guiConfig.getCobbleXDropDecorations()) {
            if (decoration.slot < 0 || decoration.slot >= size) continue;
            ItemStack item = createDecorationItem(decoration);
            if (item != null) {
                inventory.setItem(decoration.slot, item);
            }
        }

        DropSectionConfig dropSection = guiConfig.getCobbleXDropSection();
        List<CobbleXReward> rewards = new ArrayList<>(plugin.getCobbleXManager().getRewards());

        int dropsPerPage = dropSection.maxSlots;
        int totalPages = Math.max(1, (int) Math.ceil((double) rewards.size() / dropsPerPage));
        page = Math.max(0, Math.min(page, totalPages - 1));

        int startIndex = page * dropsPerPage;
        int slot = dropSection.startSlot;

        for (int i = 0; i < dropsPerPage && startIndex + i < rewards.size(); i++) {
            CobbleXReward reward = rewards.get(startIndex + i);
            ItemStack item = createCobbleXRewardIcon(reward, dropSection);
            if (item != null) {
                inventory.setItem(slot, item);
                slot++;
            }
        }

        addCobbleXDropPageButton(inventory, page, totalPages, "previous_page");
        addCobbleXDropPageButton(inventory, page, totalPages, "next_page");
        addCobbleXDropButton(inventory, player, "back");
        addCobbleXDropButton(inventory, player, "close");

        return inventory;
    }

    private void addCobbleXMenuButton(Inventory inventory, String type) {
        ButtonConfig button = guiConfig.getCobbleXMenuButton(type);
        if (button == null) {
            return;
        }

        int size = inventory.getSize();
        if (button.slot < 0 || button.slot >= size) {
            return;
        }

        ItemStack item = createSimpleItem(button.material, button.name, button.lore);
        if (item != null) {
            inventory.setItem(button.slot, item);
        }
    }

    private void addCobbleXCraftButton(Inventory inventory, String type) {
        ButtonConfig button = guiConfig.getCobbleXCraftButton(type);
        if (button == null) {
            return;
        }

        int size = inventory.getSize();
        if (button.slot < 0 || button.slot >= size) {
            return;
        }

        ItemStack item = createSimpleItem(button.material, button.name, button.lore);
        if (item != null) {
            inventory.setItem(button.slot, item);
        }
    }

    private void addCobbleXDropButton(Inventory inventory, Player player, String type) {
        ButtonConfig button = guiConfig.getCobbleXDropButton(type);
        if (button == null) {
            return;
        }

        int size = inventory.getSize();
        if (button.slot < 0 || button.slot >= size) {
            return;
        }

        ItemStack item = createSimpleItem(button.material, button.name, button.lore);
        if (item != null) {
            inventory.setItem(button.slot, item);
        }
    }

    private void addCobbleXDropPageButton(Inventory inventory, int currentPage, int totalPages, String type) {
        ButtonConfig button = guiConfig.getCobbleXDropButton(type);
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

    private ItemStack createCobbleXRewardIcon(CobbleXReward reward, DropSectionConfig dropSection) {
        Material material = Material.ENDER_CHEST;
        String key = reward.key();

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String dropColor = ItemColorUtil.getColor(material);
            String name = dropSection.name
                    .replace("{drop}", key)
                    .replace("{capitalized_drop}", capitalize(key))
                    .replace("{material}", material.name())
                    .replace("{color}", dropColor);
            meta.displayName(miniMessage.deserialize(name));

            List<Component> loreComponents = new ArrayList<>();
            for (String line : dropSection.lore) {
                String replaced = line
                        .replace("{drop}", key)
                        .replace("{capitalized_drop}", capitalize(key))
                        .replace("{material}", material.name())
                        .replace("{color}", dropColor)
                        .replace("{chance}", formatChance(reward.chance()))
                        .replace("{message}", reward.message());
                if (replaced.contains("\n")) {
                    for (String subLine : replaced.split("\\n")) {
                        loreComponents.add(miniMessage.deserialize(subLine));
                    }
                } else {
                    loreComponents.add(miniMessage.deserialize(replaced));
                }
            }
            meta.lore(loreComponents);

            item.setItemMeta(meta);
        }

        return item;
    }

    private void addStoneGeneratorButton(Inventory inventory, String type) {
        ButtonConfig button = guiConfig.getStoneGeneratorButton(type);
        if (button == null) {
            return;
        }

        int size = inventory.getSize();
        if (button.slot < 0 || button.slot >= size) {
            return;
        }

        ItemStack item = createSimpleItem(button.material, button.name, button.lore);
        if (item != null) {
            inventory.setItem(button.slot, item);
        }
    }

    private void addMainMenuButton(Inventory inventory, String type) {
        ButtonConfig button = guiConfig.getMainMenuButton(type);
        if (button == null) {
            return;
        }

        int size = inventory.getSize();
        if (button.slot < 0 || button.slot >= size) {
            return;
        }

        ItemStack item = createSimpleItem(button.material, button.name, button.lore);
        if (item == null) {
            return;
        }

        if ("open_cobblex".equalsIgnoreCase(type)) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }

        inventory.setItem(button.slot, item);
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

    private ItemStack createDropIcon(DropConfig drop, boolean enabled, DropSectionConfig dropSection) {
        Material material = drop.getMaterial();
        String dropKey = drop.getKey();
        String materialName = material.name();

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

    private ItemStack createAdminDropIcon(DropConfig drop, boolean enabled, DropSectionConfig dropSection) {
        Material material = drop.getMaterial();
        String dropKey = drop.getKey();
        String materialName = material.name();

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

    private String replaceDropPlaceholders(String line, DropConfig drop, String materialName, String dropColor, String status, boolean enabled) {
        int minY = drop.getMinY();
        int maxY = drop.getMaxY();
        String yLevel = (minY == Integer.MIN_VALUE && maxY == Integer.MAX_VALUE) ? "Any" : minY + " – " + maxY;

        String tool = drop.getRequiredTool().name();

        double baseChance = drop.getChance();
        int minAmount = drop.getMinAmount();
        int maxAmount = drop.getMaxAmount();

        String fortuneEnabledText = drop.isFortuneEnabled() ? "<green>Yes</green>" : "<red>No</red>";

        // Fortune level placeholders
        StringBuilder fortuneLines = new StringBuilder();
        for (int level = 1; level <= 3; level++) {
            DropConfig.FortuneLevel levelConfig = drop.getFortuneLevel(level);
            if (levelConfig != null) {
                String fortuneLine = " <dark_gray>▸</dark_gray> <yellow>F" + level + ":</yellow> <aqua>" + formatChance(levelConfig.chance())
                        + "%</aqua> <dark_gray>➜</dark_gray> <green>+" + levelConfig.bonusMin() + "-" + levelConfig.bonusMax() + "</green>";
                if (!fortuneLines.isEmpty()) {
                    fortuneLines.append("\n");
                }
                fortuneLines.append(fortuneLine);
            }
        }

        String toggleAction = enabled ? "<red>disable</red>" : "<green>enable</green>";

        return line
                .replace("{drop}", drop.getKey())
                .replace("{capitalized_drop}", capitalize(drop.getKey()))
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
                .replace("{exp}", String.valueOf(drop.getExp()))
                .replace("{line}", "<dark_gray>" + "▬".repeat(24) + "</dark_gray>")
                .replace("{toggle_action}", toggleAction)
                .replace("{fortune_lines}", fortuneLines.toString());
    }

    private String getFortuneValue(DropConfig drop, int level, String key) {
        DropConfig.FortuneLevel levelConfig = drop.getFortuneLevel(level);
        if (levelConfig == null) {
            return "0";
        }
        if (key.equals("chance")) {
            return formatChance(levelConfig.chance());
        }
        if (key.equals("bonus-min")) {
            return String.valueOf(levelConfig.bonusMin());
        }
        return String.valueOf(levelConfig.bonusMax());
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

    private ItemStack createDecorationItem(DecorationConfig decoration) {
        if (decoration.texture != null && !decoration.texture.isEmpty()) {
            return createCustomHead(decoration.texture, decoration.name, decoration.lore);
        }
        return createSimpleItem(decoration.material, decoration.name, decoration.lore);
    }

    private ItemStack createCustomHead(String base64Texture, String name, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.displayName(miniMessage.deserialize(name));

            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(miniMessage.deserialize(line));
            }
            if (!loreComponents.isEmpty()) {
                meta.lore(loreComponents);
            }

            try {
                PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
                profile.setProperty(new ProfileProperty("textures", base64Texture));
                meta.setPlayerProfile(profile);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not apply custom head texture: " + e.getMessage());
            }

            head.setItemMeta(meta);
        }
        return head;
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
