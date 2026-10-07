package net.scwunge.rotarycraft.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.registry.FarmRegistry;

/**
 * The Sprinkler, as the original: it takes water (180 mB held, 3 mB each tick it works) and the pressure of the pipe above it, and sprays a
 * square of ground out to its range (8 blocks at most). In each column it waters crops (a growth tick, now and then), wets farmland
 * through, and puts out fires; the spray is twice as strong in the middle as at the edge. The original visits every column every tick; this
 * visits some at random and makes up the chance, so the rates are the same.
 */
public class SprinklerBlockEntity extends SprinklerBaseBlockEntity {
    public static final int CAPACITY = 180;
    public static final int CONSUMPTION = 3;
    private static final int DEPTH = 12;
    private static final int COLUMNS_PER_TICK = 17;

    /** The original's chances (one in) for each thing the spray does to a column. */
    private static final int FIRE = 20, CROP = 80, FARMLAND = 15;

    public SprinklerBlockEntity(BlockPos pos, BlockState state) {
        super(FarmRegistry.SPRINKLER_BE.get(), pos, state, CAPACITY);
    }

    @Override
    protected String switchName() {
        return "sprinkler";
    }

    @Override
    public int waterConsumption() {
        return CONSUMPTION;
    }

    @Override
    public Direction pipeSide() {
        return Direction.UP;
    }

    @Override
    protected void performEffects() {
        ServerLevel server = server();
        int range = range();
        if (server.getGameTime() % 3 == 0) {
            server.sendParticles(ParticleTypes.RAIN, worldPosition.getX() + 0.5, worldPosition.getY() + 0.4, worldPosition.getZ() + 0.5,
                    Math.round(6 * net.scwunge.rotarycraft.config.RotaryConfig.get(net.scwunge.rotarycraft.config.RotaryConfig.SPRINKLER_PARTICLES) / 4F), range / 3.0, 0.2, range / 3.0, 0.1);
        }
        for (int n = 0; n < COLUMNS_PER_TICK; n++) {
            int i = server.random.nextInt(2 * range + 1) - range;
            int k = server.random.nextInt(2 * range + 1) - range;
            float factor = 0.5F + 1.5F * (i * i + k * k) / (float) (range * range); // 2x the rate in the middle, 0.5x at the edge
            spray(server, worldPosition.getX() + i, worldPosition.getZ() + k, factor);
        }
    }

    /** One chance in {@code in}, made likelier by the number of columns each tick leaves out. */
    private static boolean chance(ServerLevel server, int in, float factor) {
        return server.random.nextInt(Math.max(1, (int) (in * factor))) < COLUMNS_PER_TICK;
    }

    private void spray(ServerLevel server, int x, int z, float factor) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, worldPosition.getY(), z);
        for (int dy = 0; dy < DEPTH && at.getY() > server.getMinBuildHeight(); dy++, at.move(Direction.DOWN)) {
            if (!server.isLoaded(at)) {
                return;
            }
            BlockState state = server.getBlockState(at);
            if (state.isAir()) {
                continue;
            }
            if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                if (chance(server, FIRE, factor)) {
                    server.playSound(null, at, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 0.5F + 0.5F * server.random.nextFloat());
                    server.removeBlock(at, false);
                }
                return;
            }
            if (state.is(Blocks.FARMLAND)) {
                if (chance(server, FARMLAND, factor)) {
                    Crops.hydrateFarmland(server, at);
                }
                return;
            }
            if (Crops.isGrowing(state)) {
                if (chance(server, CROP, factor)) {
                    Crops.forceTick(server, at);
                }
                if (Crops.isStackPlant(state)) {
                    return;
                }
                continue;
            }
            if (state.canOcclude() && !state.is(net.minecraft.tags.BlockTags.LEAVES)) {
                return;
            }
        }
    }
}
