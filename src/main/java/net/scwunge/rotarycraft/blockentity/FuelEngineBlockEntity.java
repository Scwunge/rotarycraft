package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * An engine that burns a liquid fuel, as in the original: a 240-bucket tank, {@link #FUEL_PER_UNIT} mB burned every
 * fuel-unit duration (four times as fast while spinning up), and a slot whose fuel items each add a bucket.
 * Air-breathing engines stop when drowned (every side fluid, none open). Subclasses may add slots, a water tank and heat.
 */
public abstract class FuelEngineBlockEntity extends EngineBlockEntity implements MenuProvider {
    public static final int CAPACITY = 240_000;
    public static final int FUEL_PER_UNIT = 10;
    public static final int DATA_COUNT = 9;
    public static final int SLOT_FUEL = 0;

    protected final FluidTank fuel = new FluidTank(CAPACITY, s -> s.is(fuelTag())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    protected final ItemStackHandler items = new ItemStackHandler(slotCount()) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isValidInSlot(slot, stack);
        }
    };
    private int fuelTicks;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> fuel.getFluidAmount() & 0xFFFF;
                case 1 -> fuel.getFluidAmount() >>> 16;
                case 2 -> omega & 0xFFFF;
                case 3 -> omega >>> 16;
                case 4 -> torque & 0xFFFF;
                case 5 -> torque >>> 16;
                case 6 -> waterAmount();
                case 7 -> temperatureForDisplay();
                case 8 -> additives();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    protected FuelEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Fluids this engine burns. */
    protected abstract TagKey<Fluid> fuelTag();

    /** The fluid a fuel item turns into. */
    protected abstract Fluid fuelFluid();

    /** The item that adds a bucket of fuel when put in the fuel slot. */
    protected abstract Item fuelItem();

    /** Ticks per {@link #FUEL_PER_UNIT} mB at full speed. */
    protected abstract int fuelUnitTicks();

    protected int slotCount() {
        return 1;
    }

    protected boolean isValidInSlot(int slot, ItemStack stack) {
        return slot == SLOT_FUEL && stack.is(fuelItem());
    }

    protected boolean airBreathing() {
        return true;
    }

    /** Called each time a unit of fuel is burned. */
    protected void onFuelBurned() {
    }

    /** Called every tick before burning fuel, to take items from the slots. */
    protected void takeItems() {
    }

    protected int waterAmount() {
        return 0;
    }

    protected int temperatureForDisplay() {
        return 0;
    }

    protected int additives() {
        return 0;
    }

    public FluidTank fuel() {
        return fuel;
    }

    public ItemStackHandler items() {
        return items;
    }

    /** Fuel can come in from any side but the output. */
    public IFluidHandler fuelHandler(Direction side) {
        return side == facing() ? null : fuel;
    }

    @Override
    protected boolean canRun() {
        return !fuel.isEmpty() && (!airBreathing() || !isDrowned());
    }

    /** The original's test: drowned when a fluid touches it and no side is open (air or a replaceable block). */
    public boolean isDrowned() {
        if (level == null) {
            return false;
        }
        boolean wet = false;
        for (Direction d : Direction.values()) {
            BlockState s = level.getBlockState(worldPosition.relative(d));
            if (s.isAir()) {
                return false;
            }
            if (!s.getFluidState().isEmpty()) {
                wet = true;
            } else if (s.canBeReplaced()) {
                return false;
            }
        }
        return wet;
    }

    @Override
    protected void afterTick(boolean running) {
        ItemStack in = items.getStackInSlot(SLOT_FUEL);
        if (!in.isEmpty() && in.is(fuelItem()) && fuel.getFluidAmount() + 1000 <= CAPACITY) {
            items.extractItem(SLOT_FUEL, 1, false);
            fuel.fill(new FluidStack(fuelFluid(), 1000), FluidTank.FluidAction.EXECUTE);
        }
        takeItems();
        if (!running) {
            return;
        }
        int unit = omega < targetSpeed() ? Math.max(fuelUnitTicks() / 4, 1) : fuelUnitTicks();
        if (++fuelTicks >= unit) {
            fuelTicks = 0;
            fuel.drain(FUEL_PER_UNIT, FluidTank.FluidAction.EXECUTE);
            onFuelBurned();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("fuel", fuel.writeToNBT(registries, new CompoundTag()));
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("fuelTicks", fuelTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel.readFromNBT(registries, tag.getCompound("fuel"));
        items.deserializeNBT(registries, tag.getCompound("items"));
        fuelTicks = tag.getInt("fuelTicks");
    }
}
