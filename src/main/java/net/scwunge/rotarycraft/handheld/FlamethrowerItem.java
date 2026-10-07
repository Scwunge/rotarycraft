package net.scwunge.rotarycraft.handheld;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The original's flamethrower: while it is held in use, a stream of flame comes from you along your line of sight. As in the original it is a show and
 * nothing more: it burns nothing and needs no charge.
 */
public class FlamethrowerItem extends Item {
    public FlamethrowerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        Vec3 look = entity.getLookAngle();
        level.addParticle(ParticleTypes.FLAME, entity.getX() + look.x, entity.getEyeY() - 0.2 + look.y, entity.getZ() + look.z, look.x / 4, look.y / 4, look.z / 4);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }
}
