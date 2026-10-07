package net.scwunge.rotarycraft.charged;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.blockentity.BlockCannonBlockEntity;
import net.scwunge.rotarycraft.blockentity.ItemCannonBlockEntity;

/** The target designator: aims every cannon within 16 blocks of you that is in target mode at the block you point at (up to 512 blocks off). */
public class TargetItem extends Item {
    public static final double RANGE = 512;
    public static final int REACH = 16;

    public TargetItem(Properties properties) {
        super(properties.stacksTo(1));
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
            BlockPos target = hit.getBlockPos().immutable();
            int set = 0;
            BlockPos at = player.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-REACH, -REACH, -REACH), at.offset(REACH, REACH, REACH))) {
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof BlockCannonBlockEntity cannon && cannon.targetMode()) {
                    cannon.aimAt(target);
                    set++;
                } else if (be instanceof ItemCannonBlockEntity cannon && cannon.hasTarget()) {
                    cannon.setTarget(0, target.getX(), target.getY(), target.getZ());
                    set++;
                }
            }
            player.displayClientMessage(Component.translatable("message.rotarycraft.target", set, target.getX(), target.getY(), target.getZ()), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
