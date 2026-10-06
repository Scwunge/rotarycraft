package net.scwunge.rotarycraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.blockentity.DynamometerBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/** Measures the power passing through it; comparators read it. */
public class DynamometerBlock extends MachineBlock {
    public DynamometerBlock(Properties props) {
        super(props, RotaryBlockEntities.DYNAMOMETER, DynamometerBlockEntity::new);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(DynamometerBlock::new);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof DynamometerBlockEntity dyn ? dyn.comparatorSignal() : 0;
    }
}
