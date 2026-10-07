package net.scwunge.rotarycraft.process;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.registry.ProcessRegistry;

/**
 * The Magnetic Motor, as the original's: Forge Energy in (on any side), shaft power out of its front, by the rules of
 * {@link EnergyConverterBlockEntity}. It holds forty ticks of what it uses.
 */
public class MagneticMotorBlockEntity extends EnergyConverterBlockEntity {
    private final IEnergyStorage energy = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return add(maxReceive, !simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return stored;
        }

        @Override
        public int getMaxEnergyStored() {
            return maxStorage();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    public MagneticMotorBlockEntity(BlockPos pos, BlockState state) {
        super(ProcessRegistry.MAGNETIC_MOTOR_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "magneticMotor";
    }

    public IEnergyStorage energy() {
        return energy;
    }

    @Override
    public int maxStorage() {
        return Math.max(1000, consumedPerTick() * 40);
    }

    @Override
    protected double idealUnitsPerTick(long power) {
        return (double) power / RotaryConfig.get(RotaryConfig.WATTS_PER_FE);
    }
}
