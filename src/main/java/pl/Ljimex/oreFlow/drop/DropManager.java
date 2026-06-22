package pl.Ljimex.oreFlow.drop;

import pl.Ljimex.oreFlow.OreFlow;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DropManager {

    private final OreFlow plugin;
    private final FortuneCalculator fortuneCalculator;

    public DropManager(OreFlow plugin) {
        this.plugin = plugin;
        this.fortuneCalculator = new FortuneCalculator();
    }

    /**
     * Przetwarza dropy dla zniszczonego bloku.
     *
     * @param player    gracz, ktory zniszczyl blok
     * @param location  lokalizacja zniszczonego bloku
     * @param tool      narzedzie uzyte do kopania
     * @return lista przedmiotow, ktore wypadly
     */
    public List<ItemStack> processDrops(Player player, Location location, ItemStack tool) {
        List<ItemStack> result = new ArrayList<>();

        if (!hasPermission(player, location.getWorld())) {
            return result;
        }

        int fortuneLevel = fortuneCalculator.getFortuneLevel(tool);
        ConfigurationSection dropsSection = plugin.getConfigManager().getDrops();

        int maxDrops = plugin.getConfigManager().getConfig().getInt("settings.max-drops-per-block", 0);
        int dropsProcessed = 0;

        for (String dropKey : dropsSection.getKeys(false)) {
            if (maxDrops > 0 && dropsProcessed >= maxDrops) {
                break;
            }

            ConfigurationSection drop = dropsSection.getConfigurationSection(dropKey);
            if (drop == null || !drop.getBoolean("enabled", true)) {
                continue;
            }

            if (!isDropApplicable(drop, player, location, tool)) {
                continue;
            }

            double chance = drop.getDouble("chance", 0.0);
            if (ThreadLocalRandom.current().nextDouble(100.0) > chance) {
                continue;
            }

            ItemStack item = createDropItem(drop, fortuneLevel);
            if (item != null && item.getAmount() > 0) {
                result.add(item);
                dropsProcessed++;

                int exp = drop.getInt("exp", 0);
                if (exp > 0) {
                    giveExp(player, location, exp);
                }
            }
        }

        return result;
    }

    /**
     * Dodaje przedmioty do ekwipunku gracza lub upuszcza je na ziemie.
     */
    public void deliverDrops(Player player, Location location, List<ItemStack> drops) {
        if (drops.isEmpty()) {
            return;
        }

        boolean dropToInventory = plugin.getConfigManager().getConfig()
                .getBoolean("settings.drop-to-inventory", true);

        if (dropToInventory) {
            PlayerInventory inventory = player.getInventory();
            for (ItemStack drop : drops) {
                if (inventory.firstEmpty() != -1) {
                    inventory.addItem(drop);
                } else {
                    location.getWorld().dropItemNaturally(location, drop);
                }
            }
        } else {
            for (ItemStack drop : drops) {
                location.getWorld().dropItemNaturally(location, drop);
            }
        }
    }

    private boolean hasPermission(Player player, World world) {
        if (!player.hasPermission("oreflow.mine")) {
            return false;
        }

        String worldMode = plugin.getConfigManager().getConfig()
                .getString("settings.world-mode", "blacklist");
        List<String> worlds = plugin.getConfigManager().getConfig()
                .getStringList("settings.worlds");

        if (worlds.isEmpty()) {
            return true;
        }

        boolean isListed = worlds.contains(world.getName());

        if ("whitelist".equalsIgnoreCase(worldMode)) {
            return isListed;
        } else {
            return !isListed;
        }
    }

    private boolean isDropApplicable(ConfigurationSection drop, Player player, Location location, ItemStack tool) {
        // Sprawdzenie poziomu Y
        int minY = drop.getInt("min-y", Integer.MIN_VALUE);
        int maxY = drop.getInt("max-y", Integer.MAX_VALUE);
        int blockY = location.getBlockY();
        if (blockY < minY || blockY > maxY) {
            return false;
        }

        // Sprawdzenie wymaganego narzedzia
        String requiredTool = drop.getString("required-tool", "");
        if (!requiredTool.isEmpty()) {
            if (tool == null) {
                return false;
            }

            Material requiredMaterial = Material.matchMaterial(requiredTool);
            if (requiredMaterial == null) {
                return false;
            }

            int requiredTier = getToolTier(requiredMaterial);
            if (requiredTier > 0) {
                // Dla narzedzi z tierami (kilofy) sprawdzamy czy gracz ma co najmniej taki tier
                int actualTier = getToolTier(tool.getType());
                if (actualTier < requiredTier) {
                    return false;
                }
            } else {
                // Dla innych narzedzi dokladne dopasowanie
                if (tool.getType() != requiredMaterial) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Zwraca tier narzedzia (tylko dla kilofow).
     * Wyzszy numer = lepszy kilof.
     */
    private int getToolTier(Material material) {
        if (material == null) {
            return 0;
        }
        return switch (material) {
            case WOODEN_PICKAXE, GOLDEN_PICKAXE -> 1;
            case STONE_PICKAXE -> 2;
            case IRON_PICKAXE -> 3;
            case DIAMOND_PICKAXE -> 4;
            case NETHERITE_PICKAXE -> 5;
            default -> 0;
        };
    }

    private ItemStack createDropItem(ConfigurationSection drop, int fortuneLevel) {
        String materialName = drop.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            plugin.getLogger().warning("Unknown material in drops.yml: " + materialName);
            return null;
        }

        int minAmount = drop.getInt("min-amount", 1);
        int maxAmount = drop.getInt("max-amount", 1);
        int amount = ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);

        boolean applyFortune = drop.getBoolean("fortune-multiplier", true);
        if (applyFortune && amount > 0) {
            int multiplier = fortuneCalculator.calculateFortuneMultiplier(fortuneLevel);
            amount = amount * multiplier;
        }

        if (amount <= 0) {
            amount = 1;
        }

        // Ograniczenie do maksymalnego stacku
        int maxStackSize = material.getMaxStackSize();
        if (amount > maxStackSize) {
            amount = maxStackSize;
        }

        return new ItemStack(material, amount);
    }

    private void giveExp(Player player, Location location, int exp) {
        String expMode = plugin.getConfigManager().getConfig()
                .getString("settings.exp-mode", "direct-give");
        double multiplier = plugin.getConfigManager().getConfig()
                .getDouble("settings.exp-multiplier", 1.0);

        int finalExp = (int) Math.round(exp * multiplier);
        if (finalExp <= 0) {
            return;
        }

        if ("orb-spawn".equalsIgnoreCase(expMode)) {
            location.getWorld().spawn(location, org.bukkit.entity.ExperienceOrb.class, orb -> orb.setExperience(finalExp));
        } else {
            player.giveExp(finalExp);
        }
    }

}
