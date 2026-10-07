package net.scwunge.rotarycraft.process;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scwunge.rotarycraft.config.FarmConfig;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.registry.ProcessRegistry;

/**
 * The Pneumatic Engine, as the original's: compressed air in (30000 mB held), shaft power out of its front, by the rules of
 * {@link EnergyConverterBlockEntity}, using as much air as the power it makes is worth (a watt-count given in the farm config).
 * The original needed another mod's air; this one takes this mod's compressed air, from the Air Compressor.
 */
public class PneumaticEngineBlockEntity extends EnergyConverterBlockEntity {
    public static final int CAPACITY = 30_000;

    private final IFluidHandler air = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return stored > 0 ? new FluidStack(ProcessRegistry.compressedAir(), stored) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return stack.getFluid() == ProcessRegistry.compressedAir();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return resource.getFluid() == ProcessRegistry.compressedAir() ? add(resource.getAmount(), action.execute()) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    public PneumaticEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ProcessRegistry.PNEUMATIC_ENGINE_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "pneumaticEngine";
    }

    public IFluidHandler air() {
        return air;
    }

    @Override
    public int maxStorage() {
        return CAPACITY;
    }

    @Override
    protected double idealUnitsPerTick(long power) {
        return (double) power / RotaryConfig.get(FarmConfig.WATTS_PER_AIR);
    }
}
