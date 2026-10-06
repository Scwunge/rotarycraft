package net.scwunge.rotarycraft.power;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the Terraformer can turn one biome into, and at what cost: the original's table, with its 1.7 biomes named as their 1.21 successors.
 * Each step needs a minimum power, some water, and an item of each kind listed in the machine (an item is used up only with its chance).
 * Variants of a biome (the flower forest, the warm oceans...) can be changed as the biome they are variants of.
 */
public final class BiomeTransforms {
    /** One thing the machine has to hold, and how often a use of it is used up (1 is always). */
    public record Requirement(Item item, float chance) {}

    /** One step from a biome to another. */
    public record Step(ResourceKey<Biome> from, ResourceKey<Biome> to, long power, int waterMb, List<Requirement> items) {}

    private static final Map<ResourceKey<Biome>, Map<ResourceKey<Biome>, Step>> STEPS = new HashMap<>();
    /** Each biome and the variants that count as it. */
    private static final Map<ResourceKey<Biome>, List<ResourceKey<Biome>>> VARIANTS = new LinkedHashMap<>();
    /** The 1.7 biome ids the original's biome icons are laid out by (the icon sheet has one 32 pixel cell per id, 8 to a row). */
    private static final Map<ResourceKey<Biome>, Integer> ICON_IDS = new HashMap<>();

    private static Requirement r(Item item, float chance) {
        return new Requirement(item, chance);
    }

    private static void variants(ResourceKey<Biome> parent, ResourceKey<Biome>... children) {
        VARIANTS.put(parent, List.of(children));
    }

    private static void icon(ResourceKey<Biome> biome, int id) {
        ICON_IDS.put(biome, id);
    }

    private static void step(ResourceKey<Biome> from, ResourceKey<Biome> to, long power, int water, Requirement... items) {
        List<ResourceKey<Biome>> sources = new ArrayList<>();
        sources.add(from);
        sources.addAll(VARIANTS.getOrDefault(from, List.of()));
        for (ResourceKey<Biome> source : sources) {
            STEPS.computeIfAbsent(source, k -> new LinkedHashMap<>()).put(to, new Step(source, to, power, water, List.of(items)));
        }
    }

    static {
        variants(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW);
        variants(Biomes.FOREST, Biomes.FLOWER_FOREST);
        variants(Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA);
        variants(Biomes.OCEAN, Biomes.COLD_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.WARM_OCEAN);
        variants(Biomes.DEEP_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN);
        variants(Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE);
        variants(Biomes.SWAMP, Biomes.MANGROVE_SWAMP);
        variants(Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES);
        variants(Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS);
        variants(Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST);
        variants(Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST);
        variants(Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN);

        icon(Biomes.OCEAN, 0);
        icon(Biomes.PLAINS, 1);
        icon(Biomes.DESERT, 2);
        icon(Biomes.WINDSWEPT_HILLS, 3);
        icon(Biomes.FOREST, 4);
        icon(Biomes.TAIGA, 5);
        icon(Biomes.SWAMP, 6);
        icon(Biomes.FROZEN_OCEAN, 10);
        icon(Biomes.SNOWY_PLAINS, 12);
        icon(Biomes.MUSHROOM_FIELDS, 14);
        icon(Biomes.JUNGLE, 21);
        icon(Biomes.DEEP_OCEAN, 24);
        icon(Biomes.BIRCH_FOREST, 27);
        icon(Biomes.DARK_FOREST, 29);
        icon(Biomes.SNOWY_TAIGA, 30);
        icon(Biomes.OLD_GROWTH_PINE_TAIGA, 32);
        icon(Biomes.SAVANNA, 35);
        icon(Biomes.BADLANDS, 37);

        step(Biomes.DESERT, Biomes.SAVANNA, 65536, 30, r(Items.SHORT_GRASS, 0.5f), r(Items.ACACIA_SAPLING, 0.05f));
        step(Biomes.SAVANNA, Biomes.PLAINS, 32768, 20, r(Items.SHORT_GRASS, 0.3f));
        step(Biomes.PLAINS, Biomes.FOREST, 131072, 10, r(Items.OAK_SAPLING, 0.5f), r(Items.BIRCH_SAPLING, 0.2f));
        step(Biomes.FOREST, Biomes.JUNGLE, 262144, 50, r(Items.OAK_SAPLING, 0.4f), r(Items.OAK_SAPLING, 0.6f), r(Items.FERN, 0.3f));
        step(Biomes.PLAINS, Biomes.SWAMP, 32768, 100, r(Items.OAK_SAPLING, 0.1f), r(Items.RED_MUSHROOM, 0.05f), r(Items.BROWN_MUSHROOM, 0.15f));
        step(Biomes.SWAMP, Biomes.OCEAN, 131072, 500);
        step(Biomes.OCEAN, Biomes.FROZEN_OCEAN, 1024, 0, r(Items.ICE, 1));
        step(Biomes.PLAINS, Biomes.WINDSWEPT_HILLS, 65536, 0, r(Items.OAK_SAPLING, 0.05f));
        step(Biomes.PLAINS, Biomes.SNOWY_PLAINS, 8192, 0, r(Items.SNOW, 1), r(Items.OAK_SAPLING, 0.05f));
        step(Biomes.SNOWY_PLAINS, Biomes.PLAINS, 524288, 0, r(Items.SHORT_GRASS, 0.7f));
        step(Biomes.OCEAN, Biomes.MUSHROOM_FIELDS, 1048576, 0, r(Items.DIRT, 1), r(Items.MYCELIUM, 1), r(Items.RED_MUSHROOM, 0.9f), r(Items.BROWN_MUSHROOM, 0.9f));
        step(Biomes.MUSHROOM_FIELDS, Biomes.WINDSWEPT_HILLS, 262144, 0, r(Items.GRASS_BLOCK, 0.125f), r(Items.OAK_SAPLING, 0.05f), r(Items.SHORT_GRASS, 0.25f));
        step(Biomes.FOREST, Biomes.TAIGA, 131072, 0, r(Items.SPRUCE_SAPLING, 0.25f));
        step(Biomes.FOREST, Biomes.SNOWY_TAIGA, 131072, 0, r(Items.SNOW, 0.3f), r(Items.SPRUCE_SAPLING, 0.25f));
        step(Biomes.FOREST, Biomes.DARK_FOREST, 65536, 40, r(Items.DARK_OAK_SAPLING, 0.5f));
        step(Biomes.FOREST, Biomes.BIRCH_FOREST, 32768, 0, r(Items.BIRCH_SAPLING, 0.25f));
        step(Biomes.TAIGA, Biomes.SNOWY_TAIGA, 32768, 0, r(Items.SNOW, 0.3f));
        step(Biomes.TAIGA, Biomes.SNOWY_PLAINS, 65536, 0, r(Items.SNOW, 1), r(Items.OAK_SAPLING, 0.05f));
        step(Biomes.TAIGA, Biomes.FOREST, 131072, 0, r(Items.OAK_SAPLING, 0.4f), r(Items.BIRCH_SAPLING, 0.1f));
        step(Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, 32768, 20, r(Items.SPRUCE_SAPLING, 0.1f));
        step(Biomes.SNOWY_PLAINS, Biomes.FROZEN_OCEAN, 32768, 100, r(Items.ICE, 1));
        step(Biomes.PLAINS, Biomes.SAVANNA, 65536, 0, r(Items.ACACIA_SAPLING, 0.05f));
        step(Biomes.SAVANNA, Biomes.DESERT, 65536, 0, r(Items.SAND, 1), r(Items.SANDSTONE, 0.5f), r(Items.CACTUS, 0.1f));
        step(Biomes.FOREST, Biomes.PLAINS, 262144, 0, r(Items.SHORT_GRASS, 0.8f));
        step(Biomes.JUNGLE, Biomes.FOREST, 65536, 0, r(Items.OAK_SAPLING, 0.5f), r(Items.BIRCH_SAPLING, 0.2f));
        step(Biomes.SWAMP, Biomes.PLAINS, 262144, 0, r(Items.SHORT_GRASS, 0.8f), r(Items.DIRT, 0.8f));
        step(Biomes.OCEAN, Biomes.SWAMP, 524288, 0, r(Items.OAK_SAPLING, 0.1f), r(Items.RED_MUSHROOM, 0.05f), r(Items.BROWN_MUSHROOM, 0.15f), r(Items.GRASS_BLOCK, 0.125f));
        step(Biomes.FROZEN_OCEAN, Biomes.OCEAN, 524288, 0);
        step(Biomes.WINDSWEPT_HILLS, Biomes.PLAINS, 262144, 0, r(Items.SHORT_GRASS, 0.6f));
        step(Biomes.SNOWY_PLAINS, Biomes.TAIGA, 65536, 0, r(Items.SPRUCE_SAPLING, 0.4f));
        step(Biomes.FROZEN_OCEAN, Biomes.SNOWY_PLAINS, 65536, 0, r(Items.OAK_SAPLING, 0.05f), r(Items.DIRT, 1), r(Items.GRASS_BLOCK, 0.125f));
        step(Biomes.DESERT, Biomes.BADLANDS, 32768, 0, r(Items.CLAY, 0.2f));
        step(Biomes.OCEAN, Biomes.DEEP_OCEAN, 1024, 200);
        step(Biomes.BADLANDS, Biomes.DESERT, 16384, 0, r(Items.SAND, 0.5f), r(Items.SANDSTONE, 0.1f));
        step(Biomes.DARK_FOREST, Biomes.FOREST, 32768, 0, r(Items.OAK_SAPLING, 0.5f), r(Items.BIRCH_SAPLING, 0.2f));
        step(Biomes.BIRCH_FOREST, Biomes.FOREST, 32768, 0, r(Items.OAK_SAPLING, 0.5f));
    }

    private BiomeTransforms() {}

    /** The step from one biome to another, or null if the machine cannot make it. */
    public static Step step(ResourceKey<Biome> from, ResourceKey<Biome> to) {
        Map<ResourceKey<Biome>, Step> map = STEPS.get(from);
        return map == null ? null : map.get(to);
    }

    /** The biomes that {@code from} can be turned into, in a fixed order (the order of the screen's list). */
    public static List<ResourceKey<Biome>> targetsFrom(ResourceKey<Biome> from) {
        Map<ResourceKey<Biome>, Step> map = STEPS.get(from);
        if (map == null) {
            return List.of();
        }
        List<ResourceKey<Biome>> list = new ArrayList<>(map.keySet());
        list.sort(Comparator.comparing(k -> k.location().toString()));
        return Collections.unmodifiableList(list);
    }

    /** Every step the machine knows. */
    public static List<Step> all() {
        List<Step> out = new ArrayList<>();
        STEPS.values().forEach(m -> out.addAll(m.values()));
        return out;
    }

    /** The cell of the original's biome icon sheet for a biome (a variant uses its parent's), or -1. */
    public static int iconId(ResourceKey<Biome> biome) {
        Integer id = ICON_IDS.get(biome);
        if (id != null) {
            return id;
        }
        for (Map.Entry<ResourceKey<Biome>, List<ResourceKey<Biome>>> e : VARIANTS.entrySet()) {
            if (e.getValue().contains(biome)) {
                return iconId(e.getKey());
            }
        }
        return -1;
    }

    public static Set<ResourceKey<Biome>> sources() {
        return Collections.unmodifiableSet(STEPS.keySet());
    }

    public static ResourceKey<Biome> key(ResourceLocation id) {
        return ResourceKey.create(Registries.BIOME, id);
    }
}
