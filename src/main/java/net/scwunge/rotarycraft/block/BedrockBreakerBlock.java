package net.scwunge.rotarycraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scwunge.rotarycraft.blockentity.BedrockBreakerBlockEntity;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.weapon.OwnedMachineBlock;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** The Bedrock Breaker: right-click empties the dust it has stored into your hands (or onto the ground), and it drops it when broken. */
public class BedrockBreakerBlock extends OwnedMachineBlock {
    public BedrockBreakerBlock(Properties props, Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type,
                               BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory) {
        super(props, type, factory);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof BedrockBreakerBlockEntity breaker) {
            if (!level.isClientSide() && !breaker.store().getStackInSlot(0).isEmpty()) {
                ItemStack dust = breaker.store().extractItem(0, Integer.MAX_VALUE, false);
                if (!player.getInventory().add(dust) && level instanceof ServerLevel server) {
                    breaker.eject(server, dust);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BedrockBreakerBlockEntity breaker) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), breaker.store().getStackInSlot(0));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
