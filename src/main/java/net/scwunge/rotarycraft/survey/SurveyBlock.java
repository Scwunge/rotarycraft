package net.scwunge.rotarycraft.survey;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.function.Supplier;

/** The block of a machine that is not driven by a shaft: right-click opens its screen, it ticks, and breaking it drops what it holds. */
public class SurveyBlock extends BaseEntityBlock {
    private final Supplier<? extends BlockEntityType<? extends SurveyBlockEntity>> type;
    private final BlockEntityType.BlockEntitySupplier<? extends SurveyBlockEntity> factory;

    public SurveyBlock(Properties props, Supplier<? extends BlockEntityType<? extends SurveyBlockEntity>> type,
                       BlockEntityType.BlockEntitySupplier<? extends SurveyBlockEntity> factory) {
        super(props);
        this.type = type;
        this.factory = factory;
    }

    protected final Supplier<? extends BlockEntityType<? extends SurveyBlockEntity>> type() {
        return type;
    }

    protected final BlockEntityType.BlockEntitySupplier<? extends SurveyBlockEntity> factory() {
        return factory;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new SurveyBlock(p, type, factory));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
            if (player instanceof ServerPlayer sp) {
                sp.openMenu(provider, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SurveyBlockEntity be) {
            ItemStackHandler items = be.items();
            if (items != null) {
                for (int i = 0; i < items.getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.getStackInSlot(i));
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.create(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != this.type.get()) {
            return null;
        }
        return (l, p, s, be) -> ((SurveyBlockEntity) be).serverTick();
    }
}
