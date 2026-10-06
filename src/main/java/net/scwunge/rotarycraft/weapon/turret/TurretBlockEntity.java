package net.scwunge.rotarycraft.weapon.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.scwunge.rotarycraft.blockentity.ConsumerBlockEntity;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * The original's aimed cannon: a turret standing on its power input (or hanging under it), which turns towards the nearest
 * target it can see (hostile mobs, and players who are not its owner or on its whitelist) and fires when it is on aim. phi is the
 * turn about the vertical, theta the elevation, in degrees, moved a step a tick as in the original.
 */
public abstract class TurretBlockEntity extends ConsumerBlockEntity {
    public static final int MAX_LOW_ANGLE = -10;

    private final String weapon;
    @Nullable
    private WorldGuard.Owner owner;
    private final List<String> safePlayers = new ArrayList<>();
    /** Aim, synced to clients for the renderer. */
    public float phi, theta;
    @Nullable
    protected Vec3 target;
    @Nullable
    protected Entity targetEntity;
    protected int tickcount;
    private float syncedPhi = Float.NaN, syncedTheta = Float.NaN;

    protected TurretBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, String weapon) {
        super(type, pos, state);
        this.weapon = weapon;
    }

    /** 1 standing on its input, -1 hanging under it. */
    public int dir() {
        return facing() == Direction.DOWN ? -1 : 1;
    }

    @Nullable
    public WorldGuard.Owner owner() {
        return owner;
    }

    public void setOwner(Player player) {
        owner = new WorldGuard.Owner(player.getUUID(), player.getGameProfile().getName());
        setChanged();
    }

    public List<String> safePlayers() {
        return Collections.unmodifiableList(safePlayers);
    }

    public boolean addSafePlayer(String name) {
        if (safePlayers.contains(name)) {
            return false;
        }
        safePlayers.add(name);
        setChanged();
        return true;
    }

    public void removeSafePlayer(String name) {
        safePlayers.remove(name);
        setChanged();
    }

    public boolean enabled() {
        return RotaryConfig.weaponEnabled(weapon);
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!enabled()) {
            return;
        }
        tickcount++;
        if (!powered) {
            return;
        }
        target = findTarget();
        if (target != null) {
            aimAt(target);
        }
        turretTick();
        if (phi != syncedPhi || theta != syncedTheta) {
            syncedPhi = phi;
            syncedTheta = theta;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    /** One tick of the turret's own work once powered and aimed (usually: fire when on target and loaded). */
    protected abstract void turretTick();

    /** Ticks between shots. */
    public int operationTime() {
        return 20;
    }

    public abstract int range();

    /** Aiming error added to each coordinate of the aim point. */
    protected double randomOffset() {
        return 0;
    }

    protected double thetaOffset() {
        return 0;
    }

    protected float aimingSpeed() {
        return 1;
    }

    // ---- targeting --------------------------------------------------------------------------------------------------

    /** The nearest valid target the turret can see and swing to, as the point to shoot at; null for none. */
    @Nullable
    protected Vec3 findTarget() {
        int r = range();
        double x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        AABB box = new AABB(worldPosition).inflate(r);
        double best = r + 2;
        Entity found = null;
        for (Entity e : level.getEntities((Entity) null, box, this::isValidTarget)) {
            double dist = e.position().distanceTo(new Vec3(x + 0.5, y + 0.5, z + 0.5));
            if (dist >= best || !canSee(e.position(), r)) {
                continue;
            }
            double dy = -(e.getY() - y);
            double reqTheta = -90 + Math.toDegrees(Math.abs(Math.acos(dy / dist)));
            if (dir() == 1 ? reqTheta >= MAX_LOW_ANGLE : reqTheta <= -MAX_LOW_ANGLE) {
                best = dist;
                found = e;
            }
        }
        targetEntity = found;
        if (found == null) {
            return null;
        }
        return new Vec3(found.getX() + randomOffset(), found.getY() + found.getEyeHeight() * 0.25 + randomOffset(), found.getZ() + randomOffset());
    }

    protected boolean isValidTarget(Entity e) {
        return e instanceof LivingEntity living && living.isAlive() && living.getHealth() > 0 && isMobOrUnlistedPlayer(living);
    }

    protected final boolean isMobOrUnlistedPlayer(LivingEntity e) {
        return e instanceof Enemy || e instanceof Player p && RotaryConfig.get(RotaryConfig.TURRETS_TARGET_PLAYERS) && !isSafe(p);
    }

    public boolean isSafe(Player player) {
        if (!RotaryConfig.get(RotaryConfig.TURRETS_TARGET_PLAYERS) || player.isCreative() || player.isSpectator()) {
            return true;
        }
        String name = player.getGameProfile().getName();
        return owner != null && owner.id().equals(player.getUUID()) || safePlayers.contains(name);
    }

    /**
     * The original's line of sight: rays from the corners, middle and side of the turret block to the point; it sees the point if
     * any ray gets through (passing out of its own block).
     */
    protected boolean canSee(Vec3 to, double range) {
        if (to.distanceTo(worldPosition.getCenter()) > range + 2) {
            return false;
        }
        double[][] corners = {{0, 0, 0}, {1, 0, 0}, {0, 1, 0}, {1, 1, 0}, {0, 0, 1}, {1, 0, 1}, {0, 1, 1}, {1, 1, 1}, {0.5, 0.5, 0.5}, {0, 0.5, 0}};
        for (double[] c : corners) {
            if (clear(new Vec3(worldPosition.getX() + c[0], worldPosition.getY() + c[1], worldPosition.getZ() + c[2]), to)) {
                return true;
            }
        }
        return false;
    }

    private boolean clear(Vec3 from, Vec3 to) {
        for (int i = 0; i < 3; i++) {
            BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            if (hit.getType() == HitResult.Type.MISS) {
                return true;
            }
            if (!hit.getBlockPos().equals(worldPosition)) {
                return false;
            }
            from = hit.getLocation().add(to.subtract(from).normalize().scale(0.05));
        }
        return false;
    }

    // ---- aiming (the original's maths) ------------------------------------------------------------------------------

    /** Polar angles of (x, y, z): [length, polar angle from +y, azimuth], degrees, as DragonAPI's cartesianToPolar. */
    static double[] polar(double x, double y, double z) {
        double r = Math.sqrt(x * x + y * y + z * z);
        return new double[] {r, Math.toDegrees(Math.acos(y / r)), 180 + Math.toDegrees(Math.atan2(x, z))};
    }

    protected void aimAt(Vec3 t) {
        double[] tg = polar(worldPosition.getX() + 0.5 - t.x, worldPosition.getY() + 0.5 - t.y, worldPosition.getZ() + 0.5 - t.z);
        adjustAim(tg[2], Math.abs(tg[1]) - 90 + 0.25);
    }

    /** Steps the aim towards (ang, incl); true once within 3 degrees of both. */
    protected boolean adjustAim(double ang, double incl) {
        incl += thetaOffset();
        if (ang - phi > 180) {
            ang -= 360;
        }
        float speed = aimingSpeed();
        if (phi < ang) {
            phi += speed * 2;
        }
        if (phi > ang) {
            phi -= speed * 2;
        }
        if (theta < incl) {
            theta += speed;
        }
        if (theta > incl) {
            theta -= speed;
        }
        if (theta < MAX_LOW_ANGLE && dir() == 1) {
            theta = MAX_LOW_ANGLE;
        }
        if (theta > -MAX_LOW_ANGLE && dir() == -1) {
            theta = MAX_LOW_ANGLE;
        }
        return Math.abs(phi - ang) <= 3 && Math.abs(theta - incl) <= 3;
    }

    /** Whether the barrel points within 5 degrees of the point. */
    public boolean isAimingAt(Vec3 t) {
        double[] tg = polar(worldPosition.getX() - t.x, worldPosition.getY() - t.y, worldPosition.getZ() - t.z);
        tg[1] = Math.abs(tg[1]) - 90;
        float phi2 = phi % 360;
        if (phi2 < 0) {
            phi2 += 360;
        }
        if (tg[2] - phi2 > 180) {
            tg[2] -= 360;
        }
        return Math.abs(theta - tg[1]) <= 5 && Math.abs(phi2 - tg[2]) <= 5;
    }

    /** Where shots leave the barrel, vertically, for a shot heading {@code dy}. */
    protected double firingY(double dy) {
        return worldPosition.getY() + (dir() == 1 ? 0.75 : -0.25) + dy;
    }

    // ---- saving and syncing -----------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("owner", owner.id());
            tag.putString("ownerName", owner.name());
        }
        ListTag list = new ListTag();
        safePlayers.forEach(n -> list.add(StringTag.valueOf(n)));
        tag.put("safePlayers", list);
        tag.putFloat("phi", phi);
        tag.putFloat("theta", theta);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("owner") ? new WorldGuard.Owner(tag.getUUID("owner"), tag.getString("ownerName")) : null;
        safePlayers.clear();
        tag.getList("safePlayers", Tag.TAG_STRING).forEach(t -> safePlayers.add(t.getAsString()));
        phi = tag.getFloat("phi");
        theta = tag.getFloat("theta");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("phi", phi);
        tag.putFloat("theta", theta);
        if (owner != null) {
            tag.putUUID("owner", owner.id());
            tag.putString("ownerName", owner.name());
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        phi = tag.getFloat("phi");
        theta = tag.getFloat("theta");
        owner = tag.hasUUID("owner") ? new WorldGuard.Owner(tag.getUUID("owner"), tag.getString("ownerName")) : null;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Whether {@code id} is the owner. */
    public boolean isOwner(UUID id) {
        return owner != null && owner.id().equals(id);
    }
}
