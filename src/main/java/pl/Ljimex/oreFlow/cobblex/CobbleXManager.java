package pl.Ljimex.oreFlow.cobblex;

import pl.Ljimex.oreFlow.OreFlow;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class CobbleXManager {

    private final OreFlow plugin;
    private final NamespacedKey cobbleXKey;

    public CobbleXManager(OreFlow plugin) {
        this.plugin = plugin;
        this.cobbleXKey = new NamespacedKey(plugin, "cobblex");
    }

    public void registerRecipe() {
        if (!plugin.getConfigManager().getConfig().getBoolean("cobblex.enabled", true)) {
            return;
        }
        if (!plugin.getConfigManager().getConfig().getBoolean("cobblex.crafting.enabled", true)) {
            return;
        }

        ItemStack cobbleX = createCobbleX(1);
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "cobblex_recipe"), cobbleX);
        recipe.shape("CCC", "CCC", "CCC");
        recipe.setIngredient('C', Material.COBBLESTONE);
        Bukkit.addRecipe(recipe);
    }

    public ItemStack createCobbleX(int amount) {
        ConfigurationSection itemSection = plugin.getConfigManager().getConfig()
                .getConfigurationSection("cobblex.item");

        Material material = Material.ENDER_CHEST;
        if (itemSection != null) {
            material = Material.matchMaterial(itemSection.getString("material", "ENDER_CHEST"));
            if (material == null) {
                material = Material.ENDER_CHEST;
            }
        }

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (itemSection != null) {
                String name = itemSection.getString("name", "&8&lCobbleX");
                meta.setDisplayName(colorize(name));

                List<String> lore = itemSection.getStringList("lore");
                if (!lore.isEmpty()) {
                    List<String> coloredLore = new ArrayList<>();
                    for (String line : lore) {
                        coloredLore.add(colorize(line));
                    }
                    meta.setLore(coloredLore);
                }

                if (itemSection.getBoolean("enchant-glow", true)) {
                    meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                }
            }

            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(cobbleXKey, PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean isCobbleX(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(cobbleXKey, PersistentDataType.BYTE);
    }

    public CobbleXDrop rollDrop() {
        ConfigurationSection dropsSection = plugin.getConfigManager().getConfig()
                .getConfigurationSection("cobblex.drops");
        if (dropsSection == null) {
            return null;
        }

        Set<String> dropKeys = dropsSection.getKeys(false);
        if (dropKeys.isEmpty()) {
            return null;
        }

        double totalChance = 0;
        for (String key : dropKeys) {
            ConfigurationSection drop = dropsSection.getConfigurationSection(key);
            if (drop != null) {
                totalChance += drop.getDouble("chance", 0);
            }
        }

        if (totalChance <= 0) {
            return null;
        }

        double roll = ThreadLocalRandom.current().nextDouble(totalChance);
        double current = 0;

        for (String key : dropKeys) {
            ConfigurationSection drop = dropsSection.getConfigurationSection(key);
            if (drop == null) continue;

            current += drop.getDouble("chance", 0);
            if (roll < current) {
                return new CobbleXDrop(
                        drop.getString("command", ""),
                        drop.getString("message", "&7Wylosowano nagrode")
                );
            }
        }

        // Fallback do ostatniego dropu
        String lastKey = new ArrayList<>(dropKeys).get(dropKeys.size() - 1);
        ConfigurationSection lastDrop = dropsSection.getConfigurationSection(lastKey);
        return new CobbleXDrop(
                lastDrop.getString("command", ""),
                lastDrop.getString("message", "&7Wylosowano nagrode")
        );
    }

    public int getCraftingCost() {
        return plugin.getConfigManager().getConfig()
                .getInt("cobblex.crafting.cost-per-slot", 64) * 9;
    }

    public int getCooldown() {
        return plugin.getConfigManager().getConfig()
                .getInt("cobblex.cooldown", 2);
    }

    private String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }

    public static class CobbleXDrop {
        private final String command;
        private final String message;

        public CobbleXDrop(String command, String message) {
            this.command = command;
            this.message = message;
        }

        public String getCommand() {
            return command;
        }

        public String getMessage() {
            return message;
        }
    }
}
