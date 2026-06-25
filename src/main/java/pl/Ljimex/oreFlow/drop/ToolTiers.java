package pl.Ljimex.oreFlow.drop;

import org.bukkit.Material;

/**
 * Tiers for pickaxes. Higher number = better pickaxe.
 */
public final class ToolTiers {

    private ToolTiers() {
    }

    public static int getTier(Material material) {
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
}
