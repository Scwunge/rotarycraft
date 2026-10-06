package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/**
 * Wind Engine: 8 N*m at up to 1024 rad/s. Its blades sit on the back (input) side and need the 3x3 around them clear.
 * Speed falls with obstructions in a 32-block cone behind it, using the original's weighting (near rows count most).
 */
public class WindEngineBlockEntity extends EngineBlockEntity {
    public static final int TORQUE = 8;
    public static final int SPEED = 1024;
    private static final int SEARCH = 32;
    private static final int STEP_RATIO = 3;

    private final double[] blockFraction = new double[SEARCH];
    private boolean scanned;
    private int step = 1;
    private float penalty;

    public WindEngineBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.WIND_ENGINE.get(), pos, state);
    }

    @Override
    protected int ratedTorque() {
        return TORQUE;
    }

    /** Wind factor worked out in {@link #canRun()} this tick. */
    private float factor;

    @Override
    protected int targetSpeed() {
        return (int) (SPEED * factor);
    }

    @Override
    protected boolean canRun() {
        factor = 0;
        if (level == null || level.dimensionType().hasCeiling()) {
            return false;
        }
        Direction blades = inputSide();
        // the two directions spanning the blade plane
        Direction a = blades.getAxis() == Direction.Axis.Y ? Direction.NORTH : blades.getClockWise();
        Direction b = blades.getAxis() == Direction.Axis.Y ? Direction.EAST : Direction.UP;
        BlockPos center = worldPosition.relative(blades);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (!isSoft(level, center.relative(a, i).relative(b, j))) {
                    return false;
                }
            }
        }
        factor = windFactor();
        return factor > 0;
    }

    /** 1 with clear air behind the blades, less as the area fills up. Advances the rolling scan by one row. */
    public float windFactor() {
        if (level == null) {
            return 0;
        }
        if (!scanned) {
            for (int i = 1; i <= SEARCH; i++) {
                scanRow(i);
            }
            scanned = true;
            computePenalty();
        } else {
            scanRow(step);
            step = step % SEARCH + 1;
            computePenalty();
        }
        return Math.max(0, Math.min(1, 1 - penalty));
    }

    private void scanRow(int row) {
        Direction behind = inputSide();
        Direction side = perpendicular(behind);
        int r = 1 + (row - 1) / STEP_RATIO;
        int blocked = 0;
        BlockPos rowCenter = worldPosition.relative(behind, row);
        for (int i = -r; i <= r; i++) {
            if (!isSoft(level, rowCenter.relative(side, i))) {
                blocked++;
            }
        }
        blockFraction[row - 1] = blocked / (r * 2D + 1);
    }

    private void computePenalty() {
        double p = 0;
        double max = 0;
        for (int i = 0; i < SEARCH; i++) {
            double rowValue = Math.pow(1.2D - (i / (double) SEARCH), 12);
            p += rowValue * blockFraction[i];
            max += rowValue;
        }
        penalty = (float) Math.sqrt(p / Math.sqrt(max));
    }

    private static Direction perpendicular(Direction d) {
        return d.getAxis() == Direction.Axis.Y ? Direction.NORTH : d.getClockWise();
    }

    private static boolean isSoft(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return true;
        }
        BlockState s = level.getBlockState(pos);
        return s.isAir() || s.canBeReplaced();
    }
}
