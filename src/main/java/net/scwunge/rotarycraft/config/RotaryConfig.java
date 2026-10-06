package net.scwunge.rotarycraft.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Gameplay settings (SERVER config, synced to clients). Reads go through {@link #get}, which falls back to the default
 * while no world is loaded.
 */
public class RotaryConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue SHAFT_FAILURE;
    public static final ModConfigSpec.BooleanValue EXPLOSIONS_BREAK_BLOCKS;
    public static final ModConfigSpec.IntValue WATTS_PER_FE;
    public static final ModConfigSpec.IntValue GENERATOR_BUFFER;
    public static final ModConfigSpec.IntValue MOTOR_TORQUE;
    public static final ModConfigSpec.IntValue MOTOR_OMEGA;
    public static final ModConfigSpec.IntValue MOTOR_BUFFER;
    public static final ModConfigSpec.BooleanValue JET_HARMS_PLAYERS;

    // ---- weapons and defence (each can be turned off; block damage also needs mobGriefing and respects claims) ----
    public static final ModConfigSpec.BooleanValue TURRETS_TARGET_PLAYERS;
    public static final ModConfigSpec.BooleanValue WEAPON_BLOCK_DAMAGE;
    public static final ModConfigSpec.BooleanValue RAILGUN_BLOCK_DAMAGE;
    public static final ModConfigSpec.IntValue FORCE_FIELD_RANGE;
    public static final ModConfigSpec.IntValue CAVE_SCANNER_RANGE;
    public static final java.util.Map<String, ModConfigSpec.BooleanValue> WEAPONS = new java.util.LinkedHashMap<>();
    // ---- utility machines that load or change the world (off unless the server turns them on) ----
    public static final java.util.Map<String, ModConfigSpec.BooleanValue> WORLD_MACHINES = new java.util.LinkedHashMap<>();

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("transmission");
        SHAFT_FAILURE = b.comment("Shafts and gearboxes break when the torque or speed they carry exceeds their material's limit.")
                .define("shaftFailure", true);
        EXPLOSIONS_BREAK_BLOCKS = b.comment("Bursting flywheels, overpressured steam engines and failing jet engines damage blocks around them.")
                .define("explosionsBreakBlocks", true);
        b.pop();

        b.push("engines");
        JET_HARMS_PLAYERS = b.comment("Jet engines pull in and kill players in front of their intake, as in the original. Turn off to make them",
                        "ignore players (mobs and items are still pulled in).")
                .define("jetHarmsPlayers", true);
        b.pop();

        b.push("energy_conversion");
        WATTS_PER_FE = b.comment("Shaft power (watts) that converts to 1 FE per tick in the Generator, and the reverse in the Electric Motor.")
                .defineInRange("wattsPerFE", 20, 1, 1_000_000);
        GENERATOR_BUFFER = b.comment("FE the Generator can hold.").defineInRange("generatorBuffer", 100_000, 1_000, Integer.MAX_VALUE);
        MOTOR_TORQUE = b.comment("Torque (N*m) the Electric Motor delivers while powered.").defineInRange("motorTorque", 16, 1, 1_000_000);
        MOTOR_OMEGA = b.comment("Speed (rad/s) the Electric Motor delivers while powered.").defineInRange("motorOmega", 256, 1, 1_000_000);
        MOTOR_BUFFER = b.comment("FE the Electric Motor can hold.").defineInRange("motorBuffer", 100_000, 1_000, Integer.MAX_VALUE);
        b.pop();

        b.push("weapons");
        TURRETS_TARGET_PLAYERS = b.comment("Turrets shoot players who are not their owner and not on its whitelist (Cannon Key), as in the original.")
                .define("turretsTargetPlayers", true);
        WEAPON_BLOCK_DAMAGE = b.comment("Destructive machines (rail gun, heat ray, sonic weapon, EMP, landmine...) damage blocks. They also need the",
                        "mobGriefing rule, and act as their owner, so claim and protection mods can stop them.")
                .define("weaponBlockDamage", true);
        RAILGUN_BLOCK_DAMAGE = b.comment("Rail gun shots damage blocks (as well as weaponBlockDamage).").define("railgunBlockDamage", true);
        FORCE_FIELD_RANGE = b.comment("The largest radius the Force Field and Containment can make (never under 64).").defineInRange("forceFieldRange", 128, 1, 512);
        for (String w : new String[] {"railgun", "freezeGun", "antiAir", "gatling", "laserGun", "flameTurret", "heatRay", "tntCannon",
                "sonicWeapon", "emp", "landmine", "forceField", "containment"}) {
            WEAPONS.put(w, b.comment("Enable the " + w + ". Off: it stays placed but does nothing.").define(w, true));
        }
        b.pop();

        b.push("surveying");
        CAVE_SCANNER_RANGE = b.comment("How many blocks each way the Cave Scanner's box reaches (the client does the scanning, so large values cost frames).")
                .defineInRange("caveScannerRange", 16, 4, 128);
        b.pop();

        b.push("world_machines");
        for (String m : new String[] {"chunkLoader", "terraformer", "weatherController"}) {
            WORLD_MACHINES.put(m, b.comment("Enable the " + m + ", which loads or changes the world (off by default).").define(m, false));
        }
        b.pop();

        SPEC = b.build();
    }

    /** Whether the named weapon (a key of {@link #WEAPONS}) is enabled. */
    public static boolean weaponEnabled(String name) {
        return get(WEAPONS.get(name));
    }

    /** Whether the named world machine (a key of {@link #WORLD_MACHINES}) is enabled. */
    public static boolean worldMachineEnabled(String name) {
        return get(WORLD_MACHINES.get(name));
    }

    public static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
