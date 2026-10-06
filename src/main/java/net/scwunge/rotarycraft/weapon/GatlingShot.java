package net.scwunge.rotarycraft.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * A Gatling round, as the original: 1 damage to everything within a block and a half of where it lands, with no hurt delay so a
 * stream of them adds up, a tiny shove away from the gun, and glass and ice within a block of its path shattered. Slimes take an
 * eighth of the damage and a hard shove. Every tenth round leaves a tracer.
 */
public class GatlingShot extends TurretShot {
    public static final float DAMAGE = 1;

    public GatlingShot(EntityType<? extends GatlingShot> type, Level level) {
        super(type, level);
    }

    public GatlingShot(Level level, double x, double y, double z, Vec3 velocity, BlockPos gun, @Nullable WorldGuard.Owner owner) {
        this(WeaponRegistry.GATLING_SHOT.get(), level);
        launch(x, y, z, velocity, gun, owner);
    }

    @Override
    protected void clientTick() {
        if (getId() % 10 == 0) {
            Vec3 m = getDeltaMovement();
            for (double d = -0.25; d <= 0.25; d += 0.0625) {
                level().addParticle(ParticleTypes.FLAME, getX() + m.x * d, getY() + m.y * d, getZ() + m.z * d, 0, 0, 0);
            }
        }
    }

    @Override
    protected void flightTick(ServerLevel level, BlockPos at, BlockState state) {
        shatterNearby(level, at);
    }

    @Override
    protected void impact(ServerLevel level) {
        shatterNearby(level, blockPosition());
        Vec3 from = gun == null ? position() : Vec3.atCenterOf(gun);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(1.5))) {
            if (e instanceof Slime) {
                push(e, from, 1);
                e.hurt(damageSource(), DAMAGE / 8);
            } else {
                e.hurt(damageSource(), DAMAGE);
                push(e, from, 0.0625);
            }
            e.invulnerableTime = 0;
        }
    }

    private static void push(LivingEntity e, Vec3 from, double strength) {
        Vec3 away = e.position().subtract(from).multiply(1, 0, 1).normalize().scale(strength);
        e.setDeltaMovement(e.getDeltaMovement().add(away.x, 0.0, away.z));
        e.hurtMarked = true;
    }

    private void shatterNearby(ServerLevel level, BlockPos at) {
        if (!RotaryConfig.get(RotaryConfig.WEAPON_BLOCK_DAMAGE)) {
            return;
        }
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES) || state.is(BlockTags.ICE)) {
                WorldGuard.breakBlock(level, pos.immutable(), owner, false);
            }
        }
    }
}
