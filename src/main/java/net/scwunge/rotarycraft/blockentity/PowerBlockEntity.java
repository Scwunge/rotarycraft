package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;

/**
 * Base for every shaft-power block. Power enters through the back (opposite the block's facing) and leaves through the
 * front (the facing). {@link #torque} and {@link #omega} are what this block currently delivers out of its front.
 */
public abstract class PowerBlockEntity extends BlockEntity implements IShaftPowerOutput {
    protected int torque;
    protected int omega;

    protected PowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(MachineBlock.FACING);
    }

    /** The direction from this block towards the block that feeds it. */
    public Direction inputSide() {
        return facing().getOpposite();
    }

    public int getTorque() {
        return torque;
    }

    public int getOmega() {
        return omega;
    }

    public long getPower() {
        return (long) torque * (long) omega;
    }

    /** True for blocks that pass power on out of their front. Consumers return false. */
    protected boolean outputsPower() {
        return true;
    }

    @Override
    public int getTorqueOut(Direction side) {
        return outputsPower() && side == facing() ? torque : 0;
    }

    @Override
    public int getOmegaOut(Direction side) {
        return outputsPower() && side == facing() ? omega : 0;
    }

    protected IShaftPowerOutput.Reading readInput() {
        return level == null ? IShaftPowerOutput.Reading.NONE : IShaftPowerOutput.readInput(level, worldPosition, inputSide());
    }

    protected void setPower(int torque, int omega) {
        if (torque <= 0 || omega <= 0) {
            torque = 0;
            omega = 0;
        }
        if (torque != this.torque || omega != this.omega) {
            this.torque = torque;
            this.omega = omega;
            setChanged();
        }
    }

    /** Runs once per server tick. */
    public abstract void serverTick();

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("torque", torque);
        tag.putInt("omega", omega);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        torque = tag.getInt("torque");
        omega = tag.getInt("omega");
    }
}
