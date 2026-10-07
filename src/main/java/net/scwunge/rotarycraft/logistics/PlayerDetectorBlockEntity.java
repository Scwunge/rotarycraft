package net.scwunge.rotarycraft.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.scwunge.rotarycraft.blockentity.ConsumerBlockEntity;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.MachineInteractions;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;
import net.scwunge.rotarycraft.weapon.RangeHost;
import net.scwunge.rotarycraft.weapon.RangeMenu;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Player Detector (TileEntityPlayerDetector): gives a redstone signal while a player is within its range. The range is what the power allows (one block for
 * every 128 W) up to the detector range setting, and no more than the range set on its screen. Its reaction time is 100 ticks less one tick for every 32 rad/s
 * (so instant at 3200). A screwdriver switches it to analog, where the signal is the number of players in range, to fifteen. Power comes in from below.
 */
public class PlayerDetectorBlockEntity extends ConsumerBlockEntity implements RangeHost, MenuProvider, MachineInteractions {
    public static final int FALLOFF = 128;
    public static final int SPEED_FACTOR = 32;
    public static final int BASE_SPEED = 100;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 1);

    private boolean analog;
    private boolean active;
    private int signal;
    private int selectedRange;
    private int ticksDetected;
    private int lastOutput;

    public PlayerDetectorBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsRegistry.PLAYER_DETECTOR.type().get(), pos, state);
    }

    @Override
    public Direction inputSide() {
        return Direction.DOWN;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    public boolean analog() {
        return analog;
    }

    public boolean isActive() {
        return active;
    }

    /** The ticks a player has to be there before it reacts. */
    public int reactionTime() {
        return Math.max(1, BASE_SPEED - omega / SPEED_FACTOR);
    }

    // ---- range ----

    @Override
    public int setRange() {
        return selectedRange;
    }

    @Override
    public void setSetRange(int range) {
        selectedRange = Math.max(0, Math.min(range, 1024));
        setChanged();
    }

    @Override
    public int maxRange() {
        int range = (int) (getPower() / FALLOFF);
        return Math.min(range, Math.max(64, MachineConfig.get(MachineConfig.DETECTOR_RANGE)));
    }

    @Override
    public int range() {
        return Math.min(maxRange(), selectedRange);
    }

    private int countPlayers(ServerLevel server) {
        int range = range();
        AABB box = new AABB(worldPosition).inflate(range);
        return Math.min(15, (int) server.getEntitiesOfClass(Player.class, box, p -> !p.isSpectator()).size());
    }

    // ---- the signal ----

    @Override
    public int redstoneOutput() {
        return analog ? signal : active ? 15 : 0;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (!powered || !MachineConfig.enabled("playerDetector")) {
            active = false;
            ticksDetected = 0;
            signal = 0;
        } else if (!analog) {
            signal = 0;
            if (countPlayers(server) > 0) {
                if (++ticksDetected >= reactionTime()) {
                    active = true;
                }
            } else {
                active = false;
                ticksDetected = 0;
            }
        } else {
            active = false;
            int count = countPlayers(server);
            if (count > 0) {
                if (++ticksDetected >= reactionTime()) {
                    signal = count;
                }
            } else {
                signal = 0;
                ticksDetected = 0;
            }
        }
        int output = redstoneOutput();
        if (output != lastOutput) {
            lastOutput = output;
            server.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            for (Direction dir : Direction.values()) {
                server.updateNeighborsAt(worldPosition.relative(dir), getBlockState().getBlock());
            }
            setChanged();
        }
    }

    @Override
    public boolean onScrewdriver(UseOnContext context) {
        analog = !analog;
        ticksDetected = 0;
        setChanged();
        return true;
    }

    // ---- screen ----

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
        tag.putInt("range", selectedRange);
        tag.putBoolean("analog", analog);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        selectedRange = tag.getInt("range");
        analog = tag.getBoolean("analog");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("range", selectedRange);
        tag.putInt("torque", torque);
        tag.putInt("omega", omega);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        selectedRange = tag.getInt("range");
        torque = tag.getInt("torque");
        omega = tag.getInt("omega");
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
