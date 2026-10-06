package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/**
 * DC Electric Engine: the starter engine. Runs while it gets a redstone signal and delivers 4 N*m at 256 rad/s (1 kW)
 * out of its front, the values of the original.
 */
public class EngineBlockEntity extends PowerBlockEntity {
    public static final int DC_TORQUE = 4;
    public static final int DC_OMEGA = 256;

    public EngineBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.DC_ENGINE.get(), pos, state);
    }

    @Override
    public void serverTick() {
        if (level != null && level.hasNeighborSignal(worldPosition)) {
            setPower(DC_TORQUE, DC_OMEGA);
        } else {
            setPower(0, 0);
        }
    }
}
