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
import pl.Ljimex.oreFlow.config.MessageManager;
import pl.Ljimex.oreFlow.config.PlayerSettingsManager;
import pl.Ljimex.oreFlow.util.ActionBarUtil;
import pl.Ljimex.oreFlow.util.ItemColorUtil;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class BlockBreakListener implements Listener {

    private final OreFlow plugin;
    private final DropManager dropManager;
    private final PlayerSettingsManager playerSettings;
    private final MessageManager messageManager;
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
        this.playerSettings = plugin.getPlayerSettingsManager();
        this.messageManager = plugin.getMessageManager();
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

        // Tryb kreatywny - brak dropow + opcjonalna wiadomosc Action Bar (tylko przy uzyciu kilofa)
        if (player.getGameMode() == GameMode.CREATIVE) {
            ItemStack tool = player.getInventory().getItemInMainHand();
            if (tool.getType().name().endsWith("_PICKAXE")
                    && player.hasPermission("oreflow.mine")
                    && !disabledCreativeMessage.contains(player.getUniqueId())) {
                sendCreativeMessage(player);
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

        // Wyłączenie domyslnych dropow
        boolean isOre = isOre(block.getType());
        boolean disableDefaultDrops;

        if (isOre) {
            // Rudy: zawsze wylaczamy domyslne dropy - zadne rudy nie maja dropic vanilla
            disableDefaultDrops = true;
        } else {
            // Stone: domyslne dropy (cobblestone) zaleza od ustawien gracza
            disableDefaultDrops = !playerSettings.isCobbleEnabled(player.getUniqueId());
        }

        if (disableDefaultDrops) {
            event.setDropItems(false);
            event.setExpToDrop(0);

            // Informacja o zablokowanych domyslnych dropach z rud
            if (isOre) {
                sendOreBlockedMessage(player, block.getType());
            }
        }

        // Przetworzenie dropow - tylko dla blokow kamiennych, nie dla rud
        if (!isOre) {
            List<DropResult> drops = dropManager.processDrops(player, block.getLocation(), tool);
            if (!drops.isEmpty()) {
                List<ItemStack> items = drops.stream().map(DropResult::item).toList();
                dropManager.deliverDrops(player, block.getLocation(), items);
                sendDropMessage(player, drops);
            }
        }

        // Bazowy EXP za zniszczenie bloku
        if (playerSettings.isExpEnabled(player.getUniqueId())) {
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
    }

    private void sendCreativeMessage(Player player) {
        if (!messageManager.isActionBarEnabled("creative")) {
            return;
        }
        actionBarUtil.send(player, messageManager.getActionBarMessage("creative",
                "prefix", messageManager.getRaw("actionbar.prefix", new HashMap<>())),
                messageManager.getActionBarDuration("creative"));
    }

    private void sendDropMessage(Player player, List<DropResult> drops) {
        List<DropResult> visible = drops.stream()
                .filter(drop -> playerSettings.isDropMessageEnabled(player.getUniqueId(), drop.dropKey()))
                .toList();

        if (visible.isEmpty()) {
            return;
        }

        String prefix = messageManager.getRaw("actionbar.prefix", new HashMap<>());

        if (visible.size() == 1) {
            if (!messageManager.isActionBarEnabled("drop-success")) {
                return;
            }
            DropResult drop = visible.get(0);
            String coloredName = formatColoredMaterialName(drop.item().getType());
            actionBarUtil.send(player, messageManager.getActionBarMessage("drop-success",
                    "prefix", prefix,
                    "amount", String.valueOf(drop.item().getAmount()),
                    "material", coloredName),
                    messageManager.getActionBarDuration("drop-success"));
        } else {
            if (!messageManager.isActionBarEnabled("drop-success-multi")) {
                return;
            }
            String dropsText = visible.stream()
                    .map(drop -> "<white>+" + drop.item().getAmount() + " " + formatColoredMaterialName(drop.item().getType()))
                    .collect(Collectors.joining("<dark_gray>, "));
            actionBarUtil.send(player, messageManager.getActionBarMessage("drop-success-multi",
                    "prefix", prefix,
                    "drops", dropsText),
                    messageManager.getActionBarDuration("drop-success-multi"));
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

    private String formatColoredMaterialName(Material material) {
        String color = getItemColor(material);
        return "<" + color + ">" + formatMaterialName(material.name()) + "</" + color + ">";
    }

    private String getItemColor(Material material) {
        return ItemColorUtil.getColor(material);
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

    private void sendOreBlockedMessage(Player player, Material ore) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = oreMessageCooldown.get(uuid);
        if (last != null && (now - last) < ORE_MESSAGE_COOLDOWN_MS) {
            return;
        }
        oreMessageCooldown.put(uuid, now);

        if (!messageManager.isActionBarEnabled("ore-blocked")) {
            return;
        }
        String prefix = messageManager.getRaw("actionbar.prefix", new HashMap<>());
        actionBarUtil.send(player, messageManager.getActionBarMessage("ore-blocked",
                "prefix", prefix),
                messageManager.getActionBarDuration("ore-blocked"));
    }
}
