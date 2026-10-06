package net.scwunge.rotarycraft.weapon.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.blockentity.ConsumerBlockEntity;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;

/**
 * A machine that takes shaft power from any side and adds it up, as the original's "summative sided power": the speed of the
 * fastest shaft, and the torque the total power makes at that speed. Clients are told the power now and then, for screens that
 * show what it allows.
 */
public abstract class OmniConsumerBlockEntity extends ConsumerBlockEntity {
    private int syncedTorque, syncedOmega;

    protected OmniConsumerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void serverTick() {
        long total = 0;
        int fastest = 0;
        for (Direction side : Direction.values()) {
            IShaftPowerOutput.Reading in = IShaftPowerOutput.readInput(level, worldPosition, side);
            total += (long) in.torque() * in.omega();
            fastest = Math.max(fastest, in.omega());
        }
        setPower(fastest == 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, total / fastest), fastest);
        if ((torque != syncedTorque || omega != syncedOmega) && level.getGameTime() % 20 == 0) {
            syncedTorque = torque;
            syncedOmega = omega;
            syncNow();
        }
        machineTick(hasEnoughPower());
    }

    protected final void syncNow() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    protected final void writePower(CompoundTag tag) {
        tag.putInt("torque", torque);
        tag.putInt("omega", omega);
    }

    protected final void readPower(CompoundTag tag) {
        torque = tag.getInt("torque");
        omega = tag.getInt("omega");
    }
}
