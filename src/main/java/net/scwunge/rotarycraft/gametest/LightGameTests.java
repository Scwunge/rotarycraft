package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.BridgeBlock;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.FloodlightBlockEntity;
import net.scwunge.rotarycraft.blockentity.LampBlockEntity;
import net.scwunge.rotarycraft.blockentity.LightBridgeBlockEntity;
import net.scwunge.rotarycraft.blockentity.ParticleEmitterBlockEntity;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.item.CoilItem;
import net.scwunge.rotarycraft.machine.LayoutMenu;
import net.scwunge.rotarycraft.machine.LayoutMenus;
import net.scwunge.rotarycraft.registry.DecorRegistry;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.WeaponRegistry;

/** The lights (lamp, flood light, light bridge) and the particle emitter. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class LightGameTests {
    static final String SMALL = RotaryGameTests.TEMPLATE;
    static final String WIDE = "empty20x8x7";
    static final BlockPos MACHINE = new BlockPos(2, 2, 2);

    static ItemStack coil(int charge) {
        ItemStack coil = new ItemStack(WeaponRegistry.SPRING.get());
        CoilItem.setCharge(coil, charge);
        return coil;
    }

    // ---- Lamp ----

    /** The lamp tests keep the lamp's range down to three blocks, so its light stays inside the test's own space and does not fill a neighbour's. */
    static final BlockPos LAMP = new BlockPos(10, 4, 3);

    static Runnable shortLamp() {
        Runnable restore = DecorGameTests.enable("lamp");
        MachineConfig.override(MachineConfig.LAMP_RANGE, 3);
        return () -> {
            restore.run();
            MachineConfig.clearOverride(MachineConfig.LAMP_RANGE);
        };
    }

    static LampBlockEntity lamp(GameTestHelper helper, int charge) {
        helper.setBlock(LAMP, DecorRegistry.LAMP.block().get().defaultBlockState());
        LampBlockEntity lamp = helper.getBlockEntity(LAMP);
        if (charge > 0) {
            lamp.items().setStackInSlot(0, coil(charge));
        }
        return lamp;
    }

    static boolean isLight(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).is(Blocks.LIGHT);
    }

    @GameTest(template = WIDE, batch = "light_lampshines", timeoutTicks = 60)
    public static void lampLightsTheAirAroundItWhileItsCoilIsWound(GameTestHelper helper) {
        Runnable restore = shortLamp();
        LampBlockEntity lamp = lamp(helper, 100);
        helper.runAfterDelay(10, () -> {
            restore.run();
            helper.assertTrue(lamp.isLit(), "the lamp is not lit");
            helper.assertTrue(isLight(helper, LAMP.east(3)) && isLight(helper, LAMP.above(3)) && isLight(helper, LAMP.north(3)), "no light along the axes");
            helper.assertFalse(isLight(helper, LAMP.east(4)), "the light went past the range");
            helper.assertTrue(isLight(helper, LAMP.offset(2, 0, 2)) && isLight(helper, LAMP.offset(-2, 2, 2)), "no light along the diagonals");
            helper.assertFalse(isLight(helper, LAMP.offset(1, 0, 1)), "a block off the lines was lit");
            helper.assertTrue(lamp.lightBlocks() > 20, "only " + lamp.lightBlocks() + " light blocks");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "light_lampcoil", timeoutTicks = 60)
    public static void lampStaysDarkWithoutAWoundCoil(GameTestHelper helper) {
        Runnable restore = shortLamp();
        LampBlockEntity noCoil = lamp(helper, 0);
        helper.runAfterDelay(10, () -> {
            helper.assertFalse(noCoil.isLit() || isLight(helper, LAMP.east(2)), "a lamp with no coil lit up");
            noCoil.items().setStackInSlot(0, coil(0));
            helper.assertTrue(!noCoil.hasCoil(), "an unwound coil counted");
            noCoil.items().setStackInSlot(0, ItemStack.EMPTY);
            helper.assertFalse(noCoil.automationItems().insertItem(0, new ItemStack(net.minecraft.world.item.Items.STICK), true).isEmpty(), "only coils go in");
            restore.run();
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "light_lampredstone", timeoutTicks = 60)
    public static void redstoneSwitchesTheLampOffAndSavesItsCoil(GameTestHelper helper) {
        Runnable restore = shortLamp();
        LampBlockEntity lamp = lamp(helper, 100);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(isLight(helper, LAMP.east(2)), "not lit before the signal");
            helper.setBlock(LAMP.north(), Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(25, () -> {
            restore.run();
            helper.assertFalse(lamp.isLit(), "still lit with a signal");
            helper.assertFalse(isLight(helper, LAMP.east(2)), "the light stayed on");
            helper.assertTrue(lamp.lightBlocks() == 0, "it still remembers light blocks");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "light_lampbreak", timeoutTicks = 60)
    public static void brokenLampTakesItsLightAway(GameTestHelper helper) {
        Runnable restore = shortLamp();
        lamp(helper, 100);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(isLight(helper, LAMP.east(2)), "not lit");
            helper.destroyBlock(LAMP);
            restore.run();
            helper.assertFalse(isLight(helper, LAMP.east(2)) || isLight(helper, LAMP.above(2)), "light stayed after the lamp went");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "light_lampclaim", timeoutTicks = 60)
    public static void lampDoesNotLightClaimedSpace(GameTestHelper helper) {
        Runnable restore = shortLamp();
        Runnable release = DecorGameTests.claim(new AABB(helper.absolutePos(LAMP.east(2))).inflate(0.1));
        lamp(helper, 100);
        helper.runAfterDelay(10, () -> {
            restore.run();
            release.run();
            helper.assertFalse(isLight(helper, LAMP.east(2)), "lit a claimed block");
            helper.assertTrue(isLight(helper, LAMP.west(2)), "did not light the free side");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "light_lampswitch", timeoutTicks = 60)
    public static void lampSwitchedOffInTheConfigStaysDark(GameTestHelper helper) {
        Runnable restore = DecorGameTests.disable("lamp");
        lamp(helper, 100);
        helper.runAfterDelay(10, () -> {
            restore.run();
            helper.assertFalse(isLight(helper, LAMP.east(2)), "a switched-off lamp lit up");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "light_lampcoilrun", timeoutTicks = 200)
    public static void lampCoilRunsDownOneChargeEveryHundredAndTwentyTicks(GameTestHelper helper) {
        Runnable restore = shortLamp();
        LampBlockEntity lamp = lamp(helper, 3);
        helper.runAfterDelay(100, () -> helper.assertTrue(CoilItem.charge(lamp.items().getStackInSlot(0)) == 3, "ran down early"));
        helper.runAfterDelay(140, () -> {
            restore.run();
            helper.assertTrue(CoilItem.charge(lamp.items().getStackInSlot(0)) == 2, "should have lost a charge, has " + CoilItem.charge(lamp.items().getStackInSlot(0)));
            helper.succeed();
        });
    }

    // ---- Particle Emitter ----

    @GameTest(template = SMALL, batch = "light_particlemenu", timeoutTicks = 40)
    public static void particleEmitterScreenPicksTheParticle(GameTestHelper helper) {
        helper.setBlock(MACHINE, DecorRegistry.PARTICLE_EMITTER.block().get().defaultBlockState());
        ParticleEmitterBlockEntity emitter = helper.getBlockEntity(MACHINE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        LayoutMenu menu = LayoutMenus.create(ParticleEmitterBlockEntity.NAME, 1, player.getInventory(), emitter);
        helper.assertTrue(emitter.kind() == ParticleEmitterBlockEntity.Kind.SMOKE, "starts on smoke");
        helper.assertTrue(menu.clickMenuButton(player, 3) && emitter.kind() == ParticleEmitterBlockEntity.Kind.LIST[3], "button 3 did not pick the fourth particle");
        helper.assertTrue(menu.extra(0) == 3, "the screen should be told the pick");
        helper.assertFalse(menu.clickMenuButton(player, ParticleEmitterBlockEntity.Kind.LIST.length), "a button that is not there did something");
        helper.assertTrue(ParticleEmitterBlockEntity.Kind.LIST.length == 27, "the original's particles: " + ParticleEmitterBlockEntity.Kind.LIST.length);
        helper.succeed();
    }

    @GameTest(template = SMALL, batch = "light_particlecoil", timeoutTicks = 40)
    public static void particleEmitterRunsOnlyOnAWoundCoil(GameTestHelper helper) {
        helper.setBlock(MACHINE, DecorRegistry.PARTICLE_EMITTER.block().get().defaultBlockState());
        ParticleEmitterBlockEntity emitter = helper.getBlockEntity(MACHINE);
        helper.assertFalse(emitter.canEmit(), "no coil, but it can emit");
        emitter.items().setStackInSlot(0, coil(5));
        helper.assertTrue(emitter.canEmit(), "a wound coil should run it");
        helper.assertTrue(emitter.automationItems().extractItem(0, 1, true).isEmpty(), "a wound coil must not be taken out");
        emitter.items().setStackInSlot(0, coil(0));
        helper.assertFalse(emitter.canEmit(), "an unwound coil should not run it");
        helper.assertFalse(emitter.automationItems().extractItem(0, 1, true).isEmpty(), "an unwound coil may be taken out");
        helper.succeed();
    }

    // ---- Flood Light ----

    static FloodlightBlockEntity floodlight(GameTestHelper helper, int torque, int omega, BlockPos stone) {
        WeaponGameTests.spinningFlywheel(helper, MACHINE.west(), torque, omega, Direction.EAST);
        helper.setBlock(MACHINE, DecorRegistry.FLOODLIGHT.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        if (stone != null) {
            helper.setBlock(stone, Blocks.STONE);
        }
        return helper.getBlockEntity(MACHINE);
    }

    @GameTest(template = WIDE, batch = "light_floodlight", timeoutTicks = 80)
    public static void floodLightFillsTheAirUpToTheFirstSolidBlock(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("floodLight");
        FloodlightBlockEntity light = floodlight(helper, 1024, 1, MACHINE.east(7));
        helper.runAfterDelay(40, () -> {
            restore.run();
            helper.assertTrue(light.range() == 7, "range " + light.range());
            for (int d = 1; d <= 6; d++) {
                helper.assertTrue(isLight(helper, MACHINE.east(d)), "no light " + d + " blocks out");
            }
            helper.assertTrue(helper.getBlockState(MACHINE.east(7)).is(Blocks.STONE), "the stone was replaced");
            helper.assertFalse(isLight(helper, MACHINE.east().above()), "the beam is a line without a lens");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "light_floodpower", timeoutTicks = 80)
    public static void floodLightNeedsAKilowatt(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("floodLight");
        FloodlightBlockEntity light = floodlight(helper, 1023, 1, MACHINE.east(7));
        helper.runAfterDelay(40, () -> {
            restore.run();
            helper.assertTrue(light.range() == 0, "range " + light.range());
            helper.assertFalse(isLight(helper, MACHINE.east()), "lit with 1023 W");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "light_floodlens", timeoutTicks = 80)
    public static void fresnelLensWidensTheLightIntoACone(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("floodLight");
        FloodlightBlockEntity light = floodlight(helper, 2048, 1, MACHINE.east(8));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack lens = new ItemStack(WeaponRegistry.PARTS.get("lens").get());
        helper.assertTrue(light.onItemUse(lens, player, InteractionHand.MAIN_HAND) && lens.isEmpty() && light.fresnel(), "the lens was not fitted and used up");
        helper.assertFalse(light.onItemUse(new ItemStack(WeaponRegistry.PARTS.get("lens").get()), player, InteractionHand.MAIN_HAND), "a second lens went in");
        helper.runAfterDelay(40, () -> {
            restore.run();
            // five blocks out the cone is two either side
            helper.assertTrue(isLight(helper, MACHINE.east(5).above(2)) && isLight(helper, MACHINE.east(5).north(2)), "the cone is not wide enough");
            helper.assertFalse(isLight(helper, MACHINE.east(5).above(3)), "the cone is too wide");
            helper.assertTrue(isLight(helper, MACHINE.east(1)) && !isLight(helper, MACHINE.east(1).above(1)), "one block out it is one block wide");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "light_floodbeam", timeoutTicks = 80)
    public static void screwdriverWhileSneakingMakesTheLightABeam(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("floodLight");
        FloodlightBlockEntity light = floodlight(helper, 1024, 1, MACHINE.east(5));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        BlockPos abs = helper.absolutePos(MACHINE);
        UseOnContext context = new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, new ItemStack(RotaryItems.SCREWDRIVER.get()),
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        helper.assertTrue(light.onScrewdriver(context) && light.beamMode(), "sneaking with the screwdriver should switch to the beam");
        helper.runAfterDelay(40, () -> {
            restore.run();
            helper.assertTrue(helper.getBlockState(MACHINE.east(2)).is(DecorRegistry.BEAM.get()), "the light is not a visible beam");
            helper.assertFalse(isLight(helper, MACHINE.east(2)), "invisible light as well");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "light_floodbreak", timeoutTicks = 80)
    public static void brokenFloodLightPutsOutItsLightAndReturnsItsLens(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("floodLight");
        FloodlightBlockEntity light = floodlight(helper, 1024, 1, MACHINE.east(5));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        light.onItemUse(new ItemStack(WeaponRegistry.PARTS.get("lens").get()), player, InteractionHand.MAIN_HAND);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(isLight(helper, MACHINE.east(2)), "not lit");
            helper.destroyBlock(MACHINE);
            restore.run();
            helper.assertFalse(isLight(helper, MACHINE.east(2)), "light stayed behind");
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(helper.absolutePos(MACHINE)).inflate(2),
                    e -> e.getItem().is(WeaponRegistry.PARTS.get("lens").get())).isEmpty(), "the lens was not dropped");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "light_floodclaim", timeoutTicks = 80)
    public static void floodLightKeepsOutOfClaimedSpace(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("floodLight");
        Runnable release = DecorGameTests.claim(new AABB(helper.absolutePos(MACHINE.east(4))).inflate(1.1, 0.1, 0.1));
        floodlight(helper, 1024, 1, MACHINE.east(7));
        helper.runAfterDelay(40, () -> {
            restore.run();
            release.run();
            helper.assertTrue(isLight(helper, MACHINE.east(1)) && isLight(helper, MACHINE.east(2)), "the free blocks were not lit");
            helper.assertFalse(isLight(helper, MACHINE.east(3)) || isLight(helper, MACHINE.east(4)) || isLight(helper, MACHINE.east(5)), "lit claimed blocks");
            helper.succeed();
        });
    }

    // ---- Light Bridge ----

    static LightBridgeBlockEntity bridge(GameTestHelper helper, int torque, int omega, boolean lit) {
        WeaponGameTests.spinningFlywheel(helper, MACHINE.west(), torque, omega, Direction.EAST);
        helper.setBlock(MACHINE, DecorRegistry.LIGHT_BRIDGE.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        // light level 13 or more above is what it asks for (the sun in the original); a block of glowstone gives it, stone takes it away
        helper.setBlock(MACHINE.above(), lit ? Blocks.GLOWSTONE : Blocks.STONE);
        return helper.getBlockEntity(MACHINE);
    }

    static boolean isBridge(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).is(DecorRegistry.BRIDGE.get());
    }

    @GameTest(template = WIDE, batch = "light_bridge", timeoutTicks = 100)
    public static void lightBridgeGrowsAcrossTheGapAsFarAsItsPowerReaches(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("lightBridge");
        // 1 MW: 1048576 * 128 / 33554432 = 4 blocks
        LightBridgeBlockEntity bridge = bridge(helper, 1024, 1024, true);
        helper.runAfterDelay(2, () -> helper.assertTrue(bridge.length() < 4 && bridge.length() >= 1, "it should grow a block at a time, but is " + bridge.length() + " long"));
        helper.runAfterDelay(30, () -> {
            restore.run();
            helper.assertTrue(bridge.range() == 4, "range " + bridge.range());
            for (int d = 1; d <= 4; d++) {
                helper.assertTrue(isBridge(helper, MACHINE.east(d)), "no bridge " + d + " blocks out");
            }
            helper.assertFalse(isBridge(helper, MACHINE.east(5)), "it went past its range");
            helper.assertTrue(helper.getBlockState(MACHINE.east(2)).getValue(BridgeBlock.AXIS) == Direction.Axis.X, "the bridge should run along x");
            BlockState state = helper.getBlockState(MACHINE.east(2));
            helper.assertFalse(state.getCollisionShape(helper.getLevel(), helper.absolutePos(MACHINE.east(2))).isEmpty(), "the bridge is not solid");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "light_bridgeblocked", timeoutTicks = 100)
    public static void lightBridgeStopsAtASolidBlock(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("lightBridge");
        bridge(helper, 1024, 1024, true);
        helper.setBlock(MACHINE.east(3), Blocks.STONE);
        helper.runAfterDelay(30, () -> {
            restore.run();
            helper.assertTrue(isBridge(helper, MACHINE.east(1)) && isBridge(helper, MACHINE.east(2)), "no bridge up to the stone");
            helper.assertTrue(helper.getBlockState(MACHINE.east(3)).is(Blocks.STONE) && !isBridge(helper, MACHINE.east(4)), "it built through the stone");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "light_bridgelight", timeoutTicks = 100)
    public static void lightBridgeNeedsLightAbove(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("lightBridge");
        bridge(helper, 1024, 1024, false);
        helper.runAfterDelay(30, () -> {
            restore.run();
            helper.assertFalse(isBridge(helper, MACHINE.east()), "built without light");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "light_bridgepower", timeoutTicks = 100)
    public static void lightBridgeNeedsPowerForEvenOneBlock(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("lightBridge");
        // 262143 W is just under what one block of a 128 block bridge costs
        LightBridgeBlockEntity bridge = bridge(helper, 512, 511, true);
        helper.runAfterDelay(30, () -> {
            restore.run();
            helper.assertTrue(bridge.range() == 0, "range " + bridge.range());
            helper.assertFalse(isBridge(helper, MACHINE.east()), "built with too little power");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "light_bridgepowerloss", timeoutTicks = 120)
    public static void lightBridgeVanishesWhenItLosesPower(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("lightBridge");
        bridge(helper, 1024, 1024, true);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(isBridge(helper, MACHINE.east(4)), "the bridge was not built");
            WeaponGameTests.spinningFlywheel(helper, MACHINE.west(), 1, 1, Direction.EAST);
        });
        helper.runAfterDelay(45, () -> {
            restore.run();
            helper.assertFalse(isBridge(helper, MACHINE.east(1)) || isBridge(helper, MACHINE.east(4)), "the bridge stayed without power");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "light_bridgebreak", timeoutTicks = 100)
    public static void brokenLightBridgeTakesItsBeamAway(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("lightBridge");
        bridge(helper, 1024, 1024, true);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(isBridge(helper, MACHINE.east(2)), "the bridge was not built");
            helper.destroyBlock(MACHINE);
            restore.run();
            helper.assertFalse(isBridge(helper, MACHINE.east(1)) || isBridge(helper, MACHINE.east(4)), "the beam stayed");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "light_bridgeclaim", timeoutTicks = 100)
    public static void lightBridgeKeepsOutOfClaimedSpace(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("lightBridge");
        Runnable release = DecorGameTests.claim(new AABB(helper.absolutePos(MACHINE.east(3))).inflate(0.1));
        bridge(helper, 1024, 1024, true);
        helper.runAfterDelay(30, () -> {
            restore.run();
            release.run();
            helper.assertTrue(isBridge(helper, MACHINE.east(1)) && isBridge(helper, MACHINE.east(2)), "the free blocks were not built");
            helper.assertFalse(isBridge(helper, MACHINE.east(3)) || isBridge(helper, MACHINE.east(4)), "built on claimed space");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "light_bridgeswitch", timeoutTicks = 100)
    public static void lightBridgeSwitchedOffBuildsNothing(GameTestHelper helper) {
        Runnable restore = DecorGameTests.disable("lightBridge");
        bridge(helper, 1024, 1024, true);
        helper.runAfterDelay(30, () -> {
            restore.run();
            helper.assertFalse(isBridge(helper, MACHINE.east()), "a switched-off bridge built");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "light_blocks", timeoutTicks = 20)
    public static void beamAndBridgeBlocksBehaveAsTheOriginals(GameTestHelper helper) {
        Block beam = DecorRegistry.BEAM.get();
        BlockState beamState = beam.defaultBlockState();
        helper.assertTrue(beamState.getLightEmission() == 15, "the beam glows at full brightness");
        helper.assertTrue(beamState.getCollisionShape(helper.getLevel(), helper.absolutePos(MACHINE)).isEmpty(), "the beam is solid");
        BlockState bridgeState = DecorRegistry.BRIDGE.get().defaultBlockState();
        helper.assertTrue(bridgeState.getLightEmission() == 8, "the bridge glows at half brightness");
        helper.assertTrue(bridgeState.getDestroySpeed(helper.getLevel(), helper.absolutePos(MACHINE)) < 0, "the bridge can be broken by hand");
        helper.succeed();
    }
}
