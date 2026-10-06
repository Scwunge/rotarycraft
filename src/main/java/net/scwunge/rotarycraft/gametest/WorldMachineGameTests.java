package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.ChunkLoaderBlockEntity;
import net.scwunge.rotarycraft.blockentity.WeatherControllerBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryItems;
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

    // ---- Weather Controller ----

    /** A controller under open sky with plenty of power, in clear weather; the config switch is the caller's business. */
    static WeatherControllerBlockEntity weatherController(GameTestHelper helper) {
        helper.getLevel().setWeatherParameters(100000, 0, false, false);
        WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), 256, 256);
        helper.setBlock(MACHINE, WorldMachineRegistry.WEATHER_CONTROLLER.get().defaultBlockState());
        return helper.getBlockEntity(MACHINE);
    }

    static void finishWeather(GameTestHelper helper, Runnable restore) {
        helper.setBlock(MACHINE, Blocks.AIR);
        helper.getLevel().setWeatherParameters(100000, 0, false, false);
        restore.run();
    }

    @GameTest(template = TEMPLATE, batch = "world_weatherisoffbydefault", timeoutTicks = 80)
    public static void weatherControllerIsOffByDefault(GameTestHelper helper) {
        WeatherControllerBlockEntity controller = weatherController(helper);
        controller.items().setStackInSlot(0, new ItemStack(WorldMachineRegistry.SILVER_IODIDE.get(), 4));
        helper.runAfterDelay(30, () -> {
            try {
                helper.assertFalse(helper.getLevel().getLevelData().isRaining(), "a disabled weather controller made rain");
                helper.assertTrue(controller.items().getStackInSlot(0).getCount() == 4, "a disabled weather controller used its items");
                helper.succeed();
            } finally {
                finishWeather(helper, () -> {});
            }
        });
    }

    @GameTest(template = TEMPLATE, batch = "world_weatherrains", timeoutTicks = 120)
    public static void weatherControllerRainsWithSilverIodide(GameTestHelper helper) {
        Runnable restore = enable("weatherController");
        WeatherControllerBlockEntity controller = weatherController(helper);
        controller.items().setStackInSlot(0, new ItemStack(WorldMachineRegistry.SILVER_IODIDE.get(), 4));
        helper.runAfterDelay(30, () -> {
            try {
                helper.assertTrue(controller.hasEnoughPower(), "the controller has no power");
                helper.assertTrue(helper.getLevel().getLevelData().isRaining(), "no rain");
                helper.assertFalse(helper.getLevel().getLevelData().isThundering(), "silver iodide alone should not thunder");
                helper.assertTrue(controller.items().getStackInSlot(0).getCount() == 3, "should use one silver iodide, left " + controller.items().getStackInSlot(0).getCount());
                net.minecraft.world.phys.AABB above = new net.minecraft.world.phys.AABB(helper.absolutePos(MACHINE)).inflate(16, 256, 16);
                helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, above).isEmpty(), "no spent iodide shot into the air");
                helper.succeed();
            } finally {
                finishWeather(helper, restore);
            }
        });
    }

    @GameTest(template = TEMPLATE, batch = "world_weatherthunders", timeoutTicks = 120)
    public static void weatherControllerThundersWithRedstone(GameTestHelper helper) {
        Runnable restore = enable("weatherController");
        WeatherControllerBlockEntity controller = weatherController(helper);
        controller.items().setStackInSlot(0, new ItemStack(WorldMachineRegistry.SILVER_IODIDE.get(), 2));
        controller.items().setStackInSlot(1, new ItemStack(Items.REDSTONE, 2));
        helper.runAfterDelay(30, () -> {
            try {
                helper.assertTrue(helper.getLevel().getLevelData().isRaining() && helper.getLevel().getLevelData().isThundering(), "no thunderstorm");
                helper.assertTrue(controller.items().getStackInSlot(0).getCount() == 1 && controller.items().getStackInSlot(1).getCount() == 1,
                        "should use one of each");
                helper.assertTrue(controller.mode() == WeatherControllerBlockEntity.Mode.THUNDER, "mode " + controller.mode());
                helper.succeed();
            } finally {
                finishWeather(helper, restore);
            }
        });
    }

    @GameTest(template = TEMPLATE, batch = "world_weathersuperstorm", timeoutTicks = 120)
    public static void weatherControllerMakesASuperStormWithGlowstone(GameTestHelper helper) {
        // no lightning: it would strike the other tests' structures
        net.minecraft.world.level.GameRules.BooleanValue griefing = helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        griefing.set(false, helper.getLevel().getServer());
        Runnable enabled = enable("weatherController");
        Runnable restore = () -> {
            enabled.run();
            griefing.set(true, helper.getLevel().getServer());
        };
        WeatherControllerBlockEntity controller = weatherController(helper);
        controller.items().setStackInSlot(0, new ItemStack(WorldMachineRegistry.SILVER_IODIDE.get(), 2));
        controller.items().setStackInSlot(1, new ItemStack(Items.GLOWSTONE_DUST, 2));
        helper.runAfterDelay(30, () -> {
            try {
                helper.assertTrue(helper.getLevel().getLevelData().isRaining() && helper.getLevel().getLevelData().isThundering(), "no storm");
                helper.assertTrue(controller.mode() == WeatherControllerBlockEntity.Mode.SUPERSTORM, "mode " + controller.mode());
                helper.succeed();
            } finally {
                finishWeather(helper, restore);
            }
        });
    }

    @GameTest(template = TEMPLATE, batch = "world_weatherclears", timeoutTicks = 120)
    public static void weatherControllerClearsTheSkyWithSawdust(GameTestHelper helper) {
        Runnable restore = enable("weatherController");
        WeatherControllerBlockEntity controller = weatherController(helper);
        helper.getLevel().setWeatherParameters(0, 100000, true, true);
        controller.items().setStackInSlot(0, new ItemStack(RotaryItems.SAWDUST.get(), 3));
        helper.runAfterDelay(30, () -> {
            try {
                helper.assertFalse(helper.getLevel().getLevelData().isRaining(), "still raining");
                helper.assertFalse(helper.getLevel().getLevelData().isThundering(), "still thundering");
                helper.assertTrue(controller.items().getStackInSlot(0).getCount() == 2, "should use one sawdust");
                helper.succeed();
            } finally {
                finishWeather(helper, restore);
            }
        });
    }

    @GameTest(template = TEMPLATE, batch = "world_weathernosky", timeoutTicks = 80)
    public static void weatherControllerNeedsOpenSky(GameTestHelper helper) {
        Runnable restore = enable("weatherController");
        WeatherControllerBlockEntity controller = weatherController(helper);
        helper.setBlock(MACHINE.above(), Blocks.STONE);
        controller.items().setStackInSlot(0, new ItemStack(WorldMachineRegistry.SILVER_IODIDE.get(), 2));
        helper.runAfterDelay(30, () -> {
            try {
                helper.assertFalse(helper.getLevel().getLevelData().isRaining(), "made rain from under a roof (sees sky "
                        + helper.getLevel().canSeeSky(helper.absolutePos(MACHINE.above())) + ", items " + controller.items().getStackInSlot(0).getCount()
                        + ", mode " + controller.mode() + ", cooldown " + controller.cooldown() + ")");
                helper.assertTrue(controller.items().getStackInSlot(0).getCount() == 2, "used items under a roof");
                helper.succeed();
            } finally {
                helper.setBlock(MACHINE.above(), Blocks.AIR);
                finishWeather(helper, restore);
            }
        });
    }

    @GameTest(template = TEMPLATE, batch = "world_weatherbanrain", timeoutTicks = 80)
    public static void weatherControllerCanBeBannedFromMakingRain(GameTestHelper helper) {
        Runnable restore = enable("weatherController");
        net.neoforged.neoforge.common.ModConfigSpec.BooleanValue ban = RotaryConfig.WEATHER_BANS_RAIN;
        boolean before = ban.get();
        ban.set(true);
        WeatherControllerBlockEntity controller = weatherController(helper);
        controller.items().setStackInSlot(0, new ItemStack(WorldMachineRegistry.SILVER_IODIDE.get(), 2));
        helper.runAfterDelay(30, () -> {
            try {
                helper.assertFalse(helper.getLevel().getLevelData().isRaining(), "made rain though banned");
                helper.assertTrue(controller.items().getStackInSlot(0).getCount() == 2, "used items though banned");
                helper.succeed();
            } finally {
                ban.set(before);
                finishWeather(helper, restore);
            }
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void weatherControllerOnlyTakesWeatherItems(GameTestHelper helper) {
        WeatherControllerBlockEntity controller = weatherController(helper);
        helper.assertTrue(controller.items().isItemValid(0, new ItemStack(Items.REDSTONE)), "redstone refused");
        helper.assertTrue(controller.items().isItemValid(0, new ItemStack(Items.GLOWSTONE_DUST)), "glowstone dust refused");
        helper.assertTrue(controller.items().isItemValid(0, new ItemStack(RotaryItems.SAWDUST.get())), "sawdust refused");
        helper.assertTrue(controller.items().isItemValid(0, new ItemStack(WorldMachineRegistry.SILVER_IODIDE.get())), "silver iodide refused");
        helper.assertFalse(controller.items().isItemValid(0, new ItemStack(Items.COBBLESTONE)), "cobblestone accepted");
        helper.assertTrue(controller.automationItems().extractItem(0, 1, true).isEmpty(), "automation could take items out");
        helper.setBlock(MACHINE, Blocks.AIR);
        helper.succeed();
    }
}
