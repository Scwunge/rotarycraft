package net.scwunge.rotarycraft.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * TNT fired from the TNT Cannon: vanilla primed TNT (same gravity, drag and blast of power 4), except that it only breaks
 * blocks if weaponBlockDamage and mobGriefing allow it and its owner may change the blocks there (so claims can stop it).
 */
public class CannonTnt extends PrimedTnt {
    @Nullable
    private WorldGuard.Owner owner;

    public CannonTnt(EntityType<? extends PrimedTnt> type, Level level) {
        super(type, level);
    }

    public CannonTnt(Level level, double x, double y, double z, int fuse, @Nullable WorldGuard.Owner owner) {
        this(WeaponRegistry.CANNON_TNT.get(), level);
        setPos(x, y, z);
        setFuse(fuse);
        this.owner = owner;
        xo = x;
        yo = y;
        zo = z;
    }

    @Override
    public void tick() {
        applyGravity();
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.98));
        if (onGround()) {
            setDeltaMovement(getDeltaMovement().multiply(0.7, -0.5, 0.7));
        }
        int fuse = getFuse() - 1;
        setFuse(fuse);
        if (fuse <= 0) {
            discard();
            if (!level().isClientSide) {
                detonate((ServerLevel) level());
            }
        } else {
            updateInWaterStateAndDoFluidPushing();
            if (level().isClientSide) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 0, 0, 0);
            }
        }
    }

    private void detonate(ServerLevel level) {
        boolean breaks = RotaryConfig.get(RotaryConfig.WEAPON_BLOCK_DAMAGE) && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                && WorldGuard.mayChange(level, BlockPos.containing(position()), owner);
        level.explode(this, getX(), getY(0.0625), getZ(), 4, breaks ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
    }
}
