package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.BeamMirrorBlockEntity;
import net.scwunge.rotarycraft.blockentity.FireworkMachineBlockEntity;
import net.scwunge.rotarycraft.registry.DecorRegistry;

import java.util.List;

/** The Beam Mirror, which needs the sun, and the Firework Machine, which makes rockets. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SkyMachineGameTests {
    static final String WIDE = "empty20x8x7";
    static final String SMALL = RotaryGameTests.TEMPLATE;
    static final BlockPos MACHINE = new BlockPos(2, 2, 2);

    // ---- Beam Mirror ----

    /** The test world's clock is set for the test and put back: the mirror's light is the sun's. */
    static Runnable timeOfDay(GameTestHelper helper, long time) {
        long before = helper.getLevel().getDayTime();
        helper.getLevel().setDayTime(time);
        return () -> helper.getLevel().setDayTime(before);
    }

    static BeamMirrorBlockEntity mirror(GameTestHelper helper, BlockPos stone) {
        helper.setBlock(MACHINE, DecorRegistry.BEAM_MIRROR.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        if (stone != null) {
            helper.setBlock(stone, Blocks.STONE);
        }
        return helper.getBlockEntity(MACHINE);
    }

    @GameTest(template = WIDE, batch = "sky_mirrorday", timeoutTicks = 80)
    public static void beamMirrorThrowsDaylightAlongItsBeam(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("beamMirror");
        Runnable clock = timeOfDay(helper, 6000);
        BeamMirrorBlockEntity mirror = mirror(helper, MACHINE.east(9));
        helper.runAfterDelay(30, () -> {
            restore.run();
            clock.run();
            helper.assertTrue(mirror.range() == 9, "range " + mirror.range());
            for (int d = 1; d <= 8; d++) {
                helper.assertTrue(helper.getBlockState(MACHINE.east(d)).is(Blocks.LIGHT), "no light " + d + " blocks along");
            }
            helper.assertTrue(helper.getBlockState(MACHINE.east(9)).is(Blocks.STONE), "the stone was replaced");
            helper.assertFalse(helper.getBlockState(MACHINE.west()).is(Blocks.LIGHT), "light behind the mirror");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "sky_mirrornight", timeoutTicks = 20)
    public static void beamMirrorGivesNoLightAtNightOrWithoutSky(GameTestHelper helper) {
        // the test world's clock is the tests' own (it is always day), so the rule itself is looked at: the sun is at its brightest all the time here
        helper.assertTrue(BeamMirrorBlockEntity.reach(true, 6000, 1, 128) == 128, "full sun by day should reach 128 blocks");
        helper.assertTrue(BeamMirrorBlockEntity.reach(true, 6000, 0.5, 128) == 11, "half sun reaches 2^3.5 blocks");
        helper.assertTrue(BeamMirrorBlockEntity.reach(true, 18000, 1, 128) == 0, "no beam at midnight");
        helper.assertTrue(BeamMirrorBlockEntity.reach(true, 13501, 1, 128) == 0 && BeamMirrorBlockEntity.reach(true, 22499, 1, 128) == 0, "none from dusk to dawn");
        helper.assertTrue(BeamMirrorBlockEntity.reach(true, 13500, 1, 128) == 128 && BeamMirrorBlockEntity.reach(true, 22500, 1, 128) == 128, "the edges count as day");
        helper.assertTrue(BeamMirrorBlockEntity.reach(false, 6000, 1, 128) == 0, "no sky, no beam");
        helper.assertTrue(BeamMirrorBlockEntity.reach(true, 6000 + 24000, 1, 128) == 128, "days after days");
        helper.succeed();
    }

    @GameTest(template = WIDE, batch = "sky_mirrorroof", timeoutTicks = 80)
    public static void beamMirrorNeedsOpenSkyAbove(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("beamMirror");
        Runnable clock = timeOfDay(helper, 6000);
        BeamMirrorBlockEntity mirror = mirror(helper, MACHINE.east(9));
        helper.setBlock(MACHINE.above(), Blocks.STONE);
        helper.runAfterDelay(20, () -> {
            restore.run();
            clock.run();
            helper.assertTrue(mirror.range() == 0 && !helper.getBlockState(MACHINE.east()).is(Blocks.LIGHT), "light from under a roof");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "sky_mirrorburn", timeoutTicks = 80)
    public static void beamMirrorSetsUndeadAlight(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("beamMirror");
        Runnable clock = timeOfDay(helper, 6000);
        mirror(helper, MACHINE.east(9));
        // a roof over the zombie, so it is the beam and not the sky that burns it
        helper.setBlock(new BlockPos(5, 4, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 1, 2), Blocks.STONE);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(5.5, 2, 2.5));
        helper.assertFalse(zombie.isOnFire(), "it started on fire");
        helper.runAfterDelay(15, () -> {
            restore.run();
            clock.run();
            helper.assertTrue(zombie.isOnFire(), "the zombie in the beam is not burning");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "sky_mirrorswitch", timeoutTicks = 80)
    public static void switchedOffBeamMirrorDoesNothing(GameTestHelper helper) {
        Runnable restore = DecorGameTests.disable("beamMirror");
        Runnable clock = timeOfDay(helper, 6000);
        mirror(helper, MACHINE.east(9));
        helper.runAfterDelay(20, () -> {
            restore.run();
            clock.run();
            helper.assertFalse(helper.getBlockState(MACHINE.east()).is(Blocks.LIGHT), "a switched-off mirror lit up");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "sky_mirrorbreak", timeoutTicks = 80)
    public static void brokenBeamMirrorTakesItsLightAway(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("beamMirror");
        Runnable clock = timeOfDay(helper, 6000);
        mirror(helper, MACHINE.east(9));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(helper.getBlockState(MACHINE.east(3)).is(Blocks.LIGHT), "not lit");
            helper.destroyBlock(MACHINE);
            restore.run();
            clock.run();
            helper.assertFalse(helper.getBlockState(MACHINE.east(3)).is(Blocks.LIGHT), "the light stayed");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "sky_mirrorclaim", timeoutTicks = 80)
    public static void beamMirrorKeepsItsLightOutOfClaims(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("beamMirror");
        Runnable clock = timeOfDay(helper, 6000);
        Runnable release = DecorGameTests.claim(new AABB(helper.absolutePos(MACHINE.east(4))).inflate(0.1));
        mirror(helper, MACHINE.east(9));
        helper.runAfterDelay(20, () -> {
            restore.run();
            clock.run();
            release.run();
            helper.assertTrue(helper.getBlockState(MACHINE.east(3)).is(Blocks.LIGHT), "the free block is dark");
            helper.assertFalse(helper.getBlockState(MACHINE.east(4)).is(Blocks.LIGHT), "a claimed block was lit");
            helper.succeed();
        });
    }

    // ---- Firework Machine ----

    static FireworkMachineBlockEntity fireworks(GameTestHelper helper, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, new BlockPos(2, 1, 2), torque, omega);
        helper.setBlock(new BlockPos(2, 2, 2), DecorRegistry.FIREWORK_MACHINE.block().get().defaultBlockState());
        return helper.getBlockEntity(new BlockPos(2, 2, 2));
    }

    static List<FireworkRocketEntity> rockets(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(FireworkRocketEntity.class, new AABB(helper.absolutePos(new BlockPos(2, 2, 2))).inflate(30));
    }

    @GameTest(template = SMALL, batch = "sky_fireworkmake", timeoutTicks = 120)
    public static void fireworkMachineMakesAStarAndARocketFromDyeGunpowderAndPaper(GameTestHelper helper) {
        FireworkMachineBlockEntity machine = fireworks(helper, 16, 65_536);
        machine.items().setStackInSlot(0, new ItemStack(Items.RED_DYE, 8));
        machine.items().setStackInSlot(1, new ItemStack(Items.GUNPOWDER, 16));
        machine.items().setStackInSlot(2, new ItemStack(Items.PAPER, 8));
        machine.items().setStackInSlot(3, new ItemStack(Items.DIAMOND, 8));
        helper.assertTrue(machine.canCraftARocket(), "it has what it needs");
        // a rocket is gone again a second after it goes up, so watch for it
        String[] why = {"none seen"};
        helper.succeedWhen(() -> {
            List<FireworkRocketEntity> launched = rockets(helper);
            helper.assertFalse(launched.isEmpty(), "no rocket was launched, or not a right one: " + why[0]);
            Fireworks fireworks = launched.get(0).getItem().get(DataComponents.FIREWORKS);
            why[0] = "saw " + fireworks;
            helper.assertTrue(fireworks != null && fireworks.flightDuration() >= 1 && fireworks.flightDuration() <= 3, "flight " + fireworks);
            helper.assertTrue(fireworks.explosions().size() == 1 && fireworks.explosions().get(0).colors().getInt(0) == net.minecraft.world.item.DyeColor.RED.getFireworkColor(),
                    "the star should be red: " + fireworks.explosions());
        });
    }

    @GameTest(template = SMALL, batch = "sky_fireworkstar", timeoutTicks = 120)
    public static void fireworkMachineUsesReadyMadeStarsFirst(GameTestHelper helper) {
        FireworkMachineBlockEntity machine = fireworks(helper, 16, 65_536);
        ItemStack star = new ItemStack(Items.FIREWORK_STAR);
        star.set(DataComponents.FIREWORK_EXPLOSION, new FireworkExplosion(FireworkExplosion.Shape.CREEPER,
                new it.unimi.dsi.fastutil.ints.IntArrayList(new int[] {0x00FF00}), new it.unimi.dsi.fastutil.ints.IntArrayList(), true, true));
        machine.items().setStackInSlot(0, star);
        machine.items().setStackInSlot(1, new ItemStack(Items.GUNPOWDER, 8));
        machine.items().setStackInSlot(2, new ItemStack(Items.PAPER, 8));
        helper.succeedWhen(() -> {
            List<FireworkRocketEntity> launched = rockets(helper);
            helper.assertFalse(launched.isEmpty(), "no rocket");
            Fireworks fireworks = launched.get(0).getItem().get(DataComponents.FIREWORKS);
            FireworkExplosion explosion = fireworks.explosions().get(0);
            helper.assertTrue(explosion.shape() == FireworkExplosion.Shape.CREEPER && explosion.hasTrail() && explosion.hasTwinkle(), "it should carry the star: " + explosion);
        });
    }

    @GameTest(template = SMALL, batch = "sky_fireworkspeed", timeoutTicks = 120)
    public static void fireworkMachineNeedsSpeedAndPower(GameTestHelper helper) {
        FireworkMachineBlockEntity machine = fireworks(helper, 16, 4_000);
        machine.items().setStackInSlot(0, new ItemStack(Items.RED_DYE, 8));
        machine.items().setStackInSlot(1, new ItemStack(Items.GUNPOWDER, 16));
        machine.items().setStackInSlot(2, new ItemStack(Items.PAPER, 8));
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(rockets(helper).isEmpty(), "it launched at 4000 rad/s");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "sky_fireworkempty", timeoutTicks = 60)
    public static void fireworkMachineWithoutPaperMakesNothing(GameTestHelper helper) {
        FireworkMachineBlockEntity machine = fireworks(helper, 16, 65_536);
        machine.items().setStackInSlot(0, new ItemStack(Items.RED_DYE, 8));
        machine.items().setStackInSlot(1, new ItemStack(Items.GUNPOWDER, 16));
        helper.assertFalse(machine.canCraftARocket(), "no paper, no rocket");
        helper.assertFalse(machine.makeRocket(helper.getLevel()), "it made a rocket without paper");
        helper.assertTrue(machine.isIdle(), "it should say it is idle");
        helper.assertTrue(machine.automationItems().extractItem(0, 1, true).isEmpty(), "pipes may not take things out");
        helper.succeed();
    }

    @GameTest(template = SMALL, batch = "sky_fireworkswitch", timeoutTicks = 120)
    public static void switchedOffFireworkMachineDoesNothing(GameTestHelper helper) {
        Runnable restore = DecorGameTests.disable("fireworkMachine");
        FireworkMachineBlockEntity machine = fireworks(helper, 16, 65_536);
        machine.items().setStackInSlot(0, new ItemStack(Items.RED_DYE, 8));
        machine.items().setStackInSlot(1, new ItemStack(Items.GUNPOWDER, 16));
        machine.items().setStackInSlot(2, new ItemStack(Items.PAPER, 8));
        helper.runAfterDelay(80, () -> {
            restore.run();
            helper.assertTrue(rockets(helper).isEmpty(), "a switched-off machine launched");
            helper.succeed();
        });
    }
}
