package net.scwunge.rotarycraft.charged;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** The range finder: tells you what block you point at (up to 512 blocks off) and how far it is, for a unit of charge. */
public class RangeFinderItem extends ChargedItem {
    public static final double RANGE = 512;

    public RangeFinderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(RANGE)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide()) {
            if (!Charge.use(stack, player, 1, "tool")) {
                return InteractionResultHolder.fail(stack);
            }
            double d = eye.distanceTo(hit.getLocation());
            player.sendSystemMessage(Component.translatable("message.rotarycraft.range_finder", level.getBlockState(hit.getBlockPos()).getBlock().getName(), String.format("%.3f", d)));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
