package net.scwunge.rotarycraft.process;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Views of machine tanks for pipes: fluid goes into one tank and comes out of another, or only goes in, or only comes out. */
public final class Tanks {
    private Tanks() {}

    /** Joins pipes to a machine without giving them any of its fluid, or taking any of theirs (the Pipe Pump, which moves it itself). */
    public static final IFluidHandler NONE = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 0;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
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

    /** Fluid goes into {@code in} and comes out of {@code out}. */
    public static IFluidHandler split(IFluidHandler in, IFluidHandler out) {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 2;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return tank == 0 ? in.getFluidInTank(0) : out.getFluidInTank(0);
            }

            @Override
            public int getTankCapacity(int tank) {
                return tank == 0 ? in.getTankCapacity(0) : out.getTankCapacity(0);
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return tank == 0 && in.isFluidValid(0, stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return in.fill(resource, action);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return out.drain(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return out.drain(maxDrain, action);
            }
        };
    }
}
