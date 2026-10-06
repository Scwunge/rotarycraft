package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/** DC Electric Engine: runs on a redstone signal. 4 N*m at 256 rad/s (1 kW), as in the original. */
public class DCEngineBlockEntity extends EngineBlockEntity {
    public static final int TORQUE = 4;
    public static final int SPEED = 256;

    public DCEngineBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.DC_ENGINE.get(), pos, state);
    }

    @Override
    protected int ratedTorque() {
        return TORQUE;
    }

    @Override
    protected int targetSpeed() {
        return SPEED;
    }

    @Override
    protected boolean canRun() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }
}
