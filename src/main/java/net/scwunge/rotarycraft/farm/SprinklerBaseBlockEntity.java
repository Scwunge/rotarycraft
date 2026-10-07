package net.scwunge.rotarycraft.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.blockentity.PipeBlockEntity;
import net.scwunge.rotarycraft.pipe.PipeType;
import net.scwunge.rotarycraft.power.PowerRequirement;

/**
 * What the Sprinkler and the Lawn Sprinkler have in common, as the original's sprinkler base: no shaft, but water. It holds a little water
 * (given by pipes on any side), spends some each tick it works, and learns the pressure of the pipe on its pipe side (above for the Sprinkler,
 * below for the Lawn Sprinkler): the range is the pressure over 80 up to 8 blocks, and no pressure, no range. The pressure is kept until
 * the pipe next gives a new reading, as the original's.
 */
public abstract class SprinklerBaseBlockEntity extends FarmBlockEntity {
    public static final int MAX_RANGE = 8;

    private final FluidTank tank;
    private int pressure;

    protected SprinklerBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state);
        tank = new FluidTank(capacity, fluid -> fluid.getFluid() == Fluids.WATER) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    public abstract int waterConsumption();

    /** The side its pipe is on. */
    public abstract Direction pipeSide();

    /** What one working tick does. */
    protected abstract void performEffects();

    @Override
    public PowerRequirement requirement() {
        return new PowerRequirement(0, 0, 0);
    }

    @Override
    protected boolean usesShaftPower() {
        return false;
    }

    public FluidTank tank() {
        return tank;
    }

    public int water() {
        return tank.getFluidAmount();
    }

    public int pressure() {
        return pressure;
    }

    /** How far the spray reaches. */
    public int range() {
        if (pressure <= 0) {
            return 0;
        }
        return Math.min(pressure / 80, MAX_RANGE);
    }

    public boolean canPerformEffects() {
        return range() > 0 && water() >= waterConsumption();
    }

    @Override
    protected void machineTick(boolean powered) {
        BlockPos pipePos = worldPosition.relative(pipeSide());
        if (level.getBlockEntity(pipePos) instanceof PipeBlockEntity pipe && pipe.contents().getFluid() == Fluids.WATER && pipe.amount() > 0) {
            int reading = (int) Math.min(Integer.MAX_VALUE, PipeType.pressure(pipe.amount()));
            if (reading != pressure) {
                pressure = reading;
                setChanged();
            }
        }
        boolean was = working;
        working = canPerformEffects();
        if (working) {
            performEffects();
            tank.drain(waterConsumption(), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        }
        if (was != working || level.getGameTime() % 40 == 0) {
            syncNow();
        }
    }

    private boolean working;

    public boolean isWorking() {
        return working;
    }

    @Override
    protected void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("pressure", pressure);
    }

    @Override
    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {
        tank.readFromNBT(registries, tag.getCompound("tank"));
        pressure = tag.getInt("pressure");
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("water", tank.getFluidAmount());
        tag.putInt("pressure", pressure);
        tag.putBoolean("working", working);
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        tank.setFluid(tag.getInt("water") > 0 ? new FluidStack(Fluids.WATER, tag.getInt("water")) : FluidStack.EMPTY);
        pressure = tag.getInt("pressure");
        working = tag.getBoolean("working");
    }
}
