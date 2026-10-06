package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.menu.ComposterMenu;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.recipe.CompostingRecipe;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

import java.util.Optional;

/**
 * Composter, as in the original: needs no power, only warmth (40 to 70 C) and yeast. It rots one item from the top slot a
 * cycle into Compost (the recipe says how much), faster the warmer it is, and now and then uses up a yeast. It heats and cools
 * towards its surroundings (5 C colder next to water, 15 next to ice, 50 warmer next to fire, 200 next to lava).
 */
public class ComposterBlockEntity extends BlockEntity implements MenuProvider, Heatable {
    public static final int MIN_TEMPERATURE = 40;
    public static final int KILL_TEMPERATURE = 70;
    public static final int MAX_TEMPERATURE = 100;
    public static final int CYCLE = 100;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_YEAST = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOTS = 3;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_INPUT -> compostValue(stack) > 0;
                case SLOT_YEAST -> stack.is(RotaryItems.YEAST.get());
                default -> false;
            };
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
            return slot == SLOT_OUTPUT ? stack : items.insertItem(slot, stack, simulate);
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

    private int temperature = Integer.MIN_VALUE;
    private int timer;
    private int ticks;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> timer;
                case 1 -> getTemperature();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return ComposterMenu.DATA_COUNT;
        }
    };

    public ComposterBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.COMPOSTER.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    public IItemHandler automationItems() {
        return automation;
    }

    public int timer() {
        return timer;
    }

    /** How much Compost one of this item rots into; 0 if it can't be composted. */
    public int compostValue(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return 0;
        }
        return level.getRecipeManager().getRecipeFor(RotaryRecipes.COMPOSTING.get(), new SingleRecipeInput(stack), level)
                .map(RecipeHolder::value).map(CompostingRecipe::value).orElse(0);
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
        return MAX_TEMPERATURE;
    }

    @Override
    public void addTemperature(int amount) {
        temperature = getTemperature() + amount;
        setChanged();
    }

    @Override
    public boolean canBeCooledWithFins() {
        return true;
    }

    private int composting() {
        int t = getTemperature();
        if (t < MIN_TEMPERATURE || t > KILL_TEMPERATURE) {
            return 0;
        }
        ItemStack in = items.getStackInSlot(SLOT_INPUT);
        if (in.isEmpty() || !items.getStackInSlot(SLOT_YEAST).is(RotaryItems.YEAST.get())) {
            return 0;
        }
        int value = compostValue(in);
        if (value <= 0) {
            return 0;
        }
        ItemStack out = items.getStackInSlot(SLOT_OUTPUT);
        if (out.isEmpty()) {
            return value;
        }
        return out.is(RotaryItems.COMPOST.get()) && out.getCount() + value <= out.getMaxStackSize() ? value : 0;
    }

    public void serverTick() {
        if (++ticks % 20 == 0) {
            updateTemperature();
        }
        int value = composting();
        if (value <= 0) {
            return;
        }
        timer += 1 + (getTemperature() - MIN_TEMPERATURE) / 4;
        if (timer < CYCLE) {
            return;
        }
        timer = 0;
        items.extractItem(SLOT_INPUT, 1, false);
        ItemStack out = items.getStackInSlot(SLOT_OUTPUT);
        items.setStackInSlot(SLOT_OUTPUT, new ItemStack(RotaryItems.COMPOST.get(), out.getCount() + value));
        if (level.random.nextInt(Math.max(1, 75 - getTemperature())) == 0) {
            items.extractItem(SLOT_YEAST, 1, false);
        }
        setChanged();
    }

    private void updateTemperature() {
        int tAmb = Ambient.temperature(level, worldPosition);
        boolean water = false;
        boolean ice = false;
        boolean fire = false;
        boolean lava = false;
        for (Direction d : Direction.values()) {
            BlockState s = level.getBlockState(worldPosition.relative(d));
            water |= s.getFluidState().is(FluidTags.WATER);
            ice |= s.is(Blocks.ICE) || s.is(Blocks.PACKED_ICE) || s.is(Blocks.BLUE_ICE);
            fire |= s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.CAMPFIRE) || s.is(Blocks.MAGMA_BLOCK);
            lava |= s.getFluidState().is(FluidTags.LAVA);
        }
        if (water) {
            tAmb -= 5;
        }
        if (ice) {
            tAmb -= 15;
        }
        if (fire) {
            tAmb += 50;
        }
        if (lava) {
            tAmb += 200;
        }
        int t = getTemperature();
        if (t > tAmb) {
            t--;
        }
        if (t > tAmb * 2) {
            t--;
        }
        if (t < tAmb) {
            t++;
        }
        if (t * 2 < tAmb) {
            t++;
        }
        temperature = t;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft.composter");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ComposterMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("temperature", getTemperature());
        tag.putInt("timer", timer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : Integer.MIN_VALUE;
        timer = tag.getInt("timer");
    }
}
