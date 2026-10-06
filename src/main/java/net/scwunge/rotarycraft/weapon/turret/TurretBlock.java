package net.scwunge.rotarycraft.weapon.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.scwunge.rotarycraft.block.MachineBlock;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A turret's block: it stands on its power input, or hangs under it when placed against the underside of a block (facing up or
 * down). It remembers who placed it.
 */
public class TurretBlock extends MachineBlock {
    public TurretBlock(Properties props, Supplier<? extends BlockEntityType<? extends TurretBlockEntity>> type,
                       BiFunction<BlockPos, BlockState, ? extends TurretBlockEntity> factory) {
        super(props, type, factory);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace() == Direction.DOWN ? Direction.DOWN : Direction.UP);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof TurretBlockEntity turret) {
            turret.setOwner(player);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AmmoTurretBlockEntity turret) {
            for (int i = 0; i < turret.items().getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), turret.items().getStackInSlot(i));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
