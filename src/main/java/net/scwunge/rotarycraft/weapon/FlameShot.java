package net.scwunge.rotarycraft.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import net.scwunge.rotarycraft.weapon.turret.FlameTurretBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * A burst of flame from the Flame Turret, as the original: it arcs under a little gravity, passes through creatures and only
 * strikes when it meets a block (not for its first five ticks, so it clears the turret), where it lights a fire and burns every
 * creature within a block and a half. It dies in water and after twelve seconds.
 */
public class FlameShot extends TurretShot {
    private FlameTurretBlockEntity.Attack attack = new FlameTurretBlockEntity.Attack(1, 3, 1, 4);

    public FlameShot(EntityType<? extends FlameShot> type, Level level) {
        super(type, level);
    }

    public FlameShot(Level level, double x, double y, double z, Vec3 velocity, BlockPos gun, @Nullable WorldGuard.Owner owner,
                     FlameTurretBlockEntity.Attack attack) {
        this(WeaponRegistry.FLAME_SHOT.get(), level);
        launch(x, y, z, velocity, gun, owner);
        this.attack = attack;
    }

    @Override
    protected boolean hitsEntities() {
        return false;
    }

    @Override
    protected int maxLife() {
        return 240;
    }

    @Override
    protected boolean strikesAtEndOfLife() {
        return false;
    }

    @Override
    protected int minHitTicks() {
        return 5;
    }

    @Override
    protected double gravity() {
        return 0.005;
    }

    @Override
    protected void clientTick() {
        level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0, 0, 0);
    }

    @Override
    protected void inWater(ServerLevel level) {
        if (random.nextInt(6) == 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, random.nextFloat() * 0.5f + 0.5f);
        }
        discard();
    }

    @Override
    protected void impact(ServerLevel level) {
        if (RotaryConfig.get(RotaryConfig.WEAPON_BLOCK_DAMAGE)) {
            // the air block in front of what it struck
            Vec3 back = position().subtract(getDeltaMovement().normalize().scale(0.1));
            BlockPos at = BlockPos.containing(back);
            BlockState fire = Blocks.FIRE.defaultBlockState().setValue(FireBlock.AGE, Math.min(15, attack.fireAge()));
            if (level.getBlockState(at).isAir() && BaseFireBlock.canBePlacedAt(level, at, net.minecraft.core.Direction.UP)) {
                WorldGuard.setBlock(level, at, fire, owner);
            }
        }
        for (Entity e : level.getEntities(this, new AABB(position(), position()).inflate(1.5))) {
            if (e instanceof LivingEntity living) {
                living.hurt(level.damageSources().onFire(), attack.damage());
                living.igniteForSeconds(attack.burnSeconds());
            } else if (e instanceof EndCrystal || e instanceof HangingEntity) {
                e.hurt(damageSource(), attack.damage());
            }
        }
    }
}
