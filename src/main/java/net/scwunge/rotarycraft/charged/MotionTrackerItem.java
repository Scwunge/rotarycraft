package net.scwunge.rotarycraft.charged;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;
import java.util.Set;

/** The motion tracker: lists what is along the line you look down (up to 128 blocks; mobs within 32, wither and dragon from any way), coloured by what it is. */
public class MotionTrackerItem extends ChargedItem {
    public static final double RANGE = 128;
    public static final double NEAR = 32;

    public MotionTrackerItem(Properties properties) {
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
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Set<LivingEntity> seen = new LinkedHashSet<>();
        for (double d = 1; d <= RANGE; d += 0.5) {
            Vec3 at = eye.add(look.scale(d));
            seen.addAll(level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(0.5), e -> e != player));
        }
        for (LivingEntity e : seen) {
            double distance = eye.distanceTo(e.getEyePosition());
            boolean boss = e instanceof WitherBoss || e instanceof EnderDragon;
            if (e instanceof Player || (distance > NEAR && !boss)) {
                continue;
            }
            ChatFormatting color = e instanceof EnderDragon ? ChatFormatting.DARK_PURPLE : e instanceof WitherBoss ? ChatFormatting.DARK_GRAY
                    : e instanceof EnderMan || e instanceof ZombifiedPiglin ? ChatFormatting.YELLOW : e instanceof Enemy ? ChatFormatting.RED
                    : e instanceof Animal || e instanceof Bat || e instanceof Squid ? ChatFormatting.GREEN : ChatFormatting.WHITE;
            player.sendSystemMessage(Component.translatable("message.rotarycraft.motion.away", e.getDisplayName().copy().withStyle(color), String.format("%.2f", distance)));
            if (e instanceof Mob mob && mob instanceof Enemy && mob.getTarget() == player) {
                player.sendSystemMessage(Component.translatable("message.rotarycraft.motion.attacking").withStyle(ChatFormatting.RED));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
