package net.scwunge.rotarycraft.blockentity;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.WorldMachineRegistry;

/**
 * Chunk Loader, as the original: fed from below at 2,097,152 rad/s or more it keeps the chunk it stands in loaded, and every
 * 524,288 W of power over its minimum speed widens the loaded square by one chunk in each direction (up to the configured
 * maximum radius, 8 as in the original). Below the speed, or when broken, it lets go. The chunks are held with NeoForge chunk
 * tickets, so they survive restarts and are validated on load. Off unless the server enables it (config, world_machines).
 */
public class ChunkLoaderBlockEntity extends ConsumerBlockEntity {
    public static final int MIN_SPEED = 2_097_152;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, MIN_SPEED, 1);
    public static final int BASE_RADIUS = 0;
    public static final int FALLOFF = 524_288;
    private static final int SYNC_INTERVAL = 20;

    /** Chunks this block holds tickets for ({@link ChunkPos#toLong}). */
    private final LongSet forced = new LongOpenHashSet();
    private int syncedOmega;

    public ChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(WorldMachineRegistry.CHUNK_LOADER_BE.get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    /** Chunks out from its own that it loads at this power (the original: BASE_RADIUS + (power - minimum speed) / FALLOFF, at most the maximum). */
    public static int radiusFor(long power, int maxRadius) {
        long r = BASE_RADIUS + (power - MIN_SPEED) / FALLOFF;
        return (int) Math.max(0, Math.min(maxRadius, r));
    }

    public int radius() {
        return radiusFor(getPower(), RotaryConfig.get(RotaryConfig.CHUNK_LOADER_RADIUS));
    }

    /** How many chunks it holds right now. */
    public int loadedChunks() {
        return forced.size();
    }

    public boolean isActive() {
        return omega >= MIN_SPEED;
    }

    /** It takes power from below only. */
    @Override
    public void serverTick() {
        IShaftPowerOutput.Reading in = IShaftPowerOutput.readInput(level, worldPosition, Direction.DOWN);
        setPower(in.torque(), in.omega());
        machineTick(hasEnoughPower());
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (powered && RotaryConfig.worldMachineEnabled("chunkLoader")) {
            load(server, radius());
            if (server.getGameTime() % 4 == 0) {
                server.sendParticles(ParticleTypes.PORTAL, worldPosition.getX() + 0.5, worldPosition.getY() + 0.6, worldPosition.getZ() + 0.5, 4, 0.2, 0.25, 0.2, 0.4);
            }
        } else {
            release();
        }
        if (omega != syncedOmega && server.getGameTime() % SYNC_INTERVAL == 0) {
            syncedOmega = omega;
            server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    /** Holds exactly the square of chunks {@code radius} out from this block's chunk. */
    private void load(ServerLevel server, int radius) {
        ChunkPos centre = new ChunkPos(worldPosition);
        LongSet wanted = new LongOpenHashSet();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                wanted.add(ChunkPos.asLong(centre.x + dx, centre.z + dz));
            }
        }
        if (wanted.equals(forced)) {
            return;
        }
        for (long chunk : new LongArrayList(forced)) {
            if (!wanted.contains(chunk)) {
                WorldMachineRegistry.CHUNK_TICKETS.forceChunk(server, worldPosition, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false, true);
                forced.remove(chunk);
            }
        }
        for (long chunk : wanted) {
            if (!forced.contains(chunk)) {
                WorldMachineRegistry.CHUNK_TICKETS.forceChunk(server, worldPosition, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), true, true);
                forced.add(chunk);
            }
        }
        setChanged();
    }

    /** Lets go of every chunk it holds. */
    public void release() {
        if (forced.isEmpty() || !(level instanceof ServerLevel server)) {
            return;
        }
        for (long chunk : new LongArrayList(forced)) {
            WorldMachineRegistry.CHUNK_TICKETS.forceChunk(server, worldPosition, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false, true);
        }
        forced.clear();
        setChanged();
    }

    /** A machine burnt out by an EMP lets go of its chunks. */
    @Override
    public void onEmp() {
        super.onEmp();
        release();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (isShutdown()) {
            release();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLongArray("forced", forced.toLongArray());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        forced.clear();
        for (long chunk : tag.getLongArray("forced")) {
            forced.add(chunk);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("omega", omega);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        omega = tag.getInt("omega");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
