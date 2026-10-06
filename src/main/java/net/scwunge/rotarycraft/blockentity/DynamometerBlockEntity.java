package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/**
 * Passes power through unchanged and measures it: right-click with the meter for the numbers, or read it with a comparator
 * (signal strength grows with the logarithm of the power: 1 kW is about 5, 1 MW about 10).
 */
public class DynamometerBlockEntity extends PowerBlockEntity {
    private int lastSignal;

    public DynamometerBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.DYNAMOMETER.get(), pos, state);
    }

    public static int signalFor(long power) {
        if (power <= 0) {
            return 0;
        }
        // log2(power) / 2, so each step is a factor of 4; 1 W gives 1
        return (int) Math.min(15, Math.max(1, (63 - Long.numberOfLeadingZeros(power)) / 2 + 1));
    }

    public int comparatorSignal() {
        return signalFor(getPower());
    }

    @Override
    public void serverTick() {
        IShaftPowerOutput.Reading in = readInput();
        setPower(in.torque(), in.omega());
        int signal = comparatorSignal();
        if (signal != lastSignal && level != null) {
            lastSignal = signal;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }
}
