package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.menu.FermenterMenu;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryItems;

/**
 * Fermenter, as in the original. Needs 32 rad/s and 1 kW, and water (50 mB per batch; it also draws from an adjacent water
 * source). Sugar + dirt -> yeast (dirt used 1 time in 4). Yeast + plant matter -> sludge, as much as the plant is worth
 * (yeast used 1 time in 2). Speed depends on temperature: best at 25 C for yeast and 35 C for sludge, slow below 20 C or
 * above 40 C, and yeast dies at 60 C. Plant values come from the item tags rotarycraft:mulch/1, /2, /4 and /8.
 */
public class FermenterBlockEntity extends ConsumerBlockEntity implements MenuProvider {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 32, 1024);
    public static final int SLOT_A = 0;
    public static final int SLOT_B = 1;
    public static final int SLOT_OUT = 2;
    public static final int CAPACITY = 4000;
    public static final int WATER_PER_BATCH = 50;
    public static final int MIN_USEFUL = 20;
    public static final int OPT_MULTIPLY = 25;
    public static final int MAX_USEFUL = 40;
    public static final int OPT_FERMENT = 35;
    public static final int KILL_TEMPERATURE = 60;
    private static final int[] MULCH_VALUES = {8, 4, 2, 1};

    private final ItemStackHandler items = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot != SLOT_OUT;
        }
    };
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return 3;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == SLOT_OUT ? stack : items.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == SLOT_OUT ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot != SLOT_OUT;
        }
    };
    private final FluidTank water = new FluidTank(CAPACITY, s -> s.is(FluidTags.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private int temperature = Integer.MIN_VALUE;
    private int cookTime;
    private int operationTime = 1;
    private int tickCount;
    private int yeastCheck;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cookTime;
                case 1 -> operationTime;
                case 2 -> temperature();
                case 3 -> water.getFluidAmount();
                case 4 -> omega & 0xFFFF;
                case 5 -> omega >>> 16;
                case 6 -> torque & 0xFFFF;
                case 7 -> torque >>> 16;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return FermenterMenu.DATA_COUNT;
        }
    };

    public FermenterBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.FERMENTER.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    public FluidTank water() {
        return water;
    }

    public IItemHandler automationItems() {
        return automation;
    }

    public int temperature() {
        return temperature == Integer.MIN_VALUE ? ambient() : temperature;
    }

    public void setTemperature(int t) {
        temperature = t;
        setChanged();
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    /** How much sludge one of this item makes, 0 if it isn't plant matter. */
    public static int mulchValue(ItemStack stack) {
        for (int v : MULCH_VALUES) {
            if (stack.is(TagKey.create(net.minecraft.core.registries.Registries.ITEM, RotaryCraft.id("mulch/" + v)))) {
                return v;
            }
        }
        return 0;
    }

    /** What the current inputs make (one batch), or empty. */
    public ItemStack product() {
        ItemStack a = items.getStackInSlot(SLOT_A);
        ItemStack b = items.getStackInSlot(SLOT_B);
        if (a.isEmpty() || b.isEmpty() || water.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (a.is(Items.SUGAR) && b.is(ItemTags.DIRT)) {
            return new ItemStack(RotaryItems.YEAST.get());
        }
        if (a.is(RotaryItems.YEAST.get())) {
            int value = mulchValue(b);
            if (value > 0) {
                return new ItemStack(RotaryItems.SLUDGE.get(), value);
            }
        }
        return ItemStack.EMPTY;
    }

    /** The original's temperature factor (1 at the optimum). */
    public static float fermentRate(int temperature, boolean makingYeast) {
        if (temperature < MIN_USEFUL) {
            return 1F / (MIN_USEFUL - temperature);
        }
        if (temperature > MAX_USEFUL) {
            return 1F / (temperature - MAX_USEFUL);
        }
        float diff = Math.abs(temperature - (makingYeast ? OPT_MULTIPLY : OPT_FERMENT));
        return (float) Math.pow(Math.max(0, 1 - diff / 16F), 0.2);
    }

    private int ambient() {
        return Ambient.temperature(level, worldPosition);
    }

    private void updateTemperature() {
        int tAmb = ambient();
        boolean waterSource = false;
        for (Direction d : Direction.values()) {
            BlockState s = level.getBlockState(worldPosition.relative(d));
            if (s.getFluidState().is(FluidTags.WATER) && s.getFluidState().isSource()) {
                waterSource = true;
            }
        }
        if (waterSource) {
            tAmb -= 5;
            water.fill(new FluidStack(Fluids.WATER, 1000), FluidTank.FluidAction.EXECUTE);
        }
        for (Direction d : Direction.values()) {
            BlockState s = level.getBlockState(worldPosition.relative(d));
            if (s.is(Blocks.ICE)) {
                tAmb -= 15;
                break;
            }
        }
        for (Direction d : Direction.values()) {
            if (level.getBlockState(worldPosition.relative(d)).getBlock() instanceof BaseFireBlock) {
                tAmb += 50;
                break;
            }
        }
        for (Direction d : Direction.values()) {
            BlockState s = level.getBlockState(worldPosition.relative(d));
            if (s.getFluidState().is(FluidTags.LAVA) && s.getFluidState().isSource()) {
                tAmb += 200;
                break;
            }
        }
        int t = temperature();
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

    private void killYeast() {
        if (temperature() < KILL_TEMPERATURE) {
            return;
        }
        for (int slot = 0; slot < 3; slot++) {
            if (items.getStackInSlot(slot).is(RotaryItems.YEAST.get())) {
                items.extractItem(slot, 1, false);
                level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.8F);
                return;
            }
        }
    }

    @Override
    protected void machineTick(boolean powered) {
        if (++tickCount % 20 == 0) {
            updateTemperature();
        }
        if (--yeastCheck <= 0) {
            yeastCheck = 2 + level.random.nextInt(18);
            killYeast();
        }
        ItemStack product = product();
        if (!powered || product.isEmpty() || !fits(product)) {
            if (cookTime != 0) {
                cookTime = 0;
                setChanged();
            }
            return;
        }
        boolean yeast = product.is(RotaryItems.YEAST.get());
        int base = PowerRequirement.operationTime(480, 35, omega);
        operationTime = Math.max(1, (int) (base / fermentRate(temperature(), yeast)));
        if (++cookTime < operationTime) {
            setChanged();
            return;
        }
        cookTime = 0;
        ItemStack out = items.getStackInSlot(SLOT_OUT);
        items.setStackInSlot(SLOT_OUT, out.isEmpty() ? product.copy() : out.copyWithCount(out.getCount() + product.getCount()));
        if (yeast) {
            items.extractItem(SLOT_A, 1, false);
            if (level.random.nextInt(4) == 0) {
                items.extractItem(SLOT_B, 1, false);
            }
        } else {
            items.extractItem(SLOT_B, 1, false);
            if (level.random.nextInt(2) == 0) {
                items.extractItem(SLOT_A, 1, false);
            }
        }
        water.drain(WATER_PER_BATCH, FluidTank.FluidAction.EXECUTE);
        setChanged();
    }

    private boolean fits(ItemStack product) {
        ItemStack out = items.getStackInSlot(SLOT_OUT);
        return out.isEmpty() || (out.is(product.getItem()) && out.getCount() + product.getCount() <= out.getMaxStackSize());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft.fermenter");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FermenterMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.putInt("temperature", temperature());
        tag.putInt("cookTime", cookTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        water.readFromNBT(registries, tag.getCompound("water"));
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : Integer.MIN_VALUE;
        cookTime = tag.getInt("cookTime");
    }
}
