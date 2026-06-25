package pl.Ljimex.oreFlow.drop;

import pl.Ljimex.oreFlow.OreFlow;
import pl.Ljimex.oreFlow.config.OreFlowConfig;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DropManager {

    private final OreFlow plugin;
    private final OreFlowConfig config;
    private final FortuneCalculator fortuneCalculator;

    public DropManager(OreFlow plugin) {
        this.plugin = plugin;
        this.config = plugin.getOreFlowConfig();
        this.fortuneCalculator = new FortuneCalculator();
    }

    public OreFlow getPlugin() {
        return plugin;
    }

    /**
     * Przetwarza dropy dla zniszczonego bloku.
     *
     * @param player    gracz, ktory zniszczyl blok
     * @param location  lokalizacja zniszczonego bloku
     * @param tool      narzedzie uzyte do kopania
     * @return lista wynikow dropu (klucz + przedmiot)
     */
    public List<DropResult> processDrops(Player player, Location location, ItemStack tool) {
        List<DropResult> result = new ArrayList<>();

        if (!hasPermission(player, location.getWorld())) {
            return result;
        }

        int fortuneLevel = fortuneCalculator.getFortuneLevel(tool);
        int maxDrops = config.getMaxDropsPerBlock();
        int dropsProcessed = 0;

        for (DropConfig drop : plugin.getDropConfigManager().getDrops()) {
            if (maxDrops > 0 && dropsProcessed >= maxDrops) {
                break;
            }

            if (!drop.isEnabled()) {
                continue;
            }

            // Sprawdzenie per-gracz wylaczenia dropu
            if (!plugin.getPlayerSettingsManager().isDropEnabled(player.getUniqueId(), drop.getKey())) {
                continue;
            }

            if (!isDropApplicable(drop, player, location, tool)) {
                continue;
            }

            if (ThreadLocalRandom.current().nextDouble(100.0) > drop.getChance()) {
                continue;
            }

            ItemStack item = createDropItem(drop, fortuneLevel);
            if (item != null && item.getAmount() > 0) {
                result.add(new DropResult(drop.getKey(), item));
                dropsProcessed++;

                if (drop.getExp() > 0) {
                    giveExp(player, location, drop.getExp());
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

        boolean dropToInventory = plugin.getPlayerSettingsManager().isDropToInventory(player.getUniqueId());

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
        return config.isWorldAllowed(world.getName());
    }

    private boolean isDropApplicable(DropConfig drop, Player player, Location location, ItemStack tool) {
        // Sprawdzenie poziomu Y
        if (!drop.isInYRange(location.getBlockY())) {
            return false;
        }

        // Sprawdzenie wymaganego narzedzia
        if (tool == null) {
            return false;
        }
        if (!drop.canUseTool(tool.getType())) {
            return false;
        }

        return true;
    }

    private ItemStack createDropItem(DropConfig drop, int fortuneLevel) {
        Material material = drop.getMaterial();
        int amount = drop.rollBaseAmount();

        // Fortune - konfigurowalne bonusy z drops.yml
        if (config.isFortuneEnabled() && drop.isFortuneEnabled() && amount > 0 && fortuneLevel > 0) {
            DropConfig.FortuneLevel level = drop.getFortuneLevel(fortuneLevel);
            if (level != null) {
                // Zawsze dodajemy bonus-min, aby Fortune zawsze dawalo wiecej niz brak Fortune
                amount += level.bonusMin();

                // Dodatkowy losowy bonus z szansa `chance`
                if (level.bonusMax() > level.bonusMin() && ThreadLocalRandom.current().nextDouble(100.0) <= level.chance()) {
                    int extraBonus = ThreadLocalRandom.current().nextInt(0, level.bonusMax() - level.bonusMin() + 1);
                    amount += extraBonus;
                }
            } else if (config.isAllowHigherFortune()) {
                // Fallback do vanilla-like mnoznika jesli brak konfiguracji fortune levels
                int multiplier = fortuneCalculator.calculateFortuneMultiplier(fortuneLevel);
                amount = amount * multiplier;
            }
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
        if (!plugin.getPlayerSettingsManager().isExpEnabled(player.getUniqueId())) {
            return;
        }

        int finalExp = (int) Math.round(exp * config.getExpMultiplier());
        if (finalExp <= 0) {
            return;
        }

        if ("orb-spawn".equalsIgnoreCase(config.getExpMode())) {
            location.getWorld().spawn(location, org.bukkit.entity.ExperienceOrb.class, orb -> orb.setExperience(finalExp));
        } else {
            player.giveExp(finalExp);
        }
    }
}
