package net.scwunge.rotarycraft.handheld;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.charged.Charge;
import net.scwunge.rotarycraft.charged.ChargedItem;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.registry.HandheldRegistry;
import net.scwunge.rotarycraft.tool.BedrockArmorItem;

import java.util.ArrayList;
import java.util.List;

/**
 * The original's gravel gun: loaded with a piece of gravel it fires a supersonic flint along your line of sight, up to 128 blocks, at the first creature in the way
 * (not you, and players only if the server allows it). The more charge it has, the harder it hits, and each shot takes the log2 of what it holds off: a full gun kills
 * nearly anything in one blow. Bedrock armour cuts a hit on a player by a quarter a piece.
 */
public class GravelGunItem extends ChargedItem {
    public static final int RANGE = 128;

    public GravelGunItem(Properties properties) {
        super(properties);
    }

    /** What a shot does at this charge, in half hearts. */
    public static float damage(int charge) {
        if (charge <= 0) {
            return 0;
        }
        if (charge == 1) {
            return 1;
        }
        boolean hard = MachineConfig.get(MachineConfig.HARD_GRAVEL_GUN);
        double pow = Math.pow(charge / 2, 3);
        double base = (hard ? 1.00005 : 1.0001) + Math.pow(charge, hard ? 0.15 : 0.1875) / 150000D;
        return (float) (1 + (Math.log(pow) / Math.log(2) / 2) * Math.pow(base, charge));
    }

    /** What a shot takes off the charge. */
    public static int cost(int charge) {
        return Math.max(1, (int) (Math.log(1 + charge) / Math.log(2)));
    }

    private static boolean attackable(Entity entity, Player shooter) {
        if (entity == shooter) {
            return false;
        }
        if (entity instanceof EndCrystal || entity instanceof EnderDragonPart) {
            return true;
        }
        return entity instanceof LivingEntity && (MachineConfig.get(MachineConfig.GRAVEL_GUN_PVP) || !(entity instanceof Player));
    }

    private static boolean hasGravel(Player player) {
        return player.isCreative() || player.getInventory().hasAnyMatching(s -> s.is(Items.GRAVEL));
    }

    private static void takeGravel(Player player) {
        if (player.isCreative()) {
            return;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.GRAVEL)) {
                stack.shrink(1);
                return;
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.consume(stack);
        }
        int charge = Charge.get(stack);
        if (charge <= 0) {
            Charge.use(stack, player, 1, "tool");
            return InteractionResultHolder.fail(stack);
        }
        if (!hasGravel(player)) {
            level.levelEvent(1001, player.blockPosition(), 0);
            return InteractionResultHolder.fail(stack);
        }
        ServerLevel server = (ServerLevel) level;
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        for (float t = 1; t <= RANGE; t += 0.5F) {
            Vec3 at = eye.add(look.scale(t));
            List<Entity> hit = new ArrayList<>();
            for (Entity e : server.getEntities(player, new AABB(at, at).inflate(0.5))) {
                if (attackable(e, player) && player.hasLineOfSight(e)) {
                    hit.add(e);
                }
            }
            if (hit.isEmpty()) {
                continue;
            }
            float damage = damage(charge);
            boolean dragon = false;
            for (Entity target : hit) {
                if (target instanceof EnderDragonPart part) {
                    if (dragon) {
                        continue;
                    }
                    dragon = true;
                    part.parentMob.hurt(part, source(server, player), damage);
                } else {
                    strike(server, player, target, damage);
                }
                flint(server, player, target);
            }
            server.playSound(null, player.blockPosition(), SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 1.5F, 2F);
            takeGravel(player);
            Charge.use(stack, player, cost(charge), "tool");
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    private static DamageSource source(ServerLevel level, Player player) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(HandheldRegistry.GRAVEL_GUN_DAMAGE), player, player);
    }

    /** Hits a creature or crystal, making sure that a creature that survives has lost the best part of the shot (up to ten points) whatever its armour. */
    private static void strike(ServerLevel level, Player player, Entity target, float damage) {
        if (!(target instanceof LivingEntity living)) {
            target.hurt(source(level, player), damage);
            return;
        }
        if (target instanceof Player) {
            for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                if (living.getItemBySlot(slot).getItem() instanceof BedrockArmorItem) {
                    damage *= 0.75F;
                }
            }
        }
        float before = living.getHealth();
        living.invulnerableTime = 0;
        living.hurt(source(level, player), damage);
        float after = living.getHealth();
        if (after > 0) {
            float wanted = Math.min(after, before - Math.min(10, damage));
            if (wanted <= 0) {
                living.setHealth(0.01F);
                living.invulnerableTime = 0;
                living.hurt(source(level, player), damage);
            } else {
                living.setHealth(wanted);
            }
        }
    }

    /** The flint that does it: thrown at the target and gone in a few ticks. */
    private static void flint(ServerLevel level, Player player, Entity target) {
        Vec3 look = player.getLookAngle().normalize();
        ItemEntity flint = new ItemEntity(level, player.getX() + look.x, player.getY() + player.getEyeHeight() + look.y, player.getZ() + look.z, new ItemStack(Items.FLINT));
        flint.setPickUpDelay(100);
        flint.setDeltaMovement(target.getX() - player.getX(), target.getY() - player.getY() + 1, target.getZ() - player.getZ());
        flint.lifespan = 5;
        level.addFreshEntity(flint);
        Vec3 to = target.position().subtract(player.position());
        double dist = Math.max(0.1, to.length());
        for (float t = 0; t < 2; t += 0.25F) {
            level.sendParticles(ParticleTypes.CRIT, player.getX() + look.x, player.getY() + player.getEyeHeight() + look.y, player.getZ() + look.z, 0, to.x / dist * t, to.y / dist * t, to.z / dist * t, 1);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        float damage = damage(Charge.get(stack));
        tooltip.add(damage > 0 ? Component.translatable("item.rotarycraft.gravel_gun.damage", String.format("%.1f", damage / 2F)) : Component.translatable("item.rotarycraft.gravel_gun.empty"));
    }
}
