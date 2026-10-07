package net.scwunge.rotarycraft.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.blockentity.ConsumerBlockEntity;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Aggregator (TileEntityAggregator): condenses water out of the air, into a tank of 128 buckets, which it gives to anything that takes fluid beside it. It
 * makes the biome's humidity times torque squared (at least two) divided by (80 less 5 per doubling of the speed) mB a tick, but only while it is colder than the
 * air round it, which needs cooling, and not above 100 C. Power comes from below: 1 N*m at 4096 rad/s and 8 kW.
 */
public class AggregatorBlockEntity extends ConsumerBlockEntity implements Heatable {
    public static final int CAPACITY = 128_000;
    public static final int MAX_TEMPERATURE = 100;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 4096, 8192);

    private final FluidTank tank = new FluidTank(CAPACITY, s -> s.getFluid() == Fluids.WATER) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IFluidHandler out = FluidAccess.drainOnly(tank);
    private int temperature;
    private int tempTicks;

    public AggregatorBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsRegistry.AGGREGATOR.type().get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    public Direction inputSide() {
        return Direction.DOWN;
    }

    public FluidTank tank() {
        return tank;
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return side == null || side.getAxis().isHorizontal() ? out : null;
    }

    /** How much humidity the air here has, 0 to 1 (the biome's downfall). */
    public static float humidity(Level level, BlockPos pos) {
        return level.getBiome(pos).value().getModifiedClimateSettings().downfall();
    }

    /** Millibuckets of water a tick, for this humidity. */
    public int perTick(float humidity) {
        if (omega < REQUIREMENT.minOmega() || getPower() < REQUIREMENT.minPower()) {
            return 0;
        }
        int ticks = Math.max(1, (int) (80 - 5 * (Math.log(omega + 1D) / Math.log(2))));
        return Math.max(2, (int) ((long) torque * torque * humidity)) / ticks;
    }

    // ---- Heatable ----

    @Override
    public int getTemperature() {
        return temperature;
    }

    @Override
    public int getMaxTemperature() {
        return MAX_TEMPERATURE;
    }

    @Override
    public void addTemperature(int amount) {
        temperature += amount;
    }

    @Override
    public boolean canBeCooledWithFins() {
        return true;
    }

    public void setCurrentTemperature(int t) {
        temperature = Math.max(1, t);
        setChanged();
    }

    private void giveToNeighbours() {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (tank.isEmpty()) {
                return;
            }
            IFluidHandler there = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(dir), dir.getOpposite());
            if (there != null) {
                int taken = there.fill(tank.getFluid(), IFluidHandler.FluidAction.EXECUTE);
                if (taken > 0) {
                    tank.drain(taken, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    @Override
    protected void machineTick(boolean powered) {
        if (level == null || level.isClientSide()) {
            return;
        }
        int ambient = Ambient.temperature(level, worldPosition);
        if (++tempTicks >= 20) {
            tempTicks = 0;
            temperature += (ambient - temperature) / 4;
        }
        if (!tank.isEmpty()) {
            giveToNeighbours();
        }
        if (!powered || !MachineConfig.enabled("aggregator") || tank.getFluidAmount() >= CAPACITY || temperature >= MAX_TEMPERATURE) {
            return;
        }
        if (temperature < ambient) {
            tank.fill(new FluidStack(Fluids.WATER, perTick(humidity(level, worldPosition))), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("temperature", temperature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("tank"));
        temperature = tag.getInt("temperature");
    }
}
