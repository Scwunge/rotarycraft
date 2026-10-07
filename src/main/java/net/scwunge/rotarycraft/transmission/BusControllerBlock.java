package net.scwunge.rotarycraft.transmission;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** The Bus Controller block: power comes in at its back, lubricant through its top or bottom; right-click for what the bus is doing. */
public class BusControllerBlock extends MachineBlock {
    public BusControllerBlock(Properties props, Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type,
                              BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory) {
        super(props, type, factory);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new BusControllerBlock(p, TransmissionRegistry.BUS_CONTROLLER_BE, BusControllerBlockEntity::new));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof BusControllerBlockEntity hub) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.rotarycraft.bus.status", hub.busSize(), hub.sides(), hub.getTorque(), hub.getOmega(),
                        hub.tank().getFluidAmount(), BusControllerBlockEntity.TANK), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BusControllerBlockEntity hub) {
            hub.release();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
