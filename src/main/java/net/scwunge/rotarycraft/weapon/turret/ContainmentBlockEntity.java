package net.scwunge.rotarycraft.weapon.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.registry.WeaponRegistry;

/**
 * Containment, as the original: from 131 kW it makes a dome (2 blocks plus one for each 8 kW above that) that keeps hostile
 * creatures in: they are kept from despawning and driven back from its skin. The Ender Dragon and the Wither are too strong
 * for that unless the power is 2.1 MW and 524 kW respectively, when they are held in too, and the Wither forgets targets that are
 * outside.
 */
public class ContainmentBlockEntity extends DomeBlockEntity {
    public static final long MIN_POWER = 131_072;
    public static final int FALLOFF = 8_192;
    public static final long DRAGON_POWER = 2_097_152;
    public static final long WITHER_POWER = 524_288;

    public ContainmentBlockEntity(BlockPos pos, BlockState state) {
        super(WeaponRegistry.CONTAINMENT_BE.get(), pos, state);
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
        return "containment";
    }

    @Override
    protected ParticleOptions particle() {
        return ParticleTypes.PORTAL;
    }

    @Override
    public int domeColor(long gameTime) {
        return (120 << 16) | 150;
    }

    @Override
    protected void protect(ServerLevel level, int range) {
        Vec3 centre = centre();
        long power = getPower();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box(range))) {
            Vec3 out = new Vec3(e.getX() - centre.x, e.getY() - centre.y, e.getZ() - centre.z);
            double dd = out.length();
            if (dd == 0) {
                continue;
            }
            if (isGeneralCapturable(e)) {
                if (e instanceof Mob mob) {
                    mob.setNoActionTime(0);
                }
                if (dd > range - 0.5) {
                    e.setDeltaMovement(out.scale(-0.5 / dd));
                    e.hurtMarked = true;
                }
            }
            if (e instanceof EnderDragon && power >= DRAGON_POWER && dd > range - 2) {
                e.setDeltaMovement(e.getDeltaMovement().subtract(out.scale(1 / dd)));
                e.hurtMarked = true;
            }
            if (e instanceof WitherBoss wither && power >= WITHER_POWER) {
                if (dd > range - 2) {
                    wither.setDeltaMovement(wither.getDeltaMovement().subtract(out.scale(1 / dd)));
                    wither.hurtMarked = true;
                }
                int id = wither.getAlternativeTarget(0);
                var target = level.getEntity(id);
                if (target != null && target.position().distanceTo(centre) > range) {
                    wither.setAlternativeTarget(0, 0);
                }
            }
        }
    }

    private static boolean isGeneralCapturable(LivingEntity e) {
        return !(e instanceof EnderDragon) && !(e instanceof WitherBoss) && e instanceof Enemy;
    }
}
