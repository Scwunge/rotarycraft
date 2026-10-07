package net.scwunge.rotarycraft.transmission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.blockentity.ConsumerBlockEntity;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Bus Controller, as the original: the head of a Power Bus network. Shaft power comes in at its back; Power Bus blocks built out from it
 * (in any direction, in a connected group) share that power among their output sides. It runs on lubricant, which goes in through its top or
 * bottom and is used up (a little for every bus block and output side, every five seconds) while power flows; with no lubricant it passes
 * on nothing. The group is looked for again every half second, so blocks can be added and taken away.
 */
public class BusControllerBlockEntity extends ConsumerBlockEntity {
    public static final int TANK = 8000;
    public static final int LUBE_INTERVAL = 100;
    public static final int REBUILD_INTERVAL = 10;
    private static final int MAX_BLOCKS = 512;

    private final FluidTank tank = new FluidTank(TANK, fluid -> fluid.is(RotaryFluids.LUBRICANT.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final List<PowerBusBlockEntity> buses = new ArrayList<>();
    private int sides = 1;
    private int lubeTimer;
    private boolean dirty = true;

    public BusControllerBlockEntity(BlockPos pos, BlockState state) {
        super(TransmissionRegistry.BUS_CONTROLLER_BE.get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return new PowerRequirement(1, 1, 1);
    }

    public FluidTank tank() {
        return tank;
    }

    /** How many output sides the whole bus has (at least one, so that shares can be worked out). */
    public int sides() {
        return sides;
    }

    public int busSize() {
        return buses.size();
    }

    /** Asks for the group to be looked for again at once. */
    public void markDirty() {
        dirty = true;
    }

    private int lubricantUsed() {
        return Math.max(1, 2 * buses.size() + sides);
    }

    @Override
    protected void machineTick(boolean powered) {
        if (tank.isEmpty()) {
            setPower(0, 0);
        } else if (getPower() > 0 && ++lubeTimer >= LUBE_INTERVAL) {
            lubeTimer = 0;
            tank.drain(lubricantUsed(), IFluidHandler.FluidAction.EXECUTE);
        }
        if (dirty || level.getGameTime() % REBUILD_INTERVAL == 0) {
            dirty = false;
            rebuild();
        }
    }

    /** Looks outward from the controller for connected Power Bus blocks, hands each the side it is fed from, and counts the output sides. */
    private void rebuild() {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<PowerBusBlockEntity> found = new ArrayList<>();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos next = worldPosition.relative(side);
            if (side != inputSide() && level.getBlockEntity(next) instanceof PowerBusBlockEntity bus && seen.add(next)) {
                bus.attach(this, side.getOpposite());
                found.add(bus);
                queue.add(next);
            }
        }
        while (!queue.isEmpty() && found.size() < MAX_BLOCKS) {
            BlockPos at = queue.poll();
            for (Direction side : Direction.values()) {
                BlockPos next = at.relative(side);
                if (level.getBlockEntity(next) instanceof PowerBusBlockEntity bus && seen.add(next)) {
                    bus.attach(this, side.getOpposite());
                    found.add(bus);
                    queue.add(next);
                }
            }
        }
        for (PowerBusBlockEntity old : buses) {
            if (!seen.contains(old.getBlockPos()) && !old.isRemoved() && old.controller() == this) {
                old.detach();
            }
        }
        buses.clear();
        buses.addAll(found);
        int count = 0;
        for (PowerBusBlockEntity bus : buses) {
            count += bus.outputSideCount();
        }
        sides = Math.max(count, 1);
    }

    /** The block was broken: let go of every bus block. */
    public void release() {
        for (PowerBusBlockEntity bus : buses) {
            if (!bus.isRemoved()) {
                bus.detach();
            }
        }
        buses.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("tank"));
        dirty = true;
    }
}
