package net.scwunge.rotarycraft.transmission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.machine.MachineInteractions;
import net.scwunge.rotarycraft.menu.DistributionClutchMenu;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * The Distribution Clutch, as the original: power comes in at the back and is shared out to the other three sides at the same speed. Each side
 * that is on (set in the screen, or by the redstone strength in Redstone mode: north 1, east 2, south 4, west 8) takes the torque it asks for,
 * in the order north, south, west, east, as long as there is any left; whatever is left over leaves by the front. The front is always on (in
 * Redstone mode it follows its bit too, as the original), the back never.
 * <p>
 * Not here: the original's Bundled Redstone and Computer control modes (ProjectRed and ComputerCraft).
 */
public class DistributionClutchBlockEntity extends PowerBlockEntity implements MenuProvider, MachineInteractions {
    /** The four sides it can send power to, in the original's order. */
    public static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    /** The redstone strength bit that turns each of {@link #SIDES} on in Redstone mode. */
    private static final int[] BITS = {1, 4, 8, 2};

    public enum Control {
        GUI,
        REDSTONE;

        public Control next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public Component label() {
            return Component.translatable("gui.rotarycraft.distribution_clutch." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    private int[] requested = new int[4];
    private int[] output = new int[4];
    private boolean[] enabled = new boolean[4];
    private Control control = Control.GUI;

    public DistributionClutchBlockEntity(BlockPos pos, BlockState state) {
        super(TransmissionRegistry.DISTRIBUTION_CLUTCH_BE.get(), pos, state);
    }

    public static int indexOf(Direction side) {
        for (int i = 0; i < SIDES.length; i++) {
            if (SIDES[i] == side) {
                return i;
            }
        }
        return -1;
    }

    public Control control() {
        return control;
    }

    public void stepControl() {
        control = control.next();
        Arrays.fill(enabled, false);
        setChanged();
        flushPowerSync();
    }

    public boolean isSideEnabled(Direction side) {
        int i = indexOf(side);
        return i >= 0 && enabled[i];
    }

    public void setSideEnabled(Direction side, boolean on) {
        int i = indexOf(side);
        if (i >= 0 && side != inputSide() && control == Control.GUI) {
            enabled[i] = on;
            setChanged();
            flushPowerSync();
        }
    }

    public int torqueRequest(Direction side) {
        int i = indexOf(side);
        return i < 0 ? 0 : requested[i];
    }

    public void setTorqueRequests(int[] values) {
        for (int i = 0; i < 4 && i < values.length; i++) {
            requested[i] = Math.max(0, values[i]);
        }
        setChanged();
    }

    public int outputTorque(Direction side) {
        int i = indexOf(side);
        return i < 0 ? 0 : output[i];
    }

    /** True if power leaves by this side: it is on, is not the input side, and has been asked for something or is getting something. */
    public boolean isOutputtingToSide(Direction side) {
        int i = indexOf(side);
        return i >= 0 && side != inputSide() && enabled[i] && (requested[i] > 0 || output[i] > 0);
    }

    @Override
    public int getTorqueOut(Direction side) {
        int i = indexOf(side);
        return !isShutdown() && i >= 0 && isOutputtingToSide(side) ? output[i] : 0;
    }

    @Override
    public int getOmegaOut(Direction side) {
        return !isShutdown() && isOutputtingToSide(side) ? omega : 0;
    }

    @Override
    public void serverTick() {
        Direction front = facing();
        Direction back = inputSide();
        if (indexOf(front) < 0) {
            // a block set facing up or down has no sides to share to
            setPower(0, 0);
            return;
        }
        enabled[indexOf(front)] = true;
        enabled[indexOf(back)] = false;
        if (control == Control.REDSTONE) {
            int strength = level.getBestNeighborSignal(worldPosition);
            for (int i = 0; i < 4; i++) {
                enabled[i] = (strength & BITS[i]) != 0;
            }
        }
        IShaftPowerOutput.Reading in = readInput();
        int leftover = in.torque();
        for (int i = 0; i < 4; i++) {
            output[i] = 0;
            if (SIDES[i] != front && isOutputtingToSide(SIDES[i])) {
                int amount = Math.min(requested[i], leftover);
                leftover -= amount;
                output[i] = amount;
            }
        }
        output[indexOf(front)] = leftover;
        setPower(in.torque(), in.omega());
    }

    @Override
    public boolean onScrewdriver(UseOnContext context) {
        if (!context.getLevel().isClientSide()) {
            stepControl();
        }
        return true;
    }

    // ---- client ----

    @Override
    protected int statusKey() {
        return Arrays.hashCode(requested) * 31 + Arrays.hashCode(output) * 7 + Arrays.hashCode(enabled) + control.ordinal() * 1000003;
    }

    @Override
    protected void writeStatus(CompoundTag tag) {
        tag.putIntArray("requested", requested);
        tag.putIntArray("output", output);
        tag.putInt("enabled", bits());
        tag.putInt("control", control.ordinal());
    }

    @Override
    protected void readStatus(CompoundTag tag) {
        copy(tag.getIntArray("requested"), requested);
        copy(tag.getIntArray("output"), output);
        setBits(tag.getInt("enabled"));
        control = Control.values()[Math.floorMod(tag.getInt("control"), Control.values().length)];
    }

    private int bits() {
        int bits = 0;
        for (int i = 0; i < 4; i++) {
            if (enabled[i]) {
                bits |= 1 << i;
            }
        }
        return bits;
    }

    private void setBits(int bits) {
        for (int i = 0; i < 4; i++) {
            enabled[i] = (bits & 1 << i) != 0;
        }
    }

    private static void copy(int[] from, int[] to) {
        Arrays.fill(to, 0);
        System.arraycopy(from, 0, to, 0, Math.min(from.length, to.length));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new DistributionClutchMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putIntArray("requested", requested);
        tag.putInt("enabled", bits());
        tag.putInt("control", control.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        copy(tag.getIntArray("requested"), requested);
        setBits(tag.getInt("enabled"));
        control = Control.values()[Math.floorMod(tag.getInt("control"), Control.values().length)];
    }
}
