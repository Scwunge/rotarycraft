package net.scwunge.rotarycraft.process;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.config.FarmConfig;
import net.scwunge.rotarycraft.config.RotaryConfig;

/**
 * The original's "energy to power" machines (the Magnetic Motor, Steam Turbine and Pneumatic Engine): they store some energy unit (FE, mB of
 * steam, mB of compressed air), and while they hold enough of it they spin up to their top speed, by 4 x log2 of it a tick, and turn that
 * speed and their torque out of their front; without it they coast down. Each tick at speed they use up
 * ceil(units for the power they make / efficiency) of the stored energy (half as much again while still spinning up), the efficiency being
 * 0.9 less 0.08 for each tier of the machine, times the machine's own factor. The tier (set in the farm config, as there are no upgrade items
 * yet) gives 8 x 4^tier N*m and up to 2^(8 + tier) rad/s.
 */
public abstract class EnergyConverterBlockEntity extends PowerBlockEntity {
    public static final int MAX_TIER = 5;

    protected int stored;
    private boolean switchedOn = true;

    protected EnergyConverterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract String switchName();

    /** How much of the stored energy the machine can keep. */
    public abstract int maxStorage();

    /** The units a tick of making {@code power} watts would take at perfect efficiency. */
    protected abstract double idealUnitsPerTick(long power);

    /** The efficiency of this kind of machine relative to the tier's. */
    protected double relativeEfficiency() {
        return 1;
    }

    public int tier() {
        return Math.max(0, Math.min(MAX_TIER, RotaryConfig.get(FarmConfig.CONVERTER_TIER)));
    }

    public int ratedTorque() {
        return 8 * (int) Math.pow(4, tier());
    }

    public int maxSpeed() {
        return 1 << (8 + tier());
    }

    public long ratedPower() {
        return (long) ratedTorque() * maxSpeed();
    }

    public double efficiency() {
        return (0.9 - 0.08 * tier()) * relativeEfficiency() * RotaryConfig.get(FarmConfig.CONVERTER_EFFICIENCY);
    }

    public int consumedPerTick() {
        return (int) Math.ceil(idealUnitsPerTick(ratedPower()) / efficiency());
    }

    public int stored() {
        return stored;
    }

    public boolean hasEnough() {
        return stored > consumedPerTick();
    }

    public boolean isSwitchedOn() {
        return switchedOn && FarmConfig.enabled(switchName());
    }

    protected final int add(int amount, boolean execute) {
        int add = Math.max(0, Math.min(amount, maxStorage() - stored));
        if (execute && add > 0) {
            stored += add;
            setChanged();
        }
        return add;
    }

    @Override
    public void serverTick() {
        int max = maxSpeed();
        boolean accelerate = isSwitchedOn() && hasEnough() && omega <= max;
        float multiplier = 1;
        int w = omega;
        if (accelerate) {
            if (w < max) {
                multiplier = 1.5F;
            }
            w += (int) (4 * (Math.log(max + 1) / Math.log(2)));
            w = Math.min(w, max);
        } else if (w > 0) {
            w -= w / 256 + 1;
        }
        w = Math.max(0, w);
        setPower(w > 0 ? ratedTorque() : 0, w);
        if (w > 0) {
            stored = Math.max(0, stored - (int) (consumedPerTick() * multiplier));
            setChanged();
        }
        afterTick();
    }

    /** Anything else the machine does each tick. */
    protected void afterTick() {
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("stored", stored);
        tag.putBoolean("on", switchedOn);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stored = tag.getInt("stored");
        switchedOn = !tag.contains("on") || tag.getBoolean("on");
    }
}
