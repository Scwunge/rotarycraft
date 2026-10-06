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
import net.scwunge.rotarycraft.blockentity.GearboxBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/** A gearbox with a fixed ratio. Right-click with an empty hand to switch between reduction and acceleration. */
public class GearboxBlock extends MachineBlock {
    private final int ratio;

    public GearboxBlock(Properties props, int ratio) {
        super(props, RotaryBlockEntities.GEARBOX, GearboxBlockEntity::new);
        this.ratio = ratio;
    }

    public int ratio() {
        return ratio;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new GearboxBlock(p, ratio));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GearboxBlockEntity gearbox) {
            gearbox.toggleMode();
            player.displayClientMessage(Component.translatable(gearbox.isReduction()
                    ? "message.rotarycraft.gearbox.reduction" : "message.rotarycraft.gearbox.acceleration", ratio), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
