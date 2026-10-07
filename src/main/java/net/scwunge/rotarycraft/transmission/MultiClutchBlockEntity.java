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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.menu.MultiClutchMenu;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * The Multi-Clutch, as the original: power comes in at the back and goes out of whichever side its redstone signal picks. Each of the sixteen
 * signal strengths has a side set for it in the screen (cycling down, up, north, south, west, east); the output is that side at the strength
 * now at the block, and nothing leaves while the chosen side is the input side.
 */
public class MultiClutchBlockEntity extends PowerBlockEntity implements MenuProvider {
    public static final int STATES = 16;

    private final int[] sides = new int[STATES];
    private int redstone;

    public MultiClutchBlockEntity(BlockPos pos, BlockState state) {
        super(TransmissionRegistry.MULTI_CLUTCH_BE.get(), pos, state);
    }

    /** The side set for a redstone strength, as an index into {@link Direction#values()}. */
    public int sideOfState(int state) {
        return sides[Math.floorMod(state, STATES)];
    }

    public void setSideOfState(int state, int side) {
        sides[Math.floorMod(state, STATES)] = Math.floorMod(side, Direction.values().length);
        setChanged();
    }

    public void cycleState(int state) {
        setSideOfState(state, sideOfState(state) + 1);
    }

    public int redstoneLevel() {
        return redstone;
    }

    /** The side power leaves by now, or null when it would leave by the input side. */
    @Nullable
    public Direction outputSide() {
        Direction out = Direction.values()[sideOfState(redstone)];
        return out == inputSide() ? null : out;
    }

    @Override
    public int getTorqueOut(Direction side) {
        return !isShutdown() && side == outputSide() ? torque : 0;
    }

    @Override
    public int getOmegaOut(Direction side) {
        return !isShutdown() && side == outputSide() ? omega : 0;
    }

    @Override
    public void serverTick() {
        redstone = level.getBestNeighborSignal(worldPosition);
        IShaftPowerOutput.Reading in = readInput();
        setPower(in.torque(), in.omega());
    }

    // ---- screen ----

    public static final int DATA_COUNT = STATES + 1;

    public ContainerData data() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return index < STATES ? sides[index] : redstone;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MultiClutchMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putIntArray("sides", sides);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] saved = tag.getIntArray("sides");
        Arrays.fill(sides, 0);
        for (int i = 0; i < STATES && i < saved.length; i++) {
            sides[i] = Math.floorMod(saved[i], Direction.values().length);
        }
    }
}
