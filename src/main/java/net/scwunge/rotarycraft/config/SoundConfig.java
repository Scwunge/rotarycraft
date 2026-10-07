package net.scwunge.rotarycraft.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** How loud the machines are (rotarycraft-sound.toml): all of them, and the engines on top of that, as the original's two volume options. */
public final class SoundConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue MACHINE_VOLUME;
    public static final ModConfigSpec.DoubleValue ENGINE_VOLUME;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        MACHINE_VOLUME = b.comment("How loud the machines are, from 0 (silent) to 1.").defineInRange("machineVolume", 1.0, 0.0, 1.0);
        ENGINE_VOLUME = b.comment("How loud the engines are, on top of the machine volume.").defineInRange("engineVolume", 1.0, 0.0, 1.0);
        SPEC = b.build();
    }

    private SoundConfig() {}

    public static float machineVolume() {
        return (float) (double) RotaryConfig.get(MACHINE_VOLUME);
    }

    public static float engineVolume() {
        return (float) (double) RotaryConfig.get(ENGINE_VOLUME);
    }
}
