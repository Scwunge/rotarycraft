package net.scwunge.rotarycraft.handheld;

import net.minecraft.core.BlockPos;

/** A machine that takes the places a Tile Selector points at (the original's SelectableTiles). */
public interface SelectableTiles {
    /** Adds the place at {@code pos} to the machine's selection; whether it was taken. */
    boolean addTile(BlockPos pos);

    /** What to call it when telling the player. */
    String selectionName();
}
