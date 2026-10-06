package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.BedrockBreakerBlockEntity;
import net.scwunge.rotarycraft.blockentity.BorerBlockEntity;
import net.scwunge.rotarycraft.blockentity.ChunkLoaderBlockEntity;
import net.scwunge.rotarycraft.blockentity.SonicBorerBlockEntity;
import net.scwunge.rotarycraft.blockentity.WeatherControllerBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.RotaryParts;
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
        helper.setBlock(MACHINE.above(), Blocks.STONE);
        // the world's sky-light heightmap catches up with the new roof a moment after it is placed
        helper.startSequence().thenWaitUntil(() -> helper.assertFalse(helper.getLevel().canSeeSky(helper.absolutePos(MACHINE.above())), "roof not registered"))
                .thenExecute(() -> {
                    WeatherControllerBlockEntity controller = weatherController(helper);
                    controller.items().setStackInSlot(0, new ItemStack(WorldMachineRegistry.SILVER_IODIDE.get(), 2));
                    helper.runAfterDelay(30, () -> checkUnderRoof(helper, controller, restore));
                });
    }

    private static void checkUnderRoof(GameTestHelper helper, WeatherControllerBlockEntity controller, Runnable restore) {
        {
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
        }
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

    // ---- Borer ----
    static final BlockPos BORER = new BlockPos(2, 1, 3);
    static final String LONG = "empty20x8x7";
    /** A speed that makes a slice take a single tick (the original's cutting time falls with the logarithm of the speed). */
    static final int FAST = 1 << 20;

    /** A borer facing east with a flywheel behind it, in a space where every slice at x = 3 and beyond is solid stone. */
    static BorerBlockEntity borer(GameTestHelper helper, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, BORER.west(), torque, omega, Direction.EAST);
        helper.setBlock(BORER, WorldMachineRegistry.BORER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        for (int x = 3; x <= 8; x++) {
            for (int y = 1; y <= 5; y++) {
                for (int z = 0; z <= 6; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        return helper.getBlockEntity(BORER);
    }

    static boolean isPipe(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).is(WorldMachineRegistry.MINING_PIPE.get());
    }

    @GameTest(template = LONG, timeoutTicks = 100)
    public static void borerCutsTheFirstSliceWholeAndLeavesPipes(GameTestHelper helper) {
        BorerBlockEntity borer = borer(helper, 1024, FAST);
        borer.setCutMask(1L << (3 * BorerBlockEntity.ROWS + 4));
        helper.setBlock(BORER.south(), Blocks.CHEST);
        helper.succeedWhen(() -> {
            for (int y = 1; y <= 5; y++) {
                for (int z = 0; z <= 6; z++) {
                    helper.assertTrue(isPipe(helper, new BlockPos(3, y, z)), "no pipe at the first slice " + y + ", " + z);
                }
            }
            helper.assertFalse(isPipe(helper, new BlockPos(4, 2, 2)), "cut a cell that was not picked");
            helper.assertTrue(helper.getBlockState(new BlockPos(3, 3, 3)).getValue(net.scwunge.rotarycraft.block.MiningPipeBlock.KIND)
                    == net.scwunge.rotarycraft.block.MiningPipeBlock.Kind.COLLAR, "the first slice should be a collar");
            net.minecraft.world.Container chest = helper.getBlockEntity(BORER.south());
            helper.assertTrue(chest.hasAnyMatching(s -> s.is(Items.COBBLESTONE)), "the cobblestone did not go into the neighbouring chest");
        });
    }

    @GameTest(template = LONG, timeoutTicks = 200)
    public static void borerCutsOnlyThePickedCellsAfterTheFirstSlice(GameTestHelper helper) {
        BorerBlockEntity borer = borer(helper, 1024, FAST);
        borer.setCutMask(1L << (3 * BorerBlockEntity.ROWS + 4));
        helper.succeedWhen(() -> {
            BlockPos centre = new BlockPos(5, 1, 3);
            helper.assertTrue(isPipe(helper, centre), "the picked cell was not cut");
            helper.assertTrue(helper.getBlockState(centre).getValue(net.scwunge.rotarycraft.block.MiningPipeBlock.KIND)
                    == net.scwunge.rotarycraft.block.MiningPipeBlock.Kind.X, "a pipe along the tunnel should run east-west");
            helper.assertTrue(helper.getBlockState(new BlockPos(5, 1, 4)).is(Blocks.STONE), "cut a cell that was not picked");
            helper.assertTrue(helper.getBlockState(new BlockPos(5, 2, 3)).is(Blocks.STONE), "cut a cell above that was not picked");
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60)
    public static void borerNeedsEnoughTorque(GameTestHelper helper) {
        BorerBlockEntity borer = borer(helper, 1, FAST);
        borer.setCutMask(1L);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(borer.isJammed(), "a borer without the torque should jam");
            helper.assertFalse(isPipe(helper, new BlockPos(3, 3, 3)), "cut without the torque");
            helper.assertTrue(borer.requiredTorque() > 1, "required torque " + borer.requiredTorque());
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60)
    public static void borerCannotCutBedrock(GameTestHelper helper) {
        BorerBlockEntity borer = borer(helper, 1 << 20, FAST);
        borer.setCutMask(1L);
        helper.setBlock(new BlockPos(3, 3, 3), Blocks.BEDROCK);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(borer.isJammed(), "bedrock should jam the borer");
            helper.assertTrue(helper.getBlockState(new BlockPos(3, 3, 3)).is(Blocks.BEDROCK), "bedrock was cut");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, batch = "world_boreriength", timeoutTicks = 100)
    public static void borerStopsAtItsMaximumLength(GameTestHelper helper) {
        var max = RotaryConfig.BORER_MAX_LENGTH;
        int before = max.get();
        max.set(4);
        BorerBlockEntity borer = borer(helper, 1024, FAST);
        borer.setCutMask(1L << (3 * BorerBlockEntity.ROWS + 4));
        helper.runAfterDelay(40, () -> {
            try {
                helper.assertTrue(borer.isJammed(), "a borer at its maximum length should jam");
                helper.assertTrue(borer.step() == 5, "should stop after slice 4, at " + borer.step());
                helper.assertTrue(helper.getBlockState(new BlockPos(7, 1, 3)).is(Blocks.STONE), "bored past its maximum length");
                helper.succeed();
            } finally {
                max.set(before);
            }
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100)
    public static void borerDropsCanBeTurnedOff(GameTestHelper helper) {
        BorerBlockEntity borer = borer(helper, 1024, FAST);
        borer.setCutMask(1L);
        borer.toggleDrops();
        helper.setBlock(BORER.south(), Blocks.CHEST);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(isPipe(helper, new BlockPos(3, 3, 3)), "did not cut");
            net.minecraft.world.Container chest = helper.getBlockEntity(BORER.south());
            helper.assertTrue(chest.isEmpty(), "dropped items with drops off");
            helper.assertTrue(helper.getEntities(net.minecraft.world.entity.EntityType.ITEM).isEmpty(), "threw items out with drops off");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100)
    public static void borerWithSilkTouchKeepsTheBlock(GameTestHelper helper) {
        BorerBlockEntity borer = borer(helper, 1024, FAST);
        borer.setCutMask(1L);
        borer.enchantments().set(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH, 1);
        helper.setBlock(BORER.south(), Blocks.CHEST);
        helper.runAfterDelay(30, () -> {
            net.minecraft.world.Container chest = helper.getBlockEntity(BORER.south());
            helper.assertTrue(chest.hasAnyMatching(s -> s.is(Items.STONE)), "silk touch should keep the stone");
            helper.assertFalse(chest.hasAnyMatching(s -> s.is(Items.COBBLESTONE)), "silk touch gave cobblestone");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, batch = "world_borermobgriefing", timeoutTicks = 100)
    public static void borerRespectsMobGriefing(GameTestHelper helper) {
        net.minecraft.world.level.GameRules.BooleanValue rule = helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        rule.set(false, helper.getLevel().getServer());
        BorerBlockEntity borer = borer(helper, 1024, FAST);
        borer.setCutMask(1L);
        helper.runAfterDelay(30, () -> {
            try {
                helper.assertTrue(borer.isJammed(), "a borer where blocks may not be changed should jam");
                helper.assertTrue(helper.getBlockState(new BlockPos(3, 3, 3)).is(Blocks.STONE), "cut with mobGriefing off");
                helper.succeed();
            } finally {
                rule.set(true, helper.getLevel().getServer());
            }
        });
    }

    @GameTest(template = LONG, timeoutTicks = 200)
    public static void borerPicksUpWhereItLeftOffAfterPowerCutsOut(GameTestHelper helper) {
        BorerBlockEntity borer = borer(helper, 1024, FAST);
        borer.setCutMask(1L << (3 * BorerBlockEntity.ROWS + 4));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(borer.step() > 2, "should have cut a few slices, at " + borer.step());
            WeaponGameTests.spinningFlywheel(helper, BORER.west(), 0, 0, Direction.EAST);
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(borer.step() == 1, "step should reset without power, at " + borer.step());
            WeaponGameTests.spinningFlywheel(helper, BORER.west(), 1024, FAST, Direction.EAST);
        });
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(borer.step() > 2, "should have found its place again from its pipes, at " + borer.step());
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100)
    public static void breakingAPipeClearsItsLine(GameTestHelper helper) {
        BlockState pipe = WorldMachineRegistry.MINING_PIPE.get().defaultBlockState().setValue(net.scwunge.rotarycraft.block.MiningPipeBlock.KIND,
                net.scwunge.rotarycraft.block.MiningPipeBlock.Kind.X);
        for (int x = 2; x <= 10; x++) {
            helper.setBlock(new BlockPos(x, 2, 2), pipe);
        }
        helper.setBlock(new BlockPos(11, 2, 3), pipe);
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos middle = helper.absolutePos(new BlockPos(6, 2, 2));
        pipe.getBlock().playerWillDestroy(helper.getLevel(), middle, helper.getLevel().getBlockState(middle), player);
        for (int x = 2; x <= 10; x++) {
            helper.assertFalse(isPipe(helper, new BlockPos(x, 2, 2)) && x != 6, "pipe left at x " + x);
        }
        helper.assertTrue(isPipe(helper, new BlockPos(11, 2, 3)), "cleared a pipe that was not in the line");
        helper.succeed();
    }

    @GameTest(template = LONG, timeoutTicks = 40)
    public static void borerTakesEnchantedBooksAndKnowsItsTorqueMath(GameTestHelper helper) {
        helper.assertTrue(BorerBlockEntity.torqueForHardness(1.5f, 0) == 16, "stone torque " + BorerBlockEntity.torqueForHardness(1.5f, 0));
        helper.assertTrue(BorerBlockEntity.torqueForHardness(0f, 0) == 1, "air-soft torque " + BorerBlockEntity.torqueForHardness(0f, 0));
        helper.assertTrue(BorerBlockEntity.torqueForHardness(50f, 0) == 512, "obsidian torque " + BorerBlockEntity.torqueForHardness(50f, 0));
        helper.assertTrue(BorerBlockEntity.torqueForHardness(50f, 3) <= 512, "sharpness should not raise the torque");
        BorerBlockEntity borer = borer(helper, 1, 1);
        net.minecraft.world.item.ItemStack book = new net.minecraft.world.item.ItemStack(Items.ENCHANTED_BOOK);
        var lookup = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        net.minecraft.world.item.enchantment.ItemEnchantments.Mutable stored = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(
                net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        stored.set(lookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), 3);
        stored.set(lookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.MENDING), 1);
        book.set(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        helper.assertTrue(borer.enchantments().apply(book), "the book gave nothing");
        helper.assertTrue(borer.enchantments().level(net.minecraft.world.item.enchantment.Enchantments.FORTUNE) == 3, "fortune not taken");
        helper.assertFalse(borer.enchantments().has(net.minecraft.world.item.enchantment.Enchantments.MENDING), "took an enchantment it does not use");
        helper.assertTrue(borer.operationTime() >= 1, "operation time");
        helper.succeed();
    }

    // ---- Bedrock Breaker ----

    /** A bedrock breaker facing east with a flywheel behind it that gives it its full power at a speed that makes every step take a tick. */
    static BedrockBreakerBlockEntity breaker(GameTestHelper helper, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, BORER.west(), torque, omega, Direction.EAST);
        helper.setBlock(BORER, WorldMachineRegistry.BEDROCK_BREAKER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        return helper.getBlockEntity(BORER);
    }

    @GameTest(template = LONG, timeoutTicks = 200)
    public static void bedrockBreakerGrindsBedrockToDustInAChest(GameTestHelper helper) {
        BedrockBreakerBlockEntity breaker = breaker(helper, 16384, FAST);
        helper.setBlock(new BlockPos(4, 1, 3), Blocks.BEDROCK);
        helper.setBlock(BORER.south(), Blocks.CHEST);
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(new BlockPos(4, 1, 3)).isAir(), "the bedrock is still there");
            net.minecraft.world.Container chest = helper.getBlockEntity(BORER.south());
            int dust = BedrockBreakerBlockEntity.dustPerBlock(helper.getLevel().getDifficulty());
            helper.assertTrue(chest.countItem(RotaryParts.part("bedrock_dust").get()) == dust, "expected " + dust + " dust, chest had "
                    + chest.countItem(RotaryParts.part("bedrock_dust").get()));
        });
    }

    @GameTest(template = LONG, timeoutTicks = 200)
    public static void bedrockBreakerThinsBedrockAnotchAtATime(GameTestHelper helper) {
        BedrockBreakerBlockEntity breaker = breaker(helper, 16384, FAST);
        helper.setBlock(new BlockPos(4, 1, 3), Blocks.BEDROCK);
        helper.setBlock(BORER.south(), Blocks.CHEST);
        helper.succeedWhen(() -> {
            BlockState state = helper.getBlockState(new BlockPos(4, 1, 3));
            helper.assertTrue(state.is(WorldMachineRegistry.BEDROCK_SLICE.get()) && state.getValue(net.scwunge.rotarycraft.block.BedrockSliceBlock.PROGRESS) >= 3,
                    "the bedrock is not being ground down (" + state.getBlock() + ")");
            helper.assertTrue(state.getValue(net.scwunge.rotarycraft.block.BedrockSliceBlock.FACING) == Direction.WEST, "the thinning side should face the machine");
            helper.assertTrue(state.getDestroySpeed(helper.getLevel(), helper.absolutePos(new BlockPos(4, 1, 3))) < 0, "a slice of bedrock must stay unbreakable");
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100)
    public static void bedrockBreakerGrindsOrdinaryBlocksAwayWithoutDrops(GameTestHelper helper) {
        BedrockBreakerBlockEntity breaker = breaker(helper, 16384, FAST);
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
        helper.setBlock(BORER.south(), Blocks.CHEST);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(helper.getBlockState(new BlockPos(3, 1, 3)).isAir(), "stone in the way was not ground away");
            helper.assertTrue(helper.getEntities(net.minecraft.world.entity.EntityType.ITEM).isEmpty(), "ground-away stone dropped something");
            net.minecraft.world.Container chest = helper.getBlockEntity(BORER.south());
            helper.assertTrue(chest.isEmpty(), "ground-away stone went into the chest");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100)
    public static void bedrockBreakerNeedsItsTorque(GameTestHelper helper) {
        BedrockBreakerBlockEntity breaker = breaker(helper, 1000, FAST);
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.BEDROCK);
        helper.runAfterDelay(30, () -> {
            helper.assertFalse(breaker.hasEnoughPower(), "1000 N*m should not be enough");
            helper.assertTrue(helper.getBlockState(new BlockPos(3, 1, 3)).is(Blocks.BEDROCK), "ground bedrock without the torque");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100)
    public static void bedrockBreakerLeavesBlocksThatHoldItems(GameTestHelper helper) {
        BedrockBreakerBlockEntity breaker = breaker(helper, 16384, FAST);
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.CHEST);
        net.minecraft.world.Container chest = helper.getBlockEntity(new BlockPos(3, 1, 3));
        chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(helper.getBlockState(new BlockPos(3, 1, 3)).is(Blocks.CHEST), "ground away a chest full of items");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, batch = "world_bedrockmobgriefing", timeoutTicks = 100)
    public static void bedrockBreakerRespectsMobGriefing(GameTestHelper helper) {
        net.minecraft.world.level.GameRules.BooleanValue rule = helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        rule.set(false, helper.getLevel().getServer());
        BedrockBreakerBlockEntity breaker = breaker(helper, 16384, FAST);
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.BEDROCK);
        helper.runAfterDelay(30, () -> {
            try {
                helper.assertTrue(helper.getBlockState(new BlockPos(3, 1, 3)).is(Blocks.BEDROCK), "ground bedrock with mobGriefing off");
                helper.succeed();
            } finally {
                rule.set(true, helper.getLevel().getServer());
            }
        });
    }

    @GameTest(template = LONG, timeoutTicks = 200)
    public static void bedrockBreakerKeepsDustInItsOwnStoreAndAutomationCanTakeItOut(GameTestHelper helper) {
        BedrockBreakerBlockEntity breaker = breaker(helper, 16384, FAST);
        helper.setBlock(new BlockPos(4, 1, 3), Blocks.BEDROCK);
        helper.succeedWhen(() -> {
            int dust = BedrockBreakerBlockEntity.dustPerBlock(helper.getLevel().getDifficulty());
            helper.assertTrue(breaker.store().getStackInSlot(0).getCount() == dust, "store holds " + breaker.store().getStackInSlot(0));
            helper.assertTrue(breaker.automationItems().extractItem(0, 1, true).getCount() == 1, "automation could not take dust out");
            helper.assertTrue(breaker.automationItems().insertItem(0, new ItemStack(Items.DIRT), true).getCount() == 1, "automation could put things in");
        });
    }

    // ---- Sonic Borer ----
    static final BlockPos SONIC = new BlockPos(2, 4, 3);

    /** A sonic borer facing east at the top of the room, with just its minimum power and some pressure already built up. */
    static SonicBorerBlockEntity sonicBorer(GameTestHelper helper, int torque, int omega, int pressure) {
        WeaponGameTests.spinningFlywheel(helper, SONIC.west(), torque, omega, Direction.EAST);
        helper.setBlock(SONIC, WorldMachineRegistry.SONIC_BORER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        SonicBorerBlockEntity borer = helper.getBlockEntity(SONIC);
        borer.addPressure(pressure);
        return borer;
    }

    /** A wall of stone 7 by 7 across the line of fire, at x = 8, with one stone just outside it. */
    static void sonicWall(GameTestHelper helper) {
        for (int y = 1; y <= 7; y++) {
            for (int z = 0; z <= 6; z++) {
                helper.setBlock(new BlockPos(8, y, z), Blocks.STONE);
            }
        }
        helper.setBlock(new BlockPos(8, 0, 3), Blocks.STONE);
    }

    @GameTest(template = LONG, timeoutTicks = 100)
    public static void sonicBorerShattersTheSquareAtTheFirstSolidLayer(GameTestHelper helper) {
        SonicBorerBlockEntity borer = sonicBorer(helper, 4096, 16, 450);
        sonicWall(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(borer.pressure() < SonicBorerBlockEntity.FIRE_PRESSURE + 100, "it never fired (pressure " + borer.pressure() + ")");
            for (int y = 1; y <= 7; y++) {
                for (int z = 0; z <= 6; z++) {
                    helper.assertTrue(helper.getBlockState(new BlockPos(8, y, z)).isAir(), "left stone at " + y + ", " + z);
                }
            }
            helper.assertTrue(helper.getBlockState(new BlockPos(8, 0, 3)).is(Blocks.STONE), "broke a block outside the 7 by 7 square");
            helper.assertTrue(!helper.getEntities(net.minecraft.world.entity.EntityType.ITEM).isEmpty(), "no drops from the shattered stone");
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100)
    public static void sonicBorerHoldsFireWhenSomethingOnTheWayCannotBeBroken(GameTestHelper helper) {
        SonicBorerBlockEntity borer = sonicBorer(helper, 4096, 16, 450);
        sonicWall(helper);
        helper.setBlock(new BlockPos(5, 6, 4), Blocks.BEDROCK);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(helper.getBlockState(new BlockPos(8, 4, 3)).is(Blocks.STONE), "fired past unbreakable bedrock");
            helper.assertTrue(borer.pressure() >= SonicBorerBlockEntity.FIRE_PRESSURE, "pressure was spent without firing");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100)
    public static void sonicBorerHoldsFireWhenAWaterLayerIsInTheWay(GameTestHelper helper) {
        SonicBorerBlockEntity borer = sonicBorer(helper, 4096, 16, 450);
        sonicWall(helper);
        helper.setBlock(new BlockPos(4, 4, 3), Blocks.WATER);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(helper.getBlockState(new BlockPos(8, 4, 3)).is(Blocks.STONE), "fired through water");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100)
    public static void sonicBorerNeedsItsTorqueToBuildPressure(GameTestHelper helper) {
        SonicBorerBlockEntity borer = sonicBorer(helper, 100, 1 << 20, 0);
        sonicWall(helper);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(borer.pressure() < 200, "built pressure without enough torque: " + borer.pressure());
            helper.assertTrue(helper.getBlockState(new BlockPos(8, 4, 3)).is(Blocks.STONE), "fired without enough torque");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, batch = "world_sonicmobgriefing", timeoutTicks = 100)
    public static void sonicBorerRespectsMobGriefingAndBurstsHarmlesslyThere(GameTestHelper helper) {
        net.minecraft.world.level.GameRules.BooleanValue rule = helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        rule.set(false, helper.getLevel().getServer());
        SonicBorerBlockEntity borer = sonicBorer(helper, 4096, 16, 450);
        sonicWall(helper);
        helper.runAfterDelay(30, () -> {
            try {
                helper.assertTrue(helper.getBlockState(new BlockPos(8, 4, 3)).is(Blocks.STONE), "shattered blocks with mobGriefing off");
                borer.addPressure(2000);
            } finally {
                // the overpressure blast, which does no block damage while mobGriefing is off
            }
        });
        helper.runAfterDelay(40, () -> {
            try {
                helper.assertTrue(borer.pressure() < SonicBorerBlockEntity.MAX_PRESSURE, "pressure kept climbing past the burst");
                helper.assertTrue(helper.getBlockState(SONIC.west()).getBlock() == net.scwunge.rotarycraft.registry.RotaryBlocks.FLYWHEELS
                        .get(net.scwunge.rotarycraft.power.FlywheelType.BEDROCK).get(), "the burst broke the flywheel with mobGriefing off");
                helper.succeed();
            } finally {
                rule.set(true, helper.getLevel().getServer());
            }
        });
    }

    // ---- Laserable ----

    /** A chest that records the beam's calls (power and step) and says whether it blocks the beam. */
    static class BeamChest extends net.minecraft.world.level.block.entity.ChestBlockEntity implements net.scwunge.rotarycraft.api.Laserable {
        final boolean blocks;
        int calls;
        long lastPower;
        int lastStep;

        BeamChest(BlockPos pos, BlockState state, boolean blocks) {
            super(pos, state);
            this.blocks = blocks;
        }

        @Override
        public void whenInBeam(net.minecraft.world.level.Level level, BlockPos pos, long power, int step) {
            calls++;
            lastPower = power;
            lastStep = step;
        }

        @Override
        public boolean blockBeam(net.minecraft.world.level.Level level, BlockPos pos, long power) {
            return blocks;
        }
    }

    static BeamChest beamChest(GameTestHelper helper, BlockPos pos, boolean blocks) {
        helper.setBlock(pos, Blocks.CHEST);
        BlockPos abs = helper.absolutePos(pos);
        BeamChest chest = new BeamChest(abs, helper.getLevel().getBlockState(abs), blocks);
        helper.getLevel().setBlockEntity(chest);
        return chest;
    }

    static class BeamHusk extends net.minecraft.world.entity.monster.Husk implements net.scwunge.rotarycraft.api.Laserable {
        int calls;
        long lastPower;

        BeamHusk(net.minecraft.world.level.Level level) {
            super(net.minecraft.world.entity.EntityType.HUSK, level);
        }

        @Override
        public void whenInBeam(net.minecraft.world.level.Level level, BlockPos pos, long power, int step) {
            calls++;
            lastPower = power;
        }

        @Override
        public boolean blockBeam(net.minecraft.world.level.Level level, BlockPos pos, long power) {
            return false;
        }
    }

    @GameTest(template = "empty20x8x7", batch = "world_laserable_heatray", timeoutTicks = 100)
    public static void heatRayHandsItsBeamToLaserableBlockEntitiesAndStopsAtOneThatBlocks(GameTestHelper helper) {
        WeaponGameTests.spinningFlywheel(helper, new BlockPos(0, 2, 2), 8192, 1024, Direction.EAST);
        WeaponGameTests.heatRay(helper, 8192);
        BeamChest first = beamChest(helper, new BlockPos(5, 2, 2), true);
        BeamChest behind = beamChest(helper, new BlockPos(8, 2, 2), false);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(first.calls > 0, "the Laserable in the beam was never called");
            helper.assertTrue(first.lastPower == 8192L * 1024L, "power " + first.lastPower);
            helper.assertTrue(first.lastStep == 4, "step " + first.lastStep);
            helper.assertTrue(behind.calls == 0, "the beam went on past a Laserable that blocks it");
            helper.succeed();
        });
    }

    @GameTest(template = "empty20x8x7", batch = "world_laserable_heatray2", timeoutTicks = 100)
    public static void heatRayBeamGoesOnPastALaserableThatDoesNotBlock(GameTestHelper helper) {
        WeaponGameTests.spinningFlywheel(helper, new BlockPos(0, 2, 2), 8192, 1024, Direction.EAST);
        WeaponGameTests.heatRay(helper, 8192);
        BeamChest first = beamChest(helper, new BlockPos(5, 2, 2), false);
        BeamChest behind = beamChest(helper, new BlockPos(8, 2, 2), false);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(first.calls > 0 && behind.calls > 0, "calls " + first.calls + ", " + behind.calls);
            helper.assertTrue(behind.lastStep == 7, "step " + behind.lastStep);
            helper.succeed();
        });
    }

    @GameTest(template = "empty20x8x7", batch = "world_laserable_heatray3", timeoutTicks = 100)
    public static void heatRayHandsItsBeamToLaserableCreatures(GameTestHelper helper) {
        WeaponGameTests.spinningFlywheel(helper, new BlockPos(0, 2, 2), 8192, 1024, Direction.EAST);
        WeaponGameTests.heatRay(helper, 8192);
        BeamHusk husk = new BeamHusk(helper.getLevel());
        net.minecraft.world.phys.Vec3 at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(5.5, 2, 2.5));
        husk.setPos(at);
        husk.setNoAi(true);
        husk.setPersistenceRequired();
        helper.getLevel().addFreshEntity(husk);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(husk.calls > 0, "the Laserable creature in the beam was never called");
            helper.assertTrue(husk.lastPower == 8192L * 1024L, "power " + husk.lastPower);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "world_laserable_laser", timeoutTicks = 100)
    public static void laserGunHandsItsBeamToLaserableBlockEntities(GameTestHelper helper) {
        WeaponGameTests.spinningFlywheel(helper, new BlockPos(2, 1, 2), 4096, 4096);
        helper.setBlock(new BlockPos(2, 2, 2), net.scwunge.rotarycraft.registry.WeaponRegistry.LASER_GUN.get().defaultBlockState());
        BeamChest chest = beamChest(helper, new BlockPos(2, 2, 4), true);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(chest.calls > 0, "the Laserable in the beam was never called");
            helper.assertTrue(chest.lastPower == 4096L * 4096L, "power " + chest.lastPower);
            helper.succeed();
        });
    }
}
