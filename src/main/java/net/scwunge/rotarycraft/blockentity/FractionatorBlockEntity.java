package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.menu.FractionatorMenu;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryItems;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Fractionation Unit: turns ethanol into jet fuel. Powered from below (8192 rad/s, 64 kW). Needs one each of the six
 * ingredients (blaze powder, coal dust, magma cream, pink dye, netherrack dust, tar) and a ghast tear as solvent, which is
 * never used up. Each batch (800 - 40 x log2(speed) ticks) takes 250 mB of ethanol and one or two ingredients, weighted.
 * The yield depends on the pressure inside, which the input torque builds up: about 460 N*m holds the 720 kPa where a
 * batch gives as much fuel as it took ethanol. Values from the original (medium difficulty).
 */
public class FractionatorBlockEntity extends ConsumerBlockEntity implements MenuProvider {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 8192, 65536);
    public static final int ETHANOL_CAPACITY = 16_000;
    public static final int FUEL_CAPACITY = 240_000;
    public static final int MAX_PRESSURE = 1000;
    public static final int MIN_TIME = 10;
    public static final int ETHANOL_PER_BATCH = 250;
    public static final int SLOT_SOLVENT = 6;
    public static final int SLOTS = 7;
    private static final double AMBIENT_PRESSURE = 101.3;
    /** Ingredients consumed per batch: six ingredients x the original's medium-difficulty fraction (0.25). */
    private static final float CONSUMED_PER_BATCH = 6 * 0.25F;
    private static final int MIN_PRODUCED = 1000;
    private static final int MAX_PRODUCED = 2200;
    private static final double[][] YIELD = {{0, 0.01}, {100, 0.05}, {180, 0.1}, {500, 0.4}, {720, 1}, {850, 1.5}, {1000, 2.5}};

    private record Ingredient(Predicate<ItemStack> test, float weight) {
    }

    private static TagKey<Item> tag(String ns, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ns, path));
    }

    private static final List<Ingredient> INGREDIENTS = List.of(
            new Ingredient(s -> s.is(Items.BLAZE_POWDER), 1.5F),
            new Ingredient(s -> s.is(tag("c", "dusts/coal")), 1F),
            new Ingredient(s -> s.is(Items.MAGMA_CREAM), 0.75F),
            new Ingredient(s -> s.is(tag("c", "dyes/pink")), 0.5F),
            new Ingredient(s -> s.is(RotaryItems.NETHERRACK_DUST.get()), 2F),
            new Ingredient(s -> s.is(RotaryItems.TAR.get()), 1.5F));

    /** Which of the six ingredients this is, or -1. */
    public static int ingredientIndex(ItemStack stack) {
        for (int i = 0; i < INGREDIENTS.size(); i++) {
            if (!stack.isEmpty() && INGREDIENTS.get(i).test().test(stack)) {
                return i;
            }
        }
        return -1;
    }

    public static double yieldAt(int pressure) {
        if (pressure <= YIELD[0][0]) {
            return YIELD[0][1];
        }
        for (int i = 1; i < YIELD.length; i++) {
            if (pressure <= YIELD[i][0]) {
                double t = (pressure - YIELD[i - 1][0]) / (YIELD[i][0] - YIELD[i - 1][0]);
                return YIELD[i - 1][1] + t * (YIELD[i][1] - YIELD[i - 1][1]);
            }
        }
        return YIELD[YIELD.length - 1][1];
    }

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_SOLVENT) {
                return stack.is(Items.GHAST_TEAR);
            }
            int kind = ingredientIndex(stack);
            if (kind < 0) {
                return false;
            }
            // each ingredient kind may sit in only one slot
            for (int i = 0; i < SLOT_SOLVENT; i++) {
                if (i != slot && ingredientIndex(getStackInSlot(i)) == kind) {
                    return false;
                }
            }
            return true;
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
            return items.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
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
    private final FluidTank ethanol = new FluidTank(ETHANOL_CAPACITY, s -> s.is(GasEngineBlockEntity.ETHANOL)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank fuel = new FluidTank(FUEL_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    /** The top gives out jet fuel and takes nothing. */
    private final IFluidHandler output = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int t) {
            return fuel.getFluid();
        }

        @Override
        public int getTankCapacity(int t) {
            return FUEL_CAPACITY;
        }

        @Override
        public boolean isFluidValid(int t, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return fuel.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return fuel.drain(maxDrain, action);
        }
    };

    private final int[] torqueHistory = new int[20];
    private int historyIndex;
    private int pressure;
    private int mixTime;
    private int ticks;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> mixTime;
                case 1 -> operationTime();
                case 2 -> ethanol.getFluidAmount();
                case 3 -> fuel.getFluidAmount() & 0xFFFF;
                case 4 -> fuel.getFluidAmount() >>> 16;
                case 5 -> pressure;
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
            return FractionatorMenu.DATA_COUNT;
        }
    };

    public FractionatorBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.FRACTIONATOR.get(), pos, state);
    }

    @Override
    public Direction inputSide() {
        return Direction.DOWN;
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

    public FluidTank ethanol() {
        return ethanol;
    }

    public FluidTank fuel() {
        return fuel;
    }

    /** Ethanol goes in from the four sides; jet fuel comes out of the top. */
    public IFluidHandler fluidHandler(Direction side) {
        if (side == Direction.UP) {
            return output;
        }
        return side == Direction.DOWN ? null : ethanol;
    }

    public int pressure() {
        return pressure;
    }

    public void setPressure(int p) {
        pressure = p;
        setChanged();
    }

    public int operationTime() {
        return Math.max(MIN_TIME, PowerRequirement.operationTime(800, 40, omega));
    }

    private double averageTorque() {
        long sum = 0;
        for (int t : torqueHistory) {
            sum += t;
        }
        return sum / (double) torqueHistory.length;
    }

    @Override
    protected void machineTick(boolean powered) {
        torqueHistory[historyIndex] = torque;
        historyIndex = (historyIndex + 1) % torqueHistory.length;
        if (++ticks % 20 == 0) {
            updatePressure();
        }
        if (!powered || !canProcess()) {
            mixTime = 0;
            return;
        }
        if (++mixTime >= operationTime()) {
            mixTime = 0;
            make();
        }
        setChanged();
    }

    private void updatePressure() {
        int local = pressure;
        int dp = local - (int) AMBIENT_PRESSURE;
        int sub = (int) (Math.signum(dp) * Math.max(1, Math.abs(dp / 16)));
        int avg = (int) averageTorque();
        if (avg <= 0) {
            sub *= 8;
        }
        local -= sub;
        if (avg > 0) {
            local += (int) (1.8 * Math.sqrt(avg));
        }
        local = Math.min(local, MAX_PRESSURE);
        // gain pressure slowly, lose it quickly
        if (pressure < local) {
            int jitter = 6 + level.random.nextInt(27) - 13;
            pressure += Math.max(1, Math.min(jitter, (local - pressure) / 4));
        } else {
            pressure = local;
        }
        setChanged();
    }

    private boolean hasAllIngredients() {
        boolean[] found = new boolean[INGREDIENTS.size()];
        for (int i = 0; i < SLOT_SOLVENT; i++) {
            int kind = ingredientIndex(items.getStackInSlot(i));
            if (kind < 0 || found[kind]) {
                return false;
            }
            found[kind] = true;
        }
        return ethanol.getFluidAmount() >= ETHANOL_PER_BATCH;
    }

    private boolean canProcess() {
        return fuel.getFluidAmount() + MAX_PRODUCED < FUEL_CAPACITY && items.getStackInSlot(SLOT_SOLVENT).is(Items.GHAST_TEAR) && hasAllIngredients();
    }

    private void make() {
        List<Integer> slots = new ArrayList<>();
        List<Float> weights = new ArrayList<>();
        for (int i = 0; i < SLOT_SOLVENT; i++) {
            slots.add(i);
            weights.add(INGREDIENTS.get(ingredientIndex(items.getStackInSlot(i))).weight());
        }
        float consume = CONSUMED_PER_BATCH;
        while (consume > 0 && !slots.isEmpty()) {
            float total = 0;
            for (float wt : weights) {
                total += wt;
            }
            float r = level.random.nextFloat() * total;
            int pick = 0;
            while (pick < weights.size() - 1 && r >= weights.get(pick)) {
                r -= weights.get(pick);
                pick++;
            }
            if (consume >= 1 || level.random.nextFloat() < consume) {
                items.extractItem(slots.get(pick), 1, false);
            }
            slots.remove(pick);
            weights.remove(pick);
            consume -= 1;
        }
        ethanol.drain(ETHANOL_PER_BATCH, IFluidHandler.FluidAction.EXECUTE);
        int produced = MIN_PRODUCED + level.random.nextInt(1 + MAX_PRODUCED - MIN_PRODUCED);
        fuel.fill(new FluidStack(RotaryFluids.JET_FUEL.get(), (int) (yieldAt(pressure) * produced)), IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft.fractionator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FractionatorMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.put("ethanol", ethanol.writeToNBT(registries, new CompoundTag()));
        tag.put("fuel", fuel.writeToNBT(registries, new CompoundTag()));
        tag.putInt("pressure", pressure);
        tag.putInt("mixTime", mixTime);
        tag.putIntArray("torqueHistory", torqueHistory);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        ethanol.readFromNBT(registries, tag.getCompound("ethanol"));
        fuel.readFromNBT(registries, tag.getCompound("fuel"));
        pressure = tag.getInt("pressure");
        mixTime = tag.getInt("mixTime");
        int[] h = tag.getIntArray("torqueHistory");
        System.arraycopy(h, 0, torqueHistory, 0, Math.min(h.length, torqueHistory.length));
    }
}
