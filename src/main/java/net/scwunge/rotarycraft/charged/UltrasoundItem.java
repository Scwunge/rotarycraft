package net.scwunge.rotarycraft.charged;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

/** The ultrasound scanner: looks five blocks into what you point at and says whether it holds ore, silverfish, a fluid or a cave, for a unit of charge. */
public class UltrasoundItem extends ChargedItem {
    public static final double RANGE = 5;

    public UltrasoundItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!Charge.use(stack, player, 1, "tool")) {
            return InteractionResultHolder.fail(stack);
        }
        boolean ore = false, silverfish = false, cave = false, solidSeen = false, fluid = false;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        for (double d = 0; d <= RANGE; d += 0.2) {
            BlockPos pos = BlockPos.containing(eye.add(look.scale(d)));
            BlockState state = level.getBlockState(pos);
            if (state.is(Tags.Blocks.ORES) && !ore) {
                ore = true;
                player.sendSystemMessage(Component.translatable("message.rotarycraft.ultrasound.ore"));
            }
            if (state.getBlock() instanceof InfestedBlock && !silverfish) {
                silverfish = true;
                player.sendSystemMessage(Component.translatable("message.rotarycraft.ultrasound.silverfish"));
            }
            if (!state.getFluidState().isEmpty() && !fluid) {
                fluid = true;
                player.sendSystemMessage(Component.translatable("message.rotarycraft.ultrasound.fluid", state.getFluidState().getType().getFluidType().getDescription()));
            }
            if (!state.isAir() && state.getFluidState().isEmpty() && state.isSolidRender(level, pos)) {
                solidSeen = true;
            } else if (solidSeen && state.isAir() && !cave) {
                cave = true;
                player.sendSystemMessage(Component.translatable("message.rotarycraft.ultrasound.cave"));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
