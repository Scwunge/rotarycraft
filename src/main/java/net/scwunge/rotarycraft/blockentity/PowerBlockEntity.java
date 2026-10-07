package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;

/**
 * Base for every shaft-power block. Power enters through the back (opposite the block's facing) and leaves through the
 * front (the facing). {@link #torque} and {@link #omega} are what this block currently delivers out of its front.
 */
public abstract class PowerBlockEntity extends BlockEntity implements IShaftPowerOutput {
    protected int torque;
    /** The game time of the next time the machine's sound is played (see MachineSounds). */
    public long nextSoundTick;
    /** Whether the machine's start-up sound has played for this run (see MachineSounds). */
    public boolean soundStarted;
    private boolean powerDirty;
    private int lastStatus;

    /** A number that changes when something the client draws (and the server alone knows) changes: see {@link #writeStatus}. */
    protected int statusKey() {
        return 0;
    }

    /** What the machine tells clients besides its speed, for its model's looks. */
    protected void writeStatus(CompoundTag tag) {
    }

    protected void readStatus(CompoundTag tag) {
    }
    private long lastPowerSync;

    /** Tells the clients the machine's speed now and then while it changes, for the animations and sounds (at most every half second). */
    public final void flushPowerSync() {
        int status = statusKey();
        if (status != lastStatus) {
            lastStatus = status;
            powerDirty = true;
        }
        if (powerDirty && level != null && !level.isClientSide && level.getGameTime() - lastPowerSync >= 10) {
            powerDirty = false;
            lastPowerSync = level.getGameTime();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("torque", torque);
        tag.putInt("omega", omega);
        writeStatus(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        torque = tag.getInt("torque");
        omega = tag.getInt("omega");
        readStatus(tag);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    /** Client side: the angle of the machine's turning parts, and the game time it was last moved on (see MachineRenderer). */
    public float phi;
    public long phiTime;
    protected int omega;

    protected PowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(MachineBlock.FACING);
    }

    /** The direction from this block towards the block that feeds it. */
    public Direction inputSide() {
        return facing().getOpposite();
    }

    public int getTorque() {
        return torque;
    }

    public int getOmega() {
        return omega;
    }

    /** Burnt out by an EMP: it does nothing and passes nothing on, until it is broken and placed again (as the original). */
    private boolean shutdown;

    public boolean isShutdown() {
        return shutdown;
    }

    public void onEmp() {
        shutdown = true;
        torque = 0;
        omega = 0;
        setChanged();
    }

    public long getPower() {
        return (long) torque * (long) omega;
    }

    /** True for blocks that pass power on out of their front. Consumers return false. */
    protected boolean outputsPower() {
        return true;
    }

    @Override
    public int getTorqueOut(Direction side) {
        return !shutdown && outputsPower() && side == facing() ? torque : 0;
    }

    @Override
    public int getOmegaOut(Direction side) {
        return !shutdown && outputsPower() && side == facing() ? omega : 0;
    }

    protected IShaftPowerOutput.Reading readInput() {
        return level == null ? IShaftPowerOutput.Reading.NONE : IShaftPowerOutput.readInput(level, worldPosition, inputSide());
    }

    protected void setPower(int torque, int omega) {
        if (torque <= 0 || omega <= 0) {
            torque = 0;
            omega = 0;
        }
        if (torque != this.torque || omega != this.omega) {
            this.torque = torque;
            this.omega = omega;
            powerDirty = true;
            setChanged();
        }
    }

    /** Runs once per server tick. */
    public abstract void serverTick();

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("torque", torque);
        tag.putInt("omega", omega);
        if (shutdown) {
            tag.putBoolean("emp", true);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        torque = tag.getInt("torque");
        omega = tag.getInt("omega");
        shutdown = tag.getBoolean("emp");
    }
}
