package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.menu.CentrifugeMenu;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.recipe.CentrifugeRecipe;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

import java.util.Optional;

/**
 * Centrifuge: powered from below (from above if it faces down), as in the original. Needs 4096 rad/s and 16 kW. Each spin
 * takes 1200 - 60 x log2(speed) ticks and turns one input (two for recipes that allow it) into chanced outputs in the 3x3
 * grid, plus an optional fluid into its 10-bucket tank.
 */
public class CentrifugeBlockEntity extends ConsumerBlockEntity implements MenuProvider {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 4096, 16384);
    public static final int CAPACITY = 10_000;
    public static final int SLOT_INPUT = 0;
    public static final int SLOTS = 10;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_INPUT;
        }
    };
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == SLOT_INPUT ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == SLOT_INPUT ? ItemStack.EMPTY : items.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_INPUT;
        }
    };
    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private int progress;
    private int operationTime = 1;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> operationTime;
                case 2 -> tank.getFluidAmount();
                case 3 -> omega & 0xFFFF;
                case 4 -> omega >>> 16;
                case 5 -> torque & 0xFFFF;
                case 6 -> torque >>> 16;
                case 7 -> net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return CentrifugeMenu.DATA_COUNT;
        }
    };

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.CENTRIFUGE.get(), pos, state);
    }

    @Override
    public Direction inputSide() {
        return facing() == Direction.DOWN ? Direction.UP : Direction.DOWN;
    }

    public ItemStackHandler items() {
        return items;
    }

    public FluidTank tank() {
        return tank;
    }

    public IItemHandler automationItems() {
        return automation;
    }

    public int progress() {
        return progress;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    private Optional<CentrifugeRecipe> recipe() {
        ItemStack in = items.getStackInSlot(SLOT_INPUT);
        if (in.isEmpty() || level == null) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(RotaryRecipes.CENTRIFUGE.get(), new SingleRecipeInput(in), level).map(RecipeHolder::value);
    }

    /** Room for every possible output of one input (worst case) and its fluid. */
    private boolean hasRoom(CentrifugeRecipe r) {
        ItemStackHandler sim = new ItemStackHandler(SLOTS);
        for (int s = 1; s < SLOTS; s++) {
            sim.setStackInSlot(s, items.getStackInSlot(s).copy());
        }
        for (CentrifugeRecipe.ChancedOutput o : r.outputs()) {
            int max = (int) Math.ceil(o.chance() / 100F) * o.item().getCount();
            if (!insertOutputs(sim, o.item().copyWithCount(max)).isEmpty()) {
                return false;
            }
        }
        return r.fluid().isEmpty() || tank.fill(r.fluid().copy(), FluidTank.FluidAction.SIMULATE) == r.fluid().getAmount();
    }

    private static ItemStack insertOutputs(ItemStackHandler handler, ItemStack stack) {
        ItemStack left = stack;
        for (int s = 1; s < SLOTS && !left.isEmpty(); s++) {
            ItemStack cur = handler.getStackInSlot(s);
            if (cur.isEmpty()) {
                handler.setStackInSlot(s, left);
                return ItemStack.EMPTY;
            }
            if (ItemStack.isSameItemSameComponents(cur, left)) {
                int move = Math.min(left.getCount(), cur.getMaxStackSize() - cur.getCount());
                if (move > 0) {
                    handler.setStackInSlot(s, cur.copyWithCount(cur.getCount() + move));
                    left = left.copyWithCount(left.getCount() - move);
                }
            }
        }
        return left;
    }

    @Override
    protected void machineTick(boolean powered) {
        Optional<CentrifugeRecipe> recipe = recipe();
        if (!powered || recipe.isEmpty() || !hasRoom(recipe.get())) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return;
        }
        operationTime = PowerRequirement.operationTime(1200, 60, omega);
        if (++progress < operationTime) {
            setChanged();
            return;
        }
        progress = 0;
        CentrifugeRecipe r = recipe.get();
        int spins = Math.min(r.perOperation(), items.getStackInSlot(SLOT_INPUT).getCount());
        for (int n = 0; n < spins && hasRoom(r); n++) {
            items.extractItem(SLOT_INPUT, 1, false);
            for (CentrifugeRecipe.ChancedOutput o : r.outputs()) {
                int whole = (int) (o.chance() / 100F);
                float rest = o.chance() / 100F - whole;
                int count = whole + (level.random.nextFloat() < rest ? 1 : 0);
                if (count > 0) {
                    insertOutputs(items, o.item().copyWithCount(o.item().getCount() * count));
                }
            }
            if (!r.fluid().isEmpty() && level.random.nextFloat() * 100F < r.fluidChance()) {
                tank.fill(r.fluid().copy(), FluidTank.FluidAction.EXECUTE);
            }
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft.centrifuge");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CentrifugeMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        tank.readFromNBT(registries, tag.getCompound("tank"));
        progress = tag.getInt("progress");
    }
}
