package net.scwunge.rotarycraft.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Switches and numbers for the world, decoration, logistics and item machines (SERVER config, its own file). Every machine has a switch: off, it
 * stays placed but does nothing. Machines that change blocks or set things alight start off, and they act as their owner, so claim and protection
 * mods can stop them, and they need the mobGriefing rule.
 */
public final class MachineConfig {
    public static final ModConfigSpec SPEC;
    public static final Map<String, ModConfigSpec.BooleanValue> SWITCHES = new LinkedHashMap<>();
    public static final ModConfigSpec.IntValue PILE_DRIVER_DEPTH;
    public static final ModConfigSpec.IntValue LINE_BUILDER_LENGTH;
    public static final ModConfigSpec.IntValue BLOCK_FILLER_VOLUME;
    public static final ModConfigSpec.IntValue AREA_FILLER_RANGE;
    public static final ModConfigSpec.IntValue FLOODLIGHT_RANGE;
    public static final ModConfigSpec.IntValue BRIDGE_RANGE;
    public static final ModConfigSpec.IntValue AEROSOLIZER_RANGE;
    public static final ModConfigSpec.IntValue LAMP_RANGE;
    public static final ModConfigSpec.IntValue DETECTOR_RANGE;

    /** The original's machines in this group, with whether each is on by default (the ones that change blocks or burn things are not). */
    private static final Object[][] MACHINES = {
            // world and decoration
            {"obsidianMaker", true}, {"pileDriver", false}, {"lineBuilder", false}, {"blockFiller", false}, {"floodLight", true}, {"lightBridge", true},
            {"lamp", true}, {"beamMirror", true}, {"aerosolizer", true}, {"fireworkMachine", true}, {"musicBox", true}, {"selfDestruct", false},
            {"particleEmitter", true}, {"decorativeTank", true}, {"itemCannon", true}, {"blockCannon", false}, {"arrowGun", true}, {"airGun", true},
            // logistics
            {"itemFilter", true}, {"sortingMachine", true}, {"dropProcessor", true}, {"itemRefresher", true}, {"scaleChest", true}, {"bucketFiller", true},
            {"fillingStation", true}, {"spiller", false}, {"spillway", false}, {"wetter", true}, {"aggregator", true},
            {"purifier", true}, {"grindstone", true}, {"igniter", false}, {"heater", true}, {"furnaceHeater", true}, {"playerDetector", true},
            {"smokeDetector", true},
    };

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("machines");
        for (Object[] m : MACHINES) {
            String name = (String) m[0];
            boolean on = (Boolean) m[1];
            SWITCHES.put(name, b.comment("Enable the " + name + ". Off: it stays placed but does nothing."
                    + (on ? "" : " Off by default because it changes blocks or sets things alight.")).define(name, on));
        }
        PILE_DRIVER_DEPTH = b.comment("Deepest a pile driver drives a pile, in blocks.").defineInRange("pileDriverDepth", 64, 1, 512);
        LINE_BUILDER_LENGTH = b.comment("Longest line a line builder lays, in blocks.").defineInRange("lineBuilderLength", 512, 64, 4096);
        BLOCK_FILLER_VOLUME = b.comment("Most blocks a block filler places in one go.").defineInRange("blockFillerVolume", 4096, 1, 1_000_000);
        AREA_FILLER_RANGE = b.comment("How far out from itself a block filler or spiller looks for space to fill. 0 turns both off.")
                .defineInRange("areaFillerRange", 16, 0, 64);
        FLOODLIGHT_RANGE = b.comment("Longest beam of a flood light, in blocks (never under 64).").defineInRange("floodlightRange", 128, 64, 512);
        BRIDGE_RANGE = b.comment("Longest span of a light bridge, in blocks (never under 64).").defineInRange("bridgeRange", 128, 64, 512);
        AEROSOLIZER_RANGE = b.comment("Furthest an aerosolizer reaches along each axis, in blocks (never under 64).").defineInRange("aerosolizerRange", 128, 64, 512);
        LAMP_RANGE = b.comment("How far a lamp lights along each axis, in blocks (the original's is 12); the diagonals reach four fifths as far.")
                .defineInRange("lampRange", 12, 1, 24);
        DETECTOR_RANGE = b.comment("Longest range a player detector can be set to, in blocks (never under 64); the power it gets limits it too.")
                .defineInRange("detectorRange", 128, 64, 512);
        b.pop();
        SPEC = b.build();
    }

    private static final Map<ModConfigSpec.ConfigValue<?>, Object> OVERRIDES = new ConcurrentHashMap<>();

    private MachineConfig() {
    }

    public static boolean enabled(String name) {
        ModConfigSpec.BooleanValue value = SWITCHES.get(name);
        if (value == null) {
            throw new IllegalArgumentException("No machine switch " + name);
        }
        return get(value);
    }

    /** Makes {@link #get} return {@code value} for this setting until {@link #clearOverride}, without touching the config file. For tests. */
    public static <T> void override(ModConfigSpec.ConfigValue<T> setting, T value) {
        OVERRIDES.put(setting, value);
    }

    public static void clearOverride(ModConfigSpec.ConfigValue<?> setting) {
        OVERRIDES.remove(setting);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        Object forced = OVERRIDES.get(value);
        if (forced != null) {
            return (T) forced;
        }
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
