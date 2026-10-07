package net.scwunge.rotarycraft.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

/** What the farming machines mean by a crop or a growing plant, for the world they all work in. */
public final class Crops {
    private Crops() {}

    /** A plant that grows by random ticks (crops, saplings, stems, cane, cactus, wart, bamboo, vines, berries). */
    public static boolean isGrowing(BlockState state) {
        var block = state.getBlock();
        return block instanceof CropBlock || block instanceof BonemealableBlock && !state.is(net.minecraft.tags.BlockTags.DIRT) && !(block instanceof net.minecraft.world.level.block.GrassBlock)
                || block instanceof SugarCaneBlock || block instanceof CactusBlock || block instanceof NetherWartBlock || block instanceof BambooStalkBlock
                || block instanceof VineBlock || block instanceof SweetBerryBushBlock;
    }

    /** A plant that grows upwards in a stack, so a machine's water or spray stops there (the original's cane and cactus). */
    public static boolean isStackPlant(BlockState state) {
        return state.getBlock() instanceof SugarCaneBlock || state.getBlock() instanceof CactusBlock;
    }

    /** Whether the plant is fully grown. */
    public static boolean isRipe(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (state.getBlock() instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= 3;
        }
        return false;
    }

    /** Gives the block a random tick whatever the light, as the original's forced block tick. */
    public static void forceTick(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        state.randomTick(level, pos, level.random);
    }

    /** Wets farmland all the way; whether it changed anything. */
    public static boolean hydrateFarmland(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof FarmBlock && state.getValue(FarmBlock.MOISTURE) < 7) {
            level.setBlock(pos, state.setValue(FarmBlock.MOISTURE, 7), 2);
            return true;
        }
        return false;
    }
}
