package net.scwunge.rotarycraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.blockentity.ChunkLoaderBlockEntity;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** The Chunk Loader: breaking it lets go of the chunks it holds. */
public class ChunkLoaderBlock extends MachineBlock {
    public ChunkLoaderBlock(Properties props, Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type,
                            BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory) {
        super(props, type, factory);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ChunkLoaderBlockEntity loader) {
            loader.release();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
