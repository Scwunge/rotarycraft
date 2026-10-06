package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.MachineEnergy;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/**
 * Turns Forge Energy into shaft power: while it has enough FE it delivers the configured torque and speed out of its
 * front, using (torque x speed) / wattsPerFE FE per tick. Accepts FE on every side.
 */
public class MotorBlockEntity extends PowerBlockEntity {
    private final MachineEnergy energy;

    public MotorBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.ELECTRIC_MOTOR.get(), pos, state);
        int cap = RotaryConfig.get(RotaryConfig.MOTOR_BUFFER);
        this.energy = new MachineEnergy(cap, cap, 0, this::setChanged);
    }

    public IEnergyStorage energy() {
        return energy;
    }

    public static int fePerTick() {
        long watts = (long) RotaryConfig.get(RotaryConfig.MOTOR_TORQUE) * RotaryConfig.get(RotaryConfig.MOTOR_OMEGA);
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, watts / RotaryConfig.get(RotaryConfig.WATTS_PER_FE)));
    }

    @Override
    public void serverTick() {
        energy.setCapacity(RotaryConfig.get(RotaryConfig.MOTOR_BUFFER));
        if (energy.consume(fePerTick())) {
            setPower(RotaryConfig.get(RotaryConfig.MOTOR_TORQUE), RotaryConfig.get(RotaryConfig.MOTOR_OMEGA));
        } else {
            setPower(0, 0);
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
