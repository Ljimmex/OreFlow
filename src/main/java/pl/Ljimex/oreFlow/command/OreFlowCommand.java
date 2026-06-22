package pl.Ljimex.oreFlow.command;

import pl.Ljimex.oreFlow.OreFlow;
import pl.Ljimex.oreFlow.gui.GuiManager;

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

        GuiManager guiManager = new GuiManager(plugin);
        Inventory gui = guiManager.createDropGui();
        player.openInventory(gui);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("oreflow.reload")) {
            sender.sendMessage(colorize("&cNie masz uprawnien do tej komendy! (&eoreflow.reload&c)"));
            return true;
        }

        long startTime = System.currentTimeMillis();

        try {
            plugin.getConfigManager().reloadConfigs();
            long elapsed = System.currentTimeMillis() - startTime;
            sender.sendMessage(colorize("&a&lSUKCES! &aPrzeladowano konfiguracje OreFlow w &e" + elapsed + "ms&a."));
            plugin.getLogger().info(sender.getName() + " przeladowal konfiguracje OreFlow.");
        } catch (Exception e) {
            sender.sendMessage(colorize("&c&lBLAD! &cNie udalo sie przeladowac konfiguracji. Sprawdz konsole."));
            plugin.getLogger().log(Level.SEVERE, "Blad podczas przeladowywania konfiguracji OreFlow", e);
        }

        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        if (!sender.hasPermission("oreflow.info")) {
            sender.sendMessage(colorize("&cNie masz uprawnien do tej komendy! (&eoreflow.info&c)"));
            return true;
        }

        List<String> authorsList = plugin.getDescription().getAuthors();
        String authors = String.join(", ", authorsList);

        sender.sendMessage(colorize(""));
        sender.sendMessage(colorize("&6&lOreFlow &7- &fSystem dropow ze stone"));
        sender.sendMessage(colorize("&7Wersja: &e" + plugin.getDescription().getVersion()));
        sender.sendMessage(colorize("&7Autorzy: &e" + authors));
        sender.sendMessage(colorize("&7Target: &ePaper 1.21.x"));
        sender.sendMessage(colorize(""));

        return true;
    }

    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize("&cTej komendy moze uzyc tylko gracz!"));
            return true;
        }

        if (!player.hasPermission("oreflow.admin")) {
            player.sendMessage(colorize("&cNie masz uprawnien! (&eoreflow.admin&c)"));
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

        player.sendMessage(colorize("&6&lDebug OreFlow"));
        player.sendMessage(colorize("&7Symulacja kopania &e" + blocks + " &7blokow stone..."));

        var dropManager = plugin.getDropManager();
        java.util.Map<String, Integer> totals = new java.util.HashMap<>();
        int totalDrops = 0;

        for (int i = 0; i < blocks; i++) {
            var drops = dropManager.processDrops(player, player.getLocation(), player.getInventory().getItemInMainHand());
            for (var drop : drops) {
                String name = drop.getType().name();
                totals.merge(name, drop.getAmount(), Integer::sum);
                totalDrops += drop.getAmount();
            }
        }

        player.sendMessage(colorize("&aLacznie wylosowano przedmiotow: &e" + totalDrops));
        if (totals.isEmpty()) {
            player.sendMessage(colorize("&cBrak dropow - sprawdz poziom Y, narzedzie i szanse w drops.yml"));
        } else {
            player.sendMessage(colorize("&aRozklad:"));
            totals.entrySet().stream()
                    .sorted(java.util.Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> player.sendMessage(colorize("&7- &e" + entry.getKey() + "&7: &f" + entry.getValue())));
        }

        return true;
    }

    private boolean handleCreativeMsg(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize("&cTej komendy moze uzyc tylko gracz!"));
            return true;
        }

        if (!player.hasPermission("oreflow.admin")) {
            player.sendMessage(colorize("&cNie masz uprawnien do tej komendy! (&eoreflow.admin&c)"));
            return true;
        }

        var disabled = plugin.getDisabledCreativeMessagePlayers();
        if (disabled.contains(player.getUniqueId())) {
            disabled.remove(player.getUniqueId());
            player.sendMessage(colorize("&aWiadomosc creative Mode jest &2WLACZONA&a."));
        } else {
            disabled.add(player.getUniqueId());
            player.sendMessage(colorize("&cWiadomosc creative Mode jest &4WYLACZONA&c."));
        }

        return true;
    }

    private boolean handleHelp(CommandSender sender) {
        sender.sendMessage(colorize(""));
        sender.sendMessage(colorize("&6&lOreFlow - Pomoc"));
        sender.sendMessage(colorize("&e/oreflow reload &7- Przeladowuje konfiguracje"));
        sender.sendMessage(colorize("&e/oreflow info &7- Informacje o pluginie"));
        sender.sendMessage(colorize("&e/oreflow creativemsg &7- Wlacza/wylacza wiadomosc creative (admin)"));
        sender.sendMessage(colorize("&e/oreflow debug [bloki] &7- Symulacja dropow (admin)"));
        sender.sendMessage(colorize("&e/oreflow help &7- Wyswietla te pomoc"));
        sender.sendMessage(colorize(""));
        return true;
    }

    private String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }
}
