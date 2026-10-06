package net.scwunge.rotarycraft.pipe;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * One-way views of a machine's tank, standing in for the original's per-side pipe "flow": engines and other consumers
 * only take fluid in, producers only give it out, so a pipe next to them moves fluid the right way.
 */
public final class FluidAccess {
    private FluidAccess() {
    }

    /** Fluid can go in but not come out (fuel tanks, water tanks, lubricant). */
    public static IFluidHandler fillOnly(IFluidHandler h) {
        return h == null ? null : new IFluidHandler() {
            @Override
            public int getTanks() {
                return h.getTanks();
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return h.getFluidInTank(tank);
            }

            @Override
            public int getTankCapacity(int tank) {
                return h.getTankCapacity(tank);
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return h.isFluidValid(tank, stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return h.fill(resource, action);
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
    }

    /** Fluid can be taken out but not put in (machine outputs). */
    public static IFluidHandler drainOnly(IFluidHandler h) {
        return h == null ? null : new IFluidHandler() {
            @Override
            public int getTanks() {
                return h.getTanks();
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return h.getFluidInTank(tank);
            }

            @Override
            public int getTankCapacity(int tank) {
                return h.getTankCapacity(tank);
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
                return h.drain(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return h.drain(maxDrain, action);
            }
        };
    }
}
