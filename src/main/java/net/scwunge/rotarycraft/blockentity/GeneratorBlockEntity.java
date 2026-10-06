package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.power.MachineEnergy;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/**
 * Turns shaft power into Forge Energy: power (W) / wattsPerFE FE every tick. The FE is pushed into any neighbour that
 * accepts it (every side except the shaft input) and can also be pulled out by cables.
 */
public class GeneratorBlockEntity extends PowerBlockEntity {
    private final MachineEnergy energy;

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.GENERATOR.get(), pos, state);
        int cap = RotaryConfig.get(RotaryConfig.GENERATOR_BUFFER);
        this.energy = new MachineEnergy(cap, 0, cap, this::setChanged);
    }

    public IEnergyStorage energy() {
        return energy;
    }

    /** FE made per tick at the current input. */
    public int fePerTick() {
        return (int) Math.min(Integer.MAX_VALUE, getPower() / RotaryConfig.get(RotaryConfig.WATTS_PER_FE));
    }

    @Override
    protected boolean outputsPower() {
        return false;
    }

    @Override
    public void serverTick() {
        energy.setCapacity(RotaryConfig.get(RotaryConfig.GENERATOR_BUFFER));
        IShaftPowerOutput.Reading in = readInput();
        setPower(in.torque(), in.omega());
        energy.generate(fePerTick());
        pushEnergy();
    }

    private void pushEnergy() {
        if (level == null || energy.getEnergyStored() <= 0) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (dir == inputSide()) {
                continue;
            }
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(dir), dir.getOpposite());
            if (target != null && target.canReceive()) {
                int offered = energy.extractEnergy(energy.getEnergyStored(), true);
                int accepted = target.receiveEnergy(offered, false);
                energy.extractEnergy(accepted, false);
                if (energy.getEnergyStored() <= 0) {
                    return;
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("energy", energy.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.get("energy") instanceof IntTag stored) {
            energy.deserializeNBT(registries, stored);
        }
    }
}
