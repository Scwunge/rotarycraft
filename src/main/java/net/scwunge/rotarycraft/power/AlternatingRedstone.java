package net.scwunge.rotarycraft.power;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Detects "AC" redstone the way the original does: the signal counts as alternating while it differs from any of its
 * last three readings, so a clock toggling at least every three ticks keeps it on.
 */
public class AlternatingRedstone {
    private final boolean[] last = new boolean[3];
    private boolean alternating;
    private boolean integrated;

    /** The redstone upgrade: a clock of the machine's own, so it needs no signal from outside. */
    public void addIntegrated() {
        integrated = true;
    }

    public boolean hasIntegrated() {
        return integrated;
    }

    /** Call once per tick. */
    public boolean update(Level level, BlockPos pos) {
        if (integrated) {
            alternating = true;
            return true;
        }
        boolean now = level.hasNeighborSignal(pos);
        boolean ac = false;
        for (boolean b : last) {
            if (b != now) {
                ac = true;
                break;
            }
        }
        System.arraycopy(last, 0, last, 1, last.length - 1);
        last[0] = now;
        alternating = ac;
        return ac;
    }

    public boolean isAlternating() {
        return alternating;
    }
}
