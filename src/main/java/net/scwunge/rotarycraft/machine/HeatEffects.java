package net.scwunge.rotarycraft.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import org.jetbrains.annotations.Nullable;

/**
 * What heat does to the world around a hot machine (ReikaWorldHelper.temperatureEnvironment, the part the machines here use): from 0 degrees snow and ice
 * melt, from 100 water boils away, from 300 flammable blocks catch alight. Every change is asked of claim mods as the machine's owner first, and the block is
 * looked at before anyone is asked, so a machine that heats a lot of empty air asks nothing.
 */
public final class HeatEffects {
    public static final int BOIL = 100;
    public static final int IGNITE = 300;

    private HeatEffects() {
    }

    /** Whether a block at this temperature would be changed, without asking anyone. */
    public static boolean wouldAffect(ServerLevel level, BlockPos pos, int temperature) {
        BlockState state = level.getBlockState(pos);
        if (temperature > 0 && (state.is(BlockTags.SNOW) || state.is(BlockTags.ICE) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW))) {
            return true;
        }
        if (temperature >= BOIL && state.getFluidState().is(net.minecraft.tags.FluidTags.WATER) && state.getFluidState().isSource()) {
            return true;
        }
        return temperature >= IGNITE && canCatch(level, pos, state);
    }

    private static boolean canCatch(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.isAir()) {
            return false;
        }
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).isFlammable(level, pos.relative(dir), dir.getOpposite())) {
                return BaseFireBlock.canBePlacedAt(level, pos, Direction.NORTH);
            }
        }
        return false;
    }

    /** Heats the block at {@code pos}: true if it changed anything. */
    public static boolean affect(ServerLevel level, BlockPos pos, int temperature, @Nullable WorldGuard.Owner owner) {
        if (!level.isLoaded(pos) || !wouldAffect(level, pos, temperature) || !MachineGuard.mayChange(level, pos, owner)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (temperature > 0 && state.is(BlockTags.ICE)) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            return true;
        }
        if (temperature > 0 && (state.is(BlockTags.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return true;
        }
        if (temperature >= BOIL && state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.02);
            return true;
        }
        if (temperature >= IGNITE && state.isAir()) {
            level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
            return true;
        }
        return false;
    }
}
