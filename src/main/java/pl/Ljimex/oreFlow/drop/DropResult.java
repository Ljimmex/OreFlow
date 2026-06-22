package pl.Ljimex.oreFlow.drop;

import org.bukkit.inventory.ItemStack;

/**
 * Reprezentuje pojedynczy wylosowany drop wraz z jego kluczem z drops.yml.
 */
public record DropResult(String dropKey, ItemStack item) {
}
