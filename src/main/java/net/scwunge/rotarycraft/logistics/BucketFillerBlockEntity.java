package net.scwunge.rotarycraft.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
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
 * Bucket Filler (TileEntityBucketFiller): fills the empty buckets (and any other item that holds fluid) in its eighteen slots from its 24 bucket tank, which
 * takes fluid from pipes at the sides, or, switched the other way with the button, empties the full ones into the tank, one a go. Filled containers come out of it
 * when filling, empty ones when emptying. Needs 1 N*m at 512 rad/s and 2 kW from any side, and works faster the faster it turns.
 */
public class BucketFillerBlockEntity extends InventoryMachineBlockEntity {
    public static final String NAME = "bucket_filler";
    public static final int SLOTS = 18;
    public static final int CAPACITY = 24_000;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 512, 2048);
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).storage(2).tanks(1).button(0, 100, 4, 68, 12, 0).build();

    private final FluidTank tank;
    private final IFluidHandler fillOnly;
    private final IFluidHandler drainOnly;
    private boolean filling = true;
    private int ticks;

    public BucketFillerBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsRegistry.BUCKET_FILLER.type().get(), pos, state, SLOTS, NAME);
        tank = addTank(CAPACITY, s -> true);
        fillOnly = fillOnly(tank);
        drainOnly = drainOnly(tank);
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    protected boolean omniSided() {
        return true;
    }

    public FluidTank tank() {
        return tank;
    }

    public boolean filling() {
        return filling;
    }

    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (side != null && !side.getAxis().isHorizontal()) {
            return null;
        }
        return filling ? fillOnly : drainOnly;
    }

    @Override
    public boolean menuButton(Player player, int id) {
        if (id != 0) {
            return false;
        }
        filling = !filling;
        setChanged();
        return true;
    }

    @Override
    public int extra(int index) {
        return filling ? 0 : 1;
    }

    @Override
    protected int extraCount() {
        return 1;
    }

    /** What a stack holds as a fluid container: a handler for one of it, or null if it holds no fluid. */
    @Nullable
    private static IFluidHandlerItem handler(ItemStack stack) {
        return stack.isEmpty() ? null : stack.copyWithCount(1).getCapability(Capabilities.FluidHandler.ITEM);
    }

    @Override
    protected boolean acceptsItem(int slot, ItemStack stack) {
        return handler(stack) != null;
    }

    /** What the pipes and hoppers may take: full containers when filling, empty ones when emptying. */
    @Override
    protected boolean mayExtract(int slot) {
        ItemStack stack = items.getStackInSlot(slot);
        IFluidHandlerItem h = handler(stack);
        if (h == null) {
            return true;
        }
        boolean full = h.getFluidInTank(0).getAmount() >= h.getTankCapacity(0);
        return filling ? full : h.getFluidInTank(0).isEmpty();
    }

    public int operationTime() {
        return PowerRequirement.operationTime(200, 20, omega);
    }

    private void put(ItemStack stack) {
        ItemStack left = stack;
        for (int i = 0; i < SLOTS && !left.isEmpty(); i++) {
            left = items.insertItem(i, left, false);
        }
        if (!left.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, left);
        }
    }

    /** Fills every container it can from the tank. */
    public void fillContainers() {
        for (int i = 0; i < SLOTS && !tank.isEmpty(); i++) {
            ItemStack stack = items.getStackInSlot(i);
            IFluidHandlerItem h = handler(stack);
            if (h == null || h.getFluidInTank(0).getAmount() >= h.getTankCapacity(0)) {
                continue;
            }
            int filled = h.fill(tank.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                ItemStack result = h.getContainer();
                items.extractItem(i, 1, false);
                put(result);
            }
        }
    }

    /** Empties the first full container it can into the tank. */
    public void emptyContainer() {
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = items.getStackInSlot(i);
            IFluidHandlerItem h = handler(stack);
            if (h == null || h.getFluidInTank(0).isEmpty()) {
                continue;
            }
            FluidStack inside = h.getFluidInTank(0).copy();
            if (tank.fill(inside, IFluidHandler.FluidAction.SIMULATE) < inside.getAmount()) {
                continue;
            }
            FluidStack drained = h.drain(inside, IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty()) {
                tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                ItemStack result = h.getContainer();
                items.extractItem(i, 1, false);
                put(result);
                return;
            }
        }
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel) || !powered || !MachineConfig.enabled("bucketFiller") || ++ticks <= operationTime()) {
            return;
        }
        ticks = 0;
        if (filling) {
            fillContainers();
        } else {
            emptyContainer();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("filling", filling);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        filling = !tag.contains("filling") || tag.getBoolean("filling");
    }
}
