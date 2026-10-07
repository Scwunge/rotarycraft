package net.scwunge.rotarycraft.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.weapon.Owned;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A shaft-driven block with a layout-described screen: it drops its inventory when broken, tells its block entity who placed it, and passes
 * items used on it to {@link MachineInteractions}. A machine that only faces the four sides sets {@code horizontalOnly}.
 */
public class LayoutMachineBlock extends MachineBlock {
    private final boolean horizontalOnly;

    public LayoutMachineBlock(Properties props, Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type,
                              BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory) {
        this(props, type, factory, false);
    }

    public LayoutMachineBlock(Properties props, Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type,
                              BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory, boolean horizontalOnly) {
        super(props, type, factory);
        this.horizontalOnly = horizontalOnly;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return horizontalOnly ? defaultBlockState().setValue(FACING, ctx.getHorizontalDirection()) : super.getStateForPlacement(ctx);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof Owned owned) {
            owned.setOwner(player);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MachineInteractions machine ? Math.max(0, machine.comparatorSignal()) : 0;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!stack.isEmpty() && level.getBlockEntity(pos) instanceof MachineInteractions machine && !level.isClientSide() && machine.onItemUse(stack, player, hand)) {
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof MachineHost host && host.dropsInventory()) {
                for (int i = 0; i < host.items().getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), host.items().getStackInSlot(i));
                }
            }
            if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof MachineInteractions machine) {
                machine.onBroken(server);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
