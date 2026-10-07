package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.SelfDestructBlockEntity;
import net.scwunge.rotarycraft.registry.DecorRegistry;

/** The Self Destruct. Every blast is refused by a claim around the test, so nothing round it is hurt. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SelfDestructGameTests {
    static final String SMALL = RotaryGameTests.TEMPLATE;
    static final BlockPos MACHINE = new BlockPos(2, 2, 2);

    static SelfDestructBlockEntity bomb(GameTestHelper helper) {
        WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), 1, 1);
        helper.setBlock(MACHINE, DecorRegistry.SELF_DESTRUCT.block().get().defaultBlockState());
        return helper.getBlockEntity(MACHINE);
    }

    static Runnable claimAround(GameTestHelper helper) {
        return DecorGameTests.claim(new AABB(helper.absolutePos(MACHINE)).inflate(20));
    }

    @GameTest(template = SMALL, batch = "bomb_countdown", timeoutTicks = 100)
    public static void selfDestructCountsDownWhenItsPowerStops(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("selfDestruct");
        Runnable release = claimAround(helper);
        SelfDestructBlockEntity bomb = bomb(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertFalse(bomb.isCounting(), "it started with power on");
            helper.setBlock(MACHINE.below(), Blocks.AIR);
        });
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(bomb.isCounting() && bomb.blastsSoFar() >= 5, "it should be going off (" + bomb.blastsSoFar() + " blasts)");
            helper.assertTrue(helper.getBlockState(MACHINE).is(DecorRegistry.SELF_DESTRUCT.block().get()), "it went before the end");
        });
        helper.runAfterDelay(60, () -> {
            restore.run();
            release.run();
            helper.assertTrue(helper.getBlockState(MACHINE).isAir(), "it should be gone after the last blast");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "bomb_reprieve", timeoutTicks = 100)
    public static void powerComingBackStopsTheCountDown(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("selfDestruct");
        Runnable release = claimAround(helper);
        SelfDestructBlockEntity bomb = bomb(helper);
        helper.runAfterDelay(5, () -> helper.setBlock(MACHINE.below(), Blocks.AIR));
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(bomb.isCounting(), "not counting");
            WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), 1, 1);
        });
        helper.runAfterDelay(20, () -> {
            restore.run();
            release.run();
            helper.assertFalse(bomb.isCounting(), "still counting with power back");
            helper.assertTrue(bomb.blastsSoFar() == 0, "the count should start again from nothing");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "bomb_neverpowered", timeoutTicks = 100)
    public static void aSelfDestructThatNeverHadPowerStaysQuiet(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("selfDestruct");
        Runnable release = claimAround(helper);
        helper.setBlock(MACHINE, DecorRegistry.SELF_DESTRUCT.block().get().defaultBlockState());
        SelfDestructBlockEntity bomb = helper.getBlockEntity(MACHINE);
        helper.runAfterDelay(40, () -> {
            restore.run();
            release.run();
            helper.assertFalse(bomb.isCounting(), "it went off with no power ever");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "bomb_switch", timeoutTicks = 100)
    public static void aSwitchedOffSelfDestructDoesNothing(GameTestHelper helper) {
        Runnable restore = DecorGameTests.disable("selfDestruct");
        SelfDestructBlockEntity bomb = bomb(helper);
        helper.runAfterDelay(5, () -> helper.setBlock(MACHINE.below(), Blocks.AIR));
        helper.runAfterDelay(40, () -> {
            restore.run();
            helper.assertFalse(bomb.isCounting(), "a switched-off bomb counted");
            helper.assertTrue(helper.getBlockState(MACHINE).is(DecorRegistry.SELF_DESTRUCT.block().get()), "it went anyway");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "bomb_grief", timeoutTicks = 100)
    public static void selfDestructDoesNotBlastWhenMobGriefingIsOff(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("selfDestruct");
        net.minecraft.world.level.GameRules.BooleanValue rule = helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        boolean before = rule.get();
        rule.set(false, helper.getLevel().getServer());
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.STONE);
        SelfDestructBlockEntity bomb = bomb(helper);
        helper.runAfterDelay(5, () -> helper.setBlock(MACHINE.below(), Blocks.AIR));
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(helper.getBlockState(new BlockPos(0, 1, 0)).is(Blocks.STONE), "a blast hurt the ground with mobGriefing off");
            helper.assertTrue(bomb.isCounting(), "the count should still run");
        });
        helper.runAfterDelay(60, () -> {
            rule.set(before, helper.getLevel().getServer());
            restore.run();
            helper.succeed();
        });
    }
}
