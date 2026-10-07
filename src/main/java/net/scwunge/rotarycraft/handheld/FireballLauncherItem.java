package net.scwunge.rotarycraft.handheld;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.charged.Charge;
import net.scwunge.rotarycraft.charged.ChargedItem;

/**
 * The original's fireball launcher: hold the use button to wind it up, let go to throw a ghast's fireball along your line of sight. The longer it was held the bigger the
 * blast, in nine steps (the icon shows which) up to eight seconds; each shot takes half the blast size in charge (at least one), and a fire charge from your inventory a third of
 * the time. In creative the winding is twice as fast, and four times as fast sneaking.
 */
public class FireballLauncherItem extends ChargedItem {
    public static final int MAX_LEVEL = 8;
    private static final float[] SECONDS = {0.1F, 0.25F, 0.5F, 1F, 2F, 3F, 5F, 8F};

    public FireballLauncherItem(Properties properties) {
        super(properties);
    }

    /** The blast size for a number of ticks held (0 is a harmless fireball). */
    public static int level(int ticksHeld, boolean creative, boolean sneaking) {
        float power = ticksHeld / 20F;
        if (creative) {
            power *= sneaking ? 4 : 2;
        }
        int level = 0;
        for (float s : SECONDS) {
            if (power >= s) {
                level++;
            }
        }
        return level;
    }

    private static boolean hasFireCharge(Player player) {
        return player.isCreative() || player.getInventory().hasAnyMatching(s -> s.is(Items.FIRE_CHARGE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (Charge.get(stack) <= 0) {
            if (!level.isClientSide()) {
                Charge.use(stack, player, 1, "tool");
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!hasFireCharge(player)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player) || level.isClientSide()) {
            return;
        }
        fire(stack, level, player, level(getUseDuration(stack, entity) - timeLeft, player.isCreative(), player.isShiftKeyDown()));
    }

    /** Throws a fireball of the given blast size, and pays for it. */
    public static boolean fire(ItemStack stack, Level level, Player player, int blast) {
        int cost = Math.max(1, blast / 2);
        if (!Charge.use(stack, player, cost, "tool")) {
            return false;
        }
        Vec3 look = player.getLookAngle();
        LargeFireball ball = new LargeFireball(level, player, look.scale(0.2), blast);
        ball.setPos(player.getX() + look.x * 2, player.getY() + 1, player.getZ() + look.z * 2);
        level.addFreshEntity(ball);
        level.playSound(null, player.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1F, 1F);
        if (!player.isCreative() && level.random.nextInt(3) == 0) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack there = player.getInventory().getItem(i);
                if (there.is(Items.FIRE_CHARGE)) {
                    there.shrink(1);
                    break;
                }
            }
        }
        return true;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }
}
