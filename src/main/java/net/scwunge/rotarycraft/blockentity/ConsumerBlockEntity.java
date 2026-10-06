package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.power.PowerRequirement;

/**
 * A machine driven by shaft power: it reads the torque and speed arriving at its back every tick and runs only while they
 * meet its {@link #requirement()}.
 */
public abstract class ConsumerBlockEntity extends PowerBlockEntity {
    protected ConsumerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract PowerRequirement requirement();

    public boolean hasEnoughPower() {
        return requirement().isMetBy(torque, omega);
    }

    @Override
    protected boolean outputsPower() {
        return false;
    }

    @Override
    public void serverTick() {
        IShaftPowerOutput.Reading in = readInput();
        setPower(in.torque(), in.omega());
        machineTick(hasEnoughPower());
    }

    /** One tick of the machine's work; {@code powered} is whether the input meets the requirement. */
    protected abstract void machineTick(boolean powered);
}
