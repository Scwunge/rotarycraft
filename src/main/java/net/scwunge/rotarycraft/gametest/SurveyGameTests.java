package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.registry.SurveyRegistry;
import net.scwunge.rotarycraft.survey.GprBlockEntity;
import net.scwunge.rotarycraft.survey.MobRadarBlockEntity;

/** Tests of the survey machines (Mob Radar, GPR, Cave Scanner, CCTV, Spy Cam). */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SurveyGameTests {
    static final String TEMPLATE = RotaryGameTests.TEMPLATE;
    static final BlockPos MACHINE = new BlockPos(2, 2, 2);

    static MobRadarBlockEntity radar(GameTestHelper helper, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), torque, omega);
        helper.setBlock(MACHINE, SurveyRegistry.MOB_RADAR.get().defaultBlockState());
        return helper.getBlockEntity(MACHINE);
    }

    /** 8 blocks at the minimum power, one more for each 1024 W above it, to 256. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void mobRadarRangeFollowsPower(GameTestHelper helper) {
        var radar = radar(helper, 64, 128);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(radar.range() == 8, "range " + radar.range() + " on 8 kW");
            WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), 128, 128);
        });
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(radar.range() == 8 + (16384 - 8192) / 1024, "range " + radar.range() + " on 16 kW");
            WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), 4096, 256);
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(radar.range() == 256, "range " + radar.range() + " not capped at 256");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void mobRadarNeedsItsMinimumPower(GameTestHelper helper) {
        var radar = radar(helper, 32, 128);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(radar.range() == 0 && radar.scan().blips().isEmpty(), "saw something on 4 kW");
            helper.succeed();
        });
    }

    /** A cow is on the map, in the right place, with the cow's face. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void mobRadarShowsCreaturesWhereTheyAre(GameTestHelper helper) {
        var radar = radar(helper, 64, 128);
        helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 4));
        helper.runAfterDelay(10, () -> {
            var scan = radar.scan();
            helper.assertTrue(scan.blips().stream().anyMatch(b -> b.icon() == 92 && b.dz() > 0 && Math.abs(b.dx()) <= 20), "no cow south of the radar: " + scan.blips());
            helper.succeed();
        });
    }

    static GprBlockEntity gpr(GameTestHelper helper, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), torque, omega);
        helper.setBlock(MACHINE, SurveyRegistry.GPR.get().defaultBlockState());
        return helper.getBlockEntity(MACHINE);
    }

    /** 2 * log2 of the power above the 32 kW minimum, either side of the middle. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void gprRadiusFollowsPower(GameTestHelper helper) {
        var gpr = gpr(helper, 512, 128);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(gpr.range() == 30, "range " + gpr.range() + " on 64 kW, not 30");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void gprNeedsItsMinimumPower(GameTestHelper helper) {
        var gpr = gpr(helper, 256, 64);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(gpr.scannedRange() < 0, "scanned on 16 kW");
            helper.succeed();
        });
    }

    /** What is below it shows up in the slice, at the depth it is at. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void gprScansTheGroundBelow(GameTestHelper helper) {
        var gpr = gpr(helper, 512, 128);
        helper.setBlock(new BlockPos(2, 0, 2), net.minecraft.world.level.block.Blocks.GOLD_BLOCK);
        helper.succeedWhen(() -> {
            helper.assertTrue(gpr.scannedRange() == 30, "not scanned yet");
            byte[] columns = gpr.columns(30);
            int goldColor = net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultMapColor().col;
            int dd = 2;
            int middle = 30;
            helper.assertTrue(gpr.palette().get(columns[middle * GprBlockEntity.MAX_HEIGHT + (dd - 1)] & 255) == goldColor, "no gold two blocks down");
        });
    }

    /** The plane moves along the way the screen looks, and back. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void gprPlaneShiftsAndResets(GameTestHelper helper) {
        var gpr = gpr(helper, 512, 128);
        gpr.setDirection(true);
        helper.assertTrue(gpr.guiDirection() == net.minecraft.core.Direction.SOUTH, "a plane along x looks south");
        gpr.shift(1);
        gpr.shift(1);
        helper.assertTrue(gpr.centre().equals(helper.absolutePos(MACHINE).offset(0, 0, 2)), "not shifted two south: " + gpr.centre());
        gpr.shift(0);
        helper.assertTrue(gpr.centre().equals(helper.absolutePos(MACHINE)), "not back");
        gpr.flipDirection();
        helper.assertTrue(gpr.guiDirection() == net.minecraft.core.Direction.EAST, "a plane along z looks east");
        helper.succeed();
    }

    /** On from the minimum power; the aim moves four blocks the way asked, and starts on the machine. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void caveScannerIsOnWithPowerAndAimsWhereMoved(GameTestHelper helper) {
        WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), 1024, 256);
        helper.setBlock(MACHINE, SurveyRegistry.CAVE_SCANNER.get().defaultBlockState());
        net.scwunge.rotarycraft.survey.CaveScannerBlockEntity scanner = helper.getBlockEntity(MACHINE);
        helper.assertTrue(scanner.source().equals(helper.absolutePos(MACHINE)), "not aimed at itself at first");
        scanner.moveSource(4, net.minecraft.core.Direction.EAST);
        scanner.moveSource(-4, net.minecraft.core.Direction.UP);
        helper.assertTrue(scanner.source().equals(helper.absolutePos(MACHINE).offset(4, -4, 0)), "aim " + scanner.source());
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(scanner.isOn(), "off with 262 kW");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void caveScannerStaysOffBelowItsMinimum(GameTestHelper helper) {
        WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), 256, 256);
        helper.setBlock(MACHINE, SurveyRegistry.CAVE_SCANNER.get().defaultBlockState());
        net.scwunge.rotarycraft.survey.CaveScannerBlockEntity scanner = helper.getBlockEntity(MACHINE);
        helper.runAfterDelay(10, () -> {
            helper.assertFalse(scanner.isOn(), "on with 65 kW");
            helper.succeed();
        });
    }
}
