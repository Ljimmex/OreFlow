package pl.Ljimex.oreFlow.generator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
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
import org.bukkit.scheduler.BukkitTask;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import pl.Ljimex.oreFlow.OreFlow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class StoneGeneratorManager {

    private final OreFlow plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final NamespacedKey generatorKey;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final List<Location> generators = new ArrayList<>();
    private BukkitTask generationTask;

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
        stopGenerationTask();
    }

    /**
     * Pelne przeładowanie: zapisuje dane, rejestruje przepisy od nowa i restartuje task.
     */
    public void reload() {
        saveData();
        stopGenerationTask();
        registerRecipe();
        startGenerationTask();
    }

    public void discoverRecipes(Player player) {
        if (player == null) {
            return;
        }
        for (GeneratorConfig generator : plugin.getGeneratorConfigManager().getGenerators()) {
            if (!generator.isEnabled()) {
                continue;
            }
            NamespacedKey recipeKey = new NamespacedKey(plugin, "stone_generator_" + generator.getKey());
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
        for (GeneratorConfig generator : plugin.getGeneratorConfigManager().getGenerators()) {
            if (!generator.isEnabled()) {
                continue;
            }
            registerRecipe(generator);
        }
    }

    private void registerRecipe(GeneratorConfig generator) {
        List<String> shape = generator.getCraftingShape();
        if (shape.size() != 3) {
            plugin.getLogger().warning("Stone generator recipe '" + generator.getKey() + "' must have exactly 3 shape rows.");
            return;
        }

        NamespacedKey recipeKey = new NamespacedKey(plugin, "stone_generator_" + generator.getKey());

        // Remove old recipe if it exists (supports /reload)
        try {
            Bukkit.removeRecipe(recipeKey);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not remove old stone generator recipe: " + recipeKey, e);
        }

        ItemStack result = createGeneratorItem(generator);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape(shape.toArray(new String[0]));

        Map<Character, Material> ingredients = generator.getIngredients();
        for (Map.Entry<Character, Material> entry : ingredients.entrySet()) {
            recipe.setIngredient(entry.getKey(), entry.getValue());
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

    public ItemStack createGeneratorItem(GeneratorConfig generator) {
        Material material = generator.getItemMaterial();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(miniMessage.deserialize(generator.getItemName()));

            List<Component> lore = new ArrayList<>();
            for (String line : generator.getItemLore()) {
                String processed = line.replace("{interval}", String.valueOf(generator.getGenerationInterval()));
                lore.add(miniMessage.deserialize(processed));
            }
            meta.lore(lore);

            if (generator.isItemEnchantGlow()) {
                meta.addEnchant(Enchantment.UNBREAKING, 10, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(generatorKey, PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createGeneratorItem(String key) {
        GeneratorConfig generator = plugin.getGeneratorConfigManager().getGenerator(key);
        if (generator == null || !generator.isEnabled()) {
            return null;
        }
        return createGeneratorItem(generator);
    }

    public ItemStack getGeneratorDropItem() {
        GeneratorConfig generator = getFirstEnabledGenerator();
        if (generator == null) {
            return null;
        }
        return createGeneratorItem(generator);
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

    public GeneratorConfig getFirstEnabledGenerator() {
        return plugin.getGeneratorConfigManager().getFirstEnabledGenerator();
    }

    private void startGenerationTask() {
        GeneratorConfig generator = getFirstEnabledGenerator();
        if (generator == null) {
            return;
        }

        long ticks = generator.getGenerationInterval() * 20L;

        generationTask = new BukkitRunnable() {
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

    private void stopGenerationTask() {
        if (generationTask != null) {
            generationTask.cancel();
            generationTask = null;
        }
    }

    private void tickGenerators() {
        GeneratorConfig generator = getFirstEnabledGenerator();
        if (generator == null) {
            return;
        }

        Material generatedMaterial = generator.getGenerationMaterial();
        boolean requirePlayerNearby = generator.isRequirePlayerNearby();
        int radius = generator.getRadius();
        String soundName = generator.getSoundName();
        String particleName = generator.getParticleName();
        int particleCount = generator.getParticleCount();

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
}
