package net.scwunge.rotarycraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scwunge.rotarycraft.blockentity.BorerBlockEntity;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryParts;
import net.scwunge.rotarycraft.weapon.OwnedMachineBlock;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * The Borer: it digs the way it faces, level with the ground (never up or down), takes enchanted books and, when it needs maintenance, a new
 * Drill; and while it is jammed it gives a comparator signal of 15.
 */
public class BorerBlock extends OwnedMachineBlock {
    public BorerBlock(Properties props, Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type,
                      BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory) {
        super(props, type, factory);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof BorerBlockEntity borer) {
            if (stack.is(Items.ENCHANTED_BOOK)) {
                if (!level.isClientSide() && borer.enchantments().apply(stack)) {
                    borer.setChanged();
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
            if (stack.is(RotaryParts.part("drill").get())) {
                if (!level.isClientSide() && borer.repair() && !player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BorerBlockEntity borer && borer.isJammed() ? 15 : 0;
    }
}
