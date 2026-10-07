package net.scwunge.rotarycraft.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.weapon.OwnedMachineBlock;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** A farming or automation machine's block: facing, owner, and the machine hands back what it holds when the block is broken. */
public class FarmBlock extends OwnedMachineBlock {
    public FarmBlock(Properties props, Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type,
                     BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory) {
        super(props, type, factory);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof FarmBlockEntity machine && !level.isClientSide() && machine.interact(player, stack)) {
            return ItemInteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof FarmBlockEntity machine && level.isClientSide() && machine.wouldTake(stack)) {
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /** Machines with a screen open it on right-click; the others do nothing. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof FarmBlockEntity machine) {
            if (machine.ui() == null) {
                return InteractionResult.PASS;
            }
            if (player instanceof ServerPlayer sp) {
                sp.openMenu(machine, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide() && level.getBlockEntity(pos) instanceof FarmBlockEntity machine) {
            machine.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
