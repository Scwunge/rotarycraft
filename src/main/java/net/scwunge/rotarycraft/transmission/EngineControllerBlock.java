package net.scwunge.rotarycraft.transmission;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;

/** The Engine Control Unit block: a plain cube, filled with fuel from a bucket or pipe, set with the screwdriver. */
public class EngineControllerBlock extends BaseEntityBlock {
    public EngineControllerBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(EngineControllerBlock::new);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EngineControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != TransmissionRegistry.ENGINE_CONTROLLER_BE.get()) {
            return null;
        }
        return (l, p, s, be) -> ((EngineControllerBlockEntity) be).serverTick();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, hit.getDirection());
        if (tank != null && FluidUtil.getFluidHandler(stack).isPresent() && FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof EngineControllerBlockEntity ecu) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.rotarycraft.ecu.status", ecu.setting().label(), 100 * ecu.speedMultiplier(),
                        ecu.tank().getFluidAmount(), EngineControllerBlockEntity.FUEL_CAPACITY), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
