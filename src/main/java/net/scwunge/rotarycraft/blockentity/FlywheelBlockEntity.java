package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.block.FlywheelBlock;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.FlywheelType;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/**
 * Stores rotation. With enough input torque (a quarter of its rating) it spins up at torque / inertia rad/s per tick to the
 * input speed and passes on up to its rated torque. When the input stops it keeps turning and coasts down by 1 rad/s every
 * decay interval, so it smooths out an unsteady supply. Spun past its material's strength it bursts.
 */
public class FlywheelBlockEntity extends PowerBlockEntity {
    /** Fractional speed change carried between ticks (the speed itself is a whole number). */
    private double speedRemainder;
    private int decayTimer;

    public FlywheelBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.FLYWHEEL.get(), pos, state);
    }

    public FlywheelType type() {
        return getBlockState().getBlock() instanceof FlywheelBlock fb ? fb.type() : FlywheelType.WOOD;
    }

    @Override
    public void serverTick() {
        FlywheelType type = type();
        IShaftPowerOutput.Reading in = readInput();
        int w = omega;
        int t = torque;
        if (in.torque() >= type.minTorque() && in.omega() > 0) {
            decayTimer = 0;
            double accel = in.torque() / type.inertia;
            if (w < in.omega()) {
                speedRemainder += accel;
                int step = (int) speedRemainder;
                speedRemainder -= step;
                w = Math.min(in.omega(), w + step);
            } else if (w > in.omega()) {
                speedRemainder += accel;
                int step = (int) speedRemainder;
                speedRemainder -= step;
                w = Math.max(in.omega(), w - step);
            }
            t = Math.min(in.torque(), type.maxTorque);
        } else if (w > 0) {
            // no usable input: coast down
            if (++decayTimer >= type.decayTicks) {
                decayTimer = 0;
                w--;
            }
        }
        if (w <= 0) {
            w = 0;
            t = 0;
        }
        if (RotaryConfig.get(RotaryConfig.SHAFT_FAILURE) && type.fails(w)) {
            burst(type, w);
            return;
        }
        setPower(t, w);
    }

    private void burst(FlywheelType type, int w) {
        Level lvl = level;
        BlockPos pos = worldPosition;
        // explosion strength grows with the stored energy, capped like a large TNT blast
        float power = (float) Math.max(1, Math.min(8, Math.cbrt(type.energyAt(w) / 1e6)));
        lvl.removeBlock(pos, false);
        lvl.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power,
                RotaryConfig.get(RotaryConfig.EXPLOSIONS_BREAK_BLOCKS) ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("speedRemainder", speedRemainder);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        speedRemainder = tag.getDouble("speedRemainder");
    }
}
