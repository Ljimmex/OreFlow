package pl.Ljimex.oreFlow.config;

import pl.Ljimex.oreFlow.OreFlow;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class GuiConfigManager {

    private final OreFlow plugin;
    private File guiFile;
    private FileConfiguration guiConfig;

    public GuiConfigManager(OreFlow plugin) {
        this.plugin = plugin;
    }

    public void load() {
        guiFile = new File(plugin.getDataFolder(), "gui.yml");

        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
        }

        guiConfig = plugin.getConfigManager().getMigration().loadAndMigrate(guiFile, "gui.yml");
        loadDefaultsFromResources();
    }

    public void reload() {
        load();
    }

    private void loadDefaultsFromResources() {
        try (InputStream defaultStream = plugin.getResource("gui.yml")) {
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                guiConfig.setDefaults(defaultConfig);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load gui.yml defaults", e);
        }
    }

    public void save() {
        if (guiConfig == null || guiFile == null) {
            return;
        }
        try {
            guiConfig.save(guiFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save gui.yml", e);
        }
    }

    public FileConfiguration getConfig() {
        return guiConfig;
    }

    public String getTitle() {
        return guiConfig.getString("gui.title", "<gold><bold>Menu Dropow</bold></gold>");
    }

    public Component getGuiTitleComponent() {
        return MiniMessage.miniMessage().deserialize(getTitle());
    }

    public int getRows() {
        return Math.max(1, Math.min(6, guiConfig.getInt("gui.rows", 5)));
    }

    public int getSize() {
        return getRows() * 9;
    }

    public List<DecorationConfig> getDecorations() {
        List<DecorationConfig> result = new ArrayList<>();
        List<?> decorations = guiConfig.getList("gui.decorations");
        if (decorations == null) {
            return result;
        }

        for (Object obj : decorations) {
            if (!(obj instanceof ConfigurationSection) && !(obj instanceof java.util.LinkedHashMap)) {
                continue;
            }

            ConfigurationSection section;
            if (obj instanceof ConfigurationSection configSection) {
                section = configSection;
            } else {
                section = createMemorySectionFromMap((java.util.LinkedHashMap<?, ?>) obj);
            }

            String slots = section.getString("slots", "");
            String material = section.getString("material", "BLACK_STAINED_GLASS_PANE");
            String name = section.getString("name", " ");
            List<String> lore = section.getStringList("lore");
            String texture = section.getString("texture", null);

            for (int slot : parseSlots(slots)) {
                if (isValidSlot(slot)) {
                    result.add(new DecorationConfig(slot, material, name, lore, texture));
                }
            }
        }

        return result;
    }

    public DropSectionConfig getDropSection() {
        ConfigurationSection drops = guiConfig.getConfigurationSection("gui.drops");
        if (drops == null) {
            return new DropSectionConfig(9, 9, "<{color}>{capitalized_drop}</{color}>", Collections.emptyList());
        }

        int startSlot = drops.getInt("start-slot", 9);
        int maxSlots = drops.getInt("max-slots", 9);
        ConfigurationSection item = drops.getConfigurationSection("item");

        String name = item != null ? item.getString("name", "<{color}>{capitalized_drop}</{color}>") : "<{color}>{capitalized_drop}</{color}>";
        List<String> lore = item != null ? item.getStringList("lore") : Collections.emptyList();

        return new DropSectionConfig(startSlot, maxSlots, name, lore);
    }

    public ButtonConfig getButton(String type) {
        return getButtonFromSection("gui.buttons", type);
    }

    // Main Menu
    public String getMainMenuTitle() {
        return guiConfig.getString("main-menu.title", "<gold><bold>OreFlow</bold></gold>");
    }

    public Component getMainMenuTitleComponent() {
        return MiniMessage.miniMessage().deserialize(getMainMenuTitle());
    }

    public int getMainMenuRows() {
        return Math.max(1, Math.min(6, guiConfig.getInt("main-menu.rows", 3)));
    }

    public int getMainMenuSize() {
        return getMainMenuRows() * 9;
    }

    public List<DecorationConfig> getMainMenuDecorations() {
        return getDecorationsFromSection("main-menu.decorations");
    }

    public ButtonConfig getMainMenuButton(String type) {
        return getButtonFromSection("main-menu.buttons", type);
    }

    // Stone Generator GUI
    public String getStoneGeneratorTitle() {
        return guiConfig.getString("stone-generator.title", "<aqua><bold>Stone Generator</bold></aqua>");
    }

    public Component getStoneGeneratorTitleComponent() {
        return MiniMessage.miniMessage().deserialize(getStoneGeneratorTitle());
    }

    public int getStoneGeneratorRows() {
        return Math.max(1, Math.min(6, guiConfig.getInt("stone-generator.rows", 5)));
    }

    public int getStoneGeneratorSize() {
        return getStoneGeneratorRows() * 9;
    }

    public List<DecorationConfig> getStoneGeneratorDecorations() {
        return getDecorationsFromSection("stone-generator.decorations");
    }

    public ButtonConfig getStoneGeneratorButton(String type) {
        return getButtonFromSection("stone-generator.buttons", type);
    }

    public List<Integer> getStoneGeneratorRecipeSlots() {
        List<Integer> result = new ArrayList<>();
        List<?> slots = guiConfig.getList("stone-generator.recipe-slots");
        if (slots == null) {
            // Default recipe slots
            return List.of(10, 11, 12, 19, 20, 21, 28, 29, 30);
        }
        for (Object obj : slots) {
            if (obj instanceof Number number) {
                result.add(number.intValue());
            }
        }
        return result;
    }

    public int getStoneGeneratorResultSlot() {
        return guiConfig.getInt("stone-generator.result-slot", 24);
    }

    // CobbleX Menu
    public String getCobbleXMenuTitle() {
        return guiConfig.getString("cobblex-menu.title", "<gold><bold>CobbleX</bold></gold>");
    }

    public Component getCobbleXMenuTitleComponent() {
        return MiniMessage.miniMessage().deserialize(getCobbleXMenuTitle());
    }

    public int getCobbleXMenuRows() {
        return Math.max(1, Math.min(6, guiConfig.getInt("cobblex-menu.rows", 3)));
    }

    public int getCobbleXMenuSize() {
        return getCobbleXMenuRows() * 9;
    }

    public List<DecorationConfig> getCobbleXMenuDecorations() {
        return getDecorationsFromSection("cobblex-menu.decorations");
    }

    public ButtonConfig getCobbleXMenuButton(String type) {
        return getButtonFromSection("cobblex-menu.buttons", type);
    }

    // CobbleX Craft GUI
    public String getCobbleXCraftTitle() {
        return guiConfig.getString("cobblex-craft.title", "<gold><bold>CobbleX Crafting</bold></gold>");
    }

    public Component getCobbleXCraftTitleComponent() {
        return MiniMessage.miniMessage().deserialize(getCobbleXCraftTitle());
    }

    public int getCobbleXCraftRows() {
        return Math.max(1, Math.min(6, guiConfig.getInt("cobblex-craft.rows", 5)));
    }

    public int getCobbleXCraftSize() {
        return getCobbleXCraftRows() * 9;
    }

    public List<DecorationConfig> getCobbleXCraftDecorations() {
        return getDecorationsFromSection("cobblex-craft.decorations");
    }

    public ButtonConfig getCobbleXCraftButton(String type) {
        return getButtonFromSection("cobblex-craft.buttons", type);
    }

    public List<Integer> getCobbleXCraftRecipeSlots() {
        List<Integer> result = new ArrayList<>();
        List<?> slots = guiConfig.getList("cobblex-craft.recipe-slots");
        if (slots == null) {
            return List.of(10, 11, 12, 19, 20, 21, 28, 29, 30);
        }
        for (Object obj : slots) {
            if (obj instanceof Number number) {
                result.add(number.intValue());
            }
        }
        return result;
    }

    public int getCobbleXCraftResultSlot() {
        return guiConfig.getInt("cobblex-craft.result-slot", 24);
    }

    // CobbleX Drop GUI
    public String getCobbleXDropTitle() {
        return guiConfig.getString("cobblex-drop.title", "<gold><bold>CobbleX Drops</bold></gold>");
    }

    public Component getCobbleXDropTitleComponent() {
        return MiniMessage.miniMessage().deserialize(getCobbleXDropTitle());
    }

    public int getCobbleXDropRows() {
        return Math.max(1, Math.min(6, guiConfig.getInt("cobblex-drop.rows", 5)));
    }

    public int getCobbleXDropSize() {
        return getCobbleXDropRows() * 9;
    }

    public List<DecorationConfig> getCobbleXDropDecorations() {
        return getDecorationsFromSection("cobblex-drop.decorations");
    }

    public DropSectionConfig getCobbleXDropSection() {
        ConfigurationSection drops = guiConfig.getConfigurationSection("cobblex-drop.drops");
        if (drops == null) {
            return new DropSectionConfig(9, 9, "<{color}>{capitalized_drop}</{color}>", Collections.emptyList());
        }

        int startSlot = drops.getInt("start-slot", 9);
        int maxSlots = drops.getInt("max-slots", 9);
        ConfigurationSection item = drops.getConfigurationSection("item");

        String name = item != null ? item.getString("name", "<{color}>{capitalized_drop}</{color}>") : "<{color}>{capitalized_drop}</{color}>";
        List<String> lore = item != null ? item.getStringList("lore") : Collections.emptyList();

        return new DropSectionConfig(startSlot, maxSlots, name, lore);
    }

    public ButtonConfig getCobbleXDropButton(String type) {
        return getButtonFromSection("cobblex-drop.buttons", type);
    }

    // Helpers
    private ButtonConfig getButtonFromSection(String sectionPath, String type) {
        ConfigurationSection buttons = guiConfig.getConfigurationSection(sectionPath);
        if (buttons == null) {
            return null;
        }

        for (String key : buttons.getKeys(false)) {
            ConfigurationSection button = buttons.getConfigurationSection(key);
            if (button == null) {
                continue;
            }
            if (type.equalsIgnoreCase(button.getString("type", ""))) {
                return new ButtonConfig(
                        button.getString("type", key),
                        button.getInt("slot", 0),
                        button.getString("material", "STONE"),
                        button.getString("name", "Button"),
                        button.getStringList("lore")
                );
            }
        }
        return null;
    }

    private List<DecorationConfig> getDecorationsFromSection(String sectionPath) {
        List<DecorationConfig> result = new ArrayList<>();
        List<?> decorations = guiConfig.getList(sectionPath);
        if (decorations == null) {
            return result;
        }

        for (Object obj : decorations) {
            if (!(obj instanceof ConfigurationSection) && !(obj instanceof java.util.LinkedHashMap)) {
                continue;
            }

            ConfigurationSection section;
            if (obj instanceof ConfigurationSection configSection) {
                section = configSection;
            } else {
                section = createMemorySectionFromMap((java.util.LinkedHashMap<?, ?>) obj);
            }

            String slots = section.getString("slots", "");
            String material = section.getString("material", "BLACK_STAINED_GLASS_PANE");
            String name = section.getString("name", " ");
            List<String> lore = section.getStringList("lore");
            String texture = section.getString("texture", null);

            for (int slot : parseSlots(slots)) {
                if (isValidSlot(slot)) {
                    result.add(new DecorationConfig(slot, material, name, lore, texture));
                }
            }
        }

        return result;
    }

    private ConfigurationSection createMemorySectionFromMap(java.util.LinkedHashMap<?, ?> map) {
        org.bukkit.configuration.MemoryConfiguration section = new org.bukkit.configuration.MemoryConfiguration();
        for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
            section.set(String.valueOf(entry.getKey()), entry.getValue());
        }
        return section;
    }

    private boolean isValidSlot(int slot) {
        int maxSlot = getSize() - 1;
        if (slot < 0 || slot > maxSlot) {
            plugin.getLogger().warning("Invalid GUI slot " + slot + " in gui.yml (max: " + maxSlot + ")");
            return false;
        }
        return true;
    }

    public List<Integer> parseSlots(String slots) {
        List<Integer> result = new ArrayList<>();
        if (slots == null || slots.isEmpty()) {
            return result;
        }

        for (String part : slots.split(",")) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                if (range.length == 2) {
                    try {
                        int start = Integer.parseInt(range[0].trim());
                        int end = Integer.parseInt(range[1].trim());
                        for (int i = start; i <= end; i++) {
                            result.add(i);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else {
                try {
                    result.add(Integer.parseInt(part));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return result;
    }

    public static class DecorationConfig {
        public final int slot;
        public final String material;
        public final String name;
        public final List<String> lore;
        public final String texture;

        public DecorationConfig(int slot, String material, String name, List<String> lore, String texture) {
            this.slot = slot;
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.texture = texture;
        }
    }

    public static class DropSectionConfig {
        public final int startSlot;
        public final int maxSlots;
        public final String name;
        public final List<String> lore;

        public DropSectionConfig(int startSlot, int maxSlots, String name, List<String> lore) {
            this.startSlot = startSlot;
            this.maxSlots = maxSlots;
            this.name = name;
            this.lore = lore;
        }
    }

    public static class ButtonConfig {
        public final String type;
        public final int slot;
        public final String material;
        public final String name;
        public final List<String> lore;

        public ButtonConfig(String type, int slot, String material, String name, List<String> lore) {
            this.type = type;
            this.slot = slot;
            this.material = material;
            this.name = name;
            this.lore = lore;
        }
    }
}
