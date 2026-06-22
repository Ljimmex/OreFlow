package pl.Ljimex.oreFlow.util;

import org.bukkit.Material;

public final class ItemColorUtil {

    private ItemColorUtil() {
        // utility class
    }

    public static String getColor(Material material) {
        return switch (material) {
            case DIAMOND -> "#55FFFF";
            case EMERALD -> "#55FF55";
            case GOLD_INGOT -> "#FFFF55";
            case IRON_INGOT -> "#AAAAAA";
            case COAL -> "#555555";
            case REDSTONE -> "#FF5555";
            case LAPIS_LAZULI -> "#5555FF";
            case COPPER_INGOT -> "#FFAA55";
            case OBSIDIAN -> "#AA00AA";
            case ANCIENT_DEBRIS -> "#AA5555";
            case NETHERITE_INGOT -> "#555555";
            default -> "white";
        };
    }
}
