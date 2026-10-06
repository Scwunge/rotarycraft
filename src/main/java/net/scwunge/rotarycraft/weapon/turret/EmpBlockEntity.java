package net.scwunge.rotarycraft.weapon.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.scwunge.rotarycraft.blockentity.ConsumerBlockEntity;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import net.scwunge.rotarycraft.weapon.Owned;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * EMP, as the original: fed at least 268 MW from below it stores up the energy of a ton of TNT (4.184 GJ, a second's worth
 * of power at that rate, which takes about 16 seconds at the minimum), spends a while "loading" a listing of the machines within 64 blocks of it, then fires
 * once. Every RotaryCraft machine in range is burnt out for good (until it is broken and placed again) and anything else that
 * stores energy is drained flat. Machines in claims the owner may not change are spared. Players' worn power armour is drained
 * only if turretsTargetPlayers is on (drained rather than stripped and blown up, which the original did).
 */
public class EmpBlockEntity extends ConsumerBlockEntity implements Owned {
    public static final long MIN_POWER = 268_435_456L;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, MIN_POWER);
    public static final long BLAST_ENERGY = 4_184_000_000L;
    public static final int RANGE = 64;
    /** Columns of the area it lists per tick while loading (the original's charging speed of 4 gives 33). */
    private static final int LOAD_PER_TICK = 33;
    private static final int COLUMNS = (2 * RANGE + 1) * (2 * RANGE + 1);
    private static final int EFFECT_TICKS = 30;

    private long energy;
    private int loaded;
    private boolean fired;
    private long firedAt = -1;
    @Nullable
    private WorldGuard.Owner owner;

    public EmpBlockEntity(BlockPos pos, BlockState state) {
        super(WeaponRegistry.EMP_BE.get(), pos, state);
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

    public boolean isLoading() {
        return loaded < COLUMNS;
    }

    public boolean usable() {
        return !fired;
    }

    /** Ticks since it fired, while the blast is still shown; -1 when not. */
    public int effectAge() {
        if (firedAt < 0 || level == null) {
            return -1;
        }
        long age = level.getGameTime() - firedAt;
        return age >= 0 && age <= EFFECT_TICKS ? (int) age : -1;
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
        if (fired || !RotaryConfig.weaponEnabled("emp")) {
            return;
        }
        ServerLevel server = (ServerLevel) level;
        if (powered) {
            energy += getPower();
        }
        if (isLoading()) {
            int before = loaded;
            loaded = Math.min(COLUMNS, loaded + LOAD_PER_TICK);
            server.sendParticles(ParticleTypes.PORTAL, worldPosition.getX() + 0.5, worldPosition.getY() + 0.9, worldPosition.getZ() + 0.5, 6, 0.5, 0.3, 0.5, 0.5);
            if (!isLoading() || before == 0) {
                syncNow();
            }
        }
        if (energy / 20L >= BLAST_ENERGY && !isLoading()) {
            fire(server);
        }
    }

    private void fire(ServerLevel server) {
        fired = true;
        firedAt = server.getGameTime();
        setChanged();
        syncNow();
        server.playSound(null, worldPosition, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 4, 1.6f);
        List<BlockEntity> targets = new ArrayList<>();
        int minChunkX = (worldPosition.getX() - RANGE) >> 4, maxChunkX = (worldPosition.getX() + RANGE) >> 4;
        int minChunkZ = (worldPosition.getZ() - RANGE) >> 4, maxChunkZ = (worldPosition.getZ() + RANGE) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!server.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = server.getChunk(cx, cz);
                for (BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values())) {
                    BlockPos p = be.getBlockPos();
                    if (Math.abs(p.getX() - worldPosition.getX()) <= RANGE && Math.abs(p.getZ() - worldPosition.getZ()) <= RANGE) {
                        targets.add(be);
                    }
                }
            }
        }
        for (BlockEntity be : targets) {
            if (be.isRemoved() || !WorldGuard.mayChange(server, be.getBlockPos(), owner)) {
                continue;
            }
            if (be instanceof PowerBlockEntity machine) {
                if (!machine.isShutdown()) {
                    machine.onEmp();
                    spark(server, be.getBlockPos());
                }
            } else if (drain(server, be)) {
                spark(server, be.getBlockPos());
            }
        }
        if (RotaryConfig.get(RotaryConfig.TURRETS_TARGET_PLAYERS)) {
            for (Player player : server.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(worldPosition).inflate(128, 64, 128))) {
                for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    ItemStack worn = player.getItemBySlot(slot);
                    IEnergyStorage energyStorage = worn.getCapability(Capabilities.EnergyStorage.ITEM);
                    if (energyStorage != null && energyStorage.getEnergyStored() > 0) {
                        energyStorage.extractEnergy(energyStorage.getEnergyStored(), false);
                    }
                }
            }
        }
    }

    /** Empties whatever energy the block holds, from whichever side lets it. */
    private static boolean drain(ServerLevel server, BlockEntity be) {
        BlockPos pos = be.getBlockPos();
        boolean drained = false;
        List<Direction> sides = new ArrayList<>();
        sides.add(null);
        sides.addAll(List.of(Direction.values()));
        for (Direction side : sides) {
            IEnergyStorage storage = server.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
            if (storage != null && storage.getEnergyStored() > 0) {
                drained |= storage.extractEnergy(storage.getEnergyStored(), false) > 0;
            }
        }
        return drained;
    }

    private static void spark(ServerLevel server, BlockPos pos) {
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.4, 0.4, 0.4, 0.2);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("energy", energy);
        tag.putInt("loaded", loaded);
        tag.putBoolean("fired", fired);
        tag.putLong("firedAt", firedAt);
        if (owner != null) {
            tag.putUUID("owner", owner.id());
            tag.putString("ownerName", owner.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy = tag.getLong("energy");
        loaded = tag.getInt("loaded");
        fired = tag.getBoolean("fired");
        firedAt = tag.contains("firedAt") ? tag.getLong("firedAt") : -1;
        owner = tag.hasUUID("owner") ? new WorldGuard.Owner(tag.getUUID("owner"), tag.getString("ownerName")) : null;
    }

    private void syncNow() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("loaded", loaded);
        tag.putBoolean("fired", fired);
        tag.putLong("firedAt", firedAt);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loaded = tag.getInt("loaded");
        fired = tag.getBoolean("fired");
        firedAt = tag.getLong("firedAt");
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
