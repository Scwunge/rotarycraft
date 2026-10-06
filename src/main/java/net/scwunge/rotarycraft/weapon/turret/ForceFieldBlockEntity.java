package net.scwunge.rotarycraft.weapon.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import net.scwunge.rotarycraft.weapon.RailgunShot;

import java.util.List;

/**
 * Force Field, as the original: from 524 kW it makes a dome, 2 blocks across plus one more for each 32 kW above that, that stops
 * what comes through its skin: arrows hang in the air, fireballs, wither skulls, primed TNT and rail gun slugs go off at the edge
 * (as explosions that hurt creatures but do not break blocks), potions burst, snowballs and eggs splatter, monsters are shoved
 * back, and players other than the owner are thrown out. The enchantment range boost is left out.
 */
public class ForceFieldBlockEntity extends DomeBlockEntity {
    public static final long MIN_POWER = 524_288;
    public static final int FALLOFF = 32_768;

    public ForceFieldBlockEntity(BlockPos pos, BlockState state) {
        super(WeaponRegistry.FORCE_FIELD_BE.get(), pos, state);
    }

    @Override
    protected long minimumPower() {
        return MIN_POWER;
    }

    @Override
    protected int falloff() {
        return FALLOFF;
    }

    @Override
    protected String switchName() {
        return "forceField";
    }

    @Override
    protected ParticleOptions particle() {
        return ParticleTypes.ENCHANTED_HIT;
    }

    /** The original's flash: white when something has just been stopped, fading back to blue. */
    @Override
    public int domeColor(long gameTime) {
        long t = Math.max(1, gameTime - lastHit() + 1);
        int r = (int) Math.min(255, 256 / t);
        int g = (int) Math.min(255, 128 + 512 / t);
        return (r << 16) | (g << 8) | 255;
    }

    @Override
    protected void protect(ServerLevel level, int range) {
        Vec3 centre = centre();
        List<Entity> threats = level.getEntities((Entity) null, box(range), e -> !e.isRemoved() && e != null);
        for (Entity threat : threats) {
            if (threat instanceof AbstractArrow arrow) {
                holdArrow(level, arrow);
            } else if (isAtBorder(threat.position(), centre, range)) {
                stop(level, threat, centre, range);
            }
        }
    }

    private void holdArrow(ServerLevel level, AbstractArrow arrow) {
        Vec3 v = arrow.getDeltaMovement();
        if (v.x == 0 && v.z == 0 || arrow.onGround()) {
            return;
        }
        level.playSound(null, arrow.blockPosition(), SoundEvents.ARROW_HIT, SoundSource.BLOCKS, 1, 1);
        arrow.setXRot(-90);
        arrow.setDeltaMovement(0, Math.min(0, v.y), 0);
        arrow.hurtMarked = true;
        hit();
    }

    /** Within three blocks of the skin, on the inside. */
    private static boolean isAtBorder(Vec3 at, Vec3 centre, int range) {
        double dist = at.distanceTo(centre);
        return dist <= range && Math.abs(dist - range) <= 3;
    }

    private void stop(ServerLevel level, Entity threat, Vec3 centre, int range) {
        Vec3 at = threat.position();
        if (threat instanceof WitherSkull) {
            threat.discard();
            blast(level, at, 1);
        } else if (threat instanceof AbstractHurtingProjectile fireball) {
            Entity shooter = fireball.getOwner();
            if (shooter instanceof Ghast || shooter instanceof Player) {
                blast(level, at, 2);
            }
            if (shooter instanceof Blaze) {
                level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0);
                level.levelEvent(1009, threat.blockPosition(), 0);
            }
            threat.discard();
            hit();
        } else if (threat instanceof RailgunShot shot) {
            shot.strikeNow(level);
            hit();
        } else if (threat instanceof ThrownPotion potion) {
            burst(level, potion);
            hit();
        } else if (threat instanceof ThrownEgg) {
            threat.discard();
            level.sendParticles(ParticleTypes.ITEM_SNOWBALL, at.x, at.y, at.z, 4, 0.2, 0.2, 0.2, 0);
            level.playSound(null, threat.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1, 2);
            if (level.random.nextInt(8) == 0) {
                for (int i = level.random.nextInt(32) == 0 ? 4 : 1; i > 0; i--) {
                    Chicken chick = EntityType.CHICKEN.create(level);
                    if (chick != null) {
                        chick.setAge(-24000);
                        chick.moveTo(at.x, at.y, at.z, threat.getYRot(), 0);
                        level.addFreshEntity(chick);
                    }
                }
            }
            hit();
        } else if (threat instanceof Snowball) {
            threat.discard();
            level.sendParticles(ParticleTypes.ITEM_SNOWBALL, at.x, at.y, at.z, 4, 0.2, 0.2, 0.2, 0);
            level.playSound(null, threat.blockPosition(), SoundEvents.SNOW_BREAK, SoundSource.BLOCKS, 1, 1);
            hit();
        } else if (threat instanceof PrimedTnt) {
            threat.discard();
            blast(level, at, 4);
        } else if (threat instanceof Player player) {
            if (!isOwner(player.getUUID()) && !player.isSpectator()) {
                Vec3 out = new Vec3(at.x - centre.x, at.y - worldPosition.getY() - 1, at.z - centre.z);
                double dd = out.length();
                if (dd >= range - 1.5 && dd > 0) {
                    player.setDeltaMovement(out.scale(1 / dd));
                    player.hurtMarked = true;
                    hit();
                }
            }
        } else if (threat instanceof Enemy && threat instanceof LivingEntity mob) {
            Vec3 out = at.subtract(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
            double dist = out.length();
            if (dist > 0) {
                mob.setDeltaMovement(out.x / dist / 10, mob.onGround() ? out.y / dist / 10 : mob.getDeltaMovement().y, out.z / dist / 10);
                mob.setYRot(mob.getYRot() - 30 + level.random.nextInt(61));
                mob.hurtMarked = true;
            }
        } else if (threat instanceof Wolf wolf && wolf.isAngry()) {
            // the original pushes the wolf towards the middle; here it is kept off the skin from whichever side it is on
            Vec3 out = at.subtract(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
            double dist = out.length();
            if (dist > 0) {
                double sign = at.distanceTo(centre) > range ? 1 : -1;
                wolf.setDeltaMovement(sign * out.x / dist / 15, wolf.onGround() ? sign * out.y / dist / 15 : wolf.getDeltaMovement().y, sign * out.z / dist / 15);
                wolf.setYRot(level.random.nextInt(360));
                wolf.hurtMarked = true;
            }
        }
    }

    /** An explosion that hurts creatures but breaks no blocks and starts no fires. */
    private void blast(ServerLevel level, Vec3 at, float power) {
        level.explode(null, at.x, at.y, at.z, power, Level.ExplosionInteraction.NONE);
        hit();
    }

    /** The potion takes effect on those around it, as when it lands, scaled down with the distance. */
    private static void burst(ServerLevel level, ThrownPotion potion) {
        PotionContents contents = potion.getItem().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (contents.hasEffects()) {
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, potion.getBoundingBox().inflate(4, 2, 4))) {
                double distSqr = potion.distanceToSqr(target);
                if (distSqr >= 16) {
                    continue;
                }
                double scale = 1 - Math.sqrt(distSqr) / 4;
                contents.forEachEffect(effect -> {
                    if (effect.getEffect().value().isInstantenous()) {
                        effect.getEffect().value().applyInstantenousEffect(potion, potion.getOwner(), target, effect.getAmplifier(), scale);
                    } else {
                        int ticks = (int) (scale * effect.getDuration() + 0.5);
                        if (ticks > 20) {
                            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect.getEffect(), ticks, effect.getAmplifier(), effect.isAmbient(), effect.isVisible()));
                        }
                    }
                });
            }
        }
        level.levelEvent(2002, potion.blockPosition(), contents.getColor());
        potion.discard();
    }
}
