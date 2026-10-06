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
import net.scwunge.rotarycraft.blockentity.HydroEngineBlockEntity;
import net.scwunge.rotarycraft.power.ShaftMaterial;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryParts;

/** The Hydrokinetic Engine block: right-click with a bedrock rod for bedrock blades (also mends broken ones). */
public class HydroEngineBlock extends MachineBlock {
    public HydroEngineBlock(Properties props) {
        super(props, RotaryBlockEntities.HYDRO_ENGINE, HydroEngineBlockEntity::new);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(HydroEngineBlock::new);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.is(RotaryParts.RODS.get(ShaftMaterial.BEDROCK).get()) && level.getBlockEntity(pos) instanceof HydroEngineBlockEntity hydro) {
            if (!level.isClientSide() && hydro.makeBedrock()) {
                stack.consume(1, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}
