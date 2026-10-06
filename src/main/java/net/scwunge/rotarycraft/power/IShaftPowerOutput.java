package net.scwunge.rotarycraft.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Anything that delivers shaft power out of one of its faces. Power is torque (N*m) times angular speed (rad/s), in watts.
 * Each machine reads its input from the neighbour behind its input face once per tick, so power moves one block per tick,
 * as in the original.
 */
public interface IShaftPowerOutput {
    /** Torque delivered out of {@code side}, or 0. */
    int getTorqueOut(Direction side);

    /** Angular speed delivered out of {@code side}, or 0. */
    int getOmegaOut(Direction side);

    /** A torque/speed pair. */
    record Reading(int torque, int omega) {
        public static final Reading NONE = new Reading(0, 0);

        public long power() {
            return (long) torque * (long) omega;
        }
    }

    /** What the block at {@code pos} receives through its face pointing {@code toward} (the neighbour in that direction). */
    static Reading readInput(Level level, BlockPos pos, Direction toward) {
        BlockPos from = pos.relative(toward);
        if (!level.isLoaded(from)) {
            return Reading.NONE;
        }
        BlockEntity be = level.getBlockEntity(from);
        if (be instanceof IShaftPowerOutput out) {
            Direction side = toward.getOpposite();
            int torque = out.getTorqueOut(side);
            int omega = out.getOmegaOut(side);
            if (torque > 0 && omega > 0) {
                return new Reading(torque, omega);
            }
        }
        return Reading.NONE;
    }
}
