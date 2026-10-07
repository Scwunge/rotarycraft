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
    public static final ModConfigSpec.IntValue FORCE_FIELD_RANGE, HEAT_RAY_RANGE, SPAWNER_MOB_LIMIT;
    public static final ModConfigSpec.BooleanValue FRICTION_HEATER_XP, LOCK_BEDROCK_ENCHANTS, BEDROCK_PICK_SPAWNERS;
    public static final ModConfigSpec.BooleanValue JETPACK_NEEDS_JET_FUEL, JETPACK_CONSERVATIVE, JETPACK_BYPASSES_FLY_CHECK, JETPACK_WINGS_ON_SNEAK, SPAWN_WITH_HANDBOOK;
    public static final ModConfigSpec.IntValue CAVE_SCANNER_RANGE;
    public static final java.util.Map<String, ModConfigSpec.BooleanValue> WEAPONS = new java.util.LinkedHashMap<>();
    // ---- utility machines that load or change the world (off unless the server turns them on) ----
    public static final java.util.Map<String, ModConfigSpec.BooleanValue> WORLD_MACHINES = new java.util.LinkedHashMap<>();
    public static final ModConfigSpec.IntValue CHUNK_LOADER_RADIUS;
    public static final ModConfigSpec.BooleanValue WEATHER_BANS_RAIN;
    // ---- digging machines (borer, bedrock breaker, sonic borer): on by default; they act as their owner, so claims and mobGriefing stop them ----
    public static final java.util.Map<String, ModConfigSpec.BooleanValue> DIGGERS = new java.util.LinkedHashMap<>();
    public static final ModConfigSpec.DoubleValue BORER_POWER_FACTOR;
    public static final ModConfigSpec.BooleanValue BORER_MAINTENANCE;
    public static final ModConfigSpec.IntValue BORER_MAX_LENGTH;
    public static final ModConfigSpec.BooleanValue SOLAR_TOWER;
    public static final ModConfigSpec.DoubleValue SOLAR_FLUID_USE;
    public static final ModConfigSpec.IntValue TERRAFORMER_MAX_RADIUS;
    public static final ModConfigSpec.BooleanValue TERRAFORMER_EDITS_BLOCKS;
    public static final ModConfigSpec.IntValue SONIC_BORER_RANGE;
    public static final ModConfigSpec.BooleanValue BEDROCK_VOID_HOLE;
    // options the original had that were left for later (see the "extras" section)
    public static final ModConfigSpec.IntValue EMP_CHARGE_SPEED, FLUID_FLOW_SPEED, BORER_CHUNK_RADIUS, SPRINKLER_PARTICLES, VACUUM_POWER_PER_METRE;
    public static final ModConfigSpec.BooleanValue OWNER_ONLY_MACHINES, EXTRACTOR_WEAR, FAKE_PLAYER_BEDROCK, SPAWNERS_LEAK, CRAFTER_PROFILING, STEEL_HARVEST_HIGHER, GPR_SHOWS_ORES, PROJECTOR_LINES;
    public static final ModConfigSpec.DoubleValue PIPE_HARDNESS;

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
        HEAT_RAY_RANGE = b.comment("The longest beam of a Heat Ray, in blocks.").defineInRange("heatRayRange", 128, 8, 512);
        SPAWNER_MOB_LIMIT = b.comment("The number of creatures near a spawner past which the Spawner Controller stops it.").defineInRange("spawnerMobLimit", 128, 1, 1024);
        FRICTION_HEATER_XP = b.comment("The Friction Heater gives experience for what it smelts that a furnace would not.").define("frictionHeaterXp", true);
        LOCK_BEDROCK_ENCHANTS = b.comment("Bedrock tools and armour take no further enchantments (they come with theirs, which cannot be removed).").define("lockBedrockEnchants", true);
        BEDROCK_PICK_SPAWNERS = b.comment("The bedrock pickaxe picks up mob spawners whole.").define("bedrockPickHarvestsSpawners", true);
        JETPACK_NEEDS_JET_FUEL = b.comment("Jetpacks burn jet fuel only (off: ethanol works too, as a plain thruster).").define("jetpackNeedsJetFuel", false);
        JETPACK_CONSERVATIVE = b.comment("A jetpack fires a moment after you start to jump from the ground, so an ordinary hop does not use fuel.").define("conservativeJetpack", true);
        JETPACK_BYPASSES_FLY_CHECK = b.comment("A burning jetpack stops the server kicking you for flying (allow-flight off).").define("jetpackBypassesFlyCheck", true);
        JETPACK_WINGS_ON_SNEAK = b.comment("Wings glide only while you sneak (off: they glide unless you sneak).").define("jetpackWingsOnSneak", false);
        SPAWN_WITH_HANDBOOK = b.comment("A player gets a Handbook the first time they join.").define("spawnWithHandbook", false);
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
        CHUNK_LOADER_RADIUS = b.comment("Most chunks out from its own that a chunk loader holds loaded (the original's default is 8).")
                .defineInRange("chunkLoaderMaxRadius", 8, 0, 32);
        WEATHER_BANS_RAIN = b.comment("The weather controller will not make rain, thunder or storms (it can still clear the sky).")
                .define("weatherControllerBansRain", false);
        b.pop();

        b.push("digging_machines");
        for (String m : new String[] {"borer", "bedrockBreaker", "sonicBorer"}) {
            DIGGERS.put(m, b.comment("Enable the " + m + ". Off: it stays placed but does nothing. It acts as its owner, so claim and protection mods can stop it,",
                    "and it needs the mobGriefing rule.").define(m, true));
        }
        BORER_POWER_FACTOR = b.comment("Scales the power the borer needs to cut a block (1 is the original).").defineInRange("borerPowerFactor", 1.0, 0.5, 8.0);
        BEDROCK_VOID_HOLE = b.comment("The bedrock breaker may grind through the lowest layer of the world (and so open a hole to the void).")
                .define("bedrockBreakerVoidHole", false);
        SONIC_BORER_RANGE = b.comment("How far ahead a sonic borer looks for something to shatter (at least 64).").defineInRange("sonicBorerRange", 512, 64, 4096);
        TERRAFORMER_MAX_RADIUS = b.comment("Widest area, in blocks out from itself, a terraformer can be set to work.").defineInRange("terraformerMaxRadius", 32, 4, 128);
        TERRAFORMER_EDITS_BLOCKS = b.comment("A terraformer with a diamond inside also remakes the ground and snow of a patch to suit its new biome (the original).").define("terraformerEditsBlocks", true);
        BORER_MAX_LENGTH = b.comment("Longest tunnel a borer will bore, in blocks (the original has no limit, so a borer left running in open country keeps",
                "generating the land ahead of it for ever). It jams when it gets there.").defineInRange("borerMaxLength", 1024, 8, 30_000_000);
        BORER_MAINTENANCE = b.comment("The borer wears out its drill (256 blocks) and needs a new Drill item to carry on.").define("borerRequiresMaintenance", false);
        b.pop();

        b.push("generation");
        SOLAR_TOWER = b.comment("Solar towers make power. Off: they stay placed but do nothing.").define("solarTower", true);
        SOLAR_FLUID_USE = b.comment("Scales the water (or sodium) a solar tower uses for the power it makes (1 is the original).").defineInRange("solarTowerFluidUse", 1.0, 0.01, 10.0);
        b.pop();

        b.push("extras");
        EMP_CHARGE_SPEED = b.comment("How fast an EMP lists the machines in range while it charges (the original's 4 lists 33 columns a tick; each step adds 8).").defineInRange("empChargeSpeed", 4, 0, 32);
        FLUID_FLOW_SPEED = b.comment("How often fluid moves between pipes: 5 is every tick (the original), 1 every fifth tick.").defineInRange("fluidFlowSpeed", 5, 1, 5);
        OWNER_ONLY_MACHINES = b.comment("Only whoever placed a machine can open its screen.").define("ownerOnlyMachines", false);
        EXTRACTOR_WEAR = b.comment("The Extractor's first stage wears out a drill every 4096 operations, and stops until it is given a new one.").define("extractorWear", false);
        PIPE_HARDNESS = b.comment("How hard pipes are to break, from 0 (instantly, the original) to 1.").defineInRange("pipeHardness", 0.0, 0.0, 1.0);
        FAKE_PLAYER_BEDROCK = b.comment("The bedrock axe's tree felling, the bedrock pickaxe's spawner lifting and the bedrock hoe's wide tilling also work for fake players (auto activators).")
                .define("fakePlayerBedrockAbilities", true);
        SPAWNERS_LEAK = b.comment("Lifting a mob spawner by hand with the bedrock pickaxe lets a dozen or three mobs out of it (not for fake players).").define("spawnersLeakMobs", true);
        BORER_CHUNK_RADIUS = b.comment("How many chunks round the borer's head are generated as it works (0 is just its own).").defineInRange("borerChunkRadius", 0, 0, 4);
        CRAFTER_PROFILING = b.comment("The Auto-Crafter works less often when its rounds take long, to spare the server lag.").define("crafterProfiling", true);
        STEEL_HARVEST_HIGHER = b.comment("HSLA steel pickaxes, axes and shovels harvest everything of their kind softer than obsidian (not just what iron does).").define("steelToolsHarvestHigher", false);
        GPR_SHOWS_ORES = b.comment("The ground penetrating radar shows ores as they are; off, it draws them as the stone round them.").define("gprShowsOres", true);
        PROJECTOR_LINES = b.comment("The Projector draws its beam lines.").define("projectorLines", true);
        SPRINKLER_PARTICLES = b.comment("How many particles sprinklers spray, 0 (none) to 4 (the original).").defineInRange("sprinklerParticles", 4, 0, 4);
        VACUUM_POWER_PER_METRE = b.comment("The power (W) an Item Vacuum needs for each metre of reach beyond 8 (a power of two from 1024 up, at most 524288).").defineInRange("vacuumPowerPerMetre", 4096, 1024, 524288);
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

    /** Whether the named digging machine (a key of {@link #DIGGERS}) is enabled. */
    public static boolean diggerEnabled(String name) {
        return get(DIGGERS.get(name));
    }

    /** Values that tests have put in place for a while, without saving the config file (saving it makes the game reload the file later). */
    private static final java.util.Map<ModConfigSpec.ConfigValue<?>, Object> OVERRIDES = new java.util.concurrent.ConcurrentHashMap<>();

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
