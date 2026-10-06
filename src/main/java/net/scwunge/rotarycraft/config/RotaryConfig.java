package net.scwunge.rotarycraft.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Gameplay settings (SERVER config, synced to clients). Reads go through {@link #get}, which falls back to the default
 * while no world is loaded.
 */
public class RotaryConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue SHAFT_FAILURE;
    public static final ModConfigSpec.IntValue WATTS_PER_FE;
    public static final ModConfigSpec.IntValue GENERATOR_BUFFER;
    public static final ModConfigSpec.IntValue MOTOR_TORQUE;
    public static final ModConfigSpec.IntValue MOTOR_OMEGA;
    public static final ModConfigSpec.IntValue MOTOR_BUFFER;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("transmission");
        SHAFT_FAILURE = b.comment("Shafts and gearboxes break when the torque or speed they carry exceeds their material's limit.")
                .define("shaftFailure", true);
        b.pop();

        b.push("energy_conversion");
        WATTS_PER_FE = b.comment("Shaft power (watts) that converts to 1 FE per tick in the Generator, and the reverse in the Electric Motor.")
                .defineInRange("wattsPerFE", 20, 1, 1_000_000);
        GENERATOR_BUFFER = b.comment("FE the Generator can hold.").defineInRange("generatorBuffer", 100_000, 1_000, Integer.MAX_VALUE);
        MOTOR_TORQUE = b.comment("Torque (N*m) the Electric Motor delivers while powered.").defineInRange("motorTorque", 16, 1, 1_000_000);
        MOTOR_OMEGA = b.comment("Speed (rad/s) the Electric Motor delivers while powered.").defineInRange("motorOmega", 256, 1, 1_000_000);
        MOTOR_BUFFER = b.comment("FE the Electric Motor can hold.").defineInRange("motorBuffer", 100_000, 1_000, Integer.MAX_VALUE);
        b.pop();

        SPEC = b.build();
    }

    public static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
