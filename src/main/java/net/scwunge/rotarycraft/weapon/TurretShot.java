package net.scwunge.rotarycraft.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * A turret's projectile, as the original's turret shot: it flies straight, and strikes (see {@link #impact()}) when a creature
 * comes within a block of it, when it meets a solid block other than its own gun, or after four seconds.
 */
public abstract class TurretShot extends Entity {
    @Nullable
    protected WorldGuard.Owner owner;
    @Nullable
    protected BlockPos gun;

    protected TurretShot(EntityType<? extends TurretShot> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    protected void launch(double x, double y, double z, Vec3 velocity, BlockPos gun, @Nullable WorldGuard.Owner owner) {
        setPos(x, y, z);
        setDeltaMovement(velocity);
        this.gun = gun;
        this.owner = owner;
    }

    /** What happens where the shot strikes; it is removed afterwards. */
    protected abstract void impact(ServerLevel level);

    /** Extra work each client tick (tracer particles). */
    protected void clientTick() {}

    /** Extra work each tick of flight, before moving. */
    protected void flightTick(ServerLevel level, BlockPos at, BlockState state) {}

    /** Called after moving into water. */
    protected void inWater(ServerLevel level) {}

    private boolean isGun(BlockState state) {
        return gun != null && level().isLoaded(gun) && state.is(level().getBlockState(gun).getBlock());
    }

    @Override
    public void tick() {
        if (level().isClientSide) {
            clientTick();
            super.tick();
            setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);
            return;
        }
        tickCount++;
        ServerLevel level = (ServerLevel) level();
        BlockPos at = blockPosition();
        if (!level.isLoaded(at) || gun != null && level.isLoaded(gun) && level.getBlockEntity(gun) == null) {
            discard();
            return;
        }
        BlockState state = level.getBlockState(at);
        boolean mobs = !level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(1)).isEmpty();
        boolean solid = !state.isAir() && !state.canBeReplaced() && !isGun(state);
        if (mobs || solid || tickCount > 80) {
            strike(level);
            return;
        }
        flightTick(level, at, state);
        if (isRemoved()) {
            return;
        }
        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement());
        BlockHitResult blockHit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            to = blockHit.getLocation();
        }
        boolean entityHit = false;
        double nearest = Double.MAX_VALUE;
        for (Entity e : level.getEntities(this, getBoundingBox().expandTowards(getDeltaMovement()).inflate(1))) {
            var clip = e.isPickable() ? e.getBoundingBox().inflate(0.3).clip(from, to) : java.util.Optional.<Vec3>empty();
            if (clip.isPresent() && from.distanceTo(clip.get()) < nearest) {
                nearest = from.distanceTo(clip.get());
                to = clip.get();
                entityHit = true;
            }
        }
        if (entityHit || blockHit.getType() == HitResult.Type.BLOCK) {
            if (!entityHit && isGun(level.getBlockState(blockHit.getBlockPos()))) {
                discard();
                return;
            }
            // strike where it hit, not where it was a tick ago (it may move several blocks a tick)
            setPos(to);
            strike(level);
            return;
        }
        setPos(to);
        if (isInWater()) {
            inWater(level);
        }
    }

    private void strike(ServerLevel level) {
        impact(level);
        discard();
    }

    /** The original's turret damage: named after the turret's owner where there is one. */
    protected DamageSource damageSource() {
        var type = level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(WeaponRegistry.TURRET_DAMAGE);
        return new DamageSource(type, this);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }
}
