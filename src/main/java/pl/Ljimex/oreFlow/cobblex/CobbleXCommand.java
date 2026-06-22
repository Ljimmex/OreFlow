package pl.Ljimex.oreFlow.cobblex;

import pl.Ljimex.oreFlow.OreFlow;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Level;

public class CobbleXCommand implements CommandExecutor {

    private final OreFlow plugin;
    private final CobbleXManager cobbleXManager;

    public CobbleXCommand(OreFlow plugin, CobbleXManager cobbleXManager) {
        this.plugin = plugin;
        this.cobbleXManager = cobbleXManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(colorize("&cUzyj: &e/cx craft&7, &e/cx drop&7, &e/cx info"));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "craft":
                return handleCraft(sender);
            case "drop":
                return handleDrop(sender);
            case "info":
                return handleInfo(sender);
            case "give":
                return handleGive(sender, args);
            case "reload":
                return handleReload(sender);
            default:
                sender.sendMessage(colorize("&cNieznana subkomenda. Uzyj &e/cx help"));
                return true;
        }
    }

    private boolean handleCraft(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize("&cTej komendy moze uzyc tylko gracz!"));
            return true;
        }

        if (!player.hasPermission("cobblex.use")) {
            player.sendMessage(colorize("&cNie masz uprawnien!"));
            return true;
        }

        int cost = cobbleXManager.getCraftingCost();
        int cobbleCount = countCobblestone(player);

        if (cobbleCount < cost) {
            player.sendMessage(colorize("&cBrakuje cobble! Potrzebujesz &e" + cost + " &csztuk (" + (cost / 64) + " stakow)."));
            return true;
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

        return true;
    }

    private boolean handleDrop(CommandSender sender) {
        if (!sender.hasPermission("cobblex.use")) {
            sender.sendMessage(colorize("&cNie masz uprawnien!"));
            return true;
        }

        sender.sendMessage(colorize(""));
        sender.sendMessage(colorize("&6&lCobbleX - Tabela nagrod"));
        sender.sendMessage(colorize("&7Pelna lista nagrod i ich szans bedzie dostepna"));
        sender.sendMessage(colorize("&7w GUI w ramach Sprintu 11."));
        sender.sendMessage(colorize(""));

        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        sender.sendMessage(colorize(""));
        sender.sendMessage(colorize("&6&lCobbleX"));
        sender.sendMessage(colorize("&7Skrzynia craftowana z 9 stakow cobblestone."));
        sender.sendMessage(colorize("&7Kliknij PPM, aby otworzyc i wylosowac nagrode."));
        sender.sendMessage(colorize("&7Uzyj &e/cx craft &7aby wytworzyc automatycznie."));
        sender.sendMessage(colorize(""));
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cobblex.admin")) {
            sender.sendMessage(colorize("&cNie masz uprawnien! (&ecobblex.admin&c)"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(colorize("&cUzyj: &e/cx give <gracz> <ilość>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(colorize("&cNie znaleziono gracza: &e" + args[1]));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(colorize("&cNieprawidlowa ilosc!"));
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(colorize("&cIlosc musi byc wieksza od 0!"));
            return true;
        }

        ItemStack cobbleX = cobbleXManager.createCobbleX(amount);
        target.getInventory().addItem(cobbleX);
        sender.sendMessage(colorize("&aDano &e" + amount + "x CobbleX &agraczowi &e" + target.getName() + "&a."));
        target.sendMessage(colorize("&aOtrzymales &e" + amount + "x CobbleX&a!"));

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("cobblex.admin")) {
            sender.sendMessage(colorize("&cNie masz uprawnien! (&ecobblex.admin&c)"));
            return true;
        }

        try {
            plugin.getConfigManager().reloadConfigs();
            sender.sendMessage(colorize("&a&lSUKCES! &aPrzeladowano konfiguracje CobbleX."));
        } catch (Exception e) {
            sender.sendMessage(colorize("&c&lBLAD! &cNie udalo sie przeladowac konfiguracji."));
            plugin.getLogger().log(Level.SEVERE, "Blad podczas przeladowywania CobbleX", e);
        }

        return true;
    }

    private int countCobblestone(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == org.bukkit.Material.COBBLESTONE) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeCobblestone(Player player, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == org.bukkit.Material.COBBLESTONE) {
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

    private String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }
}
