package pl.Ljimex.oreFlow.cobblex;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
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
        sender.sendMessage(colorize("&cUzyj: &e/cx craft&7, &e/cx drop&7, &e/cx info"));
        return Command.SINGLE_SUCCESS;
    }

    private static int executeCraft(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        Player player = getPlayer(ctx.getSource());
        if (player == null) {
            ctx.getSource().getSender().sendMessage(colorize("&cTej komendy moze uzyc tylko gracz!"));
            return 0;
        }

        CobbleXManager cobbleXManager = plugin.getCobbleXManager();
        int cost = cobbleXManager.getCraftingCost();
        int cobbleCount = countCobblestone(player);

        if (cobbleCount < cost) {
            player.sendMessage(colorize("&cBrakuje cobble! Potrzebujesz &e" + cost + " &csztuk (" + (cost / 64) + " stakow)."));
            return 0;
        }

        removeCobblestone(player, cost);

        ItemStack cobbleX = cobbleXManager.createCobbleX(1);
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), cobbleX);
            player.sendMessage(colorize("&aWytworzono 1x CobbleX. &7Ekwipunek pelny - wyrzucono na ziemie."));
        } else {
            player.getInventory().addItem(cobbleX);
            player.sendMessage(colorize("&aWytworzono 1x CobbleX &7(koszt: " + cost + " cobble)."));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeDrop(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        CommandSender sender = ctx.getSource().getSender();
        sender.sendMessage(colorize(""));
        sender.sendMessage(colorize("&6&lCobbleX - Tabela nagrod"));
        sender.sendMessage(colorize("&7Pelna lista nagrod i ich szans bedzie dostepna"));
        sender.sendMessage(colorize("&7w GUI w ramach Sprintu 11."));
        sender.sendMessage(colorize(""));
        return Command.SINGLE_SUCCESS;
    }

    private static int executeInfo(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        CommandSender sender = ctx.getSource().getSender();
        sender.sendMessage(colorize(""));
        sender.sendMessage(colorize("&6&lCobbleX"));
        sender.sendMessage(colorize("&7Skrzynia craftowana z 9 stakow cobblestone."));
        sender.sendMessage(colorize("&7Kliknij PPM, aby otworzyc i wylosowac nagrode."));
        sender.sendMessage(colorize("&7Uzyj &e/cx craft &7aby wytworzyc automatycznie."));
        sender.sendMessage(colorize(""));
        return Command.SINGLE_SUCCESS;
    }

    private static int executeGive(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        CommandSender sender = ctx.getSource().getSender();
        String targetName = StringArgumentType.getString(ctx, "gracz");
        int amount = IntegerArgumentType.getInteger(ctx, "ilosc");

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(colorize("&cNie znaleziono gracza: &e" + targetName));
            return 0;
        }

        ItemStack cobbleX = plugin.getCobbleXManager().createCobbleX(amount);
        target.getInventory().addItem(cobbleX);
        sender.sendMessage(colorize("&aDano &e" + amount + "x CobbleX &agraczowi &e" + target.getName() + "&a."));
        target.sendMessage(colorize("&aOtrzymales &e" + amount + "x CobbleX&a!"));

        return Command.SINGLE_SUCCESS;
    }

    private static int executeReload(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        CommandSender sender = ctx.getSource().getSender();
        try {
            plugin.getConfigManager().reloadConfigs();
            sender.sendMessage(colorize("&a&lSUKCES! &aPrzeladowano konfiguracje CobbleX."));
        } catch (Exception e) {
            sender.sendMessage(colorize("&c&lBLAD! &cNie udalo sie przeladowac konfiguracji."));
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

    private static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }
}
