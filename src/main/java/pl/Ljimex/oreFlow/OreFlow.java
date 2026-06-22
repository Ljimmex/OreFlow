package pl.Ljimex.oreFlow;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimex.oreFlow.cobblex.CobbleXCommand;
import pl.Ljimex.oreFlow.cobblex.CobbleXListener;
import pl.Ljimex.oreFlow.cobblex.CobbleXManager;
import pl.Ljimex.oreFlow.command.OreFlowCommand;
import pl.Ljimex.oreFlow.config.ConfigManager;
import pl.Ljimex.oreFlow.config.GuiConfigManager;
import pl.Ljimex.oreFlow.config.MessageManager;
import pl.Ljimex.oreFlow.config.PlayerSettingsManager;
import pl.Ljimex.oreFlow.drop.BlockBreakListener;
import pl.Ljimex.oreFlow.drop.DropManager;
import pl.Ljimex.oreFlow.gui.GuiListener;
import pl.Ljimex.oreFlow.gui.GuiManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class OreFlow extends JavaPlugin {

    private static OreFlow instance;
    private ConfigManager configManager;
    private GuiConfigManager guiConfigManager;
    private MessageManager messageManager;
    private DropManager dropManager;
    private GuiManager guiManager;
    private GuiListener guiListener;
    private PlayerSettingsManager playerSettingsManager;
    private final Set<UUID> disabledCreativeMessagePlayers = new HashSet<>();

    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();

        printBanner("ENABLE");

        try {
            this.configManager = new ConfigManager(this);
            this.configManager.loadConfigs();

            this.guiConfigManager = new GuiConfigManager(this);
            this.guiConfigManager.load();

            this.messageManager = new MessageManager(this);

            this.playerSettingsManager = new PlayerSettingsManager(this);
            this.playerSettingsManager.load();

            this.dropManager = new DropManager(this);
            this.guiManager = new GuiManager(this);
            this.guiListener = new GuiListener(this);

            CobbleXManager cobbleXManager = new CobbleXManager(this);
            cobbleXManager.registerRecipe();

            getServer().getPluginManager().registerEvents(
                    new BlockBreakListener(this, dropManager), this);
            getServer().getPluginManager().registerEvents(
                    new CobbleXListener(this, cobbleXManager), this);
            getServer().getPluginManager().registerEvents(
                    guiListener, this);

            getCommand("oreflow").setExecutor(new OreFlowCommand(this));
            getCommand("cx").setExecutor(new CobbleXCommand(this, cobbleXManager));

            long elapsed = System.currentTimeMillis() - startTime;
            logInfo("Plugin enabled successfully in " + elapsed + "ms");
            logInfo("Loaded " + configManager.getDrops().getKeys(false).size() + " drops");

            int generatorCount = configManager.getGenerators().getConfigurationSection("generators") != null
                    ? configManager.getGenerators().getConfigurationSection("generators").getKeys(false).size()
                    : 0;
            logInfo("Loaded " + generatorCount + " generators");
            logInfo("Mineable blocks: " + configManager.getConfig()
                    .getStringList("settings.mineable-blocks").size());
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable OreFlow!", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        printFooter();
    }

    @Override
    public void onDisable() {
        printBanner("DISABLE");

        try {
            if (configManager != null) {
                configManager.saveConfigs();
            }
            if (playerSettingsManager != null) {
                playerSettingsManager.save();
            }
            logInfo("Plugin disabled successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during disabling OreFlow!", e);
        }

        printFooter();
        instance = null;
    }

    public static OreFlow getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DropManager getDropManager() {
        return dropManager;
    }

    public GuiConfigManager getGuiConfigManager() {
        return guiConfigManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public GuiListener getGuiListener() {
        return guiListener;
    }

    public PlayerSettingsManager getPlayerSettingsManager() {
        return playerSettingsManager;
    }

    public Set<UUID> getDisabledCreativeMessagePlayers() {
        return disabledCreativeMessagePlayers;
    }

    private void printBanner(String mode) {
        String version = getDescription().getVersion();
        String authors = String.join(", ", getDescription().getAuthors());

        logRaw(" ");
        logRaw("+==========================================================+");
        logRaw("|                                                          |");
        logRaw("|   >> OreFlow v" + padRight(version, 43) + "|");
        logRaw("|      Status: " + (mode.equals("ENABLE") ? "ENABLED" : "DISABLED") + padRight("", 39) + "|");
        logRaw("|                                                          |");
        logRaw("|   Author:  " + padRight(authors, 45) + "|");
        logRaw("|   Target:  " + padRight("Paper 1.21.x", 45) + "|");
        logRaw("|                                                          |");
        logRaw("+==========================================================+");
        logRaw(" ");
    }

    private void printFooter() {
        logRaw(" ");
    }

    private void logInfo(String message) {
        getLogger().info("[OreFlow] " + message);
    }

    private void logRaw(String message) {
        getLogger().info(message);
    }

    private String padRight(String s, int n) {
        if (s == null) s = "";
        if (s.length() > n) {
            return s.substring(0, n - 3) + "...";
        }
        return String.format("%-" + n + "s", s);
    }
}
