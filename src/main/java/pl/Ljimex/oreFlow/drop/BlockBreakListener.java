package pl.Ljimex.oreFlow.drop;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import pl.Ljimex.oreFlow.OreFlow;
import pl.Ljimex.oreFlow.util.ActionBarUtil;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BlockBreakListener implements Listener {

    private final OreFlow plugin;
    private final DropManager dropManager;
    private final ActionBarUtil actionBarUtil;
    private final Set<UUID> disabledCreativeMessage;
    private final Map<UUID, Long> oreMessageCooldown = new HashMap<>();
    private static final long ORE_MESSAGE_COOLDOWN_MS = 3000;

    // Rudy obslugiwane przez plugin - zahardkodowane, nie edytowalne w configu
    private static final Set<Material> ORE_BLOCKS = EnumSet.of(
            Material.COAL_ORE,
            Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE,
            Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE,
            Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE,
            Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE,
            Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE,
            Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE,
            Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_GOLD_ORE,
            Material.NETHER_QUARTZ_ORE,
            Material.ANCIENT_DEBRIS
    );

    public BlockBreakListener(OreFlow plugin, DropManager dropManager) {
        this.plugin = plugin;
        this.dropManager = dropManager;
        this.actionBarUtil = new ActionBarUtil(plugin);
        this.disabledCreativeMessage = plugin.getDisabledCreativeMessagePlayers();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Sprawdzenie czy zniszczony blok jest na liscie kopanych blokow
        if (!isMineableBlock(block.getType())) {
            return;
        }

        // Tryb kreatywny - brak dropow + opcjonalna wiadomosc Action Bar
        if (player.getGameMode() == GameMode.CREATIVE) {
            if (player.hasPermission("oreflow.mine") && !disabledCreativeMessage.contains(player.getUniqueId())) {
                actionBarUtil.send(player,
                        "<#FF5555>☠ <gradient:#FF5555:#FFAA00>Dropy OreFlow wyłączone w trybie kreatywnym</gradient>");
            }
            return;
        }

        // Uprawnienie do kopania
        if (!player.hasPermission("oreflow.mine")) {
            return;
        }

        // Silk Touch - blokuje dropy z pluginu
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (hasSilkTouch(tool)) {
            return;
        }

        // Wyłączenie domyslnych dropow dla wszystkich blokow z listy (stone + rudy)
        boolean disableDefaultDrops = plugin.getConfigManager().getConfig()
                .getBoolean("settings.disable-default-drops",
                        plugin.getConfigManager().getConfig().getBoolean("settings.disable-cobble-drop", true));
        if (disableDefaultDrops) {
            event.setDropItems(false);
            event.setExpToDrop(0);

            // Informacja o zablokowanych domyslnych dropach z rud
            if (isOre(block.getType())) {
                sendOreBlockedMessage(player);
            }
        }

        // Przetworzenie dropow
        List<ItemStack> drops = dropManager.processDrops(player, block.getLocation(), tool);
        if (!drops.isEmpty()) {
            dropManager.deliverDrops(player, block.getLocation(), drops);
            sendDropMessage(player, drops);
        } else {
            // Brak szczescia - mozna pokazac symboliczna wiadomosc (opcjonalnie)
            // actionBarUtil.send(player, "<#AAAAAA>✧ Pusto... tym razem");
        }

        // Bazowy EXP za zniszczenie bloku
        int baseExp = plugin.getConfigManager().getConfig().getInt("settings.base-exp", 0);
        if (baseExp > 0) {
            double multiplier = plugin.getConfigManager().getConfig()
                    .getDouble("settings.exp-multiplier", 1.0);
            int finalExp = (int) Math.round(baseExp * multiplier);

            String expMode = plugin.getConfigManager().getConfig()
                    .getString("settings.exp-mode", "direct-give");
            if ("orb-spawn".equalsIgnoreCase(expMode)) {
                event.setExpToDrop(event.getExpToDrop() + finalExp);
            } else {
                player.giveExp(finalExp);
            }
        }
    }

    private void sendDropMessage(Player player, List<ItemStack> drops) {
        if (drops.size() == 1) {
            ItemStack drop = drops.get(0);
            String name = formatMaterialName(drop.getType().name());
            actionBarUtil.send(player,
                    "<#00FF88>✔ <gradient:#00FF88:#55FFFF>Wylosowano: "
                            + drop.getAmount() + "x " + name + "</gradient>");
        } else {
            int totalAmount = drops.stream().mapToInt(ItemStack::getAmount).sum();
            actionBarUtil.send(player,
                    "<#00FF88>✔ <gradient:#00FF88:#55FFFF>Wylosowano "
                            + drops.size() + " dropow (" + totalAmount + " przedmiotow)</gradient>");
        }
    }

    private String formatMaterialName(String materialName) {
        String[] parts = materialName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!result.isEmpty()) {
                result.append(" ");
            }
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    private boolean isMineableBlock(Material material) {
        // Rudy sa zahardkodowane w kodzie - zawsze obslugiwane przez plugin
        if (isOre(material)) {
            return true;
        }

        // Inne bloki (np. stone) sa konfigurowalne w config.yml
        List<String> mineableBlocks = plugin.getConfigManager().getConfig()
                .getStringList("settings.mineable-blocks");

        if (mineableBlocks.isEmpty()) {
            return material == Material.STONE;
        }

        return mineableBlocks.contains(material.name());
    }

    private boolean hasSilkTouch(ItemStack tool) {
        if (tool == null || !tool.hasItemMeta()) {
            return false;
        }
        return tool.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH);
    }

    private boolean isOre(Material material) {
        return ORE_BLOCKS.contains(material);
    }

    private void sendOreBlockedMessage(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = oreMessageCooldown.get(uuid);
        if (last != null && (now - last) < ORE_MESSAGE_COOLDOWN_MS) {
            return;
        }
        oreMessageCooldown.put(uuid, now);

        actionBarUtil.send(player,
                "<#FFAA00>⚠ <gradient:#FFAA00:#FF5555>Domyślne dropy z rudy zostały zablokowane</gradient>");
    }
}
