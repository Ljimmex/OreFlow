package pl.Ljimex.oreFlow.cobblex;

import pl.Ljimex.oreFlow.OreFlow;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CobbleXListener implements Listener {

    private final OreFlow plugin;
    private final CobbleXManager cobbleXManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public CobbleXListener(OreFlow plugin, CobbleXManager cobbleXManager) {
        this.plugin = plugin;
        this.cobbleXManager = cobbleXManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getOreFlowConfig().isCobbleXEnabled()) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!cobbleXManager.isCobbleX(item)) {
            return;
        }

        event.setCancelled(true);

        if (!player.hasPermission("cobblex.use")) {
            plugin.getMessageManager().send(player, "commands.no-permission",
                    "permission", "cobblex.use");
            return;
        }

        if (isOnCooldown(player)) {
            long remaining = getRemainingCooldown(player);
            plugin.getMessageManager().send(player, "commands.cx-cooldown",
                    "time", String.valueOf(remaining));
            return;
        }

        // Shift + PPM = bulk open (podstawowa wersja - otwiera 1 naraz)
        // Pelny bulk open bedzie w Sprint 11
        int amount = player.isSneaking() ? Math.min(item.getAmount(), 1) : 1;

        openCobbleX(player, amount);
        setCooldown(player);
    }

    private void openCobbleX(Player player, int amount) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getAmount() < amount) {
            amount = item.getAmount();
        }

        item.setAmount(item.getAmount() - amount);

        for (int i = 0; i < amount; i++) {
            CobbleXManager.CobbleXDrop drop = cobbleXManager.rollDrop();
            if (drop == null) {
                continue;
            }

            String command = drop.getCommand();
            if (command != null && !command.isEmpty()) {
                String formattedCommand = command.replace("%player", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
            }

            player.sendMessage(plugin.getMessageManager().deserialize(drop.getMessage()));
        }
    }

    private boolean isOnCooldown(Player player) {
        if (player.hasPermission("cobblex.bypass")) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        long cooldownSeconds = cobbleXManager.getCooldown();
        long cooldownMillis = cooldownSeconds * 1000;
        return cooldowns.containsKey(uuid) &&
                System.currentTimeMillis() - cooldowns.get(uuid) < cooldownMillis;
    }

    private long getRemainingCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        long cooldownSeconds = cobbleXManager.getCooldown();
        long cooldownMillis = cooldownSeconds * 1000;
        long elapsed = System.currentTimeMillis() - cooldowns.get(uuid);
        long remaining = (cooldownMillis - elapsed) / 1000;
        return Math.max(0, remaining);
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
}
