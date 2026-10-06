package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/**
 * Steam Engine: 32 N*m at 512 rad/s. Needs water (fill it with buckets or pipes) and heat from fire or lava directly below.
 * Heats 1 C per second over fire and 2 C over lava, runs from 100 C, and overheats above 150 C: with water left it bursts,
 * dry it melts into lava. Running dry while hot and then getting water makes it explode. Values from the original.
 */
public class SteamEngineBlockEntity extends EngineBlockEntity implements Heatable {
    public static final int TORQUE = 32;
    public static final int SPEED = 512;
    public static final int WATER_CAPACITY = 60_000;
    public static final int MAX_TEMPERATURE = 150;
    private static final int FUEL_UNIT_TICKS = 18;

    private final FluidTank water = new FluidTank(WATER_CAPACITY, s -> s.is(FluidTags.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private int temperature = Integer.MIN_VALUE;
    private int dryTicks;
    private int tickCount;

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.STEAM_ENGINE.get(), pos, state);
    }

    public FluidTank water() {
        return water;
    }

    public int temperature() {
        return temperature == Integer.MIN_VALUE ? ambient() : temperature;
    }

    @Override
    protected int ratedTorque() {
        return TORQUE;
    }

    @Override
    protected int targetSpeed() {
        return SPEED;
    }

    @Override
    protected boolean canRun() {
        return temperature() >= 100 && !water.isEmpty();
    }

    @Override
    protected void afterTick(boolean running) {
        tickCount++;
        if (tickCount % 20 == 0) {
            updateTemperature();
            if (level == null || level.getBlockEntity(worldPosition) != this) {
                return; // blew up or melted
            }
        }
        if (running && tickCount % FUEL_UNIT_TICKS == 0) {
            water.drain(waterPerUnit(), FluidTank.FluidAction.EXECUTE);
        }
        if (water.isEmpty() && temperature() >= 100) {
            dryTicks++;
        } else {
            if (dryTicks > 900 && !water.isEmpty()) {
                explode(6);
                return;
            }
            dryTicks = 0;
        }
    }

    private int waterPerUnit() {
        int t = temperature();
        int amt = t >= 130 ? 75 : t >= 125 ? 60 : t >= 120 ? 50 : t >= 110 ? 25 : 10;
        return isOver(true) ? amt * 4 : amt;
    }

    private boolean isOver(boolean lava) {
        if (level == null) {
            return false;
        }
        BlockState below = level.getBlockState(worldPosition.below());
        return lava ? below.getFluidState().is(FluidTags.LAVA) : below.getBlock() instanceof BaseFireBlock;
    }

    private int ambient() {
        return Ambient.temperature(level, worldPosition);
    }

    private void updateTemperature() {
        int tAmb = ambient();
        if (temperature == Integer.MIN_VALUE) {
            temperature = tAmb;
        }
        boolean fire = isOver(false);
        boolean lava = isOver(true);
        if (fire) {
            temperature++;
            if (level.dimensionType().ultraWarm()) {
                temperature++;
            }
        }
        if (lava) {
            temperature += 2;
        }
        if (tAmb < 0 && fire) {
            tAmb += 30;
        }
        if (temperature < tAmb) {
            temperature += Math.max((tAmb - temperature) / 40, 1);
        }
        if (!fire && !lava && temperature > tAmb) {
            temperature--;
        }
        if (temperature > tAmb) {
            temperature -= (temperature - tAmb) / 96;
        }
        setChanged();
        if (temperature > MAX_TEMPERATURE) {
            overheat();
        }
    }

    private void overheat() {
        Level lvl = level;
        if (water.isEmpty()) {
            lvl.setBlockAndUpdate(worldPosition, Blocks.LAVA.defaultBlockState());
            lvl.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 2, 1);
            return;
        }
        explode(2);
    }

    private void explode(float power) {
        Level lvl = level;
        BlockPos pos = worldPosition;
        lvl.removeBlock(pos, false);
        lvl.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power,
                RotaryConfig.get(RotaryConfig.EXPLOSIONS_BREAK_BLOCKS) ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
    }

    /** Test hook: fill with water. */
    public void fillWater(int mb) {
        water.fill(new FluidStack(Fluids.WATER, mb), FluidTank.FluidAction.EXECUTE);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.putInt("temperature", temperature());
        tag.putInt("dryTicks", dryTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water.readFromNBT(registries, tag.getCompound("water"));
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : Integer.MIN_VALUE;
        dryTicks = tag.getInt("dryTicks");
    }

    @Override
    public int getTemperature() {
        return temperature();
    }

    @Override
    public int getMaxTemperature() {
        return 150;
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
