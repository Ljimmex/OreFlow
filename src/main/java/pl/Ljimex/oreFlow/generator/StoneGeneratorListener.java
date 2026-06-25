package pl.Ljimex.oreFlow.generator;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import pl.Ljimex.oreFlow.OreFlow;

import java.util.HashMap;

public class StoneGeneratorListener implements Listener {

    private final OreFlow plugin;
    private final StoneGeneratorManager generatorManager;

    public StoneGeneratorListener(OreFlow plugin, StoneGeneratorManager generatorManager) {
        this.plugin = plugin;
        this.generatorManager = generatorManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        generatorManager.discoverRecipes(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof ShapedRecipe shapedRecipe)) {
            return;
        }

        NamespacedKey key = shapedRecipe.getKey();
        if (!"oreflow".equals(key.getNamespace())) {
            return;
        }
        String recipeId = key.getKey();
        if (!recipeId.startsWith("stone_generator_")) {
            return;
        }

        String generatorKey = recipeId.substring("stone_generator_".length());
        ItemStack result = generatorManager.createGeneratorItem(generatorKey);
        if (result != null) {
            event.getInventory().setResult(result);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        if (!generatorManager.isGeneratorItem(item)) {
            return;
        }

        if (!player.hasPermission("oreflow.generator.place")) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission",
                    "permission", "oreflow.generator.place"));
            event.setCancelled(true);
            return;
        }

        Location location = event.getBlock().getLocation();
        generatorManager.addGenerator(location);

        if (plugin.getMessageManager().isActionBarEnabled("generator-placed")) {
            String prefix = plugin.getMessageManager().getRaw("actionbar.prefix", new HashMap<>());
            String message = prefix + " <dark_gray>» <green>Stoniarka postawiona!";
            if (!message.isEmpty()) {
                player.sendActionBar(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(message));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();

        if (!generatorManager.isGeneratorBlock(location)) {
            return;
        }

        Player player = event.getPlayer();

        if (!player.hasPermission("oreflow.generator.place")) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission",
                    "permission", "oreflow.generator.place"));
            event.setCancelled(true);
            return;
        }

        generatorManager.removeGenerator(location);

        // Drop the generator item if enabled
        ConfigurationSection generatorsSection = plugin.getConfigManager().getGenerators()
                .getConfigurationSection("generators");
        if (generatorsSection != null) {
            for (String key : generatorsSection.getKeys(false)) {
                ConfigurationSection generatorSection = generatorsSection.getConfigurationSection(key);
                if (generatorSection != null && generatorSection.getBoolean("enabled", true)) {
                    if (generatorSection.getBoolean("pickup-on-break", true)) {
                        ItemStack drop = generatorManager.getGeneratorDropItem();
                        if (drop != null) {
                            block.getWorld().dropItemNaturally(block.getLocation(), drop);
                        }
                    }
                    break;
                }
            }
        }

        // Cancel normal drops from the generator block
        event.setDropItems(false);
        event.setExpToDrop(0);
    }
}
