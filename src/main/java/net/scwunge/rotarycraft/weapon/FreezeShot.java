package net.scwunge.rotarycraft.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import org.jetbrains.annotations.Nullable;

/** A Freeze Gun snowball, as the original: everything within two blocks of where it strikes is frozen solid (not players in creative). */
public class FreezeShot extends TurretShot {
    /** The original's freeze: 60000 ticks. */
    public static final int DURATION = 60000;

    public FreezeShot(EntityType<? extends FreezeShot> type, Level level) {
        super(type, level);
    }

    public FreezeShot(Level level, double x, double y, double z, Vec3 velocity, BlockPos gun, @Nullable WorldGuard.Owner owner) {
        this(WeaponRegistry.FREEZE_SHOT.get(), level);
        launch(x, y, z, velocity, gun, owner);
    }

    @Override
    protected void impact(ServerLevel level) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(2))) {
            if (!(e instanceof Player p && p.isCreative())) {
                e.addEffect(new MobEffectInstance(WeaponRegistry.FREEZE, DURATION, 0));
            }
        }
    }

    @Override
    protected void inWater(ServerLevel level) {
        discard();
    }
}
