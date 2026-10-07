package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity;
import net.scwunge.rotarycraft.machine.LayoutMenus;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.DecorRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Obsidian Maker (TileEntityObsidianMaker): driven from below at 2048 rad/s and 32768 W or more, it mixes water and lava it is piped (from the
 * sides) into cobblestone or obsidian, by its temperature: lava heats it three degrees a second, water cools it, and obsidian forms between 550 and
 * 750 C (fading out to either side as far as 500 and 900 C). Obsidian takes a bucket of lava and 2500 mB of water, cobblestone a bucket of water. It
 * stops when its nine slots are full, and at 1000 C it overheats into lava.
 */
public class ObsidianMakerBlockEntity extends InventoryMachineBlockEntity implements Heatable {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 2048, 32768);
    public static final int CAPACITY = 320_000;
    public static final int MAX_TEMPERATURE = 1000;
    public static final int MIN_OBSIDIAN_0 = 500;
    public static final int MIN_OBSIDIAN_100 = 550;
    public static final int MAX_OBSIDIAN_100 = 750;
    public static final int MAX_OBSIDIAN_0 = 900;
    public static final int SLOTS = 9;
    public static final String NAME = "obsidian_maker";

    public static final GuiLayout LAYOUT = GuiLayout.named("obsidian_maker").size(176, 166)
            .grid(62, 18, 3, 3).gauge(0, 48, 71, 7, 54, CAPACITY).gauge(1, 120, 71, 7, 54, CAPACITY).extras(1).build();

    private final FluidTank water = addTank(CAPACITY, s -> s.getFluid() == Fluids.WATER);
    private final FluidTank lava = addTank(CAPACITY, s -> s.getFluid() == Fluids.LAVA);
    private final IFluidHandler intake = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int t) {
            return t == 0 ? water.getFluid() : lava.getFluid();
        }

        @Override
        public int getTankCapacity(int t) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int t, FluidStack stack) {
            return t == 0 ? stack.getFluid() == Fluids.WATER : stack.getFluid() == Fluids.LAVA;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return resource.getFluid() == Fluids.WATER ? water.fill(resource, action) : resource.getFluid() == Fluids.LAVA ? lava.fill(resource, action) : 0;
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

    private int temperature = Integer.MIN_VALUE;
    private int tickCount;
    private int temperatureTick;
    private boolean idle;

    public ObsidianMakerBlockEntity(BlockPos pos, BlockState state) {
        super(DecorRegistry.OBSIDIAN_MAKER.type().get(), pos, state, SLOTS, NAME);
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    public net.minecraft.core.Direction inputSide() {
        return Direction.DOWN;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    protected boolean acceptsItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected int extraCount() {
        return 1;
    }

    @Override
    public int extra(int index) {
        return temperature();
    }

    public FluidTank waterTank() {
        return water;
    }

    public FluidTank lavaTank() {
        return lava;
    }

    /** Water and lava come in at the sides, never the top or bottom. */
    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return side == null || side.getAxis().isHorizontal() ? intake : null;
    }

    public int temperature() {
        return temperature == Integer.MIN_VALUE ? (level == null ? 0 : Ambient.temperature(level, worldPosition)) : temperature;
    }

    /** Whether it has stopped: its slots are full, or it is out of water or lava (the original's comparator reading is 15 then). */
    public boolean isIdle() {
        return idle;
    }

    // ---- the work ----

    /** getProducedItem: obsidian in the sweet spot, a chance of it either side of it, else cobblestone. */
    private ItemStack producedItem() {
        int t = temperature();
        if (t >= MIN_OBSIDIAN_100 && t <= MAX_OBSIDIAN_100) {
            return new ItemStack(Items.OBSIDIAN);
        } else if (t > MAX_OBSIDIAN_100) {
            if (t > MAX_OBSIDIAN_0) {
                return new ItemStack(Items.COBBLESTONE);
            }
            float f = (t - MAX_OBSIDIAN_100) / (float) (MAX_OBSIDIAN_0 - MAX_OBSIDIAN_100);
            return level.random.nextFloat() < 1 - f ? new ItemStack(Items.OBSIDIAN) : new ItemStack(Items.COBBLESTONE);
        } else {
            if (t < MIN_OBSIDIAN_0) {
                return new ItemStack(Items.COBBLESTONE);
            }
            float f = (MIN_OBSIDIAN_100 - t) / (float) (MIN_OBSIDIAN_100 - MIN_OBSIDIAN_0);
            return level.random.nextFloat() < 1 - f ? new ItemStack(Items.OBSIDIAN) : new ItemStack(Items.COBBLESTONE);
        }
    }

    private int nonFullSlot(ItemStack stack) {
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack in = items.getStackInSlot(i);
            if (in.isEmpty() || ItemStack.isSameItem(in, stack) && in.getCount() < in.getMaxStackSize()) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!MachineConfig.enabled("obsidianMaker")) {
            return;
        }
        tickCount++;
        if (++temperatureTick >= 20) {
            temperatureTick = 0;
            updateTemperature();
        }
        boolean noLiquid = water.isEmpty() || lava.isEmpty();
        idle = noLiquid || nonFullSlot(new ItemStack(Items.COBBLESTONE)) == -1 && nonFullSlot(new ItemStack(Items.OBSIDIAN)) == -1;
        if (!powered || noLiquid) {
            return;
        }
        if (tickCount >= PowerRequirement.operationTime(800, 60, omega)) {
            tickCount = 0;
            mix();
        }
    }

    private void updateTemperature() {
        int ambient = Ambient.temperature(level, worldPosition);
        int t = temperature();
        int max = MAX_TEMPERATURE;
        if (t > ambient) {
            t--;
        }
        if (t < ambient) {
            t++;
        }
        if (!water.isEmpty()) {
            max = 600;
            if (lava.isEmpty()) {
                t = Math.max(t - 2, ambient);
            }
        }
        if (!lava.isEmpty()) {
            t = Math.min(t + 3, max);
        }
        temperature = t;
        if (t >= MAX_TEMPERATURE && RotaryConfig.get(RotaryConfig.EXPLOSIONS_BREAK_BLOCKS)) {
            overheat();
        }
        setChanged();
    }

    private void overheat() {
        level.playSound(null, worldPosition, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 3F, 1F);
        level.setBlockAndUpdate(worldPosition, Blocks.LAVA.defaultBlockState());
    }

    private void mix() {
        ItemStack product = producedItem();
        int slot = nonFullSlot(product);
        if (slot == -1) {
            return;
        }
        boolean obsidian = product.is(Items.OBSIDIAN);
        int lavaAmount = obsidian ? 1000 : 50;
        int waterAmount = obsidian ? 2500 : 1000;
        if (lava.getFluidAmount() < lavaAmount || water.getFluidAmount() < 1000 || water.getFluidAmount() < waterAmount) {
            return;
        }
        ItemStack in = items.getStackInSlot(slot);
        if (in.isEmpty()) {
            items.setStackInSlot(slot, product);
        } else {
            in.grow(1);
            items.setStackInSlot(slot, in);
        }
        if (obsidian) {
            lava.drain(lavaAmount, IFluidHandler.FluidAction.EXECUTE);
        }
        water.drain(waterAmount, IFluidHandler.FluidAction.EXECUTE);
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F + 0.5F * level.random.nextFloat(), 0.7F + 0.3F * level.random.nextFloat());
        if (level instanceof ServerLevel server) {
            for (double x = 0.25; x <= 0.75; x += 0.25) {
                for (double z = 0.25; z <= 0.75; z += 0.25) {
                    server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + x, worldPosition.getY() + 0.75, worldPosition.getZ() + z, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    // ---- heat ----

    @Override
    public int getTemperature() {
        return temperature();
    }

    @Override
    public int getMaxTemperature() {
        return MAX_TEMPERATURE;
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("temperature", temperature);
        tag.putInt("ticks", tickCount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : Integer.MIN_VALUE;
        tickCount = tag.getInt("ticks");
    }
}
