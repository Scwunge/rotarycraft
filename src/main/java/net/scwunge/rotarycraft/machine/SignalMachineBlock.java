package net.scwunge.rotarycraft.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** A layout machine that gives a redstone signal on every side: what its block entity says in {@link MachineInteractions#redstoneOutput()} (the detectors). */
public class SignalMachineBlock extends LayoutMachineBlock {
    public SignalMachineBlock(Properties props, Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type,
                              BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory) {
        super(props, type, factory);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof MachineInteractions machine ? Math.max(0, Math.min(15, machine.redstoneOutput())) : 0;
    }
}
