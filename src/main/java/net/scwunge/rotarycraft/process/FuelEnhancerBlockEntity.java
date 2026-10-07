package net.scwunge.rotarycraft.process;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.config.FarmConfig;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.farm.FarmBlockEntity;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.ProcessRegistry;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryItems;

import java.util.List;

/**
 * The Fuel Enhancer (the original's Fuel Converter): it turns a fuel into jet fuel, four mB of it for each mB made, while it holds one each of
 * blaze powder, netherrack dust, tar, magma cream and pink dye, which it uses up now and then. Faster shafts make more each tick. The original's
 * fuels came from other mods; here they are the common tags {@code c:kerosene} and {@code c:fuel}. Fuel goes in from above and the jet fuel comes
 * out of its sides.
 */
public class FuelEnhancerBlockEntity extends FarmBlockEntity {
    public static final int CAPACITY = 5000;
    public static final int SLOTS = 9;

    /** What one fuel becomes: {@code ratio} mB in for each mB out, {@code speed} mB out each tick at the lowest speed; the items go {@code itemFactor} as fast. */
    public record Conversion(TagKey<Fluid> input, int speed, int ratio, double itemFactor) {
    }

    public static final TagKey<Fluid> KEROSENE = tag("kerosene");
    public static final TagKey<Fluid> FUEL = tag("fuel");

    /** What it can make: this list takes more (another mod's fuels, say), added with {@link #addConversion}. */
    public static final List<Conversion> CONVERSIONS = new java.util.concurrent.CopyOnWriteArrayList<>(List.of(new Conversion(KEROSENE, 1, 4, 1), new Conversion(FUEL, 1, 4, 1.5)));

    public static void addConversion(Conversion conversion) {
        CONVERSIONS.add(conversion);
    }

    private static TagKey<Fluid> tag(String name) {
        return TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", name));
    }

    /** The five things it needs. */
    public static List<ItemStack> ingredients() {
        return List.of(new ItemStack(Items.BLAZE_POWDER), new ItemStack(RotaryItems.NETHERRACK_DUST.get()), new ItemStack(RotaryItems.TAR.get()),
                new ItemStack(Items.MAGMA_CREAM), new ItemStack(Items.PINK_DYE));
    }

    public static boolean isIngredient(ItemStack stack) {
        for (ItemStack i : ingredients()) {
            if (stack.is(i.getItem())) {
                return true;
            }
        }
        return false;
    }

    public static Conversion conversionFor(FluidStack fluid) {
        for (Conversion c : CONVERSIONS) {
            if (!fluid.isEmpty() && fluid.is(c.input())) {
                return c;
            }
        }
        return null;
    }

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isIngredient(stack);
        }
    };
    private final FluidTank input = new FluidTank(CAPACITY, f -> conversionFor(f) != null) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank output = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public FuelEnhancerBlockEntity(BlockPos pos, BlockState state) {
        super(ProcessRegistry.FUEL_ENHANCER_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "fuelEnhancer";
    }

    @Override
    protected boolean anySide() {
        return true;
    }

    @Override
    public PowerRequirement requirement() {
        return new PowerRequirement(0, 1, 1);
    }

    @Override
    public ItemStackHandler items() {
        return items;
    }

    @Override
    public int menuRows() {
        return 1;
    }

    public FluidTank input() {
        return input;
    }

    public FluidTank output() {
        return output;
    }

    /** mB of jet fuel made each tick at the speed it is turned with: the conversion's speed, times one more for each two doublings of the speed. */
    public int boost(Conversion c) {
        return c.speed() * (1 + (31 - Integer.numberOfLeadingZeros(Math.max(1, getOmega()))) / 2);
    }

    private boolean hasIngredients() {
        for (ItemStack need : ingredients()) {
            boolean found = false;
            for (int i = 0; i < SLOTS && !found; i++) {
                found = items.getStackInSlot(i).is(need.getItem());
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private void useIngredients(Conversion c) {
        double chance = RotaryConfig.get(FarmConfig.FUEL_ENHANCER_ITEM_CHANCE) * c.itemFactor();
        for (ItemStack need : ingredients()) {
            if (level.random.nextDouble() < chance) {
                for (int i = 0; i < SLOTS; i++) {
                    if (items.getStackInSlot(i).is(need.getItem())) {
                        items.extractItem(i, 1, false);
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void machineTick(boolean powered) {
        if (powered && getOmega() >= 1) {
            Conversion c = conversionFor(input.getFluid());
            if (c != null) {
                int made = boost(c);
                FluidStack jet = new FluidStack(RotaryFluids.JET_FUEL.get(), made);
                if (input.getFluidAmount() >= c.ratio() * made && output.fill(jet, IFluidHandler.FluidAction.SIMULATE) == made && hasIngredients()) {
                    input.drain(c.ratio() * made, IFluidHandler.FluidAction.EXECUTE);
                    output.fill(jet, IFluidHandler.FluidAction.EXECUTE);
                    useIngredients(c);
                }
            }
        }
        if (level.getGameTime() % 20 == 0) {
            syncNow();
        }
    }

    @Override
    protected int[] status() {
        return new int[] {input.getFluidAmount(), output.getFluidAmount(), CAPACITY};
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("input", input.writeToNBT(registries, new CompoundTag()));
        tag.put("output", output.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        input.readFromNBT(registries, tag.getCompound("input"));
        output.readFromNBT(registries, tag.getCompound("output"));
    }

    @Override
    protected void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        writeClientData(tag, registries);
    }

    @Override
    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {
        readClientData(tag, registries);
    }
}
