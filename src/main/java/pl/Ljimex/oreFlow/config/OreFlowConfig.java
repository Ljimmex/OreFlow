package pl.Ljimex.oreFlow.config;

import pl.Ljimex.oreFlow.OreFlow;

import org.bukkit.Material;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Type-safe wrapper dla głównego configu (config.yml).
 * Ładuje wartości raz przy starcie/reloadzie i cache'uje je.
 */
public class OreFlowConfig {

    private final OreFlow plugin;

    private String language;
    private String worldMode;
    private Set<String> worlds;
    private int maxDropsPerBlock;
    private boolean dropToInventory;
    private boolean expEnabled;
    private boolean cobblestoneEnabled;
    private int baseExp;
    private double expMultiplier;
    private String expMode;
    private Set<Material> mineableBlocks;

    private boolean fortuneEnabled;
    private boolean allowHigherFortune;

    private boolean cobbleXEnabled;

    public OreFlowConfig(OreFlow plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        var config = plugin.getConfigManager().getConfig();

        this.language = config.getString("settings.language", "pl").toLowerCase();
        this.worldMode = config.getString("settings.world-mode", "blacklist").toLowerCase();
        this.worlds = Set.copyOf(config.getStringList("settings.worlds"));
        this.maxDropsPerBlock = Math.max(0, config.getInt("settings.max-drops-per-block", 0));
        this.dropToInventory = config.getBoolean("settings.drop-to-inventory", true);
        this.expEnabled = config.getBoolean("settings.exp-enabled", true);
        this.cobblestoneEnabled = config.getBoolean("settings.cobblestone-enabled", false);
        this.baseExp = Math.max(0, config.getInt("settings.base-exp", 1));
        this.expMultiplier = Math.max(0.0, config.getDouble("settings.exp-multiplier", 1.0));
        this.expMode = config.getString("settings.exp-mode", "direct-give");
        this.mineableBlocks = loadMineableBlocks(config.getStringList("settings.mineable-blocks"));

        this.fortuneEnabled = config.getBoolean("fortune.enabled", true);
        this.allowHigherFortune = config.getBoolean("fortune.allow-higher-fortune", false);

        this.cobbleXEnabled = config.getBoolean("cobblex.enabled", true);
    }

    private Set<Material> loadMineableBlocks(List<String> blockNames) {
        if (blockNames == null || blockNames.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Material> result = EnumSet.noneOf(Material.class);
        for (String name : blockNames) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                result.add(material);
            } else {
                plugin.getLogger().warning("Unknown mineable block material: " + name);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    public String getLanguage() {
        return language;
    }

    public String getWorldMode() {
        return worldMode;
    }

    public Set<String> getWorlds() {
        return worlds;
    }

    public boolean isWorldAllowed(String worldName) {
        if (worlds.isEmpty()) {
            return true;
        }
        boolean listed = worlds.contains(worldName);
        return "whitelist".equals(worldMode) ? listed : !listed;
    }

    public int getMaxDropsPerBlock() {
        return maxDropsPerBlock;
    }

    public boolean isDropToInventory() {
        return dropToInventory;
    }

    public boolean isExpEnabled() {
        return expEnabled;
    }

    public boolean isCobblestoneEnabled() {
        return cobblestoneEnabled;
    }

    public int getBaseExp() {
        return baseExp;
    }

    public double getExpMultiplier() {
        return expMultiplier;
    }

    public String getExpMode() {
        return expMode;
    }

    public Set<Material> getMineableBlocks() {
        return mineableBlocks;
    }

    public boolean isMineableBlock(Material material) {
        return mineableBlocks.contains(material);
    }

    public boolean isFortuneEnabled() {
        return fortuneEnabled;
    }

    public boolean isAllowHigherFortune() {
        return allowHigherFortune;
    }

    public boolean isCobbleXEnabled() {
        return cobbleXEnabled;
    }
}
