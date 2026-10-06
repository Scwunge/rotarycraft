package net.scwunge.rotarycraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scwunge.rotarycraft.blockentity.JetEngineBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.RotaryParts;

/** The Jet Engine block: right-click with a turbine to repair it fully, a compressor to repair one point, the afterburner upgrade to fit it. */
public class JetEngineBlock extends MachineBlock {
    public JetEngineBlock(Properties props) {
        super(props, RotaryBlockEntities.JET_ENGINE, JetEngineBlockEntity::new);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(JetEngineBlock::new);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof JetEngineBlockEntity jet) {
            boolean used = false;
            boolean matched = true;
            if (stack.is(RotaryParts.part("turbine").get())) {
                used = !level.isClientSide() && jet.repairFully();
            } else if (stack.is(RotaryParts.part("compressor").get())) {
                used = !level.isClientSide() && jet.repairPartly();
            } else if (stack.is(RotaryItems.AFTERBURNER_UPGRADE.get())) {
                used = !level.isClientSide() && !jet.canAfterburn();
                if (used) {
                    jet.installAfterburner();
                }
            } else {
                matched = false;
            }
            if (matched) {
                if (used) {
                    stack.consume(1, player);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}
