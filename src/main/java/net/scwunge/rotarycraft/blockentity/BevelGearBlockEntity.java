package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.block.BevelGearBlock;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/** Turns a power line through any angle: power comes in on its INPUT side and leaves through FACING, unchanged. */
public class BevelGearBlockEntity extends PowerBlockEntity {
    public BevelGearBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.BEVEL_GEAR.get(), pos, state);
    }

    @Override
    public Direction inputSide() {
        return getBlockState().getValue(BevelGearBlock.INPUT);
    }

    @Override
    public void serverTick() {
        IShaftPowerOutput.Reading in = readInput();
        setPower(in.torque(), in.omega());
    }
}
