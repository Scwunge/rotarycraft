package net.scwunge.rotarycraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scwunge.rotarycraft.blockentity.ClutchBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/** Redstone-controlled clutch. Right-click with an empty hand to invert its redstone mode. */
public class ClutchBlock extends MachineBlock {
    public ClutchBlock(Properties props) {
        super(props, RotaryBlockEntities.CLUTCH, ClutchBlockEntity::new);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(ClutchBlock::new);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ClutchBlockEntity clutch) {
            clutch.toggleMode();
            player.displayClientMessage(Component.translatable(clutch.needsRedstone()
                    ? "message.rotarycraft.clutch.powered" : "message.rotarycraft.clutch.unpowered"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
