package net.scwunge.rotarycraft.process;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scwunge.rotarycraft.farm.FarmBlockEntity;
import net.scwunge.rotarycraft.farm.FarmUi;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.ProcessRegistry;

/**
 * The Big Furnace, as the original's: eighteen items at once, smelted together each time its timer runs out, into eighteen output slots. It needs
 * 400 C, which lava fed to it (15 mB each second) raises it towards, and some shaft power to run (it turns nothing). Hotter it is quicker: 150 ticks
 * a batch from 600 C, half that from 1000 C, 200 at lower temperatures. Lava goes in from the sides.
 */
public class BigFurnaceBlockEntity extends FarmBlockEntity implements Heatable {
    public static final int INPUTS = 18;
    public static final int MAX_TEMPERATURE = 1200;
    public static final int SMELT_TEMPERATURE = 400;
    public static final int CAPACITY = 16000;

    private final ItemStackHandler items = new ItemStackHandler(INPUTS * 2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < INPUTS && !smeltResult(stack).isEmpty();
        }
    };
    private final FluidTank lava = new FluidTank(CAPACITY, f -> f.getFluid() == Fluids.LAVA) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private int temperature = Integer.MIN_VALUE;
    private int progress;

    public BigFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ProcessRegistry.BIG_FURNACE_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "bigFurnace";
    }

    @Override
    protected boolean anySide() {
        return true;
    }

    @Override
    public PowerRequirement requirement() {
        return new PowerRequirement(0, 0, 1);
    }

    @Override
    public ItemStackHandler items() {
        return items;
    }

    @Override
    public FarmUi ui() {
        int[] slots = new int[INPUTS * 4];
        for (int i = 0; i < INPUTS; i++) {
            slots[2 * i] = 8 + (i % 9) * 18;
            slots[2 * i + 1] = 18 + (i / 9) * 21;
            slots[2 * (INPUTS + i)] = 8 + (i % 9) * 18;
            slots[2 * (INPUTS + i) + 1] = 72 + (i / 9) * 21;
        }
        return new FarmUi("big_furnace", 190, 207, slots, 125, 0);
    }

    @Override
    public Slot slot(ItemStackHandler handler, int index, int x, int y) {
        return index >= INPUTS ? new SlotItemHandler(handler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        } : super.slot(handler, index, x, y);
    }

    public FluidTank lava() {
        return lava;
    }

    public int progress() {
        return progress;
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
    public void addTemperature(int amount) {
        temperature = getTemperature() + amount;
        setChanged();
    }

    @Override
    public int getMaxTemperature() {
        return MAX_TEMPERATURE;
    }

    /** The original lets no outside source heat it but its lava. */
    @Override
    public boolean canBeFrictionHeated() {
        return false;
    }

    /** What the furnace makes of this item, or empty. */
    public ItemStack smeltResult(ItemStack stack) {
        if (stack.isEmpty() || level == null) {
            return ItemStack.EMPTY;
        }
        SingleRecipeInput input = new SingleRecipeInput(stack);
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level).map(h -> h.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    /** How many ticks a batch takes at the temperature it is at. */
    public int operationTime() {
        int t = getTemperature();
        int base = t >= 600 ? 150 : 200;
        return t >= 1000 ? base / 2 : base;
    }

    public boolean canSmelt() {
        if (getTemperature() < SMELT_TEMPERATURE || getPower() < 1) {
            return false;
        }
        for (int i = 0; i < INPUTS; i++) {
            if (!smeltResult(items.getStackInSlot(i)).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void smelt() {
        for (int i = 0; i < INPUTS; i++) {
            ItemStack in = items.getStackInSlot(i);
            ItemStack to = smeltResult(in);
            if (to.isEmpty()) {
                continue;
            }
            ItemStack out = items.getStackInSlot(INPUTS + i);
            if (out.isEmpty()) {
                items.setStackInSlot(INPUTS + i, to.copy());
            } else if (ItemStack.isSameItemSameComponents(out, to) && out.getCount() + to.getCount() <= out.getMaxStackSize()) {
                out.grow(to.getCount());
                items.setStackInSlot(INPUTS + i, out);
            } else {
                continue;
            }
            in.shrink(1);
            items.setStackInSlot(i, in);
        }
    }

    private void updateTemperature(ServerLevel server) {
        int ambient = Ambient.temperature(server, worldPosition);
        if (!lava.isEmpty()) {
            lava.drain(15, IFluidHandler.FluidAction.EXECUTE);
            ambient += 600;
        }
        int t = getTemperature();
        if (t > ambient) {
            t--;
        }
        if (t > ambient * 2) {
            t--;
        }
        if (t < ambient) {
            t++;
        }
        if (t * 2 < ambient) {
            t++;
        }
        temperature = Math.min(t, MAX_TEMPERATURE);
        if (temperature > 100) {
            for (Direction side : Direction.values()) {
                BlockPos at = worldPosition.relative(side);
                BlockState state = server.getBlockState(at);
                if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
                    server.removeBlock(at, false);
                } else if (state.is(Blocks.ICE)) {
                    server.setBlockAndUpdate(at, Blocks.WATER.defaultBlockState());
                }
            }
        }
    }

    @Override
    protected void machineTick(boolean powered) {
        ServerLevel server = server();
        if (server.getGameTime() % 20 == 0) {
            updateTemperature(server);
            syncNow();
        }
        if (canSmelt()) {
            if (++progress >= operationTime()) {
                progress = 0;
                smelt();
            }
        } else {
            progress = 0;
        }
    }

    @Override
    protected int[] status() {
        return new int[] {getTemperature(), lava.getFluidAmount(), CAPACITY, progress, operationTime()};
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("lava", lava.writeToNBT(registries, new CompoundTag()));
        if (temperature != Integer.MIN_VALUE) {
            tag.putInt("temperature", temperature);
        }
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        lava.readFromNBT(registries, tag.getCompound("lava"));
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : Integer.MIN_VALUE;
    }

    @Override
    protected void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        writeClientData(tag, registries);
        tag.putInt("progress", progress);
    }

    @Override
    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {
        readClientData(tag, registries);
        progress = tag.getInt("progress");
    }
}
