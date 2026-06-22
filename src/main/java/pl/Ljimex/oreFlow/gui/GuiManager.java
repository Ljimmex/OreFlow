package pl.Ljimex.oreFlow.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import pl.Ljimex.oreFlow.OreFlow;

import java.util.ArrayList;
import java.util.List;

public class GuiManager {

    private final OreFlow plugin;

    public GuiManager(OreFlow plugin) {
        this.plugin = plugin;
    }

    public Inventory createDropGui() {
        ConfigurationSection dropsSection = plugin.getConfigManager().getDrops();
        int size = Math.min(54, ((dropsSection.getKeys(false).size() - 1) / 9 + 1) * 9);
        if (size < 9) {
            size = 9;
        }

        String title = colorize("&8&lZarzadzaj dropami ze stone");
        Inventory inventory = Bukkit.createInventory(null, size, title);

        int slot = 0;
        for (String dropKey : dropsSection.getKeys(false)) {
            if (slot >= size) {
                break;
            }

            ConfigurationSection drop = dropsSection.getConfigurationSection(dropKey);
            if (drop == null) {
                continue;
            }

            ItemStack item = createDropIcon(dropKey, drop);
            if (item != null) {
                inventory.setItem(slot, item);
                slot++;
            }
        }

        // Przycisk zamkniecia w ostatnim slocie
        inventory.setItem(size - 1, createCloseButton());

        return inventory;
    }

    private ItemStack createDropIcon(String dropKey, ConfigurationSection drop) {
        String materialName = drop.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = colorize("&e" + dropKey);
            meta.setDisplayName(displayName);

            List<String> lore = new ArrayList<>();
            lore.add(colorize("&8&m--------------------"));
            lore.add(colorize("&fMaterial: &e" + materialName));
            lore.add(colorize("&fSzansa: &e" + drop.getDouble("chance", 0.0) + "%"));
            lore.add(colorize("&fIlosc: &e" + drop.getInt("min-amount", 1) + "-" + drop.getInt("max-amount", 1)));
            lore.add(colorize("&fFortune: &e" + (drop.getBoolean("fortune-multiplier", true) ? "Tak" : "Nie")));

            String requiredTool = drop.getString("required-tool", "");
            lore.add(colorize("&fWymagany kilof: &e" + (requiredTool.isEmpty() ? "Dowolny" : requiredTool)));

            int minY = drop.getInt("min-y", Integer.MIN_VALUE);
            int maxY = drop.getInt("max-y", Integer.MAX_VALUE);
            if (minY != Integer.MIN_VALUE && maxY != Integer.MAX_VALUE) {
                lore.add(colorize("&fPoziom Y: &e" + minY + "-" + maxY));
            }

            lore.add(colorize("&fEXP: &e" + drop.getInt("exp", 0)));
            lore.add(colorize("&8&m--------------------"));
            lore.add(colorize("&7LPM - szczegoly"));
            lore.add(colorize("&7PPM - wlacz/wylacz (w przyszlosci)"));
            meta.setLore(lore);

            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize("&cZamknij"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }
}
