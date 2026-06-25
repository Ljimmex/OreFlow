package pl.Ljimex.oreFlow.cobblex;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import pl.Ljimex.oreFlow.OreFlow;

import java.util.List;
import java.util.logging.Level;

public final class CobbleXCommandRegistration {

    private static final SuggestionProvider<CommandSourceStack> PLAYER_SUGGESTIONS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    };

    public static void register(Commands commands, OreFlow plugin) {
        commands.register(
                Commands.literal("cx")
                        .executes(ctx -> executeUsage(ctx, plugin))
                        .then(Commands.literal("craft")
                                .requires(src -> src.getSender() instanceof Player && src.getSender().hasPermission("cobblex.use"))
                                .executes(ctx -> executeCraft(ctx, plugin)))
                        .then(Commands.literal("drop")
                                .requires(src -> src.getSender().hasPermission("cobblex.use"))
                                .executes(ctx -> executeDrop(ctx, plugin)))
                        .then(Commands.literal("info")
                                .executes(ctx -> executeInfo(ctx, plugin)))
                        .then(Commands.literal("give")
                                .requires(src -> src.getSender().hasPermission("cobblex.admin"))
                                .then(Commands.argument("gracz", StringArgumentType.word())
                                        .suggests(PLAYER_SUGGESTIONS)
                                        .then(Commands.argument("ilosc", IntegerArgumentType.integer(1))
                                                .executes(ctx -> executeGive(ctx, plugin)))))
                        .then(Commands.literal("reload")
                                .requires(src -> src.getSender().hasPermission("cobblex.admin"))
                                .executes(ctx -> executeReload(ctx, plugin)))
                        .build(),
                "Komenda CobbleX",
                List.of("cobblex")
        );
    }

    private static Player getPlayer(CommandSourceStack source) {
        if (source.getSender() instanceof Player player) {
            return player;
        }
        return null;
    }

    private static int executeUsage(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        CommandSender sender = ctx.getSource().getSender();
        plugin.getMessageManager().send(sender, "commands.cx-usage");
        return Command.SINGLE_SUCCESS;
    }

    private static int executeCraft(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        Player player = getPlayer(ctx.getSource());
        if (player == null) {
            plugin.getMessageManager().send(ctx.getSource().getSender(), "commands.player-only");
            return 0;
        }

        CobbleXManager cobbleXManager = plugin.getCobbleXManager();
        int cost = cobbleXManager.getCraftingCost();
        int cobbleCount = countCobblestone(player);

        if (cobbleCount < cost) {
            plugin.getMessageManager().send(player, "commands.cobblex-insufficient",
                    "required", String.valueOf(cost),
                    "have", String.valueOf(cobbleCount));
            return 0;
        }

        removeCobblestone(player, cost);

        ItemStack cobbleX = cobbleXManager.createCobbleX(1);
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), cobbleX);
            plugin.getMessageManager().send(player, "commands.cobblex-crafted-ground");
        } else {
            player.getInventory().addItem(cobbleX);
            plugin.getMessageManager().send(player, "commands.cobblex-crafted",
                    "required", String.valueOf(cost));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeDrop(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        CommandSender sender = ctx.getSource().getSender();
        sender.sendMessage(Component.empty());
        plugin.getMessageManager().send(sender, "commands.cx-drop-header");
        plugin.getMessageManager().send(sender, "commands.cx-drop-soon");
        sender.sendMessage(Component.empty());
        return Command.SINGLE_SUCCESS;
    }

    private static int executeInfo(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        CommandSender sender = ctx.getSource().getSender();
        sender.sendMessage(Component.empty());
        plugin.getMessageManager().send(sender, "commands.cx-info-header");
        plugin.getMessageManager().send(sender, "commands.cx-info-1");
        plugin.getMessageManager().send(sender, "commands.cx-info-2");
        plugin.getMessageManager().send(sender, "commands.cx-info-3");
        sender.sendMessage(Component.empty());
        return Command.SINGLE_SUCCESS;
    }

    private static int executeGive(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        CommandSender sender = ctx.getSource().getSender();
        String targetName = StringArgumentType.getString(ctx, "gracz");
        int amount = IntegerArgumentType.getInteger(ctx, "ilosc");

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            plugin.getMessageManager().send(sender, "commands.player-not-found",
                    "player", targetName);
            return 0;
        }

        ItemStack cobbleX = plugin.getCobbleXManager().createCobbleX(amount);
        target.getInventory().addItem(cobbleX);
        plugin.getMessageManager().send(sender, "commands.cx-given",
                "amount", String.valueOf(amount),
                "target", target.getName());
        plugin.getMessageManager().send(target, "commands.cx-received",
                "amount", String.valueOf(amount));

        return Command.SINGLE_SUCCESS;
    }

    private static int executeReload(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        CommandSender sender = ctx.getSource().getSender();
        try {
            plugin.reload();
            plugin.getMessageManager().send(sender, "commands.cx-reload-success");
        } catch (Exception e) {
            plugin.getMessageManager().send(sender, "commands.cx-reload-error");
            plugin.getLogger().log(Level.SEVERE, "Blad podczas przeladowywania CobbleX", e);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int countCobblestone(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COBBLESTONE) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private static void removeCobblestone(Player player, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COBBLESTONE) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    remaining -= itemAmount;
                    item.setAmount(0);
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
                if (remaining <= 0) {
                    break;
                }
            }
        }
    }
}
