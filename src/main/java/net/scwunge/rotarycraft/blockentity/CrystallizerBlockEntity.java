package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.menu.CrystallizerMenu;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.recipe.CrystallizingRecipe;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

import java.util.Optional;

/**
 * Crystallizer: needs 1024 rad/s and 2 kW. Freezes the fluid in its 8-bucket tank into items (water to ice, lava to stone,
 * ethanol to ethanol crystals...) once the machine is colder than the fluid's freezing point, 400 - 24 x log2(speed) ticks
 * a time. It chills towards its surroundings (5 C colder next to snow, 15 next to water, 30 next to ice) and dry ice in its
 * second slot takes 40 C off while it lasts. Cool it further with a Cooling Fin.
 */
public class CrystallizerBlockEntity extends ConsumerBlockEntity implements MenuProvider, Heatable {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1024, 2048);
    public static final int CAPACITY = 8000;
    public static final int SLOT_OUTPUT = 0;
    public static final int SLOT_DRY_ICE = 1;
    public static final int SLOTS = 2;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_DRY_ICE && stack.is(RotaryItems.DRY_ICE.get());
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
            return slot == SLOT_DRY_ICE ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == SLOT_OUTPUT ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return items.isItemValid(slot, stack);
        }
    };
    private final FluidTank tank = new FluidTank(CAPACITY, this::hasRecipeFor) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private int temperature = Integer.MIN_VALUE;
    private int progress;
    private int operationTime = 1;
    private int ticks;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> operationTime;
                case 2 -> tank.getFluidAmount();
                case 3 -> BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
                case 4 -> getTemperature();
                case 5 -> freezingPoint();
                case 6 -> omega & 0xFFFF;
                case 7 -> omega >>> 16;
                case 8 -> torque & 0xFFFF;
                case 9 -> torque >>> 16;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return CrystallizerMenu.DATA_COUNT;
        }
    };

    public CrystallizerBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.CRYSTALLIZER.get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    public ItemStackHandler items() {
        return items;
    }

    public IItemHandler automationItems() {
        return automation;
    }

    public FluidTank tank() {
        return tank;
    }

    /** Fluid goes in from any side; nothing comes back out. */
    public IFluidHandler fluidHandler() {
        return FluidAccess.fillOnly(tank);
    }

    private boolean hasRecipeFor(FluidStack stack) {
        return level != null && recipeFor(stack).isPresent();
    }

    private Optional<CrystallizingRecipe> recipeFor(FluidStack stack) {
        if (level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        // the biggest batch the tank can fill (water makes ice from a bucket, snowballs from a fifth of one), else the smallest
        var fits = level.getRecipeManager().getAllRecipesFor(RotaryRecipes.CRYSTALLIZING.get()).stream().map(RecipeHolder::value)
                .filter(r -> r.accepts(stack)).toList();
        return fits.stream().filter(r -> r.fluid().getAmount() <= stack.getAmount())
                .max(java.util.Comparator.comparingInt(r -> r.fluid().getAmount()))
                .or(() -> fits.stream().min(java.util.Comparator.comparingInt(r -> r.fluid().getAmount())));
    }

    public int freezingPoint() {
        return recipeFor(tank.getFluid()).map(CrystallizingRecipe::freezingPoint).orElse(0);
    }

    @Override
    public int getTemperature() {
        return temperature == Integer.MIN_VALUE ? Ambient.temperature(level, worldPosition) : temperature;
    }

    public void setTemperature(int t) {
        temperature = t;
        setChanged();
    }

    @Override
    public int getMaxTemperature() {
        return 200;
    }

    @Override
    public void addTemperature(int amount) {
        temperature = getTemperature() + amount;
        setChanged();
    }

    @Override
    public boolean canBeFrictionHeated() {
        return false;
    }

    @Override
    public boolean canBeCooledWithFins() {
        return true;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (++ticks % 20 == 0) {
            updateTemperature();
        }
        Optional<CrystallizingRecipe> recipe = recipeFor(tank.getFluid());
        if (!powered || recipe.isEmpty() || tank.getFluidAmount() < recipe.get().fluid().getAmount()
                || getTemperature() > recipe.get().freezingPoint() || !roomFor(recipe.get().result())) {
            progress = 0;
            return;
        }
        operationTime = Math.max(1, PowerRequirement.operationTime(400, 24, omega));
        if (++progress < operationTime) {
            return;
        }
        progress = 0;
        ItemStack out = items.getStackInSlot(SLOT_OUTPUT);
        ItemStack result = recipe.get().result();
        items.setStackInSlot(SLOT_OUTPUT, out.isEmpty() ? result.copy() : out.copyWithCount(out.getCount() + result.getCount()));
        tank.drain(recipe.get().fluid().getAmount(), IFluidHandler.FluidAction.EXECUTE);
        setChanged();
    }

    private boolean roomFor(ItemStack result) {
        ItemStack out = items.getStackInSlot(SLOT_OUTPUT);
        return out.isEmpty() || (ItemStack.isSameItemSameComponents(out, result) && out.getCount() + result.getCount() <= out.getMaxStackSize());
    }

    private void updateTemperature() {
        int tAmb = Ambient.temperature(level, worldPosition);
        boolean snow = false;
        boolean water = false;
        boolean ice = false;
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            BlockState s = level.getBlockState(worldPosition.relative(d));
            snow |= s.is(Blocks.SNOW) || s.is(Blocks.SNOW_BLOCK);
            water |= s.getFluidState().is(FluidTags.WATER);
            ice |= s.is(Blocks.ICE) || s.is(Blocks.PACKED_ICE) || s.is(Blocks.BLUE_ICE);
        }
        if (snow) {
            tAmb -= 5;
        }
        if (water) {
            tAmb -= 15;
        }
        if (ice) {
            tAmb -= 30;
        }
        ItemStack dry = items.getStackInSlot(SLOT_DRY_ICE);
        if (dry.is(RotaryItems.DRY_ICE.get())) {
            tAmb -= 40;
            if (getTemperature() > tAmb + 4 || level.random.nextInt(20) == 0) {
                items.extractItem(SLOT_DRY_ICE, 1, false);
            }
        }
        int t = getTemperature();
        int dT = tAmb - t;
        temperature = t + (Math.abs(dT) < 4 ? Integer.signum(dT) : dT / 4);
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft.crystallizer");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CrystallizerMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("temperature", getTemperature());
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        tank.readFromNBT(registries, tag.getCompound("tank"));
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : Integer.MIN_VALUE;
        progress = tag.getInt("progress");
    }
}
