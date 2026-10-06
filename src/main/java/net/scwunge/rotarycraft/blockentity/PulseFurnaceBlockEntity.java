package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.menu.PulseFurnaceMenu;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.recipe.PulseSmeltingRecipe;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

import java.util.Optional;

/**
 * Pulse Furnace, as in the original: a jet-fuelled furnace that needs 131072 rad/s to run its compressor. It burns jet fuel
 * (100 mB per 100 ticks while smelting) to climb towards 1000 C, smelts the item in its input slot once hot enough (the
 * recipe says how hot), and an oxygen accelerant makes it work four times as fast. Water cools it, but is boiled away. Past
 * 1000 C it blows itself apart. Cooling Fins draw heat off it.
 */
public class PulseFurnaceBlockEntity extends ConsumerBlockEntity implements MenuProvider, Heatable {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 131072, 1);
    public static final int WATER_CAPACITY = 3000;
    public static final int FUEL_CAPACITY = 8000;
    public static final int ACCEL_CAPACITY = 8000;
    public static final int MAX_TEMPERATURE = 1000;
    public static final int SMELT_TICKS_BASE = 100;
    public static final int OPERATION_TIME = 20;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOTS = 2;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_INPUT && recipeFor(stack).isPresent();
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
    private final FluidTank water = new FluidTank(WATER_CAPACITY, s -> s.getFluid().isSame(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank fuel = new FluidTank(FUEL_CAPACITY, s -> s.getFluid().isSame(RotaryFluids.JET_FUEL.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank accelerant = new FluidTank(ACCEL_CAPACITY,
            s -> s.getFluid().isSame(RotaryFluids.OXYGEN.get()) || s.getFluid().is(FluidTags.create(net.minecraft.resources.ResourceLocation.parse("c:oxygen")))) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private int temperature = Integer.MIN_VALUE;
    private int cookTime;
    private int smeltTick;
    private int ticks;
    private int fuelTicks;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cookTime;
                case 1 -> getTemperature();
                case 2 -> smeltTick;
                case 3 -> smeltingDuration();
                case 4 -> fuel.getFluidAmount();
                case 5 -> water.getFluidAmount();
                case 6 -> accelerant.getFluidAmount();
                case 7 -> omega & 0xFFFF;
                case 8 -> omega >>> 16;
                case 9 -> torque & 0xFFFF;
                case 10 -> torque >>> 16;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return PulseFurnaceMenu.DATA_COUNT;
        }
    };

    public PulseFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.PULSE_FURNACE.get(), pos, state);
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

    public FluidTank water() {
        return water;
    }

    public FluidTank fuel() {
        return fuel;
    }

    public FluidTank accelerant() {
        return accelerant;
    }

    /** Water, jet fuel and oxygen go in from the four sides (not the top or bottom); nothing comes out. */
    public IFluidHandler fluidHandler(Direction side) {
        if (side == Direction.UP || side == Direction.DOWN) {
            return null;
        }
        return new IFluidHandler() {
            private final FluidTank[] tanks = {water, fuel, accelerant};

            @Override
            public int getTanks() {
                return tanks.length;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return tanks[tank].getFluid();
            }

            @Override
            public int getTankCapacity(int tank) {
                return tanks[tank].getCapacity();
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return tanks[tank].isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                for (FluidTank t : tanks) {
                    if (t.isFluidValid(resource)) {
                        return t.fill(resource, action);
                    }
                }
                return 0;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return FluidStack.EMPTY;
            }
        };
    }

    public Optional<PulseSmeltingRecipe> recipeFor(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(RotaryRecipes.PULSE_SMELTING.get(), new SingleRecipeInput(stack), level).map(RecipeHolder::value);
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

    /** The original lets nothing but its own burner heat it. */
    @Override
    public boolean canBeFrictionHeated() {
        return false;
    }

    @Override
    public boolean canBeCooledWithFins() {
        return true;
    }

    private boolean canHeatUp(boolean powered) {
        return powered && !fuel.isEmpty();
    }

    public int smeltingDuration() {
        int t = getTemperature();
        if (t >= 980) {
            return SMELT_TICKS_BASE / 8;
        } else if (t >= 950) {
            return SMELT_TICKS_BASE / 4;
        } else if (t >= 900) {
            return SMELT_TICKS_BASE / 2;
        }
        return SMELT_TICKS_BASE;
    }

    /** The recipe that could run right now: powered, fuelled, hot enough, and with room for the result. */
    private Optional<PulseSmeltingRecipe> activeRecipe(boolean powered) {
        ItemStack in = items.getStackInSlot(SLOT_INPUT);
        if (!powered || in.isEmpty() || fuel.isEmpty()) {
            return Optional.empty();
        }
        Optional<PulseSmeltingRecipe> rec = recipeFor(in);
        if (rec.isEmpty() || rec.get().temperature() > getTemperature()) {
            return Optional.empty();
        }
        ItemStack out = items.getStackInSlot(SLOT_OUTPUT);
        ItemStack result = rec.get().result();
        if (!out.isEmpty() && (!ItemStack.isSameItemSameComponents(out, result) || out.getCount() + result.getCount() > out.getMaxStackSize())) {
            return Optional.empty();
        }
        return rec;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (++ticks % 20 == 0) {
            updateTemperature(powered);
        }
        Optional<PulseSmeltingRecipe> recipe = activeRecipe(powered);
        if (recipe.isPresent() && ++fuelTicks >= 100) {
            fuel.drain(100, IFluidHandler.FluidAction.EXECUTE);
            fuelTicks = 0;
        }
        int step = 1;
        if (powered && !fuel.isEmpty() && accelerant.getFluidAmount() > 10) {
            step = 4;
            if (recipe.isPresent() || getTemperature() >= 875) {
                accelerant.drain(10, IFluidHandler.FluidAction.EXECUTE);
                if (level.random.nextInt(4) == 0) {
                    temperature = getTemperature() + 1;
                }
            }
        }
        if (recipe.isEmpty()) {
            smeltTick = 0;
            cookTime = 0;
            return;
        }
        smeltTick += step;
        if (smeltTick < smeltingDuration()) {
            cookTime = 0;
            return;
        }
        cookTime += step;
        if (cookTime >= OPERATION_TIME) {
            cookTime = 0;
            smeltTick = 0;
            ItemStack out = items.getStackInSlot(SLOT_OUTPUT);
            ItemStack result = recipe.get().result();
            items.setStackInSlot(SLOT_OUTPUT, out.isEmpty() ? result.copy() : out.copyWithCount(out.getCount() + result.getCount()));
            items.extractItem(SLOT_INPUT, 1, false);
            setChanged();
        }
    }

    /** Once a second: burner heat, losses to the surroundings, water cooling and the environment. */
    private void updateTemperature(boolean powered) {
        int t = getTemperature();
        if (canHeatUp(powered)) {
            t += Math.max((MAX_TEMPERATURE - t) / 8, 4);
        }
        int tAmb = Ambient.temperature(level, worldPosition);
        int dT = 2;
        if (tAmb < -40) {
            dT = 8;
        } else if (tAmb < -5) {
            dT = 6;
        } else if (tAmb < 5) {
            dT = 4;
        }
        if (tAmb >= 300 && canHeatUp(powered)) {
            dT = -1;
        } else if (tAmb >= t) {
            dT = 0;
        } else if (tAmb > 30) {
            dT = 1;
        }
        if (!water.isEmpty()) {
            if (level.random.nextInt(3) == 0) {
                int rem = (t * 2 / MAX_TEMPERATURE) * 50;
                if (tAmb >= 180) {
                    rem *= 2;
                }
                if (tAmb >= 90) {
                    rem *= 2;
                }
                if (rem > 0) {
                    water.drain(rem, IFluidHandler.FluidAction.EXECUTE);
                }
            }
            t -= tAmb >= 300 ? t / 256 : t / 64;
        }
        if (dT > 0) {
            t = Math.max(tAmb, t - dT);
        } else if (dT < 0) {
            t -= dT;
        }
        temperature = t;
        setChanged();
        heatSurroundings(t);
        if (t > MAX_TEMPERATURE) {
            overheat();
        }
    }

    private void heatSurroundings(int t) {
        if (t < 300) {
            return;
        }
        for (Direction d : Direction.values()) {
            BlockPos n = worldPosition.relative(d);
            BlockState s = level.getBlockState(n);
            if (s.isFlammable(level, n, d.getOpposite()) && level.random.nextInt(4) == 0) {
                level.setBlockAndUpdate(n, Blocks.FIRE.defaultBlockState());
            } else if (s.is(Blocks.SNOW) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.ICE)) {
                level.setBlockAndUpdate(n, s.is(Blocks.ICE) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
        }
    }

    /** It tears itself apart and throws scrap around. */
    private void overheat() {
        BlockPos pos = worldPosition;
        Level l = level;
        Containers.dropContents(l, pos, net.minecraft.core.NonNullList.of(ItemStack.EMPTY, items.getStackInSlot(0), items.getStackInSlot(1)));
        l.removeBlock(pos, false);
        Containers.dropItemStack(l, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(RotaryItems.SCRAP.get(), 1 + l.random.nextInt(4)));
        l.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3F, true,
                RotaryConfig.get(RotaryConfig.EXPLOSIONS_BREAK_BLOCKS) ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft.pulse_furnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PulseFurnaceMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("fuel", fuel.writeToNBT(registries, new CompoundTag()));
        tag.put("accelerant", accelerant.writeToNBT(registries, new CompoundTag()));
        tag.putInt("temperature", getTemperature());
        tag.putInt("cookTime", cookTime);
        tag.putInt("smeltTick", smeltTick);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        water.readFromNBT(registries, tag.getCompound("water"));
        fuel.readFromNBT(registries, tag.getCompound("fuel"));
        accelerant.readFromNBT(registries, tag.getCompound("accelerant"));
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : Integer.MIN_VALUE;
        cookTime = tag.getInt("cookTime");
        smeltTick = tag.getInt("smeltTick");
    }
}
