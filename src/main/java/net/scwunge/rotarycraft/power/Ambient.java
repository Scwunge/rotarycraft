package net.scwunge.rotarycraft.power;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** The surroundings' temperature in C, which heated machines settle back towards. */
public final class Ambient {
    private Ambient() {
    }

    /** About 20 C in plains, below zero in snowy biomes, 101 C (water boils) anywhere in the Nether. */
    public static int temperature(Level level, BlockPos pos) {
        if (level == null) {
            return 20;
        }
        if (level.dimensionType().ultraWarm()) {
            return 101;
        }
        return Math.round(level.getBiome(pos).value().getBaseTemperature() * 25);
    }
}
