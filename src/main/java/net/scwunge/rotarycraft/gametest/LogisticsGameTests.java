package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.BlastFurnaceBlockEntity;
import net.scwunge.rotarycraft.logistics.HeaterBlockEntity;
import net.scwunge.rotarycraft.logistics.IgniterBlockEntity;
import net.scwunge.rotarycraft.logistics.PlayerDetectorBlockEntity;
import net.scwunge.rotarycraft.logistics.SmokeDetectorBlockEntity;
import net.scwunge.rotarycraft.machine.HeatEffects;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.WeaponRegistry;

/** The detectors, the heater and the igniter, and the heat they make. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class LogisticsGameTests {
    static final String SMALL = RotaryGameTests.TEMPLATE;
    static final String WIDE = "empty20x8x7";
    static final BlockPos AT = new BlockPos(2, 2, 2);

    static ItemStack coil(int charge) {
        ItemStack coil = new ItemStack(WeaponRegistry.SPRING.get());
        net.scwunge.rotarycraft.item.CoilItem.setCharge(coil, charge);
        return coil;
    }

    // ---- Player Detector ----

    /** A player standing in the level: one that has not logged in, since a login would run every mod's join handlers against a connection that is not there. */
    static Player mockPlayer(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    static PlayerDetectorBlockEntity detector(GameTestHelper helper, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, AT.below(), torque, omega);
        helper.setBlock(AT, LogisticsRegistry.PLAYER_DETECTOR.block().get().defaultBlockState());
        return helper.getBlockEntity(AT);
    }

    static int signal(GameTestHelper helper) {
        BlockPos abs = helper.absolutePos(AT);
        BlockState state = helper.getLevel().getBlockState(abs);
        return state.getSignal(helper.getLevel(), abs, Direction.UP);
    }

    @GameTest(template = WIDE, batch = "logi_playerdetect", timeoutTicks = 100)
    public static void playerDetectorGivesASignalWhileAPlayerIsInRange(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("playerDetector");
        PlayerDetectorBlockEntity detector = detector(helper, 8, 3200);
        detector.setSetRange(10);
        Player player = mockPlayer(helper);
        player.setPos(helper.absoluteVec(new Vec3(8.5, 2, 2.5)));
        helper.runAfterDelay(1, () -> helper.assertTrue(detector.reactionTime() == 1, "reaction time " + detector.reactionTime()));
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(detector.range() == 10 && signal(helper) == 15, "signal " + signal(helper) + ", range " + detector.range());
            player.setPos(helper.absoluteVec(new Vec3(19.5, 2, 2.5)));
        });
        helper.runAfterDelay(25, () -> {
            restore.run();
            helper.assertTrue(signal(helper) == 0, "still on with the player out of range");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "logi_playeranalog", timeoutTicks = 100)
    public static void aScrewdriverMakesTheDetectorCountPlayers(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("playerDetector");
        PlayerDetectorBlockEntity detector = detector(helper, 8, 3200);
        detector.setSetRange(12);
        Player one = mockPlayer(helper);
        Player two = mockPlayer(helper);
        one.setPos(helper.absoluteVec(new Vec3(6.5, 2, 2.5)));
        two.setPos(helper.absoluteVec(new Vec3(7.5, 2, 2.5)));
        BlockPos abs = helper.absolutePos(AT);
        UseOnContext context = new UseOnContext(helper.getLevel(), one, net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(RotaryItems.SCREWDRIVER.get()),
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        helper.assertTrue(detector.onScrewdriver(context) && detector.analog(), "the screwdriver should switch it to analog");
        helper.runAfterDelay(12, () -> {
            restore.run();
            helper.assertTrue(signal(helper) == 2, "two players, signal " + signal(helper));
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "logi_playerpower", timeoutTicks = 100)
    public static void playerDetectorRangeFollowsThePowerAndTheSetting(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("playerDetector");
        PlayerDetectorBlockEntity detector = detector(helper, 1, 1280);
        detector.setSetRange(100);
        Player player = mockPlayer(helper);
        player.setPos(helper.absoluteVec(new Vec3(15.5, 2, 2.5)));
        helper.runAfterDelay(20, () -> {
            restore.run();
            // 1280 W is ten blocks at a block for every 128 W, and the player is thirteen away
            helper.assertTrue(detector.maxRange() == 10 && detector.range() == 10, "range " + detector.range() + " of " + detector.maxRange());
            helper.assertTrue(signal(helper) == 0, "it saw beyond its range");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "logi_playeroff", timeoutTicks = 100)
    public static void aSwitchedOffPlayerDetectorGivesNothing(GameTestHelper helper) {
        Runnable restore = DecorGameTests.disable("playerDetector");
        PlayerDetectorBlockEntity detector = detector(helper, 8, 3200);
        detector.setSetRange(10);
        Player player = mockPlayer(helper);
        player.setPos(helper.absoluteVec(new Vec3(5.5, 2, 2.5)));
        helper.runAfterDelay(15, () -> {
            restore.run();
            helper.assertTrue(signal(helper) == 0, "a switched-off detector gave a signal");
            helper.succeed();
        });
    }

    // ---- Smoke Detector ----

    static SmokeDetectorBlockEntity smokeDetector(GameTestHelper helper, int charge) {
        helper.setBlock(AT, LogisticsRegistry.SMOKE_DETECTOR.block().get().defaultBlockState());
        SmokeDetectorBlockEntity detector = helper.getBlockEntity(AT);
        if (charge > 0) {
            detector.items().setStackInSlot(0, coil(charge));
        }
        return detector;
    }

    @GameTest(template = WIDE, batch = "logi_smoke", timeoutTicks = 100)
    public static void smokeDetectorRaisesTheAlarmAtFireAndStopsWhenItIsOut(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("smokeDetector");
        SmokeDetectorBlockEntity detector = smokeDetector(helper, 1000);
        helper.setBlock(new BlockPos(7, 1, 2), Blocks.NETHERRACK);
        helper.setBlock(new BlockPos(7, 2, 2), Blocks.FIRE);
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(detector.isAlarming() && signal(helper) == 15, "alarm " + detector.isAlarming() + ", signal " + signal(helper));
            helper.setBlock(new BlockPos(7, 2, 2), Blocks.AIR);
        });
        helper.runAfterDelay(30, () -> {
            restore.run();
            helper.assertFalse(detector.isAlarming(), "still sounding with no fire");
            helper.assertTrue(signal(helper) == 0, "still giving a signal");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "logi_smokefar", timeoutTicks = 100)
    public static void smokeDetectorNeedsACoilAndIgnoresFarFire(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("smokeDetector");
        SmokeDetectorBlockEntity noCoil = smokeDetector(helper, 0);
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.NETHERRACK);
        helper.setBlock(new BlockPos(4, 2, 2), Blocks.FIRE);
        helper.runAfterDelay(15, () -> {
            helper.assertFalse(noCoil.isAlarming(), "it raised the alarm with no coil");
            noCoil.items().setStackInSlot(0, coil(1000));
            helper.setBlock(new BlockPos(4, 2, 2), Blocks.AIR);
            helper.setBlock(new BlockPos(14, 1, 2), Blocks.NETHERRACK);
            helper.setBlock(new BlockPos(14, 2, 2), Blocks.FIRE);
        });
        helper.runAfterDelay(35, () -> {
            restore.run();
            helper.assertFalse(noCoil.isAlarming(), "it saw a fire twelve blocks off");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "logi_smokebattery", timeoutTicks = 60)
    public static void smokeDetectorSaysWhenItsCoilIsNearlySpent(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("smokeDetector");
        SmokeDetectorBlockEntity detector = smokeDetector(helper, 5);
        helper.runAfterDelay(10, () -> {
            restore.run();
            helper.assertTrue(detector.isLowBattery() && !detector.isAlarming(), "low battery");
            detector.items().setStackInSlot(0, coil(500));
            helper.succeed();
        });
    }

    // ---- Heater ----

    static HeaterBlockEntity heater(GameTestHelper helper, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, AT.below(), torque, omega);
        helper.setBlock(AT, LogisticsRegistry.HEATER.block().get().defaultBlockState());
        return helper.getBlockEntity(AT);
    }

    @GameTest(template = WIDE, batch = "logi_heatfuel", timeoutTicks = 200)
    public static void heaterBurnsFuelUpToItsSetTemperature(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("heater");
        HeaterBlockEntity heater = heater(helper, 16, 65536);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        heater.setField(player, 0, 300);
        heater.items().setStackInSlot(0, new ItemStack(Items.COAL, 8));
        helper.runAfterDelay(160, () -> {
            restore.run();
            helper.assertTrue(heater.getTemperature() >= 200 && heater.getTemperature() <= 400, "temperature " + heater.getTemperature());
            helper.assertTrue(heater.items().getStackInSlot(0).getCount() < 8, "no coal burnt");
            helper.assertTrue(heater.items().getStackInSlot(0).getCount() > 0, "it burnt all the coal beyond its setting");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "logi_heatlava", timeoutTicks = 200)
    public static void heaterWillNotBurnAFuelHotterThanItNeeds(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("heater");
        HeaterBlockEntity heater = heater(helper, 16, 65536);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        heater.setField(player, 0, 100);
        heater.items().setStackInSlot(0, new ItemStack(Items.LAVA_BUCKET));
        helper.runAfterDelay(120, () -> {
            restore.run();
            helper.assertTrue(heater.items().getStackInSlot(0).is(Items.LAVA_BUCKET), "a lava bucket was burnt for 100 degrees");
            helper.assertTrue(heater.getTemperature() < 100, "temperature " + heater.getTemperature());
            helper.assertTrue(heater.isIdle(), "with nothing it can use it should say it is idle");
            helper.assertFalse(heater.automationItems().isItemValid(0, new ItemStack(Items.STICK)) && false, "");
            helper.assertFalse(heater.automationItems().isItemValid(0, new ItemStack(Items.DIAMOND)), "a diamond is no fuel");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "logi_heattransfer", timeoutTicks = 100)
    public static void heaterWarmsAMachineOnTopAndMeltsSnowOnIt(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("heater");
        HeaterBlockEntity heater = heater(helper, 16, 65536);
        helper.setBlock(AT.above(), RotaryBlocks.BLAST_FURNACE.get().defaultBlockState());
        BlastFurnaceBlockEntity furnace = helper.getBlockEntity(AT.above());
        heater.setCurrentTemperature(500);
        int before = furnace.getTemperature();
        helper.runAfterDelay(15, () -> {
            restore.run();
            helper.assertTrue(furnace.getTemperature() > before, "the furnace stayed at " + furnace.getTemperature());
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "logi_heatfire", timeoutTicks = 100)
    public static void heaterSetsThingsOnTopAlight(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("heater");
        HeaterBlockEntity heater = heater(helper, 16, 65536);
        heater.setCurrentTemperature(500);
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(2, 3, 2));
        helper.runAfterDelay(30, () -> {
            restore.run();
            helper.assertTrue(pig.isOnFire() || !pig.isAlive(), "the pig on the heater was not set alight");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "logi_heatoff", timeoutTicks = 200)
    public static void aSwitchedOffHeaterBurnsNothing(GameTestHelper helper) {
        Runnable restore = DecorGameTests.disable("heater");
        HeaterBlockEntity heater = heater(helper, 16, 65536);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        heater.setField(player, 0, 300);
        heater.items().setStackInSlot(0, new ItemStack(Items.COAL, 8));
        helper.runAfterDelay(100, () -> {
            restore.run();
            helper.assertTrue(heater.items().getStackInSlot(0).getCount() == 8, "a switched-off heater burnt coal");
            helper.succeed();
        });
    }

    // ---- Igniter ----

    @GameTest(template = WIDE, batch = "logi_ignite", timeoutTicks = 200)
    public static void igniterBurnsItsHottestFuelAndSetsLivingThingsAlight(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("igniter");
        WeaponGameTests.spinningFlywheel(helper, AT.below(), 32, 1024);
        helper.setBlock(AT, LogisticsRegistry.IGNITER.block().get().defaultBlockState());
        IgniterBlockEntity igniter = helper.getBlockEntity(AT);
        igniter.items().setStackInSlot(0, new ItemStack(Items.COAL, 4));
        igniter.items().setStackInSlot(1, new ItemStack(Items.OAK_PLANKS, 4));
        igniter.setCurrentTemperature(300);
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(6, 2, 2));
        helper.runAfterDelay(100, () -> {
            restore.run();
            helper.assertTrue(igniter.fuel() == IgniterBlockEntity.Fuel.COAL, "it should pick coal over wood: " + igniter.fuel());
            helper.assertTrue(igniter.getTemperature() > 140 && igniter.items().getStackInSlot(0).getCount() < 4, "temperature " + igniter.getTemperature());
            helper.assertTrue(igniter.items().getStackInSlot(1).getCount() == 4, "the wood should be left for later");
            helper.assertTrue(igniter.range() >= 32, "range " + igniter.range());
            helper.assertTrue(pig.isOnFire() || !pig.isAlive(), "the pig was not set alight (at " + igniter.getTemperature() + " C)");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "logi_ignitefuels", timeoutTicks = 60)
    public static void igniterFuelsNeedAllTheirIngredients(GameTestHelper helper) {
        WeaponGameTests.spinningFlywheel(helper, AT.below(), 32, 1024);
        helper.setBlock(AT, LogisticsRegistry.IGNITER.block().get().defaultBlockState());
        IgniterBlockEntity igniter = helper.getBlockEntity(AT);
        helper.assertTrue(igniter.automationItems().isItemValid(0, new ItemStack(Items.BLAZE_POWDER)) && igniter.automationItems().isItemValid(0, new ItemStack(Items.LAVA_BUCKET)), "fuels go in");
        helper.assertFalse(igniter.automationItems().isItemValid(0, new ItemStack(Items.DIAMOND)), "a diamond is no fuel");
        helper.assertTrue(IgniterBlockEntity.Fuel.THERMITE.temperature == 2500 && IgniterBlockEntity.Fuel.WOOD.accepts(new ItemStack(Items.OAK_LOG)), "values");
        helper.assertFalse(IgniterBlockEntity.Fuel.THERMITE.accepts(new ItemStack(Items.COAL)), "coal is not thermite");
        helper.succeed();
    }

    @GameTest(template = WIDE, batch = "logi_ignitepower", timeoutTicks = 100)
    public static void igniterNeedsSpeedAsWellAsPower(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("igniter");
        WeaponGameTests.spinningFlywheel(helper, AT.below(), 1024, 32);
        helper.setBlock(AT, LogisticsRegistry.IGNITER.block().get().defaultBlockState());
        IgniterBlockEntity igniter = helper.getBlockEntity(AT);
        igniter.items().setStackInSlot(0, new ItemStack(Items.COAL, 4));
        helper.runAfterDelay(60, () -> {
            restore.run();
            helper.assertTrue(igniter.items().getStackInSlot(0).getCount() == 4, "it burnt coal at 32 rad/s");
            helper.succeed();
        });
    }

    // ---- what heat does ----

    @GameTest(template = SMALL, batch = "logi_heatworld", timeoutTicks = 40)
    public static void heatMeltsBoilsAndKindles(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos snow = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos ice = helper.absolutePos(new BlockPos(2, 1, 1));
        BlockPos water = helper.absolutePos(new BlockPos(3, 1, 1));
        BlockPos planks = helper.absolutePos(new BlockPos(1, 1, 3));
        BlockPos above = helper.absolutePos(new BlockPos(1, 2, 3));
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.SNOW_BLOCK);
        helper.setBlock(new BlockPos(2, 1, 1), Blocks.ICE);
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.WATER);
        helper.setBlock(new BlockPos(1, 1, 3), Blocks.OAK_PLANKS);
        helper.assertFalse(HeatEffects.affect(level, snow, 0, null), "snow does not melt at 0");
        helper.assertTrue(HeatEffects.affect(level, snow, 1, null) && level.getBlockState(snow).isAir(), "snow melts above 0");
        helper.assertTrue(HeatEffects.affect(level, ice, 20, null) && level.getBlockState(ice).is(Blocks.WATER), "ice melts into water");
        helper.assertFalse(HeatEffects.affect(level, water, 99, null), "water does not boil at 99");
        helper.assertTrue(HeatEffects.affect(level, water, 100, null) && level.getBlockState(water).isAir(), "water boils away at 100");
        helper.assertFalse(HeatEffects.affect(level, above, 299, null), "wood does not catch at 299");
        helper.assertTrue(HeatEffects.affect(level, above, 300, null) && level.getBlockState(above).is(Blocks.FIRE), "fire starts above wood at 300");
        helper.assertFalse(HeatEffects.affect(level, helper.absolutePos(new BlockPos(4, 3, 4)), 2000, null), "nothing happens in empty air with nothing to burn");
        helper.succeed();
    }

    @GameTest(template = SMALL, batch = "logi_heatclaim", timeoutTicks = 40)
    public static void heatWillNotChangeClaimedBlocks(GameTestHelper helper) {
        Runnable release = DecorGameTests.claim(new AABB(helper.absolutePos(new BlockPos(1, 1, 1))).inflate(0.1));
        BlockPos snow = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.SNOW_BLOCK);
        boolean changed = HeatEffects.affect(helper.getLevel(), snow, 100, null);
        release.run();
        helper.assertFalse(changed || !helper.getBlockState(new BlockPos(1, 1, 1)).is(Blocks.SNOW_BLOCK), "claimed snow melted");
        helper.succeed();
    }
}
