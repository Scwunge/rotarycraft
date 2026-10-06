package net.scwunge.rotarycraft.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import org.jetbrains.annotations.Nullable;

/** An anti-aircraft flak burst, as the original: 2 damage to every creature within four blocks of where it bursts. */
public class FlakShot extends TurretShot {
    public static final float DAMAGE = 2;

    public FlakShot(EntityType<? extends FlakShot> type, Level level) {
        super(type, level);
    }

    public FlakShot(Level level, double x, double y, double z, Vec3 velocity, BlockPos gun, @Nullable WorldGuard.Owner owner) {
        this(WeaponRegistry.FLAK_SHOT.get(), level);
        launch(x, y, z, velocity, gun, owner);
    }

    @Override
    protected void impact(ServerLevel level) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(4))) {
            e.hurt(damageSource(), DAMAGE);
            level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.PLAYER_HURT, SoundSource.HOSTILE, 2, 1);
        }
    }
}
