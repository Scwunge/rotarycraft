package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.menu.FuelEngineMenu;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.RotaryMenus;

/**
 * Performance Engine: 256 N*m at 1024 rad/s (262 kW) on ethanol with additives (redstone 1, gunpowder 2, blaze powder 4
 * units; each fuel unit has a 1 in 30 chance to use one). Without additives it runs like a Gas Engine. Burns 10 mB every
 * 6 ticks. It heats 1 C a second while running; 20 mB of water a second holds it steady. Past 240 C it explodes into
 * scrap. Values from the original.
 */
public class PerformanceEngineBlockEntity extends FuelEngineBlockEntity {
    public static final int TORQUE = 256;
    public static final int SPEED = 1024;
    public static final int WATER_CAPACITY = 60_000;
    public static final int MAX_TEMPERATURE = 240;
    public static final int MAX_ADDITIVES = CAPACITY / 1000;
    public static final int SLOT_ADDITIVE = 1;

    private final FluidTank water = new FluidTank(WATER_CAPACITY, s -> s.is(FluidTags.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    /** Takes ethanol into the fuel tank and water into the cooling tank. */
    private final IFluidHandler inputs = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? fuel.getFluid() : water.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? CAPACITY : WATER_CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 ? fuel.isFluidValid(stack) : water.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return fuel.isFluidValid(resource) ? fuel.fill(resource, action) : water.fill(resource, action);
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
    private int additives;
    private int temperature = Integer.MIN_VALUE;
    private int heatTicks;

    public PerformanceEngineBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.PERFORMANCE_ENGINE.get(), pos, state);
    }

    public FluidTank water() {
        return water;
    }

    public int temperature() {
        return temperature == Integer.MIN_VALUE ? Ambient.temperature(level, worldPosition) : temperature;
    }

    public void setTemperature(int t) {
        temperature = t;
        setChanged();
    }

    @Override
    public int additives() {
        return additives;
    }

    private boolean starved() {
        return additives <= 0;
    }

    @Override
    protected int ratedTorque() {
        return starved() ? GasEngineBlockEntity.TORQUE : TORQUE;
    }

    @Override
    protected int targetSpeed() {
        return starved() ? GasEngineBlockEntity.SPEED : SPEED;
    }

    @Override
    protected TagKey<Fluid> fuelTag() {
        return GasEngineBlockEntity.ETHANOL;
    }

    @Override
    protected Fluid fuelFluid() {
        return RotaryFluids.ETHANOL.get();
    }

    @Override
    protected Item fuelItem() {
        return RotaryItems.ETHANOL_CRYSTALS.get();
    }

    @Override
    protected int fuelUnitTicks() {
        return 6;
    }

    @Override
    protected int slotCount() {
        return 2;
    }

    public static int additiveValue(ItemStack stack) {
        return stack.is(Items.REDSTONE) ? 1 : stack.is(Items.GUNPOWDER) ? 2 : stack.is(Items.BLAZE_POWDER) ? 4 : 0;
    }

    @Override
    protected boolean isValidInSlot(int slot, ItemStack stack) {
        return slot == SLOT_ADDITIVE ? additiveValue(stack) > 0 : super.isValidInSlot(slot, stack);
    }

    @Override
    public IFluidHandler fuelHandler(Direction side) {
        return side == facing() ? null : inputs;
    }

    @Override
    protected void takeItems() {
        ItemStack a = items.getStackInSlot(SLOT_ADDITIVE);
        int value = additiveValue(a);
        if (value > 0 && additives < MAX_ADDITIVES) {
            items.extractItem(SLOT_ADDITIVE, 1, false);
            additives += value;
        }
    }

    @Override
    protected void onFuelBurned() {
        if (additives > 0 && level.random.nextInt(30) == 0) {
            additives--;
        }
    }

    @Override
    protected int waterAmount() {
        return water.getFluidAmount();
    }

    @Override
    protected int temperatureForDisplay() {
        return temperature();
    }

    @Override
    protected void afterTick(boolean running) {
        super.afterTick(running);
        if (++heatTicks >= 20) {
            heatTicks = 0;
            updateTemperature();
        }
    }

    private void updateTemperature() {
        int tAmb = Ambient.temperature(level, worldPosition);
        int t = Math.max(temperature(), tAmb);
        boolean on = omega > 0 && torque > 0;
        if (!on && t > tAmb) {
            int d = t - tAmb;
            t = d > 300 ? t - d / 100 : d > 100 ? t - d / 50 : d > 40 ? t - d / 10 : d > 4 ? t - d / 2 : tAmb;
        }
        if (on) {
            t++;
            if (!water.isEmpty() && t > tAmb) {
                water.drain(20, FluidTank.FluidAction.EXECUTE);
                t--;
            }
        }
        temperature = t;
        setChanged();
        if (t > MAX_TEMPERATURE) {
            overheat();
        }
    }

    private void overheat() {
        Level lvl = level;
        BlockPos pos = worldPosition;
        lvl.removeBlock(pos, false);
        lvl.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6F, true,
                RotaryConfig.get(RotaryConfig.EXPLOSIONS_BREAK_BLOCKS) ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
        int scrap = lvl.random.nextInt(28);
        for (int i = 0; i < scrap; i++) {
            Containers.dropItemStack(lvl, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(RotaryItems.SCRAP.get()));
        }
        lvl.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1, 1);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft.performance_engine");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FuelEngineMenu(RotaryMenus.PERFORMANCE_ENGINE.get(), id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.putInt("additives", additives);
        tag.putInt("temperature", temperature());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water.readFromNBT(registries, tag.getCompound("water"));
        additives = tag.getInt("additives");
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : Integer.MIN_VALUE;
    }
}
