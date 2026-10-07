package net.scwunge.rotarycraft.transmission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.machine.MachineInteractions;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * A belt, chain or split belt pulley, as the original: two of them on parallel shafts, in a straight line with nothing solid between, are joined
 * by a belt (the belt item, used on one and then the other). One is the driver and takes power from its shaft; the other, the receiving end,
 * gives it out of its shaft side. Sneak with the screwdriver to swap which is which. A belt slips above its limit (the torque is capped, the
 * speed passes on only partly), and a wet one (rain) can take only a quarter. A chain takes twice the torque and eight times the speed, but
 * explodes above it. A split belt takes only {@code TAKEOFF_TORQUE} off its driver's shaft, which carries on through it; its receiving end
 * adds the belt's torque into the shaft line it sits in.
 */
public class BeltHubBlockEntity extends PowerBlockEntity implements MachineInteractions {
    public static final int TAKEOFF_TORQUE = 64;
    public static final int TAKEOFF_TORQUE_WET = 16;
    public static final int MAX_WET_TICKS = 18000;

    private boolean receiving;
    @Nullable
    private BlockPos otherEnd;
    private int wetTimer;
    private boolean slippingTorque;
    private boolean slippingOmega;
    /** What the driver takes in from its shaft, which the receiving end copies. */
    private int torqueIn;
    private int omegaIn;

    public BeltHubBlockEntity(BlockPos pos, BlockState state) {
        super(TransmissionRegistry.BELT_HUB_BE.get(), pos, state);
    }

    public BeltHubBlock.Kind kind() {
        return getBlockState().getBlock() instanceof BeltHubBlock hub ? hub.kind() : BeltHubBlock.Kind.BELT;
    }

    public boolean isReceivingEnd() {
        return receiving;
    }

    public void setReceivingEnd(boolean receiving) {
        this.receiving = receiving;
        setChanged();
    }

    @Nullable
    public BlockPos otherEnd() {
        return otherEnd;
    }

    public boolean isWet() {
        return wetTimer > 0;
    }

    public void makeWet(float factor) {
        wetTimer = Math.min(wetTimer + (int) (3600 * factor), MAX_WET_TICKS);
        setChanged();
    }

    public boolean isSlipping() {
        return slippingTorque || slippingOmega;
    }

    public int maxTorque() {
        return kind().maxTorque;
    }

    public int maxSmoothSpeed() {
        return kind().maxSpeed;
    }

    /** The shaft side: where a driver takes its power from, and a receiving end gives it (a split belt's receiving end gives it out of the other side). */
    @Override
    public Direction inputSide() {
        return facing();
    }

    // ---- the belt ----

    /** The side the shaft power goes out of, or null for a driver that passes none on. */
    @Nullable
    private Direction outputSide() {
        if (kind() == BeltHubBlock.Kind.SPLIT) {
            return facing().getOpposite();
        }
        return receiving ? facing() : null;
    }

    @Override
    public int getTorqueOut(Direction side) {
        return !isShutdown() && side == outputSide() ? torque : 0;
    }

    @Override
    public int getOmegaOut(Direction side) {
        return !isShutdown() && side == outputSide() ? omega : 0;
    }

    /** True if a belt may join this pulley to the one at {@code other}: in a straight line, across the shaft, with a pulley of the same kind and the other role on a parallel shaft. */
    public boolean canConnect(BlockPos other) {
        if (level == null || other.equals(worldPosition)) {
            return false;
        }
        int dx = other.getX() - worldPosition.getX();
        int dy = other.getY() - worldPosition.getY();
        int dz = other.getZ() - worldPosition.getZ();
        int nonZero = (dx != 0 ? 1 : 0) + (dy != 0 ? 1 : 0) + (dz != 0 ? 1 : 0);
        if (nonZero != 1) {
            return false;
        }
        Direction dir = dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST : dy > 0 ? Direction.UP : dy < 0 ? Direction.DOWN : dz > 0 ? Direction.SOUTH : Direction.NORTH;
        if (dir.getAxis() == facing().getAxis()) {
            return false;
        }
        if (!(level.getBlockEntity(other) instanceof BeltHubBlockEntity hub) || hub.getBlockState().getBlock() != getBlockState().getBlock()
                || hub.receiving == receiving || hub.facing().getAxis() != facing().getAxis()) {
            return false;
        }
        int distance = Math.abs(dx + dy + dz);
        for (int i = 1; i < distance; i++) {
            BlockPos between = worldPosition.relative(dir, i);
            BlockState state = level.getBlockState(between);
            if (!state.getCollisionShape(level, between, CollisionContext.empty()).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean hasValidConnection() {
        return otherEnd != null && level != null && level.isLoaded(otherEnd) && canConnect(otherEnd);
    }

    @Nullable
    public BeltHubBlockEntity partner() {
        return hasValidConnection() && level.getBlockEntity(otherEnd) instanceof BeltHubBlockEntity hub ? hub : null;
    }

    public boolean tryConnect(BlockPos other) {
        if (otherEnd != null || !canConnect(other)) {
            return false;
        }
        otherEnd = other;
        setChanged();
        flushNow();
        return true;
    }

    public void reset() {
        otherEnd = null;
        setChanged();
        flushNow();
    }

    /** Lets go of the pulley at the other end of the belt too. */
    public void resetOther() {
        if (otherEnd != null && level != null && level.isLoaded(otherEnd) && level.getBlockEntity(otherEnd) instanceof BeltHubBlockEntity hub) {
            hub.reset();
        }
    }

    private void flushNow() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    /** The number of blocks from this pulley to the other one, or -1. */
    public int distanceToTarget() {
        return otherEnd == null ? -1 : otherEnd.distManhattan(worldPosition);
    }

    // ---- power ----

    private int takeoff() {
        return isWet() ? TAKEOFF_TORQUE_WET : TAKEOFF_TORQUE;
    }

    private int copyTorque(int input) {
        int max = isWet() ? maxTorque() / 4 : maxTorque();
        slippingTorque = input > max;
        if (kind() == BeltHubBlock.Kind.SPLIT) {
            return Math.min(input, takeoff());
        }
        return Math.min(input, max);
    }

    private int copyOmega(int input) {
        if (kind() == BeltHubBlock.Kind.SPLIT && isWet()) {
            input = (int) (input * (0.75 + 0.25 * level.random.nextDouble()));
        }
        int smooth = isWet() ? maxSmoothSpeed() / 4 : maxSmoothSpeed();
        slippingOmega = input > smooth;
        int speed = input <= smooth ? input : (int) (smooth + Math.sqrt(input - smooth));
        if (kind() == BeltHubBlock.Kind.CHAIN && speed > maxSmoothSpeed()) {
            // a chain does not slip, it tears itself apart
            BlockPos at = worldPosition;
            resetOther();
            level.removeBlock(at, false);
            level.explode(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 2, Level.ExplosionInteraction.BLOCK);
        }
        return speed;
    }

    @Override
    public void serverTick() {
        if (level.isRainingAt(worldPosition.above()) && level.canSeeSky(worldPosition.above()) && level.random.nextInt(900) == 0) {
            makeWet(1);
        }
        if (wetTimer > 0 && getPower() > 0) {
            wetTimer--;
        }
        if (receiving) {
            copyFromDriver();
            return;
        }
        IShaftPowerOutput.Reading in = readInput();
        torqueIn = in.torque();
        omegaIn = in.omega();
        if (kind() == BeltHubBlock.Kind.SPLIT) {
            int through = hasValidConnection() ? Math.max(0, in.torque() - takeoff()) : in.torque();
            setPower(through, in.omega());
        } else {
            setPower(in.torque(), in.omega());
        }
    }

    private void copyFromDriver() {
        int t = 0;
        int w = 0;
        boolean noInput = true;
        BeltHubBlockEntity driver = partner();
        if (driver != null) {
            w = copyOmega(driver.omegaIn);
            t = copyTorque(driver.torqueIn);
            noInput = false;
        }
        if (kind() == BeltHubBlock.Kind.SPLIT) {
            IShaftPowerOutput.Reading shaft = readInput();
            if (shaft.power() > 0) {
                if (t > 0 && w > 0) {
                    t += shaft.torque();
                    w = (w + shaft.omega()) / 2;
                } else {
                    t = shaft.torque();
                    w = shaft.omega();
                }
                noInput = false;
            }
        }
        if (noInput) {
            // with nothing driving it, it coasts down
            if (omega > 0) {
                setPower(torque, (int) (omega * 0.98));
            } else {
                setPower(0, 0);
            }
            return;
        }
        setPower(t, w);
    }

    // ---- breaking ----

    /** The block was broken: the belt comes off, and its items drop. */
    public void onBroken() {
        if (level == null || level.isClientSide()) {
            return;
        }
        int count = Math.min(Math.max(distanceToTarget() - 1, 0), 64);
        if (hasValidConnection()) {
            ItemStack belt = new ItemStack(kind().beltItem());
            for (int i = 0; i < count; i++) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, belt.copy());
            }
        }
        resetOther();
    }

    @Override
    public boolean onScrewdriver(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            receiving = !receiving;
            resetOther();
            reset();
            setChanged();
            player.displayClientMessage(Component.translatable(receiving ? "message.rotarycraft.belt.receiving" : "message.rotarycraft.belt.driving"), true);
            return true;
        }
        // turning it takes the belt off, and the usual turning follows
        resetOther();
        reset();
        return false;
    }

    // ---- client ----

    @Override
    protected int statusKey() {
        return (receiving ? 1 : 0) + (isWet() ? 2 : 0) + (isSlipping() ? 4 : 0) + (hasValidConnection() ? 8 : 0) + (otherEnd == null ? 0 : otherEnd.hashCode() * 16);
    }

    @Override
    protected void writeStatus(CompoundTag tag) {
        tag.putBoolean("receiving", receiving);
        tag.putBoolean("wet", isWet());
        tag.putBoolean("slipping", isSlipping());
        if (hasValidConnection()) {
            tag.putLong("end", otherEnd.asLong());
        }
    }

    @Override
    protected void readStatus(CompoundTag tag) {
        receiving = tag.getBoolean("receiving");
        wetTimer = tag.getBoolean("wet") ? 1 : 0;
        slippingOmega = tag.getBoolean("slipping");
        otherEnd = tag.contains("end") ? BlockPos.of(tag.getLong("end")) : null;
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("receiving", receiving);
        tag.putInt("wet", wetTimer);
        if (otherEnd != null) {
            tag.putLong("end", otherEnd.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        receiving = tag.getBoolean("receiving");
        wetTimer = tag.getInt("wet");
        otherEnd = tag.contains("end") ? BlockPos.of(tag.getLong("end")) : null;
    }
}
