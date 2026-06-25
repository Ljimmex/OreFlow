package pl.Ljimex.oreFlow.drop;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Type-safe reprezentacja jednego dropu z drops.yml.
 */
public class DropConfig {

    private final String key;
    private final Material material;
    private final double chance;
    private final int minAmount;
    private final int maxAmount;
    private final Material requiredTool;
    private final int minY;
    private final int maxY;
    private final int exp;
    private final boolean enabled;
    private final boolean fortuneEnabled;
    private final Map<Integer, FortuneLevel> fortuneLevels;

    public DropConfig(String key, ConfigurationSection section) {
        this.key = key;

        String materialName = section.getString("material", "STONE");
        Material matched = Material.matchMaterial(materialName);
        this.material = matched != null ? matched : Material.STONE;

        this.chance = Math.max(0.0, Math.min(100.0, section.getDouble("chance", 0.0)));
        this.minAmount = Math.max(1, section.getInt("min-amount", 1));
        this.maxAmount = Math.max(this.minAmount, section.getInt("max-amount", this.minAmount));

        String toolName = section.getString("required-tool", "WOODEN_PICKAXE");
        Material tool = Material.matchMaterial(toolName);
        this.requiredTool = tool != null ? tool : Material.WOODEN_PICKAXE;

        this.minY = section.getInt("min-y", -64);
        this.maxY = section.getInt("max-y", 320);
        this.exp = Math.max(0, section.getInt("exp", 0));
        this.enabled = section.getBoolean("enabled", true);
        this.fortuneEnabled = section.getBoolean("fortune.enabled", true);
        this.fortuneLevels = loadFortuneLevels(section.getConfigurationSection("fortune.levels"));
    }

    private Map<Integer, FortuneLevel> loadFortuneLevels(ConfigurationSection levelsSection) {
        if (levelsSection == null) {
            return Collections.emptyMap();
        }
        Map<Integer, FortuneLevel> result = new HashMap<>();
        for (String key : levelsSection.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                ConfigurationSection levelSection = levelsSection.getConfigurationSection(key);
                if (levelSection == null) {
                    continue;
                }
                double chance = Math.max(0.0, Math.min(100.0, levelSection.getDouble("chance", 0.0)));
                int bonusMin = Math.max(0, levelSection.getInt("bonus-min", 0));
                int bonusMax = Math.max(bonusMin, levelSection.getInt("bonus-max", bonusMin));
                result.put(level, new FortuneLevel(chance, bonusMin, bonusMax));
            } catch (NumberFormatException ignored) {
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public String getKey() {
        return key;
    }

    public Material getMaterial() {
        return material;
    }

    public double getChance() {
        return chance;
    }

    public int getMinAmount() {
        return minAmount;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public int rollBaseAmount() {
        if (minAmount >= maxAmount) {
            return minAmount;
        }
        return ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
    }

    public Material getRequiredTool() {
        return requiredTool;
    }

    public boolean canUseTool(Material tool) {
        if (tool == null) {
            return false;
        }
        int requiredTier = ToolTiers.getTier(requiredTool);
        int toolTier = ToolTiers.getTier(tool);
        return toolTier >= requiredTier;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public boolean isInYRange(int y) {
        return y >= minY && y <= maxY;
    }

    public int getExp() {
        return exp;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isFortuneEnabled() {
        return fortuneEnabled;
    }

    public FortuneLevel getFortuneLevel(int level) {
        return fortuneLevels.get(level);
    }

    public boolean hasFortuneLevel(int level) {
        return fortuneLevels.containsKey(level);
    }

    public record FortuneLevel(double chance, int bonusMin, int bonusMax) {
    }
}
