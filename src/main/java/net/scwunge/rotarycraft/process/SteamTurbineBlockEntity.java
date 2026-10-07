package net.scwunge.rotarycraft.process;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scwunge.rotarycraft.registry.ProcessRegistry;

/**
 * The Steam Turbine, as the original's: steam in (up to 300000 mB held, from any side but the front), shaft power out of its front, at half
 * the efficiency of the other converters. It uses ceil(sqrt(power)) mB of steam a tick at perfect efficiency. Any fluid in the {@code c:steam}
 * tag is steam, and this mod's own steam is in it.
 */
public class SteamTurbineBlockEntity extends EnergyConverterBlockEntity {
    public static final int CAPACITY = 300_000;

    private final IFluidHandler steam = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return stored > 0 ? new FluidStack(ProcessRegistry.steam(), stored) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return isSteam(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return isSteam(resource) ? add(resource.getAmount(), action.execute()) : 0;
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

    public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ProcessRegistry.STEAM_TURBINE_BE.get(), pos, state);
    }

    public static boolean isSteam(FluidStack stack) {
        return !stack.isEmpty() && (stack.is(ProcessRegistry.STEAM_TAG) || stack.getFluid() == ProcessRegistry.steam());
    }

    @Override
    protected String switchName() {
        return "steamTurbine";
    }

    public IFluidHandler steam() {
        return steam;
    }

    @Override
    public int maxStorage() {
        return CAPACITY;
    }

    @Override
    protected double idealUnitsPerTick(long power) {
        return Math.ceil(Math.sqrt(power));
    }

    @Override
    protected double relativeEfficiency() {
        return 0.5;
    }
}
