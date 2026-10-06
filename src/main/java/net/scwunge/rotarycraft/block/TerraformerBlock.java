package net.scwunge.rotarycraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.blockentity.TerraformerBlockEntity;
import net.scwunge.rotarycraft.weapon.OwnedMachineBlock;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** The Terraformer: it remembers who placed it, and drops what it holds when broken. */
public class TerraformerBlock extends OwnedMachineBlock {
    public TerraformerBlock(Properties props, Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type,
                            BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory) {
        super(props, type, factory);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TerraformerBlockEntity terraformer) {
            ItemStackHandler items = terraformer.items();
            for (int i = 0; i < items.getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.getStackInSlot(i));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
