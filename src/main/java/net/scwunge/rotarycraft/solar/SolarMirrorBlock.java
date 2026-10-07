package net.scwunge.rotarycraft.solar;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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
import net.scwunge.rotarycraft.registry.RotaryParts;
import net.scwunge.rotarycraft.registry.SolarRegistry;

/** A Solar Mirror. A fall of a few blocks onto it breaks it (and hurts); another mirror, used on it, mends it. */
public class SolarMirrorBlock extends BaseEntityBlock {
    /** The fall, in blocks, that breaks a mirror. */
    public static final float BREAKING_FALL = 3;

    public SolarMirrorBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(SolarMirrorBlock::new);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarMirrorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != SolarRegistry.SOLAR_MIRROR_BE.get()) {
            return null;
        }
        return (l, p, s, be) -> ((SolarMirrorBlockEntity) be).serverTick();
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (!level.isClientSide() && fallDistance > BREAKING_FALL && level.getBlockEntity(pos) instanceof SolarMirrorBlockEntity mirror && !mirror.isBroken()) {
            mirror.breakMirror();
            entity.hurt(level.damageSources().cactus(), 1);
        }
        super.fallOn(level, state, pos, entity, fallDistance);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(RotaryParts.part("mirror").get()) && level.getBlockEntity(pos) instanceof SolarMirrorBlockEntity mirror && mirror.isBroken()) {
            if (!level.isClientSide()) {
                mirror.repair();
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SolarPlantMember member && member.plant() != null) {
            member.plant().invalidate(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
