package net.scwunge.rotarycraft.tool;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.config.RotaryConfig;

/** The steelToolsHarvestHigher option (as in the original): HSLA steel tools harvest any block of their kind softer than 20, whatever tool tier it asks for. */
public final class SteelTools {
    private SteelTools() {
    }

    public static boolean harvestsHigher(BlockState state, TagKey<Block> mineable) {
        return RotaryConfig.get(RotaryConfig.STEEL_HARVEST_HIGHER) && state.is(mineable) && state.getBlock().defaultDestroyTime() < 20;
    }
}
