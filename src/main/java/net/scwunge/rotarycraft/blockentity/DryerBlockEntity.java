package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.menu.DryerMenu;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import net.scwunge.rotarycraft.recipe.DryingRecipe;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

import java.util.Optional;

/**
 * Dryer (the original's Drying Bed): needs no power. Fluid piped in from any side but the bottom dries out into items, every
 * 400 ticks for as many batches as the tank and the output slot allow (water to salt, lava to gold nuggets...).
 */
public class DryerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = 2000;
    public static final int PERIOD = 400;

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };
    private final IItemHandler output = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return items.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };
    private final FluidTank tank = new FluidTank(CAPACITY, this::hasRecipeFor) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private int progress;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> tank.getFluidAmount();
                case 2 -> BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DryerMenu.DATA_COUNT;
        }
    };

    public DryerBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.DRYER.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    public IItemHandler outputItems() {
        return output;
    }

    public FluidTank tank() {
        return tank;
    }

    public int progress() {
        return progress;
    }

    /** Fluid goes in from the sides and the top, never the bottom, and cannot be taken back out. */
    public IFluidHandler fluidHandler(Direction side) {
        return side == Direction.DOWN ? null : FluidAccess.fillOnly(tank);
    }

    private boolean hasRecipeFor(FluidStack stack) {
        return level != null && recipeFor(stack).isPresent();
    }

    private Optional<DryingRecipe> recipeFor(FluidStack stack) {
        if (level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getAllRecipesFor(RotaryRecipes.DRYING.get()).stream().map(RecipeHolder::value)
                .filter(r -> r.accepts(stack)).findFirst();
    }

    private boolean canMake(DryingRecipe r) {
        if (tank.getFluidAmount() < r.fluid().getAmount()) {
            return false;
        }
        ItemStack out = items.getStackInSlot(0);
        return out.isEmpty() || (ItemStack.isSameItemSameComponents(out, r.result()) && out.getCount() + r.result().getCount() <= out.getMaxStackSize());
    }

    public void serverTick() {
        Optional<DryingRecipe> recipe = recipeFor(tank.getFluid()).filter(this::canMake);
        if (recipe.isEmpty()) {
            progress = 0;
            return;
        }
        if (++progress < PERIOD) {
            return;
        }
        progress = 0;
        while (recipe.isPresent()) {
            DryingRecipe r = recipe.get();
            ItemStack out = items.getStackInSlot(0);
            items.setStackInSlot(0, out.isEmpty() ? r.result().copy() : out.copyWithCount(out.getCount() + r.result().getCount()));
            tank.drain(r.fluid().getAmount(), IFluidHandler.FluidAction.EXECUTE);
            recipe = recipeFor(tank.getFluid()).filter(this::canMake);
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft.dryer");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new DryerMenu(id, inventory, this, data);
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
