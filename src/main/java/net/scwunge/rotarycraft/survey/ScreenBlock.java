package net.scwunge.rotarycraft.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** The CCTV Screen's block: right-click opens its dye slots, sneak-right-click calls up the camera they name. */
public class ScreenBlock extends MachineBlock {
    public ScreenBlock(Properties props, Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type,
                       BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory) {
        super(props, type, factory);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof ScreenBlockEntity screen) {
            if (!level.isClientSide()) {
                screen.activate(player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ScreenBlockEntity screen) {
            for (int i = 0; i < screen.items().getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), screen.items().getStackInSlot(i));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
