package net.scwunge.rotarycraft.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Filling Station (TileEntityFillingStation): a 32 bucket tank that fills the item put in it, anything with a fluid tank of its own (jetpack fuel tanks, Decorative
 * Tanks, buckets): its screen has a slot for the item to fill, one for a full container to empty into the tank, an output where filled items go, and an input
 * that feeds the filling slot. It adds four mB a tick for each doubling of the speed (starting from 1 rad/s), and needs 1 kW. Fluid comes in by pipe from any side.
 */
public class FillingStationBlockEntity extends InventoryMachineBlockEntity {
    public static final String NAME = "filling_station";
    public static final int CAPACITY = 32_000;
    public static final int FILLING = 0;
    public static final int FUEL = 1;
    public static final int OUTPUT = 2;
    public static final int INPUT = 3;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 1024);
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).size(176, 187).inventoryAt(8, 105).slot(106, 71).slot(54, 21).slot(134, 71).slot(106, 21)
            .gauge(0, 82, 87, 12, 66, CAPACITY).build();

    private final FluidTank tank;
    private final IFluidHandler intake;
    private int progress;

    public FillingStationBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsRegistry.FILLING_STATION.type().get(), pos, state, 4, NAME);
        tank = addTank(CAPACITY, s -> true);
        intake = FluidAccess.fillOnly(tank);
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    public FluidTank tank() {
        return tank;
    }

    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return intake;
    }

    @Nullable
    private static IFluidHandlerItem handler(ItemStack stack) {
        return stack.isEmpty() ? null : stack.copyWithCount(1).getCapability(Capabilities.FluidHandler.ITEM);
    }

    @Override
    protected boolean acceptsItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT, FILLING -> handler(stack) != null;
            case FUEL -> handler(stack) != null && !handler(stack).getFluidInTank(0).isEmpty();
            default -> false;
        };
    }

    @Override
    protected boolean mayExtract(int slot) {
        return slot == OUTPUT;
    }

    /** Millibuckets it adds each tick. */
    public int perTick() {
        int add = 4 * (int) (Math.log(Math.max(1, omega)) / Math.log(2));
        return Math.min(add, tank.getFluidAmount());
    }

    private boolean isFull(ItemStack stack) {
        IFluidHandlerItem h = handler(stack);
        return h != null && h.getFluidInTank(0).getAmount() >= h.getTankCapacity(0);
    }

    /** Empties the container in the fuel slot into the tank when it fits and agrees with the fluid there. */
    private void takeFuel() {
        ItemStack stack = items.getStackInSlot(FUEL);
        IFluidHandlerItem h = handler(stack);
        if (h == null || h.getFluidInTank(0).isEmpty()) {
            return;
        }
        FluidStack inside = h.getFluidInTank(0).copy();
        if (tank.fill(inside, IFluidHandler.FluidAction.SIMULATE) < inside.getAmount()) {
            return;
        }
        FluidStack drained = h.drain(inside, IFluidHandler.FluidAction.EXECUTE);
        if (!drained.isEmpty()) {
            tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            ItemStack left = h.getContainer();
            items.extractItem(FUEL, 1, false);
            if (!left.isEmpty()) {
                if (!items.getStackInSlot(FUEL).isEmpty()) {
                    Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, left);
                } else {
                    items.setStackInSlot(FUEL, left);
                }
            }
        }
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel) || !powered || !MachineConfig.enabled("fillingStation")) {
            return;
        }
        takeFuel();
        ItemStack target = items.getStackInSlot(FILLING);
        if (target.isEmpty()) {
            ItemStack in = items.getStackInSlot(INPUT);
            if (handler(in) != null) {
                items.setStackInSlot(FILLING, in.copyWithCount(1));
                items.extractItem(INPUT, 1, false);
                progress = 0;
            }
            return;
        }
        IFluidHandlerItem h = handler(target);
        if (h != null && !tank.isEmpty() && !isFull(target)) {
            int add = perTick();
            if (add > 0) {
                int filled = h.fill(tank.getFluid().copyWithAmount(add), IFluidHandler.FluidAction.EXECUTE);
                if (filled == 0) {
                    // a container that only takes its whole load at once (a bucket): fill it when the pumping has added up to that
                    progress += add;
                    int whole = Math.min(tank.getFluidAmount(), h.getTankCapacity(0) - h.getFluidInTank(0).getAmount());
                    if (progress >= whole && h.fill(tank.getFluid().copyWithAmount(whole), IFluidHandler.FluidAction.SIMULATE) > 0) {
                        filled = h.fill(tank.getFluid().copyWithAmount(whole), IFluidHandler.FluidAction.EXECUTE);
                        progress = 0;
                    }
                }
                if (filled > 0) {
                    tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    items.setStackInSlot(FILLING, h.getContainer());
                    target = items.getStackInSlot(FILLING);
                }
            }
        }
        if (isFull(target) || h == null) {
            ItemStack left = putOutput(OUTPUT, target.copy());
            if (left.isEmpty()) {
                items.setStackInSlot(FILLING, ItemStack.EMPTY);
            }
        }
    }
}
