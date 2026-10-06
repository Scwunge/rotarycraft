package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.ChunkLoaderBlockEntity;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.registry.WorldMachineRegistry;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class WorldMachineGameTests {
    static final String TEMPLATE = RotaryGameTests.TEMPLATE;
    static final BlockPos MACHINE = new BlockPos(2, 2, 2);

    /** The slowest a chunk loader runs, with one more chunk of reach per 524,288 W above it. */
    static final int MIN = ChunkLoaderBlockEntity.MIN_SPEED;

    /** Turns a world machine's config switch on for a test; the returned runnable puts it back. */
    static Runnable enable(String name) {
        ModConfigSpec.BooleanValue value = RotaryConfig.WORLD_MACHINES.get(name);
        boolean before = value.get();
        value.set(true);
        return () -> value.set(before);
    }

    static ChunkLoaderBlockEntity chunkLoader(GameTestHelper helper, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), torque, omega);
        helper.setBlock(MACHINE, WorldMachineRegistry.CHUNK_LOADER.get().defaultBlockState());
        return helper.getBlockEntity(MACHINE);
    }

    @GameTest(template = TEMPLATE, batch = "world_chunkloaderradiusfollowspower", timeoutTicks = 40)
    public static void chunkLoaderRadiusFollowsPower(GameTestHelper helper) {
        helper.assertTrue(ChunkLoaderBlockEntity.radiusFor(MIN, 8) == 0, "minimum power should load only its own chunk");
        helper.assertTrue(ChunkLoaderBlockEntity.radiusFor(MIN + 524_288L, 8) == 1, "one step more should add a chunk of reach");
        helper.assertTrue(ChunkLoaderBlockEntity.radiusFor(MIN + 3 * 524_288L - 1, 8) == 2, "a step is not reached until it is all there");
        helper.assertTrue(ChunkLoaderBlockEntity.radiusFor(Long.MAX_VALUE / 4, 8) == 8, "radius should stop at the configured maximum");
        helper.assertTrue(ChunkLoaderBlockEntity.radiusFor(1, 8) == 0, "power under the minimum should not go negative");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "world_chunkloaderisoffbydefault", timeoutTicks = 60)
    public static void chunkLoaderIsOffByDefault(GameTestHelper helper) {
        ChunkLoaderBlockEntity loader = chunkLoader(helper, 1, MIN + 524_288);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(loader.hasEnoughPower(), "the loader should be turning");
            helper.assertTrue(loader.loadedChunks() == 0, "a disabled chunk loader held " + loader.loadedChunks() + " chunks");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "world_chunkloaderholdsawidersquarewithmorepower", timeoutTicks = 60)
    public static void chunkLoaderHoldsAWiderSquareWithMorePower(GameTestHelper helper) {
        Runnable restore = enable("chunkLoader");
        ChunkLoaderBlockEntity loader = chunkLoader(helper, 1, MIN + 524_288);
        helper.runAfterDelay(10, () -> {
            try {
                helper.assertTrue(loader.loadedChunks() == 9, "one chunk of reach should hold 3x3 chunks, held " + loader.loadedChunks());
                helper.succeed();
            } finally {
                helper.setBlock(MACHINE, Blocks.AIR);
                restore.run();
            }
        });
    }

    @GameTest(template = TEMPLATE, batch = "world_chunkloaderholdsitsownchunkatminimumspeed", timeoutTicks = 60)
    public static void chunkLoaderHoldsItsOwnChunkAtMinimumSpeed(GameTestHelper helper) {
        Runnable restore = enable("chunkLoader");
        ChunkLoaderBlockEntity loader = chunkLoader(helper, 1, MIN);
        helper.runAfterDelay(10, () -> {
            try {
                helper.assertTrue(loader.loadedChunks() == 1, "should hold only its own chunk, held " + loader.loadedChunks());
                helper.succeed();
            } finally {
                helper.setBlock(MACHINE, Blocks.AIR);
                restore.run();
            }
        });
    }

    @GameTest(template = TEMPLATE, batch = "world_chunkloaderletsgowhenitslowsdown", timeoutTicks = 80)
    public static void chunkLoaderLetsGoWhenItSlowsDown(GameTestHelper helper) {
        Runnable restore = enable("chunkLoader");
        ChunkLoaderBlockEntity loader = chunkLoader(helper, 1, MIN);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(loader.loadedChunks() == 1, "should have loaded its chunk");
            WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), 1, 1000);
        });
        helper.runAfterDelay(25, () -> {
            try {
                helper.assertTrue(loader.loadedChunks() == 0, "a slow chunk loader still held " + loader.loadedChunks() + " chunks");
                helper.succeed();
            } finally {
                helper.setBlock(MACHINE, Blocks.AIR);
                restore.run();
            }
        });
    }

    @GameTest(template = TEMPLATE, batch = "world_chunkloaderletsgowhenbroken", timeoutTicks = 60)
    public static void chunkLoaderLetsGoWhenBroken(GameTestHelper helper) {
        Runnable restore = enable("chunkLoader");
        ChunkLoaderBlockEntity loader = chunkLoader(helper, 1, MIN + 524_288);
        helper.runAfterDelay(10, () -> {
            try {
                helper.assertTrue(loader.loadedChunks() == 9, "should have loaded its square");
                helper.setBlock(MACHINE, Blocks.AIR);
                helper.assertTrue(loader.loadedChunks() == 0, "a broken chunk loader still held " + loader.loadedChunks() + " chunks");
                helper.succeed();
            } finally {
                restore.run();
            }
        });
    }
}
