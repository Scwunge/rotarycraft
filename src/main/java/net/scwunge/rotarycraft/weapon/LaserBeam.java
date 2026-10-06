package net.scwunge.rotarycraft.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.api.Laserable;

/** What the Heat Ray's and the Laser Gun's beams do for anything that implements {@link Laserable}. */
public final class LaserBeam {
    private LaserBeam() {}

    /** The result of the beam meeting a block. */
    public enum Touch {
        /** Nothing here is Laserable: the beam does what it does to ordinary blocks. */
        NONE,
        /** A Laserable took the beam and lets it pass. */
        PASS,
        /** A Laserable took the beam and stops it. */
        STOP
    }

    /** Hands the beam to the block and its block entity, if they are Laserable. */
    public static Touch touch(Level level, BlockPos pos, BlockState state, long power, int step) {
        Touch result = Touch.NONE;
        BlockEntity be = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        if (be instanceof Laserable laserable) {
            laserable.whenInBeam(level, pos, power, step);
            if (laserable.blockBeam(level, pos, power)) {
                return Touch.STOP;
            }
            result = Touch.PASS;
        }
        if (state.getBlock() instanceof Laserable laserable) {
            laserable.whenInBeam(level, pos, power, step);
            return laserable.blockBeam(level, pos, power) ? Touch.STOP : Touch.PASS;
        }
        return result;
    }

    /** Hands the beam to a creature or other entity in its path, if it is Laserable. */
    public static void touch(Level level, Entity entity, long power, int step) {
        if (entity instanceof Laserable laserable) {
            laserable.whenInBeam(level, entity.blockPosition(), power, step);
        }
    }
}
