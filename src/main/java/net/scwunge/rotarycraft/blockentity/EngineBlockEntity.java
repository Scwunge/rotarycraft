package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base for engines. Like the original: while an engine can run it spins up towards its top speed by 4 x log2(top + 1) rad/s
 * per tick, and when it stops it coasts down by omega/256 + 1 per tick. Torque is the engine's rated torque while it turns.
 */
public abstract class EngineBlockEntity extends PowerBlockEntity {
    protected EngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Rated torque (N*m). */
    protected abstract int ratedTorque();

    /** Speed (rad/s) the engine spins up to right now. */
    protected abstract int targetSpeed();

    /** Whether the engine's running conditions are met this tick. */
    protected abstract boolean canRun();

    /** Called every tick after the speed update; {@code running} is whether it ran. */
    protected void afterTick(boolean running) {
    }

    @Override
    public void serverTick() {
        boolean running = canRun();
        int target = running ? Math.max(0, targetSpeed()) : 0;
        int w = omega;
        if (running && target > 0) {
            if (w < target) {
                w += (int) (4 * (Math.log(target + 1) / Math.log(2)));
                w = Math.min(w, target);
            } else if (w > target) {
                w = target;
            }
        } else if (w > 0) {
            w -= w / 256 + 1;
        }
        w = Math.max(0, w);
        setPower(w > 0 ? ratedTorque() : 0, w);
        afterTick(running);
    }
}
