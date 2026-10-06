package net.scwunge.rotarycraft.weapon.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.weapon.Owned;
import net.scwunge.rotarycraft.weapon.RangeHost;
import net.scwunge.rotarycraft.weapon.RangeMenu;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import org.jetbrains.annotations.Nullable;

/**
 * The base of the Force Field and Containment, as the original's protection dome: power from any side, a radius set on a screen and
 * held to what the power allows (the minimum power gives 2 blocks, and each further "falloff" watts one more, to the configured
 * limit), and no field at all while anything that blocks light is above it within that radius.
 */
public abstract class DomeBlockEntity extends OmniConsumerBlockEntity implements RangeHost, Owned, MenuProvider {
    private static final int MAX_SET = 999;

    private int setRange;
    private int range;
    private int maxRange;
    private int syncedRange = -1, syncedMax = -1;
    private boolean clear = true;
    protected long lastHit = Long.MIN_VALUE / 2;
    private long syncedHit = Long.MIN_VALUE / 2;
    @Nullable
    protected WorldGuard.Owner owner;

    protected DomeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract long minimumPower();

    protected abstract int falloff();

    protected abstract String switchName();

    protected abstract ParticleOptions particle();

    /** What the dome does to what is in it, once a tick, while it has a field. */
    protected abstract void protect(ServerLevel level, int range);

    @Override
    public PowerRequirement requirement() {
        return new PowerRequirement(1, 1, minimumPower());
    }

    @Override
    public void setOwner(Player player) {
        owner = new WorldGuard.Owner(player.getUUID(), player.getGameProfile().getName());
        setChanged();
    }

    public boolean isOwner(java.util.UUID id) {
        return owner != null && owner.id().equals(id);
    }

    @Override
    public int setRange() {
        return setRange;
    }

    @Override
    public void setSetRange(int range) {
        setRange = Math.max(0, Math.min(range, MAX_SET));
        setChanged();
        if (level != null && !level.isClientSide) {
            clear = isClear();
            refreshRange();
            syncDome();
        }
    }

    @Override
    public int range() {
        return range;
    }

    @Override
    public int maxRange() {
        return maxRange;
    }

    /** The client's view of how recently something hit the field, in ticks (for the flash). */
    public long lastHit() {
        return lastHit;
    }

    /** The radius the power allows, with no regard to what has been set: nothing under a roof, or without the power. */
    private int computeMaxRange() {
        if (!clear || isShutdown() || !RotaryConfig.weaponEnabled(switchName()) || getPower() < minimumPower()) {
            return 0;
        }
        long range = 2 + (getPower() - minimumPower()) / falloff();
        int limit = Math.max(64, RotaryConfig.get(RotaryConfig.FORCE_FIELD_RANGE));
        return (int) Math.min(range, limit);
    }

    private void refreshRange() {
        maxRange = computeMaxRange();
        range = Math.min(setRange, maxRange);
    }

    /** Whether nothing that blocks light is above the machine within the radius that was set. */
    private boolean isClear() {
        for (int i = 1; i <= setRange; i++) {
            BlockPos at = worldPosition.above(i);
            if (!level.isLoaded(at) || at.getY() >= level.getMaxBuildHeight()) {
                break;
            }
            BlockState state = level.getBlockState(at);
            if (!state.isAir() && state.getLightBlock(level, at) > 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void machineTick(boolean powered) {
        long time = level.getGameTime();
        if (time % 10 == 0) {
            clear = isClear();
        }
        refreshRange();
        ServerLevel server = (ServerLevel) level;
        if (range > 0) {
            if (time % 3 == 0) {
                server.sendParticles(particle(), worldPosition.getX() + server.random.nextDouble(), worldPosition.getY() + server.random.nextDouble() + 0.25,
                        worldPosition.getZ() + server.random.nextDouble(), 1, 0.1, 0.3, 0.1, 0.1);
            }
            protect(server, range);
        }
        boolean flash = lastHit != syncedHit && time - syncedHit >= 4;
        if (range != syncedRange || maxRange != syncedMax || flash || time % 100 == 0) {
            syncDome();
        }
    }

    /** Called when the field stops something: the sphere flashes. */
    protected final void hit() {
        lastHit = level.getGameTime();
    }

    private void syncDome() {
        syncedRange = range;
        syncedMax = maxRange;
        syncedHit = lastHit;
        syncNow();
    }

    /** The box the field reaches into. */
    protected final AABB box(int range) {
        return new AABB(worldPosition).inflate(range);
    }

    /** Where the dome is centred. */
    protected final net.minecraft.world.phys.Vec3 centre() {
        return worldPosition.getCenter();
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RangeMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("setRange", setRange);
        if (owner != null) {
            tag.putUUID("owner", owner.id());
            tag.putString("ownerName", owner.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        setRange = tag.getInt("setRange");
        owner = tag.hasUUID("owner") ? new WorldGuard.Owner(tag.getUUID("owner"), tag.getString("ownerName")) : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("setRange", setRange);
        tag.putInt("range", range);
        tag.putInt("maxRange", maxRange);
        tag.putLong("lastHit", lastHit);
        writePower(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        setRange = tag.getInt("setRange");
        range = tag.getInt("range");
        maxRange = tag.getInt("maxRange");
        lastHit = tag.getLong("lastHit");
        readPower(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** The colour of the sphere, 0xRRGGBB, given how long ago it was last hit. */
    public abstract int domeColor(long gameTime);
}
