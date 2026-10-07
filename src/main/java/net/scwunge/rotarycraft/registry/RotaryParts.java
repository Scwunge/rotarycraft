package net.scwunge.rotarycraft.registry;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.scwunge.rotarycraft.power.ShaftMaterial;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The original's crafting components. Each gear material (wood, stone, steel, tungsten, diamond, bedrock) has a rod
 * (wood uses sticks), a gear, gear units for 2:1 to 16:1, a bearing and a shaft core; then the engine and machine parts
 * and the alloys they need.
 */
public final class RotaryParts {
    private RotaryParts() {
    }

    public static final ShaftMaterial[] GEAR_MATERIALS = {ShaftMaterial.WOOD, ShaftMaterial.STONE, ShaftMaterial.STEEL,
            ShaftMaterial.TUNGSTEN, ShaftMaterial.DIAMOND, ShaftMaterial.BEDROCK};
    public static final int[] RATIOS = {2, 4, 8, 16};

    public static final Map<ShaftMaterial, DeferredItem<Item>> RODS = new EnumMap<>(ShaftMaterial.class);
    public static final Map<ShaftMaterial, DeferredItem<Item>> GEARS = new EnumMap<>(ShaftMaterial.class);
    public static final Map<ShaftMaterial, Map<Integer, DeferredItem<Item>>> GEAR_UNITS = new EnumMap<>(ShaftMaterial.class);
    public static final Map<ShaftMaterial, DeferredItem<Item>> BEARINGS = new EnumMap<>(ShaftMaterial.class);
    public static final Map<ShaftMaterial, DeferredItem<? extends Item>> SHAFT_CORES = new EnumMap<>(ShaftMaterial.class);
    /** The single-type parts and alloys, by id. */
    public static final Map<String, DeferredItem<Item>> PARTS = new LinkedHashMap<>();

    static {
        for (ShaftMaterial m : GEAR_MATERIALS) {
            String id = m.id();
            if (m != ShaftMaterial.WOOD) {
                RODS.put(m, simple(id + "_rod"));
            }
            GEARS.put(m, simple(id + "_gear"));
            Map<Integer, DeferredItem<Item>> units = new LinkedHashMap<>();
            for (int r : RATIOS) {
                units.put(r, simple(id + "_gear_unit_" + r));
            }
            GEAR_UNITS.put(m, units);
            BEARINGS.put(m, simple(id + "_bearing"));
            SHAFT_CORES.put(m, switch (m) {
                case STEEL -> RotaryItems.SHAFT_CORE;
                case TUNGSTEN -> RotaryItems.TUNGSTEN_SHAFT_CORE;
                default -> simple(id + "_shaft_core");
            });
        }
        for (String id : new String[]{
                // structure
                "base_panel", "mount", "ball_bearing", "worm_gear", "chain", "belt", "brake",
                // engine parts (ENGINECRAFT)
                "impeller", "compressor", "turbine", "diffuser", "combustor", "high_combustor", "cylinder", "silumin_cylinder",
                "radiator", "condenser", "gold_coil", "igniter", "water_plate", "compound_turbine", "compound_compressor",
                // misc parts (MISCCRAFT / BORECRAFT)
                "propeller", "hub", "mirror", "generator_unit", "linear_induction_motor", "power_module",
                "circuit_board", "screen", "drill", "saw", "mixer", "radar_unit", "sonar_unit", "press_head",
                // alloys and materials (COMPACTS / POWDERS)
                "silicon", "aluminum_powder", "aluminum_ingot", "silumin_ingot", "red_gold_dust", "red_gold_ingot",
                "spring_tungsten_ingot", "bedrock_dust", "bedrock_ingot",
                // flywheel cores (FLYWHEELCRAFT)
                "wood_flywheel_core", "stone_flywheel_core", "iron_flywheel_core", "gold_flywheel_core", "bedrock_flywheel_core"}) {
            PARTS.put(id, id.equals("belt") || id.equals("chain") ? belt(id) : simple(id));
        }
    }

    /** A belt or a chain: used on two pulleys, it joins them. */
    private static DeferredItem<Item> belt(String id) {
        boolean chain = id.equals("chain");
        return RotaryItems.add(RotaryItems.ITEMS.register(id, () -> new net.scwunge.rotarycraft.item.BeltItem(new Item.Properties(), chain)));
    }

    private static DeferredItem<Item> simple(String id) {
        return RotaryItems.add(RotaryItems.ITEMS.registerSimpleItem(id));
    }

    public static DeferredItem<Item> part(String id) {
        DeferredItem<Item> item = PARTS.get(id);
        if (item == null) {
            throw new IllegalArgumentException("no part " + id);
        }
        return item;
    }

    /** Loads the class so its items are registered. */
    public static void init() {
    }
}
