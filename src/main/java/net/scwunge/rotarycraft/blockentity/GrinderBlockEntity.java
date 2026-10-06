package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import net.scwunge.rotarycraft.menu.GrinderMenu;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.recipe.GrindingRecipe;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

import java.util.Optional;

/**
 * Grinder: needs 128 N*m and 4 kW. Grinds one item at a time; an operation takes 840 - 60 x log2(speed + 1) ticks, so a
 * faster shaft grinds faster (the original's values). Hoppers and pipes insert into the input and pull from the output.
 */
public class GrinderBlockEntity extends ConsumerBlockEntity implements MenuProvider {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(128, 1, 4096);
    public static final int DURATION_BASE = 840;
    public static final int DURATION_SCALE = 60;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_INPUT;
        }
    };
    /** Automation view: insert into the input only, extract from the output only. */
    private final IItemHandler automation = new IItemHandler() {
        private final RangedWrapper input = new RangedWrapper(items, SLOT_INPUT, SLOT_INPUT + 1);
        private final RangedWrapper output = new RangedWrapper(items, SLOT_OUTPUT, SLOT_OUTPUT + 1);

        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == SLOT_INPUT ? input.insertItem(0, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == SLOT_OUTPUT ? output.extractItem(0, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_INPUT;
        }
    };

    private int progress;
    private int operationTime = DURATION_BASE;

    /** Synced to the open screen (ContainerData values are 16-bit, so torque and speed go as two halves). */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> operationTime;
                case 2 -> torque & 0xFFFF;
                case 3 -> torque >>> 16;
                case 4 -> omega & 0xFFFF;
                case 5 -> omega >>> 16;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // server-authoritative
        }

        @Override
        public int getCount() {
            return GrinderMenu.DATA_COUNT;
        }
    };

    public GrinderBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.GRINDER.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
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

    private Optional<RecipeHolder<GrindingRecipe>> recipe() {
        ItemStack in = items.getStackInSlot(SLOT_INPUT);
        if (in.isEmpty() || level == null) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(RotaryRecipes.GRINDING.get(), new SingleRecipeInput(in), level);
    }

    private boolean outputFits(ItemStack result) {
        ItemStack out = items.getStackInSlot(SLOT_OUTPUT);
        if (out.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(out, result) && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    @Override
    protected void machineTick(boolean powered) {
        Optional<RecipeHolder<GrindingRecipe>> recipe = recipe();
        if (!powered || recipe.isEmpty() || !outputFits(recipe.get().value().result())) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return;
        }
        operationTime = PowerRequirement.operationTime(DURATION_BASE, DURATION_SCALE, omega);
        progress++;
        if (progress >= operationTime) {
            progress = 0;
            ItemStack result = recipe.get().value().result().copy();
            items.extractItem(SLOT_INPUT, 1, false);
            ItemStack out = items.getStackInSlot(SLOT_OUTPUT);
            if (out.isEmpty()) {
                items.setStackInSlot(SLOT_OUTPUT, result);
            } else {
                out.grow(result.getCount());
                items.setStackInSlot(SLOT_OUTPUT, out);
            }
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft.grinder");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GrinderMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        progress = tag.getInt("progress");
    }
}
