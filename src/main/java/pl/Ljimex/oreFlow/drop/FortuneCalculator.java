package pl.Ljimex.oreFlow.drop;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.concurrent.ThreadLocalRandom;

public class FortuneCalculator {

    /**
     * Zwraca poziom Fortune z narzedzia.
     * Jesli narzedzie nie ma enchantu Fortune, zwraca 0.
     */
    public int getFortuneLevel(ItemStack tool) {
        if (tool == null || !tool.hasItemMeta()) {
            return 0;
        }
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) {
            return 0;
        }
        return meta.getEnchantLevel(Enchantment.FORTUNE);
    }

    /**
     * Oblicza mnoznik dropu zgodnie z oficjalna formula Fortune z Minecraft Java Edition.
     *
     * Dla poziomu Fortune N:
     * - szansa 2/(N+2) na mnoznik 1x
     * - szansa 1/(N+2) na kazdy z mnoznikow 2..N+1
     *
     * Srednie wartosci:
     * - Fortune I:   1.33x
     * - Fortune II:  1.75x
     * - Fortune III: 2.20x
     */
    public int calculateFortuneMultiplier(int fortuneLevel) {
        if (fortuneLevel <= 0) {
            return 1;
        }

        int denominator = fortuneLevel + 2;
        int roll = ThreadLocalRandom.current().nextInt(denominator);

        if (roll < 2) {
            return 1;
        }

        // roll >= 2, wiec mnoznik to roll (dla roll=2 -> 2x, roll=fortuneLevel+1 -> N+1x)
        // Maksymalny mnoznik to fortuneLevel + 1
        int multiplier = roll;
        int maxMultiplier = fortuneLevel + 1;
        return Math.min(multiplier, maxMultiplier);
    }
}
