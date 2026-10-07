package net.scwunge.rotarycraft.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** The Auto-Crafter: it drops its patterns and what it made when broken. */
public class AutoCrafterBlock extends MachineBlock {
    public AutoCrafterBlock(Properties props, Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type,
                            BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory) {
        super(props, type, factory);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AutoCrafterBlockEntity crafter) {
            crafter.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
