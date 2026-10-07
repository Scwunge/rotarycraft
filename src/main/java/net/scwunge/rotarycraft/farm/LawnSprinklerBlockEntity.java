package net.scwunge.rotarycraft.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.scwunge.rotarycraft.config.FarmConfig;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.registry.FarmRegistry;

/**
 * The Lawn Sprinkler, as the original: a spinning head that sits on the ground and waters a patch picked at random out to its range, three
 * tries a tick (nearer patches likelier: 8, 5, 3 and 1 parts for 1 to 4 blocks, and 1 part for the full range). Each try
 * hydrates farmland, gives crops and growing plants a growth tick one time in eight, and puts out a fire. It holds only 5 mB and uses
 * 1 mB a tick. Over 0.8 MPa of pressure the spray hurts the creatures it reaches (a half heart a tick, four times that over 2 MPa) unless the
 * config says no.
 */
public class LawnSprinklerBlockEntity extends SprinklerBaseBlockEntity {
    public static final int CAPACITY = 5;
    public static final int CONSUMPTION = 1;
    private static final int PRESSURE_MODIFIER = 10;
    public static final int PRESSURE_TO_HURT = 8_000_000 / PRESSURE_MODIFIER;
    public static final int PRESSURE_TO_KILL = 20_000_000 / PRESSURE_MODIFIER;
    /** The weights of a spray reaching 1 to 4 blocks out, and of the full range. */
    private static final int[] GROWTH_PATTERN = {8, 5, 3, 1, 1};

    public LawnSprinklerBlockEntity(BlockPos pos, BlockState state) {
        super(FarmRegistry.LAWN_SPRINKLER_BE.get(), pos, state, CAPACITY);
    }

    @Override
    protected String switchName() {
        return "lawnSprinkler";
    }

    @Override
    public int waterConsumption() {
        return CONSUMPTION;
    }

    @Override
    public Direction pipeSide() {
        return Direction.DOWN;
    }

    private int reach(ServerLevel server) {
        int total = 0;
        for (int w : GROWTH_PATTERN) {
            total += w;
        }
        int roll = server.random.nextInt(total);
        for (int i = 0; i < GROWTH_PATTERN.length - 1; i++) {
            roll -= GROWTH_PATTERN[i];
            if (roll < 0) {
                return i + 1;
            }
        }
        return range();
    }

    @Override
    protected void performEffects() {
        ServerLevel server = server();
        int range = range();
        for (int n = 0; n < 3; n++) {
            accelerateGrowth(server, Math.min(reach(server), range));
            extinguishFire(server, range);
        }
        if (server.getGameTime() % 2 == 0) {
            for (int i = 0; i < Math.round(3 * net.scwunge.rotarycraft.config.RotaryConfig.get(net.scwunge.rotarycraft.config.RotaryConfig.SPRINKLER_PARTICLES) / 4F); i++) {
                double angle = Math.toRadians(server.getGameTime() * 9 + i * 120);
                double v = range * 0.1 * (0.5 + server.random.nextDouble());
                server.sendParticles(ParticleTypes.SPLASH, worldPosition.getX() + 0.5 + 0.6 * Math.sin(angle), worldPosition.getY() + 0.75,
                        worldPosition.getZ() + 0.5 + 0.6 * Math.cos(angle), 0, Math.sin(angle) * v, 0.05, Math.cos(angle) * v, 1);
            }
        }
        if (pressure() > PRESSURE_TO_HURT && RotaryConfig.get(FarmConfig.LAWN_SPRINKLER_HURTS)) {
            damageMobs(server, range, pressure() >= PRESSURE_TO_KILL ? 4 : 1);
        }
    }

    private void accelerateGrowth(ServerLevel server, int r) {
        int rx = worldPosition.getX() + server.random.nextInt(2 * r + 1) - r;
        int rz = worldPosition.getZ() + server.random.nextInt(2 * r + 1) - r;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(rx, worldPosition.getY(), rz);
        for (int i = 0; i < 4; i++, at.move(Direction.DOWN)) {
            if (!server.isLoaded(at)) {
                return;
            }
            BlockState state = server.getBlockState(at);
            if (state.is(Blocks.FARMLAND)) {
                Crops.hydrateFarmland(server, at);
                return;
            }
            if (!state.isAir() && state.canOcclude() && !state.is(net.minecraft.tags.BlockTags.LEAVES)) {
                return;
            }
            if (server.random.nextInt(8) == 0 && Crops.isGrowing(state)) {
                Crops.forceTick(server, at);
            }
        }
    }

    private void extinguishFire(ServerLevel server, int r) {
        int rx = worldPosition.getX() + server.random.nextInt(2 * r + 1) - r;
        int rz = worldPosition.getZ() + server.random.nextInt(2 * r + 1) - r;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(rx, worldPosition.getY(), rz);
        for (int i = 0; i < 4; i++, at.move(Direction.DOWN)) {
            if (!server.isLoaded(at)) {
                return;
            }
            BlockState state = server.getBlockState(at);
            if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                if (!server.getBlockState(at.below()).is(Blocks.NETHERRACK)) {
                    server.removeBlock(at, false);
                    server.playSound(null, at, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1F);
                }
                return;
            }
            if (!state.isAir() && state.canOcclude() && !state.is(net.minecraft.tags.BlockTags.LEAVES)) {
                return;
            }
        }
    }

    private void damageMobs(ServerLevel server, int range, int damage) {
        AABB box = new AABB(worldPosition).inflate(range, 0, range).expandTowards(0, 2, 0);
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, box)) {
            var hit = server.clip(new ClipContext(worldPosition.getCenter(), e.position().add(0, 0.5, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e));
            if (hit.getType() == HitResult.Type.MISS) {
                e.hurt(server.damageSources().drown(), 0.5F * damage);
            }
        }
    }
}
