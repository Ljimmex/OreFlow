package pl.Ljimex.oreFlow.generator;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Type-safe reprezentacja jednego generatora z generators.yml.
 */
public class GeneratorConfig {

    private final String key;
    private final boolean enabled;

    private final List<String> craftingShape;
    private final Map<Character, Material> ingredients;

    private final Material itemMaterial;
    private final String itemName;
    private final List<String> itemLore;
    private final boolean itemEnchantGlow;

    private final Material generationMaterial;
    private final int generationInterval;
    private final boolean requirePlayerNearby;
    private final int radius;
    private final String soundName;
    private final String particleName;
    private final int particleCount;

    private final boolean pickupOnBreak;

    public GeneratorConfig(String key, ConfigurationSection section) {
        this.key = key;
        this.enabled = section.getBoolean("enabled", true);

        ConfigurationSection craftingSection = section.getConfigurationSection("crafting");
        if (craftingSection != null) {
            this.craftingShape = List.copyOf(craftingSection.getStringList("shape"));
            this.ingredients = loadIngredients(craftingSection.getConfigurationSection("ingredients"));
        } else {
            this.craftingShape = Collections.emptyList();
            this.ingredients = Collections.emptyMap();
        }

        ConfigurationSection itemSection = section.getConfigurationSection("item");
        if (itemSection != null) {
            Material matched = Material.matchMaterial(itemSection.getString("material", "END_STONE"));
            this.itemMaterial = matched != null ? matched : Material.END_STONE;
            this.itemName = itemSection.getString("name", "<gold>Stoniarka</gold>");
            this.itemLore = List.copyOf(itemSection.getStringList("lore"));
            this.itemEnchantGlow = itemSection.getBoolean("enchant-glow", true);
        } else {
            this.itemMaterial = Material.END_STONE;
            this.itemName = "<gold>Stoniarka</gold>";
            this.itemLore = Collections.emptyList();
            this.itemEnchantGlow = true;
        }

        ConfigurationSection generationSection = section.getConfigurationSection("generation");
        if (generationSection != null) {
            Material genMatched = Material.matchMaterial(generationSection.getString("material", "STONE"));
            this.generationMaterial = genMatched != null ? genMatched : Material.STONE;
            this.generationInterval = Math.max(1, generationSection.getInt("interval", 3));
            this.requirePlayerNearby = generationSection.getBoolean("require-player-nearby", true);
            this.radius = Math.max(1, generationSection.getInt("radius", 32));

            ConfigurationSection effectsSection = generationSection.getConfigurationSection("effects");
            if (effectsSection != null) {
                this.soundName = effectsSection.getString("sound", null);
                this.particleName = effectsSection.getString("particle", null);
                this.particleCount = Math.max(0, effectsSection.getInt("particle-count", 0));
            } else {
                this.soundName = null;
                this.particleName = null;
                this.particleCount = 0;
            }
        } else {
            this.generationMaterial = Material.STONE;
            this.generationInterval = 3;
            this.requirePlayerNearby = true;
            this.radius = 32;
            this.soundName = null;
            this.particleName = null;
            this.particleCount = 0;
        }

        this.pickupOnBreak = section.getBoolean("pickup-on-break", true);
    }

    private Map<Character, Material> loadIngredients(ConfigurationSection ingredientsSection) {
        if (ingredientsSection == null) {
            return Collections.emptyMap();
        }
        Map<Character, Material> result = new HashMap<>();
        for (String key : ingredientsSection.getKeys(false)) {
            if (key.length() != 1) {
                continue;
            }
            char c = key.charAt(0);
            Material material = Material.matchMaterial(ingredientsSection.getString(key, "STONE"));
            if (material != null) {
                result.put(c, material);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public String getKey() {
        return key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getCraftingShape() {
        return craftingShape;
    }

    public Map<Character, Material> getIngredients() {
        return ingredients;
    }

    public Material getItemMaterial() {
        return itemMaterial;
    }

    public String getItemName() {
        return itemName;
    }

    public List<String> getItemLore() {
        return itemLore;
    }

    public boolean isItemEnchantGlow() {
        return itemEnchantGlow;
    }

    public Material getGenerationMaterial() {
        return generationMaterial;
    }

    public int getGenerationInterval() {
        return generationInterval;
    }

    public boolean isRequirePlayerNearby() {
        return requirePlayerNearby;
    }

    public int getRadius() {
        return radius;
    }

    public String getSoundName() {
        return soundName;
    }

    public String getParticleName() {
        return particleName;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public boolean isPickupOnBreak() {
        return pickupOnBreak;
    }
}
