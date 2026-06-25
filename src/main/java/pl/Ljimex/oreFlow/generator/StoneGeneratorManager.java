package pl.Ljimex.oreFlow.generator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import pl.Ljimex.oreFlow.OreFlow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class StoneGeneratorManager {

    private final OreFlow plugin;
    private final NamespacedKey generatorKey;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final List<Location> generators = new ArrayList<>();

    public StoneGeneratorManager(OreFlow plugin) {
        this.plugin = plugin;
        this.generatorKey = new NamespacedKey(plugin, "stone_generator");
        this.dataFile = new File(plugin.getDataFolder(), "generators-data.yml");
    }

    public void load() {
        loadData();
        registerRecipe();
        startGenerationTask();
    }

    public void unload() {
        saveData();
    }

    public void discoverRecipes(Player player) {
        if (player == null) {
            return;
        }
        ConfigurationSection generatorsSection = plugin.getConfigManager().getGenerators()
                .getConfigurationSection("generators");
        if (generatorsSection == null) {
            return;
        }
        for (String key : generatorsSection.getKeys(false)) {
            ConfigurationSection generatorSection = generatorsSection.getConfigurationSection(key);
            if (generatorSection == null || !generatorSection.getBoolean("enabled", true)) {
                continue;
            }
            NamespacedKey recipeKey = new NamespacedKey(plugin, "stone_generator_" + key);
            player.discoverRecipe(recipeKey);
        }
    }

    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create generators-data.yml", e);
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        List<String> locations = dataConfig.getStringList("generators");
        for (String locString : locations) {
            Location loc = deserializeLocation(locString);
            if (loc != null) {
                generators.add(loc);
            }
        }
    }

    public void saveData() {
        if (dataConfig == null) {
            return;
        }

        List<String> locations = new ArrayList<>();
        for (Location loc : generators) {
            locations.add(serializeLocation(loc));
        }
        dataConfig.set("generators", locations);

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save generators-data.yml", e);
        }
    }

    public void registerRecipe() {
        ConfigurationSection generatorsSection = plugin.getConfigManager().getGenerators()
                .getConfigurationSection("generators");
        if (generatorsSection == null) {
            return;
        }

        for (String key : generatorsSection.getKeys(false)) {
            ConfigurationSection generatorSection = generatorsSection.getConfigurationSection(key);
            if (generatorSection == null || !generatorSection.getBoolean("enabled", true)) {
                continue;
            }

            registerRecipe(key, generatorSection);
        }
    }

    private void registerRecipe(String key, ConfigurationSection generatorSection) {
        ConfigurationSection craftingSection = generatorSection.getConfigurationSection("crafting");
        if (craftingSection == null) {
            return;
        }

        List<String> shape = craftingSection.getStringList("shape");
        if (shape.size() != 3) {
            plugin.getLogger().warning("Stone generator recipe '" + key + "' must have exactly 3 shape rows.");
            return;
        }

        NamespacedKey recipeKey = new NamespacedKey(plugin, "stone_generator_" + key);

        // Remove old recipe if it exists (supports /reload)
        try {
            Bukkit.removeRecipe(recipeKey);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not remove old stone generator recipe: " + recipeKey, e);
        }

        ItemStack result = createGeneratorItem(generatorSection);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape(shape.toArray(new String[0]));

        ConfigurationSection ingredientsSection = craftingSection.getConfigurationSection("ingredients");
        if (ingredientsSection != null) {
            for (String ingredientKey : ingredientsSection.getKeys(false)) {
                if (ingredientKey.length() != 1) {
                    continue;
                }
                String materialName = ingredientsSection.getString(ingredientKey, "STONE");
                Material material = Material.matchMaterial(materialName);
                if (material == null) {
                    plugin.getLogger().warning("Unknown ingredient material in stone generator recipe '" + key + "': " + materialName);
                    return;
                }
                recipe.setIngredient(ingredientKey.charAt(0), material);
            }
        }

        if (!Bukkit.addRecipe(recipe)) {
            plugin.getLogger().warning("Failed to register stone generator recipe: " + recipeKey);
            return;
        }

        // Discover recipe for all online players so it shows in the crafting book
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.discoverRecipe(recipeKey);
        }

        plugin.getLogger().info("Registered stone generator recipe: " + recipeKey);
    }

    public ItemStack createGeneratorItem(ConfigurationSection generatorSection) {
        ConfigurationSection itemSection = generatorSection.getConfigurationSection("item");
        Material material = Material.END_STONE;

        if (itemSection != null) {
            Material matched = Material.matchMaterial(itemSection.getString("material", "END_STONE"));
            if (matched != null) {
                material = matched;
            }
        }

        int interval = generatorSection.getInt("generation.interval", 3);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<gold>Stoniarka</gold> <dark_gray>•</dark_gray> <gray>Generator</gray>"));

            List<Component> lore = new ArrayList<>();
            lore.add(line());
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Opis"));
            lore.add(desc("Generuje kamień po postawieniu"));
            lore.add(desc("Kamień odnawia się po wykopaniu"));
            lore.add(line());
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Statystyki"));
            lore.add(stat("Interval", interval + "s", "yellow"));
            meta.lore(lore);

            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(generatorKey, PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }

        return item;
    }

    private Component line() {
        return MiniMessage.miniMessage().deserialize("<dark_gray>" + "▬".repeat(24) + "</dark_gray>");
    }

    private Component desc(String text) {
        return MiniMessage.miniMessage().deserialize(" <dark_gray>▸</dark_gray> <gray>" + text + "</gray>");
    }

    private Component stat(String name, String value, String color) {
        return MiniMessage.miniMessage().deserialize(" <dark_gray>▸</dark_gray> <gray>" + name + ":</gray> <" + color + ">" + value + "</" + color + ">");
    }

    public boolean isGeneratorItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(generatorKey, PersistentDataType.BYTE);
    }

    public boolean isGeneratorBlock(Location location) {
        return generators.contains(location);
    }

    public void addGenerator(Location location) {
        if (!generators.contains(location)) {
            generators.add(location);
            saveData();
        }
    }

    public void removeGenerator(Location location) {
        generators.remove(location);
        saveData();
    }

    public ItemStack createGeneratorItem(String key) {
        ConfigurationSection generatorsSection = plugin.getConfigManager().getGenerators()
                .getConfigurationSection("generators");
        if (generatorsSection == null) {
            return null;
        }
        ConfigurationSection generatorSection = generatorsSection.getConfigurationSection(key);
        if (generatorSection == null || !generatorSection.getBoolean("enabled", true)) {
            return null;
        }
        return createGeneratorItem(generatorSection);
    }

    public ItemStack getGeneratorDropItem() {
        ConfigurationSection generatorsSection = plugin.getConfigManager().getGenerators()
                .getConfigurationSection("generators");
        if (generatorsSection == null) {
            return null;
        }

        for (String key : generatorsSection.getKeys(false)) {
            ConfigurationSection generatorSection = generatorsSection.getConfigurationSection(key);
            if (generatorSection != null && generatorSection.getBoolean("enabled", true)) {
                return createGeneratorItem(generatorSection);
            }
        }
        return null;
    }

    private void startGenerationTask() {
        ConfigurationSection generatorsSection = plugin.getConfigManager().getGenerators()
                .getConfigurationSection("generators");
        if (generatorsSection == null) {
            return;
        }

        // Use the first enabled generator's interval
        int interval = 3;
        for (String key : generatorsSection.getKeys(false)) {
            ConfigurationSection generatorSection = generatorsSection.getConfigurationSection(key);
            if (generatorSection != null && generatorSection.getBoolean("enabled", true)) {
                interval = generatorSection.getInt("generation.interval", 3);
                break;
            }
        }

        long ticks = interval * 20L;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.isEnabled()) {
                    cancel();
                    return;
                }
                tickGenerators();
            }
        }.runTaskTimer(plugin, ticks, ticks);
    }

    private void tickGenerators() {
        ConfigurationSection generatorsSection = plugin.getConfigManager().getGenerators()
                .getConfigurationSection("generators");
        if (generatorsSection == null) {
            return;
        }

        ConfigurationSection generatorSection = null;
        for (String key : generatorsSection.getKeys(false)) {
            ConfigurationSection section = generatorsSection.getConfigurationSection(key);
            if (section != null && section.getBoolean("enabled", true)) {
                generatorSection = section;
                break;
            }
        }

        if (generatorSection == null) {
            return;
        }

        ConfigurationSection generationSection = generatorSection.getConfigurationSection("generation");
        if (generationSection == null) {
            return;
        }

        Material generatedMaterial = Material.matchMaterial(generationSection.getString("material", "STONE"));
        if (generatedMaterial == null) {
            generatedMaterial = Material.STONE;
        }

        boolean requirePlayerNearby = generationSection.getBoolean("require-player-nearby", true);
        int radius = generationSection.getInt("radius", 32);

        String soundName = null;
        String particleName = null;
        int particleCount = 0;
        ConfigurationSection effectsSection = generationSection.getConfigurationSection("effects");
        if (effectsSection != null) {
            soundName = effectsSection.getString("sound", null);
            particleName = effectsSection.getString("particle", null);
            particleCount = effectsSection.getInt("particle-count", 0);
        }

        List<Location> toRemove = new ArrayList<>();

        for (Location location : new ArrayList<>(generators)) {
            World world = location.getWorld();
            if (world == null || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                continue;
            }

            // Verify the generator block still exists
            if (location.getBlock().getType().isAir()) {
                toRemove.add(location);
                continue;
            }

            if (requirePlayerNearby && !isPlayerNearby(location, radius)) {
                continue;
            }

            Location above = location.clone().add(0, 1, 0);
            if (above.getBlock().getType().isAir()) {
                above.getBlock().setType(generatedMaterial, false);

                if (soundName != null) {
                    try {
                        world.playSound(above, org.bukkit.Sound.valueOf(soundName), 0.5f, 1.0f);
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                if (particleName != null && particleCount > 0) {
                    try {
                        world.spawnParticle(org.bukkit.Particle.valueOf(particleName), above.add(0.5, 0.5, 0.5), particleCount);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }

        for (Location loc : toRemove) {
            removeGenerator(loc);
        }
    }

    private boolean isPlayerNearby(Location location, int radius) {
        int radiusSquared = radius * radius;
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    private String serializeLocation(Location location) {
        return location.getWorld().getName() + ";" + location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ();
    }

    private Location deserializeLocation(String string) {
        String[] parts = string.split(";");
        if (parts.length != 4) {
            return null;
        }
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }
}
