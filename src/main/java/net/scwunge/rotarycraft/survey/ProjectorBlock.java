package net.scwunge.rotarycraft.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** The Projector's block: a redstone pulse moves on to the next slide, and breaking it drops the slides. */
public class ProjectorBlock extends MachineBlock {
    public ProjectorBlock(Properties props, Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type,
                          BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory) {
        super(props, type, factory);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ProjectorBlockEntity projector) {
            projector.neighbourSignal(level.hasNeighborSignal(pos));
        }
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ProjectorBlockEntity projector) {
            for (int i = 0; i < projector.items().getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), projector.items().getStackInSlot(i));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
