package net.scwunge.rotarycraft.process;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.blockentity.PipeBlockEntity;
import net.scwunge.rotarycraft.farm.FarmBlockEntity;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.ProcessRegistry;

/**
 * The Pipe Pump, as the original's: set between two pipes, it moves fluid from the one behind it into the one in front, up to a quarter of its
 * speed in millibuckets each tick, whatever the pipes would have done alone. It is turned by a shaft on any side that has no pipe.
 */
public class PipePumpBlockEntity extends FarmBlockEntity {
    public PipePumpBlockEntity(BlockPos pos, BlockState state) {
        super(ProcessRegistry.PIPE_PUMP_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "pipePump";
    }

    @Override
    protected boolean anySide() {
        return true;
    }

    @Override
    public PowerRequirement requirement() {
        return new PowerRequirement(0, 1, 1);
    }

    /** mB it can move each tick at the speed it is turned with. */
    public int rate() {
        return getOmega() / 4;
    }

    @Override
    protected int[] status() {
        return new int[] {rate()};
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!powered) {
            return;
        }
        Direction out = facing();
        if (level.getBlockEntity(worldPosition.relative(out.getOpposite())) instanceof PipeBlockEntity from
                && level.getBlockEntity(worldPosition.relative(out)) instanceof PipeBlockEntity to) {
            from.pumpInto(to, rate());
        }
    }
}
