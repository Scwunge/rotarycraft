package net.scwunge.rotarycraft.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Implement this on a Block, a BlockEntity or an Entity to give the Heat Ray and the Laser Gun's beams an effect on it (the original's
 * Laserable API, which other mods use to heat things with the beam). The beam calls {@link #whenInBeam} every tick it passes through the
 * object, then {@link #blockBeam} to ask whether the object stops the beam there. Both are only ever called on the server.
 * <p>
 * An object that implements this takes the beam into its own hands: the beam's built-in effect on blocks (melting, burning) is skipped for it.
 */
public interface Laserable {
    /**
     * Called every tick the object is in the beam.
     *
     * @param power the beam's power, in watts
     * @param step  how many blocks from the machine's barrel the object is (the Heat Ray counts from 1 at the block in front of it)
     */
    void whenInBeam(Level level, BlockPos pos, long power, int step);

    /** Whether the object stops the beam: true shields everything behind it. */
    boolean blockBeam(Level level, BlockPos pos, long power);
}
