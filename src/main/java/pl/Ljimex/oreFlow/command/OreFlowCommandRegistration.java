package pl.Ljimex.oreFlow.command;

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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import pl.Ljimex.oreFlow.OreFlow;
import pl.Ljimex.oreFlow.drop.DropResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class OreFlowCommandRegistration {

    private static final SuggestionProvider<CommandSourceStack> DROP_SUGGESTIONS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        OreFlow plugin = getPlugin(ctx);
        if (plugin != null) {
            plugin.getDropConfigManager().getDrops().stream()
                    .map(drop -> drop.getKey())
                    .filter(key -> key.toLowerCase().startsWith(remaining))
                    .forEach(builder::suggest);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> PLAYER_SUGGESTIONS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> LANGUAGE_SUGGESTIONS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        List.of("pl", "en", "de").stream()
                .filter(lang -> lang.startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    };

    public static void register(Commands commands, OreFlow plugin) {
        commands.register(
                Commands.literal("oreflow")
                        .requires(src -> src.getSender().hasPermission("oreflow.gui"))
                        .executes(ctx -> executeGui(ctx, plugin))
                        .then(Commands.literal("reload")
                                .requires(src -> src.getSender().hasPermission("oreflow.reload"))
                                .executes(ctx -> executeReload(ctx, plugin)))
                        .then(Commands.literal("info")
                                .requires(src -> src.getSender().hasPermission("oreflow.info"))
                                .executes(ctx -> executeInfo(ctx, plugin)))
                        .then(Commands.literal("help")
                                .requires(src -> src.getSender().hasPermission("oreflow.help"))
                                .executes(ctx -> executeHelp(ctx, plugin)))
                        .then(Commands.literal("creativemsg")
                                .requires(src -> src.getSender() instanceof Player && src.getSender().hasPermission("oreflow.admin"))
                                .executes(ctx -> executeCreativeMsg(ctx, plugin)))
                        .then(Commands.literal("cobble")
                                .requires(src -> src.getSender() instanceof Player && src.getSender().hasPermission("oreflow.cobble"))
                                .executes(ctx -> executeCobble(ctx, plugin)))
                        .then(Commands.literal("admin")
                                .requires(src -> src.getSender() instanceof Player && src.getSender().hasPermission("oreflow.admin"))
                                .executes(ctx -> executeAdmin(ctx, plugin)))
                        .then(Commands.literal("debug")
                                .requires(src -> src.getSender() instanceof Player && src.getSender().hasPermission("oreflow.admin"))
                                .executes(ctx -> executeDebug(ctx, plugin, 100, -1))
                                .then(Commands.argument("bloki", IntegerArgumentType.integer(1))
                                        .executes(ctx -> executeDebug(ctx, plugin,
                                                IntegerArgumentType.getInteger(ctx, "bloki"), -1))
                                        .then(Commands.argument("fortune", IntegerArgumentType.integer(0))
                                                .executes(ctx -> executeDebug(ctx, plugin,
                                                        IntegerArgumentType.getInteger(ctx, "bloki"),
                                                        IntegerArgumentType.getInteger(ctx, "fortune"))))))
                        .then(Commands.literal("toggle")
                                .requires(src -> src.getSender() instanceof Player && src.getSender().hasPermission("oreflow.toggle"))
                                .then(Commands.argument("drop", StringArgumentType.word())
                                        .suggests(DROP_SUGGESTIONS)
                                        .executes(ctx -> executeToggle(ctx, plugin))
                                        .then(Commands.argument("gracz", StringArgumentType.word())
                                                .suggests(PLAYER_SUGGESTIONS)
                                                .requires(src -> src.getSender().hasPermission("oreflow.toggle.others"))
                                                .executes(ctx -> executeToggleOther(ctx, plugin)))))
                        .then(Commands.literal("language")
                                .requires(src -> src.getSender().hasPermission("oreflow.admin"))
                                .executes(ctx -> executeLanguage(ctx, plugin, null))
                                .then(Commands.argument("jezyk", StringArgumentType.word())
                                        .suggests(LANGUAGE_SUGGESTIONS)
                                        .executes(ctx -> executeLanguage(ctx, plugin,
                                                StringArgumentType.getString(ctx, "jezyk")))))
                        .then(Commands.literal("lang")
                                .requires(src -> src.getSender().hasPermission("oreflow.admin"))
                                .executes(ctx -> executeLanguage(ctx, plugin, null))
                                .then(Commands.argument("jezyk", StringArgumentType.word())
                                        .suggests(LANGUAGE_SUGGESTIONS)
                                        .executes(ctx -> executeLanguage(ctx, plugin,
                                                StringArgumentType.getString(ctx, "jezyk")))))
                        .build(),
                "Zarzadzanie pluginem OreFlow",
                List.of("of")
        );

        commands.register(
                Commands.literal("drop")
                        .requires(src -> src.getSender() instanceof Player && src.getSender().hasPermission("oreflow.gui"))
                        .executes(ctx -> executeDropGui(ctx, plugin))
                        .build(),
                "Otwiera GUI dropow",
                List.of()
        );
    }

    private static OreFlow getPlugin(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (sender.getServer().getPluginManager().getPlugin("OreFlow") instanceof OreFlow plugin) {
            return plugin;
        }
        return null;
    }

    private static Player getPlayer(CommandSourceStack source) {
        if (source.getSender() instanceof Player player) {
            return player;
        }
        return null;
    }

    private static int executeGui(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        Player player = getPlayer(ctx.getSource());
        if (player == null) {
            plugin.getMessageManager().send(ctx.getSource().getSender(), "commands.player-only");
            return 0;
        }

        Inventory gui = plugin.getGuiManager().createMainMenu(player);
        player.openInventory(gui);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeDropGui(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        Player player = getPlayer(ctx.getSource());
        if (player == null) {
            plugin.getMessageManager().send(ctx.getSource().getSender(), "commands.player-only");
            return 0;
        }

        plugin.getGuiListener().setPlayerPage(player, 0);
        Inventory gui = plugin.getGuiManager().createDropGui(player, 0);
        player.openInventory(gui);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeAdmin(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        Player player = getPlayer(ctx.getSource());
        if (player == null) {
            plugin.getMessageManager().send(ctx.getSource().getSender(), "commands.player-only");
            return 0;
        }

        plugin.getGuiListener().setAdminPage(player, 0);
        Inventory gui = plugin.getGuiManager().createAdminGui(player, 0);
        player.openInventory(gui);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeReload(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        CommandSender sender = ctx.getSource().getSender();
        long startTime = System.currentTimeMillis();

        try {
            plugin.reload();

            long elapsed = System.currentTimeMillis() - startTime;
            plugin.getMessageManager().send(sender, "commands.reload-success",
                    "time", String.valueOf(elapsed));
            plugin.getLogger().info(sender.getName() + " przeladowal konfiguracje OreFlow.");
        } catch (Exception e) {
            plugin.getMessageManager().send(sender, "commands.reload-error");
            plugin.getLogger().log(Level.SEVERE, "Blad podczas przeladowywania konfiguracji OreFlow", e);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeInfo(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        CommandSender sender = ctx.getSource().getSender();
        List<String> authorsList = plugin.getPluginMeta().getAuthors();
        String authors = String.join(", ", authorsList);

        sender.sendMessage(Component.empty());
        plugin.getMessageManager().send(sender, "commands.info-header");
        plugin.getMessageManager().send(sender, "commands.info-version",
                "version", plugin.getPluginMeta().getVersion());
        plugin.getMessageManager().send(sender, "commands.info-authors",
                "authors", authors);
        plugin.getMessageManager().send(sender, "commands.info-target",
                "target", "Paper 1.21.x");
        sender.sendMessage(Component.empty());

        return Command.SINGLE_SUCCESS;
    }

    private static int executeCreativeMsg(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        Player player = getPlayer(ctx.getSource());
        if (player == null) {
            plugin.getMessageManager().send(ctx.getSource().getSender(), "commands.player-only");
            return 0;
        }

        var disabled = plugin.getDisabledCreativeMessagePlayers();
        if (disabled.contains(player.getUniqueId())) {
            disabled.remove(player.getUniqueId());
            plugin.getMessageManager().send(player, "commands.creativemsg-enabled");
        } else {
            disabled.add(player.getUniqueId());
            plugin.getMessageManager().send(player, "commands.creativemsg-disabled");
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeCobble(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        Player player = getPlayer(ctx.getSource());
        if (player == null) {
            plugin.getMessageManager().send(ctx.getSource().getSender(), "commands.player-only");
            return 0;
        }

        boolean newState = plugin.getPlayerSettingsManager().toggleCobble(player.getUniqueId());
        plugin.getMessageManager().send(player, newState
                ? "commands.cobble-enabled"
                : "commands.cobble-disabled");

        return Command.SINGLE_SUCCESS;
    }

    private static int executeToggle(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        Player player = getPlayer(ctx.getSource());
        if (player == null) {
            plugin.getMessageManager().send(ctx.getSource().getSender(), "commands.player-only");
            return 0;
        }

        String dropKey = StringArgumentType.getString(ctx, "drop");
        if (!plugin.getDropConfigManager().hasDrop(dropKey)) {
            plugin.getMessageManager().send(player, "commands.invalid-drop",
                    "drop", dropKey);
            return 0;
        }

        boolean enabled = plugin.getPlayerSettingsManager().toggleDrop(player.getUniqueId(), dropKey);
        plugin.getMessageManager().send(player, enabled
                ? "commands.drop-toggled-on"
                : "commands.drop-toggled-off", "drop", dropKey);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeToggleOther(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        Player player = getPlayer(ctx.getSource());
        if (player == null) {
            plugin.getMessageManager().send(ctx.getSource().getSender(), "commands.player-only");
            return 0;
        }

        String dropKey = StringArgumentType.getString(ctx, "drop");
        if (!plugin.getDropConfigManager().hasDrop(dropKey)) {
            plugin.getMessageManager().send(player, "commands.invalid-drop",
                    "drop", dropKey);
            return 0;
        }

        String targetName = StringArgumentType.getString(ctx, "gracz");
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            plugin.getMessageManager().send(player, "commands.player-not-found",
                    "player", targetName);
            return 0;
        }

        boolean enabled = plugin.getPlayerSettingsManager().toggleDrop(target.getUniqueId(), dropKey);
        plugin.getMessageManager().send(player, enabled
                ? "commands.drop-toggled-on-other"
                : "commands.drop-toggled-off-other",
                "drop", dropKey, "target", target.getName());
        plugin.getMessageManager().send(target, enabled
                ? "commands.drop-toggled-on-by"
                : "commands.drop-toggled-off-by",
                "drop", dropKey, "player", player.getName());
        return Command.SINGLE_SUCCESS;
    }

    private static int executeLanguage(CommandContext<CommandSourceStack> ctx, OreFlow plugin, String language) {
        CommandSender sender = ctx.getSource().getSender();

        if (language == null) {
            String current = plugin.getOreFlowConfig().getLanguage();
            plugin.getMessageManager().send(sender, "commands.language-current",
                    "language", current);
            plugin.getMessageManager().send(sender, "commands.language-usage");
            return Command.SINGLE_SUCCESS;
        }

        String langLower = language.toLowerCase();
        List<String> available = List.of("pl", "en", "de");
        if (!available.contains(langLower)) {
            plugin.getMessageManager().send(sender, "commands.language-invalid",
                    "languages", String.join(", ", available));
            return 0;
        }

        plugin.getConfigManager().getConfig().set("settings.language", langLower);
        plugin.getConfigManager().saveConfigs();
        plugin.reload();

        plugin.getMessageManager().send(sender, "commands.language-changed",
                "language", langLower.toUpperCase());
        return Command.SINGLE_SUCCESS;
    }

    private static int executeDebug(CommandContext<CommandSourceStack> ctx, OreFlow plugin, int blocks, int fortuneLevel) {
        Player player = getPlayer(ctx.getSource());
        if (player == null) {
            plugin.getMessageManager().send(ctx.getSource().getSender(), "commands.player-only");
            return 0;
        }

        plugin.getMessageManager().send(player, "commands.debug-header");
        plugin.getMessageManager().send(player, "commands.debug-blocks",
                "blocks", String.valueOf(blocks));

        ItemStack tool;
        String fortuneInfo;
        if (fortuneLevel >= 0) {
            tool = new ItemStack(Material.DIAMOND_PICKAXE);
            if (fortuneLevel > 0) {
                tool.addEnchantment(Enchantment.FORTUNE, fortuneLevel);
                fortuneInfo = "Fortune " + fortuneLevel;
            } else {
                fortuneInfo = plugin.getMessageManager().getRawString("commands.debug-no-fortune");
            }
        } else {
            tool = player.getInventory().getItemInMainHand();
            int actualFortune = tool.getEnchantmentLevel(Enchantment.FORTUNE);
            fortuneInfo = actualFortune > 0 ? "Fortune " + actualFortune
                    : plugin.getMessageManager().getRawString("commands.debug-no-fortune");
        }
        plugin.getMessageManager().send(player, "commands.debug-tool",
                "fortune", fortuneInfo);

        var dropManager = plugin.getDropManager();
        Map<String, Integer> totals = new HashMap<>();
        int totalDrops = 0;

        for (int i = 0; i < blocks; i++) {
            var drops = dropManager.processDrops(player, player.getLocation(), tool);
            for (DropResult drop : drops) {
                String name = drop.item().getType().name();
                totals.merge(name, drop.item().getAmount(), Integer::sum);
                totalDrops += drop.item().getAmount();
            }
        }

        plugin.getMessageManager().send(player, "commands.debug-total",
                "total", String.valueOf(totalDrops));
        if (totals.isEmpty()) {
            plugin.getMessageManager().send(player, "commands.debug-empty");
        } else {
            plugin.getMessageManager().send(player, "commands.debug-distribution");
            totals.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> plugin.getMessageManager().send(player, "commands.debug-entry",
                            "material", entry.getKey(),
                            "amount", String.valueOf(entry.getValue())));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeHelp(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        CommandSender sender = ctx.getSource().getSender();
        sender.sendMessage(Component.empty());
        plugin.getMessageManager().send(sender, "commands.help-header");
        plugin.getMessageManager().send(sender, "commands.help-reload");
        plugin.getMessageManager().send(sender, "commands.help-info");
        plugin.getMessageManager().send(sender, "commands.help-cobble");
        plugin.getMessageManager().send(sender, "commands.help-toggle");
        plugin.getMessageManager().send(sender, "commands.help-language");
        plugin.getMessageManager().send(sender, "commands.help-admin");
        plugin.getMessageManager().send(sender, "commands.help-creativemsg");
        plugin.getMessageManager().send(sender, "commands.help-debug");
        plugin.getMessageManager().send(sender, "commands.help-help");
        sender.sendMessage(Component.empty());
        return Command.SINGLE_SUCCESS;
    }
}
