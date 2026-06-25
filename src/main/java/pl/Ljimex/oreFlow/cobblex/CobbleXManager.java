package pl.Ljimex.oreFlow.cobblex;

import pl.Ljimex.oreFlow.OreFlow;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
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
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public CobbleXManager(OreFlow plugin) {
        this.plugin = plugin;
        this.cobbleXKey = new NamespacedKey(plugin, "cobblex");
    }

    /**
     * CobbleX is crafted via /cx craft (9 stacks of cobblestone), which cannot be expressed
     * as a vanilla shaped recipe because a recipe slot can only hold one item.
     * Therefore no Bukkit recipe is registered here.
     */
    public void registerRecipe() {
        // Intentionally empty — crafting is handled by CobbleXCommand.handleCraft().
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
                String name = itemSection.getString("name", "<dark_gray><bold>CobbleX</bold></dark_gray>");
                meta.displayName(miniMessage.deserialize(name));

                List<String> lore = itemSection.getStringList("lore");
                if (!lore.isEmpty()) {
                    List<Component> coloredLore = new ArrayList<>();
                    for (String line : lore) {
                        coloredLore.add(miniMessage.deserialize(line));
                    }
                    meta.lore(coloredLore);
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
                        drop.getString("message", "<gray>Rolled reward</gray>")
                );
            }
        }

        // Fallback do ostatniego dropu
        String lastKey = new ArrayList<>(dropKeys).get(dropKeys.size() - 1);
        ConfigurationSection lastDrop = dropsSection.getConfigurationSection(lastKey);
        return new CobbleXDrop(
                lastDrop.getString("command", ""),
                lastDrop.getString("message", "<gray>Rolled reward</gray>")
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

    public List<CobbleXReward> getRewards() {
        List<CobbleXReward> rewards = new ArrayList<>();
        ConfigurationSection dropsSection = plugin.getConfigManager().getConfig()
                .getConfigurationSection("cobblex.drops");
        if (dropsSection == null) {
            return rewards;
        }

        for (String key : dropsSection.getKeys(false)) {
            ConfigurationSection drop = dropsSection.getConfigurationSection(key);
            if (drop == null) {
                continue;
            }
            rewards.add(new CobbleXReward(
                    key,
                    drop.getString("command", ""),
                    drop.getString("message", "<gray>Rolled reward</gray>"),
                    drop.getDouble("chance", 0)
            ));
        }
        return rewards;
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

    public record CobbleXReward(String key, String command, String message, double chance) {
    }
}
