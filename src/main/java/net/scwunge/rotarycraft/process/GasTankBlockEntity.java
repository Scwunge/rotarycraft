package net.scwunge.rotarycraft.process;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scwunge.rotarycraft.farm.FarmBlockEntity;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.ProcessRegistry;
import net.scwunge.rotarycraft.registry.RotaryComponents;

/**
 * The Gas Tank (the original's Fluid Compressor): it holds any one fluid, and how much follows the torque it is turned with, as the original's:
 * 10^(log2(torque) / 2) / 40 mB, eight times that for a gas, up to a billion. Pipes fill it from the sides and take from its top. What it holds
 * stays in the item when it is broken.
 */
public class GasTankBlockEntity extends FarmBlockEntity {
    public static final int LIMIT = 1_000_000_000;

    private FluidStack contents = FluidStack.EMPTY;

    private final IFluidHandler handler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return contents;
        }

        @Override
        public int getTankCapacity(int tank) {
            return capacity(contents);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return contents.isEmpty() || FluidStack.isSameFluidSameComponents(contents, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isSwitchedOn() || !isFluidValid(0, resource)) {
                return 0;
            }
            int add = Math.min(resource.getAmount(), capacity(resource) - contents.getAmount());
            if (add <= 0) {
                return 0;
            }
            if (action.execute()) {
                contents = contents.isEmpty() ? resource.copyWithAmount(add) : contents.copyWithAmount(contents.getAmount() + add);
                setChanged();
            }
            return add;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return !contents.isEmpty() && FluidStack.isSameFluidSameComponents(contents, resource) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (contents.isEmpty() || maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            int take = Math.min(maxDrain, contents.getAmount());
            FluidStack out = contents.copyWithAmount(take);
            if (action.execute()) {
                contents = take >= contents.getAmount() ? FluidStack.EMPTY : contents.copyWithAmount(contents.getAmount() - take);
                setChanged();
            }
            return out;
        }
    };

    public GasTankBlockEntity(BlockPos pos, BlockState state) {
        super(ProcessRegistry.GAS_TANK_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "gasTank";
    }

    @Override
    protected boolean anySide() {
        return true;
    }

    @Override
    public PowerRequirement requirement() {
        return new PowerRequirement(1, 0, 1);
    }

    public IFluidHandler handler() {
        return handler;
    }

    public FluidStack contents() {
        return contents;
    }

    public void setContents(FluidStack stack) {
        contents = stack;
        setChanged();
    }

    /** How many mB of this fluid it can hold at the torque it is turned with now. */
    public int capacity(FluidStack fluid) {
        if (getPower() < 1 || getTorque() < 1) {
            return 0;
        }
        return capacityAt(getTorque(), !fluid.isEmpty() && fluid.getFluidType().getDensity(fluid) < 0);
    }

    public static int capacityAt(int torque, boolean gas) {
        if (torque < 1) {
            return 0;
        }
        int log2 = (31 - Integer.numberOfLeadingZeros(torque)) / 2;
        long power = 1;
        for (int i = 0; i < log2; i++) {
            power *= 10;
        }
        return (int) Math.min(LIMIT, (gas ? 8 : 1) * (power / 40));
    }

    @Override
    protected void machineTick(boolean powered) {
        if (level.getGameTime() % 20 == 0) {
            syncNow();
        }
    }

    @Override
    protected int[] status() {
        return new int[] {contents.getAmount(), capacity(contents)};
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("gas", contents.saveOptional(registries));
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        contents = FluidStack.parseOptional(registries, tag.getCompound("gas"));
    }

    @Override
    protected void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("gas", contents.saveOptional(registries));
    }

    @Override
    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {
        contents = FluidStack.parseOptional(registries, tag.getCompound("gas"));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!contents.isEmpty()) {
            components.set(RotaryComponents.GAS_CONTENTS.get(), SimpleFluidContent.copyOf(contents));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        SimpleFluidContent held = input.get(RotaryComponents.GAS_CONTENTS.get());
        if (held != null) {
            contents = held.copy();
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("gas");
    }
}
