package net.scwunge.rotarycraft.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Settings of the farming and automation machines (a config file of their own, rotarycraft-farm.toml): an off switch for each
 * machine, off by default where it harms creatures or wrecks land, and the original's range options. Reads go through
 * {@link RotaryConfig#get}, so tests can override them.
 */
public final class FarmConfig {
    public static final ModConfigSpec SPEC;

    /** Machine name -> its off switch. A machine that is off stays placed but does nothing. */
    public static final java.util.Map<String, ModConfigSpec.BooleanValue> MACHINES = new java.util.LinkedHashMap<>();
    public static final ModConfigSpec.IntValue FAN_RANGE;
    public static final ModConfigSpec.BooleanValue LAWN_SPRINKLER_HURTS;
    public static final ModConfigSpec.BooleanValue BLOWER_SPILLS;
    public static final ModConfigSpec.IntValue VACUUM_RANGE, BREEDER_RANGE, BAIT_RANGE, BAIT_MOBS;

    /** The machines and whether they are on unless the server says otherwise. */
    private static final Object[][] SWITCHES = {
            {"fan", true}, {"sprinkler", true}, {"lawnSprinkler", true}, {"fertilizer", true}, {"groundHydrator", true},
            {"defoliator", false}, {"blower", true}, {"vacuum", true}, {"autoBreeder", true}, {"baitBox", true}, {"mobHarvester", false},
            {"spawnerController", false}, {"woodcutter", false},
    };

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("machines");
        for (Object[] s : SWITCHES) {
            MACHINES.put((String) s[0], b.comment("Enable the " + s[0] + ". Off: it stays placed but does nothing. What it changes in the world it changes as its owner,",
                    "so claim and protection mods can stop it, and it needs the mobGriefing rule.").define((String) s[0], (boolean) s[1]));
        }
        b.pop();
        b.push("ranges");
        FAN_RANGE = b.comment("The longest the Fan's beam can reach (never under 32; the original's default).").defineInRange("fanRange", 32, 8, 256);
        LAWN_SPRINKLER_HURTS = b.comment("A Lawn Sprinkler on high pressure hurts the creatures it sprays (the original).").define("lawnSprinklerHurts", true);
        VACUUM_RANGE = b.comment("The longest reach of the Item Vacuum (never under 32).").defineInRange("vacuumRange", 128, 8, 512);
        BREEDER_RANGE = b.comment("The longest reach of the Auto-Breeder (never under 24).").defineInRange("breederRange", 128, 8, 512);
        BAIT_RANGE = b.comment("The longest reach of the Bait Box (never under 24).").defineInRange("baitBoxRange", 24, 8, 256);
        BAIT_MOBS = b.comment("Most creatures a Bait Box works on at once (never under 24).").defineInRange("baitBoxMobs", 256, 8, 4096);
        BLOWER_SPILLS = b.comment("An Item Pump with nothing but air in front of it sprays its items out.").define("itemPumpSpills", true);
        b.pop();
        SPEC = b.build();
    }

    private FarmConfig() {}

    public static boolean enabled(String machine) {
        return RotaryConfig.get(MACHINES.get(machine));
    }
}
