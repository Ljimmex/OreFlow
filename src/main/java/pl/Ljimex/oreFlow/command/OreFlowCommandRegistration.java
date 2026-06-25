package pl.Ljimex.oreFlow.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
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
            plugin.getConfigManager().getDrops().getKeys(false).stream()
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
            ctx.getSource().getSender().sendMessage(colorize("&cTej komendy moze uzyc tylko gracz!"));
            return 0;
        }

        Inventory gui = plugin.getGuiManager().createMainMenu(player);
        player.openInventory(gui);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeDropGui(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        Player player = getPlayer(ctx.getSource());
        if (player == null) {
            ctx.getSource().getSender().sendMessage(colorize("&cTej komendy moze uzyc tylko gracz!"));
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
            ctx.getSource().getSender().sendMessage(plugin.getMessageManager().getMessage("commands.player-only"));
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
            plugin.getConfigManager().reloadConfigs();
            plugin.getGuiConfigManager().reload();

            if (plugin.getCobbleXManager() != null) {
                plugin.getCobbleXManager().registerRecipe();
            }
            if (plugin.getStoneGeneratorManager() != null) {
                plugin.getStoneGeneratorManager().registerRecipe();
            }

            long elapsed = System.currentTimeMillis() - startTime;
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.reload-success",
                    "time", String.valueOf(elapsed)));
            plugin.getLogger().info(sender.getName() + " przeladowal konfiguracje OreFlow.");
        } catch (Exception e) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.reload-error"));
            plugin.getLogger().log(Level.SEVERE, "Blad podczas przeladowywania konfiguracji OreFlow", e);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeInfo(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        CommandSender sender = ctx.getSource().getSender();
        List<String> authorsList = plugin.getDescription().getAuthors();
        String authors = String.join(", ", authorsList);

        sender.sendMessage("");
        sender.sendMessage(colorize("&6&lOreFlow &7- &fSystem dropow ze stone"));
        sender.sendMessage(colorize("&7Wersja: &e" + plugin.getDescription().getVersion()));
        sender.sendMessage(colorize("&7Autorzy: &e" + authors));
        sender.sendMessage(colorize("&7Target: &ePaper 1.21.x"));
        sender.sendMessage("");

        return Command.SINGLE_SUCCESS;
    }

    private static int executeCreativeMsg(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        Player player = getPlayer(ctx.getSource());
        if (player == null) {
            ctx.getSource().getSender().sendMessage(plugin.getMessageManager().getMessage("commands.player-only"));
            return 0;
        }

        var disabled = plugin.getDisabledCreativeMessagePlayers();
        if (disabled.contains(player.getUniqueId())) {
            disabled.remove(player.getUniqueId());
            player.sendMessage(plugin.getMessageManager().getMessage("commands.creativemsg-enabled"));
        } else {
            disabled.add(player.getUniqueId());
            player.sendMessage(plugin.getMessageManager().getMessage("commands.creativemsg-disabled"));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeCobble(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        Player player = getPlayer(ctx.getSource());
        if (player == null) {
            ctx.getSource().getSender().sendMessage(plugin.getMessageManager().getMessage("commands.player-only"));
            return 0;
        }

        boolean newState = plugin.getPlayerSettingsManager().toggleCobble(player.getUniqueId());
        player.sendMessage(newState
                ? plugin.getMessageManager().getMessage("commands.cobble-enabled")
                : plugin.getMessageManager().getMessage("commands.cobble-disabled"));

        return Command.SINGLE_SUCCESS;
    }

    private static int executeToggle(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        Player player = getPlayer(ctx.getSource());
        if (player == null) {
            ctx.getSource().getSender().sendMessage(plugin.getMessageManager().getMessage("commands.player-only"));
            return 0;
        }

        String dropKey = StringArgumentType.getString(ctx, "drop");
        if (!plugin.getConfigManager().getDrops().contains(dropKey)) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.invalid-drop",
                    "drop", dropKey));
            return 0;
        }

        boolean enabled = plugin.getPlayerSettingsManager().toggleDrop(player.getUniqueId(), dropKey);
        player.sendMessage(enabled
                ? plugin.getMessageManager().getMessage("commands.drop-toggled-on", "drop", dropKey)
                : plugin.getMessageManager().getMessage("commands.drop-toggled-off", "drop", dropKey));
        return Command.SINGLE_SUCCESS;
    }

    private static int executeToggleOther(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        Player player = getPlayer(ctx.getSource());
        if (player == null) {
            ctx.getSource().getSender().sendMessage(plugin.getMessageManager().getMessage("commands.player-only"));
            return 0;
        }

        String dropKey = StringArgumentType.getString(ctx, "drop");
        if (!plugin.getConfigManager().getDrops().contains(dropKey)) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.invalid-drop",
                    "drop", dropKey));
            return 0;
        }

        String targetName = StringArgumentType.getString(ctx, "gracz");
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.player-not-found",
                    "player", targetName));
            return 0;
        }

        boolean enabled = plugin.getPlayerSettingsManager().toggleDrop(target.getUniqueId(), dropKey);
        player.sendMessage(enabled
                ? plugin.getMessageManager().getMessage("commands.drop-toggled-on-other",
                        "drop", dropKey, "target", target.getName())
                : plugin.getMessageManager().getMessage("commands.drop-toggled-off-other",
                        "drop", dropKey, "target", target.getName()));
        target.sendMessage(enabled
                ? plugin.getMessageManager().getMessage("commands.drop-toggled-on-by",
                        "drop", dropKey, "player", player.getName())
                : plugin.getMessageManager().getMessage("commands.drop-toggled-off-by",
                        "drop", dropKey, "player", player.getName()));
        return Command.SINGLE_SUCCESS;
    }

    private static int executeLanguage(CommandContext<CommandSourceStack> ctx, OreFlow plugin, String language) {
        CommandSender sender = ctx.getSource().getSender();

        if (language == null) {
            String current = plugin.getConfigManager().getConfig().getString("settings.language", "pl");
            sender.sendMessage(colorize("&7Aktualny jezyk: &e" + current));
            sender.sendMessage(colorize("&7Uzycie: &e/oreflow language <pl|en|de>"));
            return Command.SINGLE_SUCCESS;
        }

        String langLower = language.toLowerCase();
        List<String> available = List.of("pl", "en", "de");
        if (!available.contains(langLower)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.language-invalid",
                    "languages", String.join(", ", available)));
            return 0;
        }

        plugin.getConfigManager().getConfig().set("settings.language", langLower);
        plugin.saveConfig();
        plugin.getConfigManager().reloadConfigs();
        plugin.getGuiConfigManager().reload();

        sender.sendMessage(plugin.getMessageManager().getMessage("commands.language-changed",
                "language", langLower.toUpperCase()));
        return Command.SINGLE_SUCCESS;
    }

    private static int executeDebug(CommandContext<CommandSourceStack> ctx, OreFlow plugin, int blocks, int fortuneLevel) {
        Player player = getPlayer(ctx.getSource());
        if (player == null) {
            ctx.getSource().getSender().sendMessage(plugin.getMessageManager().getMessage("commands.player-only"));
            return 0;
        }

        player.sendMessage(plugin.getMessageManager().getMessage("commands.debug-header"));
        player.sendMessage(plugin.getMessageManager().getMessage("commands.debug-blocks",
                "blocks", String.valueOf(blocks)));

        ItemStack tool;
        String fortuneInfo;
        if (fortuneLevel >= 0) {
            tool = new ItemStack(Material.DIAMOND_PICKAXE);
            if (fortuneLevel > 0) {
                tool.addEnchantment(Enchantment.FORTUNE, fortuneLevel);
                fortuneInfo = "Fortune " + fortuneLevel;
            } else {
                fortuneInfo = "brak Fortune";
            }
        } else {
            tool = player.getInventory().getItemInMainHand();
            int actualFortune = tool.getEnchantmentLevel(Enchantment.FORTUNE);
            fortuneInfo = actualFortune > 0 ? "Fortune " + actualFortune : "brak Fortune";
        }
        player.sendMessage(colorize("&7Symulacja dla narzedzia: &e" + fortuneInfo));

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

        player.sendMessage(plugin.getMessageManager().getMessage("commands.debug-total",
                "total", String.valueOf(totalDrops)));
        if (totals.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.debug-empty"));
        } else {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.debug-distribution"));
            totals.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> player.sendMessage(plugin.getMessageManager().getMessage("commands.debug-entry",
                            "material", entry.getKey(),
                            "amount", String.valueOf(entry.getValue()))));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeHelp(CommandContext<CommandSourceStack> ctx, OreFlow plugin) {
        CommandSender sender = ctx.getSource().getSender();
        sender.sendMessage("");
        sender.sendMessage(colorize("&6&lOreFlow - Pomoc"));
        sender.sendMessage(colorize("&e/oreflow reload &7- Przeladowuje konfiguracje"));
        sender.sendMessage(colorize("&e/oreflow info &7- Informacje o pluginie"));
        sender.sendMessage(colorize("&e/oreflow cobble &7- Wlacza/wylacza drop cobblestone"));
        sender.sendMessage(colorize("&e/oreflow toggle <drop> [gracz] &7- Wlacza/wylacza drop"));
        sender.sendMessage(colorize("&e/oreflow language <pl|en|de> &7- Zmienia jezyk"));
        sender.sendMessage(colorize("&e/oreflow admin &7- Panel administratora"));
        sender.sendMessage(colorize("&e/oreflow creativemsg &7- Wlacza/wylacza wiadomosc creative (admin)"));
        sender.sendMessage(colorize("&e/oreflow debug [bloki] [fortune] &7- Symulacja dropow (admin)"));
        sender.sendMessage(colorize("&e/oreflow help &7- Wyswietla te pomoc"));
        sender.sendMessage("");
        return Command.SINGLE_SUCCESS;
    }

    private static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }
}
