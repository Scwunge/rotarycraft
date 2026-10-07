package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.DecorRegistry;
import net.scwunge.rotarycraft.weapon.Owned;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Air Gun (TileEntityAirGun): a blast of air along the way it faces, as far as ten blocks plus two for every doubling of the torque. Every living thing in
 * the beam that is standing on something is thrown along it (at a quarter of log2 of the torque, blocks a tick) and up; whoever placed the gun is never moved.
 * It needs 512 N*m and 16 kW, and fires as often as every four ticks, faster the faster the shaft turns.
 */
public class AirGunBlockEntity extends ConsumerBlockEntity implements Owned {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(512, 1, 16384);

    private int tickCount;
    @Nullable
    private WorldGuard.Owner owner;

    public AirGunBlockEntity(BlockPos pos, BlockState state) {
        super(DecorRegistry.AIR_GUN.type().get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    public void setOwner(Player player) {
        owner = new WorldGuard.Owner(player.getUUID(), player.getGameProfile().getName());
        setChanged();
    }

    private static double log2(int n) {
        return Math.log(n + 1D) / Math.log(2);
    }

    /** How far it reaches. */
    public int range() {
        return 10 + 2 * (int) log2(torque);
    }

    /** How hard it throws: blocks a tick. */
    public double force() {
        return log2(torque) / 4;
    }

    public int operationTime() {
        return Math.max(16 - (int) log2(omega), 4);
    }

    /** The beam: from the block in front, as far as it reaches. */
    public AABB beam() {
        Direction dir = facing();
        AABB box = new AABB(worldPosition).deflate(0.1).move(dir.getStepX(), 0, dir.getStepZ());
        int r = range();
        return switch (dir) {
            case EAST -> box.setMaxX(box.maxX + r);
            case WEST -> box.setMinX(box.minX - r);
            case SOUTH -> box.setMaxZ(box.maxZ + r);
            default -> box.setMinZ(box.minZ - r);
        };
    }

    private boolean isOwner(LivingEntity e) {
        return e instanceof Player player && owner != null && owner.id().equals(player.getUUID());
    }

    /** Throws what is in the beam; true if it moved anything. */
    public boolean blast(ServerLevel server) {
        Vec3 push = Vec3.atLowerCornerOf(facing().getNormal()).scale(force());
        boolean moved = false;
        List<LivingEntity> targets = server.getEntitiesOfClass(LivingEntity.class, beam(), LivingEntity::isAlive);
        for (LivingEntity e : targets) {
            BlockPos below = BlockPos.containing(e.getX(), e.getY() - 1, e.getZ());
            if (isOwner(e) || server.getBlockState(below).isAir()) {
                continue;
            }
            e.setDeltaMovement(push.x, 0.5, push.z);
            e.hurtMarked = true;
            moved = true;
        }
        if (moved) {
            server.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1F, 1F);
        }
        return moved;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server) || !powered || !MachineConfig.enabled("airGun") || ++tickCount < operationTime()) {
            return;
        }
        tickCount = 0;
        blast(server);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("owner_id", owner.id());
            tag.putString("owner_name", owner.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("owner_id") ? new WorldGuard.Owner(tag.getUUID("owner_id"), tag.getString("owner_name")) : null;
    }
}
