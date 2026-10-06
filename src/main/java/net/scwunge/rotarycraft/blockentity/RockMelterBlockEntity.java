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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.menu.RockMelterMenu;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.recipe.MeltingRecipe;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

import java.util.Optional;

/**
 * Rock Melter: any shaft power from below heats it (by log2 of the power each second, settling about 64 x log2(power)
 * above ambient). Once it is hot enough for the first meltable item, the power goes into that item until the recipe's
 * energy is reached, then the item becomes fluid in the 64-bucket tank. The tank empties from the four sides only.
 */
public class RockMelterBlockEntity extends ConsumerBlockEntity implements MenuProvider, Heatable {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 1);
    public static final int CAPACITY = 64_000;
    public static final int MAX_TEMPERATURE = 1800;
    public static final int SLOTS = 9;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return melting(stack).isPresent();
        }
    };
    /** Pipes and hoppers may insert meltables but take nothing out. */
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
    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    /** What the sides expose: drain only. */
    private final IFluidHandler output = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int t) {
            return tank.getFluid();
        }

        @Override
        public int getTankCapacity(int t) {
            return CAPACITY;
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
            return tank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return tank.drain(maxDrain, action);
        }
    };

    private int temperature = Integer.MIN_VALUE;
    /** Watt-ticks stored towards the current melt; the recipe energy in joules is 1/20 of this. */
    private long energy;
    private int timer;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> temperature();
                case 1 -> tank.getFluidAmount();
                case 2 -> net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
                case 3 -> omega & 0xFFFF;
                case 4 -> omega >>> 16;
                case 5 -> torque & 0xFFFF;
                case 6 -> torque >>> 16;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return RockMelterMenu.DATA_COUNT;
        }
    };

    public RockMelterBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.ROCK_MELTER.get(), pos, state);
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

    public FluidTank tank() {
        return tank;
    }

    public IItemHandler automationItems() {
        return automation;
    }

    /** The sides' fluid handler (drain only), or null for the top and bottom. */
    public IFluidHandler output(Direction side) {
        return side == null || side.getAxis().isHorizontal() ? output : null;
    }

    public int temperature() {
        return temperature == Integer.MIN_VALUE ? Ambient.temperature(level, worldPosition) : temperature;
    }

    public void setTemperature(int t) {
        temperature = t;
        setChanged();
    }

    public long energy() {
        return energy;
    }

    private Optional<MeltingRecipe> melting(ItemStack stack) {
        if (stack.isEmpty() || level == null) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(RotaryRecipes.MELTING.get(), new SingleRecipeInput(stack), level).map(RecipeHolder::value);
    }

    /** The original melts whatever comes first in the inventory, so that item sets the temperature needed. */
    private int meltingTemperature() {
        for (int i = 0; i < SLOTS; i++) {
            Optional<MeltingRecipe> r = melting(items.getStackInSlot(i));
            if (r.isPresent()) {
                return r.get().temperature();
            }
        }
        return Integer.MAX_VALUE;
    }

    private boolean canTake(FluidStack fluid) {
        return tank.fill(fluid, IFluidHandler.FluidAction.SIMULATE) == fluid.getAmount();
    }

    @Override
    protected void machineTick(boolean powered) {
        long power = getPower();
        if (++timer >= 20) {
            timer = 0;
            updateTemperature(power);
        }
        if (temperature() >= meltingTemperature()) {
            energy += power;
        } else {
            energy = (long) (energy * 0.85);
        }
        for (int i = 0; i < SLOTS; i++) {
            Optional<MeltingRecipe> r = melting(items.getStackInSlot(i));
            if (r.isPresent() && canTake(r.get().result()) && energy >= r.get().energy() * 20) {
                tank.fill(r.get().result().copy(), IFluidHandler.FluidAction.EXECUTE);
                items.extractItem(i, 1, false);
                energy -= r.get().energy() * 20;
                break;
            }
        }
        setChanged();
    }

    private void updateTemperature(long power) {
        int tAmb = Ambient.temperature(level, worldPosition);
        int t = temperature();
        if (power > 0) {
            t += (int) (Math.log(power) / Math.log(2));
        }
        // settle towards ambient by 1/64 of the difference each second (the original moves away from it when colder)
        t += (tAmb - t) / 64;
        if (t - tAmb <= 64 && t > tAmb) {
            t--;
        }
        t = Math.min(t, MAX_TEMPERATURE);
        temperature = t;
        if (t > 50) {
            for (Direction d : Direction.values()) {
                BlockPos p = worldPosition.relative(d);
                BlockState s = level.getBlockState(p);
                if (s.is(Blocks.SNOW) || s.is(Blocks.SNOW_BLOCK)) {
                    level.removeBlock(p, false);
                    break;
                }
                if (s.is(Blocks.ICE)) {
                    level.setBlockAndUpdate(p, Blocks.WATER.defaultBlockState());
                    break;
                }
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft.rock_melter");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RockMelterMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("temperature", temperature);
        tag.putLong("energy", energy);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        tank.readFromNBT(registries, tag.getCompound("tank"));
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : Integer.MIN_VALUE;
        energy = tag.getLong("energy");
    }

    @Override
    public int getTemperature() {
        return temperature();
    }

    @Override
    public int getMaxTemperature() {
        return 1800;
    }

    @Override
    public void addTemperature(int amount) {
        temperature = temperature() + amount;
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
}
