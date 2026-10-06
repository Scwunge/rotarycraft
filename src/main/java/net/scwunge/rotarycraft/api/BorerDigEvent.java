package net.scwunge.rotarycraft.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.Event;

/** Posted on the NeoForge event bus each time a Borer finishes cutting a slice (the original's API event). */
public class BorerDigEvent extends Event {
    private final BlockEntity borer;
    private final int step;
    private final BlockPos head;
    private final boolean silkTouch;

    public BorerDigEvent(BlockEntity borer, int step, BlockPos head, boolean silkTouch) {
        this.borer = borer;
        this.step = step;
        this.head = head;
        this.silkTouch = silkTouch;
    }

    public BlockEntity borer() {
        return borer;
    }

    /** How many slices out from the borer's face the cut was. */
    public int step() {
        return step;
    }

    /** Where the middle of the slice's bottom row is. */
    public BlockPos head() {
        return head;
    }

    public boolean silkTouch() {
        return silkTouch;
    }
}
