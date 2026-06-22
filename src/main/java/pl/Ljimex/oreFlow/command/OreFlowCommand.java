package pl.Ljimex.oreFlow.command;

import pl.Ljimex.oreFlow.OreFlow;
import pl.Ljimex.oreFlow.drop.DropResult;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.logging.Level;

public class OreFlowCommand implements CommandExecutor {

    private final OreFlow plugin;

    public OreFlowCommand(OreFlow plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return handleGui(sender);
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "info":
                return handleInfo(sender);
            case "help":
                return handleHelp(sender);
            case "creativemsg":
                return handleCreativeMsg(sender);
            case "cobble":
                return handleCobble(sender);
            case "toggle":
                return handleToggle(sender, args);
            case "language":
            case "lang":
                return handleLanguage(sender, args);
            case "admin":
                return handleAdmin(sender);
            case "debug":
                return handleDebug(sender, args);
            default:
                sender.sendMessage(colorize("&cNieznana subkomenda. Uzyj &e/oreflow help"));
                return true;
        }
    }

    private boolean handleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize("&cTej komendy moze uzyc tylko gracz!"));
            return true;
        }

        if (!player.hasPermission("oreflow.gui")) {
            player.sendMessage(colorize("&cNie masz uprawnien do tej komendy! (&eoreflow.gui&c)"));
            return true;
        }

        plugin.getGuiListener().setPlayerPage(player, 0);
        Inventory gui = plugin.getGuiManager().createDropGui(player, 0);
        player.openInventory(gui);
        return true;
    }

    private boolean handleAdmin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.player-only"));
            return true;
        }

        if (!player.hasPermission("oreflow.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission",
                    "permission", "oreflow.admin"));
            return true;
        }

        plugin.getGuiListener().setAdminPage(player, 0);
        Inventory gui = plugin.getGuiManager().createAdminGui(player, 0);
        player.openInventory(gui);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("oreflow.reload")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission",
                    "permission", "oreflow.reload"));
            return true;
        }

        long startTime = System.currentTimeMillis();

        try {
            plugin.getConfigManager().reloadConfigs();
            plugin.getGuiConfigManager().reload();
            long elapsed = System.currentTimeMillis() - startTime;
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.reload-success",
                    "time", String.valueOf(elapsed)));
            plugin.getLogger().info(sender.getName() + " przeladowal konfiguracje OreFlow.");
        } catch (Exception e) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.reload-error"));
            plugin.getLogger().log(Level.SEVERE, "Blad podczas przeladowywania konfiguracji OreFlow", e);
        }

        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        if (!sender.hasPermission("oreflow.info")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission",
                    "permission", "oreflow.info"));
            return true;
        }

        List<String> authorsList = plugin.getDescription().getAuthors();
        String authors = String.join(", ", authorsList);

        sender.sendMessage("");
        sender.sendMessage(colorize("&6&lOreFlow &7- &fSystem dropow ze stone"));
        sender.sendMessage(colorize("&7Wersja: &e" + plugin.getDescription().getVersion()));
        sender.sendMessage(colorize("&7Autorzy: &e" + authors));
        sender.sendMessage(colorize("&7Target: &ePaper 1.21.x"));
        sender.sendMessage("");

        return true;
    }

    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.player-only"));
            return true;
        }

        if (!player.hasPermission("oreflow.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission",
                    "permission", "oreflow.admin"));
            return true;
        }

        int blocks = 100;
        if (args.length >= 2) {
            try {
                blocks = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(colorize("&cNieprawidlowa liczba blokow!"));
                return true;
            }
        }

        player.sendMessage(plugin.getMessageManager().getMessage("commands.debug-header"));
        player.sendMessage(plugin.getMessageManager().getMessage("commands.debug-blocks",
                "blocks", String.valueOf(blocks)));

        var dropManager = plugin.getDropManager();
        java.util.Map<String, Integer> totals = new java.util.HashMap<>();
        int totalDrops = 0;

        for (int i = 0; i < blocks; i++) {
            var drops = dropManager.processDrops(player, player.getLocation(), player.getInventory().getItemInMainHand());
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
                    .sorted(java.util.Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> player.sendMessage(plugin.getMessageManager().getMessage("commands.debug-entry",
                            "material", entry.getKey(),
                            "amount", String.valueOf(entry.getValue()))));
        }

        return true;
    }

    private boolean handleCreativeMsg(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.player-only"));
            return true;
        }

        if (!player.hasPermission("oreflow.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission",
                    "permission", "oreflow.admin"));
            return true;
        }

        var disabled = plugin.getDisabledCreativeMessagePlayers();
        if (disabled.contains(player.getUniqueId())) {
            disabled.remove(player.getUniqueId());
            player.sendMessage(plugin.getMessageManager().getMessage("commands.creativemsg-enabled"));
        } else {
            disabled.add(player.getUniqueId());
            player.sendMessage(plugin.getMessageManager().getMessage("commands.creativemsg-disabled"));
        }

        return true;
    }

    private boolean handleCobble(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.player-only"));
            return true;
        }

        if (!player.hasPermission("oreflow.cobble")) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission",
                    "permission", "oreflow.cobble"));
            return true;
        }

        boolean newState = plugin.getPlayerSettingsManager().toggleCobble(player.getUniqueId());
        player.sendMessage(newState
                ? plugin.getMessageManager().getMessage("commands.cobble-enabled")
                : plugin.getMessageManager().getMessage("commands.cobble-disabled"));

        return true;
    }

    private boolean handleToggle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.player-only"));
            return true;
        }

        if (!player.hasPermission("oreflow.toggle")) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission",
                    "permission", "oreflow.toggle"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(colorize("&cUzycie: &e/oreflow toggle <drop> [gracz]"));
            return true;
        }

        String dropKey = args[1];
        if (!plugin.getConfigManager().getDrops().contains(dropKey)) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.invalid-drop",
                    "drop", dropKey));
            return true;
        }

        // Toggle dla innego gracza
        if (args.length >= 3) {
            if (!player.hasPermission("oreflow.toggle.others")) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission",
                        "permission", "oreflow.toggle.others"));
                return true;
            }

            Player target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.player-not-found",
                        "player", args[2]));
                return true;
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
            return true;
        }

        // Toggle dla siebie
        boolean enabled = plugin.getPlayerSettingsManager().toggleDrop(player.getUniqueId(), dropKey);
        player.sendMessage(enabled
                ? plugin.getMessageManager().getMessage("commands.drop-toggled-on", "drop", dropKey)
                : plugin.getMessageManager().getMessage("commands.drop-toggled-off", "drop", dropKey));
        return true;
    }

    private boolean handleLanguage(CommandSender sender, String[] args) {
        if (!sender.hasPermission("oreflow.admin")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission",
                    "permission", "oreflow.admin"));
            return true;
        }

        if (args.length < 2) {
            String current = plugin.getConfigManager().getConfig().getString("settings.language", "pl");
            sender.sendMessage(colorize("&7Aktualny jezyk: &e" + current));
            sender.sendMessage(colorize("&7Uzycie: &e/oreflow language <pl|en|de>"));
            return true;
        }

        String language = args[1].toLowerCase();
        List<String> available = List.of("pl", "en", "de");
        if (!available.contains(language)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.language-invalid",
                    "languages", String.join(", ", available)));
            return true;
        }

        plugin.getConfigManager().getConfig().set("settings.language", language);
        plugin.saveConfig();
        plugin.getConfigManager().reloadConfigs();
        plugin.getGuiConfigManager().reload();

        sender.sendMessage(plugin.getMessageManager().getMessage("commands.language-changed",
                "language", language.toUpperCase()));
        return true;
    }

    private boolean handleHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(colorize("&6&lOreFlow - Pomoc"));
        sender.sendMessage(colorize("&e/oreflow reload &7- Przeladowuje konfiguracje"));
        sender.sendMessage(colorize("&e/oreflow info &7- Informacje o pluginie"));
        sender.sendMessage(colorize("&e/oreflow cobble &7- Wlacza/wylacza drop cobblestone"));
        sender.sendMessage(colorize("&e/oreflow toggle <drop> [gracz] &7- Wlacza/wylacza drop"));
        sender.sendMessage(colorize("&e/oreflow language <pl|en|de> &7- Zmienia jezyk"));
        sender.sendMessage(colorize("&e/oreflow admin &7- Panel administratora"));
        sender.sendMessage(colorize("&e/oreflow creativemsg &7- Wlacza/wylacza wiadomosc creative (admin)"));
        sender.sendMessage(colorize("&e/oreflow debug [bloki] &7- Symulacja dropow (admin)"));
        sender.sendMessage(colorize("&e/oreflow help &7- Wyswietla te pomoc"));
        sender.sendMessage("");
        return true;
    }

    private String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }
}
