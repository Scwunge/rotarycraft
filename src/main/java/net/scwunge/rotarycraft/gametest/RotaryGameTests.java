package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.DynamometerBlockEntity;
import net.scwunge.rotarycraft.block.BevelGearBlock;
import net.scwunge.rotarycraft.blockentity.ClutchBlockEntity;
import net.scwunge.rotarycraft.blockentity.DCEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.FlywheelBlockEntity;
import net.scwunge.rotarycraft.blockentity.SteamEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.WindEngineBlockEntity;
import net.scwunge.rotarycraft.power.FlywheelType;
import net.scwunge.rotarycraft.blockentity.GearboxBlockEntity;
import net.scwunge.rotarycraft.blockentity.GeneratorBlockEntity;
import net.scwunge.rotarycraft.blockentity.MotorBlockEntity;
import net.scwunge.rotarycraft.power.ShaftMaterial;
import net.scwunge.rotarycraft.registry.RotaryBlocks;

/** Run with: gradlew runGameTestServer */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class RotaryGameTests {
    static final String TEMPLATE = "empty5x4x5";

    static void place(GameTestHelper helper, int x, int z, Block block) {
        helper.setBlock(new BlockPos(x, 1, z), block.defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
    }

    /** A DC engine with a redstone block on top. Power leaves eastward (+x in test space for the default rotation). */
    static void poweredEngine(GameTestHelper helper, int x, int z) {
        place(helper, x, z, RotaryBlocks.DC_ENGINE.get());
        helper.setBlock(new BlockPos(x, 2, z), Blocks.REDSTONE_BLOCK);
    }

    static <T> T be(GameTestHelper helper, int x, int z, Class<T> type) {
        return type.cast(helper.getBlockEntity(new BlockPos(x, 1, z)));
    }

    @GameTest(template = TEMPLATE)
    public static void shaftLimitsMatchTheOriginal(GameTestHelper helper) {
        // r = 0.0625 m torsion/tensile limits from the original's formulas
        helper.assertTrue(Math.abs(ShaftMaterial.WOOD.maxTorque - 278) < 2, "wood torque limit " + ShaftMaterial.WOOD.maxTorque);
        helper.assertTrue(Math.abs(ShaftMaterial.STEEL.maxTorque - 6711) < 5, "steel torque limit " + ShaftMaterial.STEEL.maxTorque);
        helper.assertTrue(ShaftMaterial.STEEL.maxSpeed > ShaftMaterial.STONE.maxSpeed && ShaftMaterial.STONE.maxSpeed > ShaftMaterial.WOOD.maxSpeed,
                "speed limits not ordered by strength");
        helper.assertTrue(!ShaftMaterial.BEDROCK.fails(Integer.MAX_VALUE, Integer.MAX_VALUE), "bedrock shaft can fail");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void engineOnlyRunsWithRedstone(GameTestHelper helper) {
        place(helper, 1, 2, RotaryBlocks.DC_ENGINE.get());
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(be(helper, 1, 2, DCEngineBlockEntity.class).getPower() == 0, "engine runs without redstone");
            helper.setBlock(new BlockPos(1, 2, 2), Blocks.REDSTONE_BLOCK);
            helper.succeedWhen(() -> helper.assertTrue(be(helper, 1, 2, DCEngineBlockEntity.class).getPower() == 1024,
                    "powered DC engine should give 4 N*m x 256 rad/s = 1024 W"));
        });
    }

    @GameTest(template = TEMPLATE)
    public static void engineShaftGeneratorMakesFe(GameTestHelper helper) {
        poweredEngine(helper, 0, 2);
        place(helper, 1, 2, RotaryBlocks.SHAFTS.get(ShaftMaterial.STEEL).get());
        place(helper, 2, 2, RotaryBlocks.GENERATOR.get());
        helper.succeedWhen(() -> {
            GeneratorBlockEntity gen = be(helper, 2, 2, GeneratorBlockEntity.class);
            helper.assertTrue(gen.getTorque() == 4 && gen.getOmega() == 256, "generator input " + gen.getTorque() + " N*m " + gen.getOmega() + " rad/s");
            // 1024 W / 20 W per FE = 51 FE per tick
            helper.assertTrue(gen.fePerTick() == 51, "generator makes " + gen.fePerTick() + " FE/t, expected 51");
            helper.assertTrue(gen.energy().getEnergyStored() > 0, "no FE stored");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void generatorPushesFeIntoNeighbours(GameTestHelper helper) {
        poweredEngine(helper, 0, 2);
        place(helper, 1, 2, RotaryBlocks.GENERATOR.get());
        place(helper, 2, 2, RotaryBlocks.ELECTRIC_MOTOR.get()); // accepts FE from its back
        helper.succeedWhen(() -> {
            MotorBlockEntity motor = be(helper, 2, 2, MotorBlockEntity.class);
            helper.assertTrue(motor.energy().getEnergyStored() > 0, "motor received no FE from the generator");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void gearboxReducesSpeedAndMultipliesTorque(GameTestHelper helper) {
        poweredEngine(helper, 0, 2);
        place(helper, 1, 2, RotaryBlocks.GEARBOXES.get(4).get());
        place(helper, 2, 2, RotaryBlocks.DYNAMOMETER.get());
        helper.succeedWhen(() -> {
            DynamometerBlockEntity dyn = be(helper, 2, 2, DynamometerBlockEntity.class);
            helper.assertTrue(dyn.getTorque() == 16 && dyn.getOmega() == 64, "4x reduction gave " + dyn.getTorque() + " N*m " + dyn.getOmega() + " rad/s");
            helper.assertTrue(dyn.getPower() == 1024, "power not conserved: " + dyn.getPower());
        });
    }

    @GameTest(template = TEMPLATE)
    public static void gearboxAccelerates(GameTestHelper helper) {
        poweredEngine(helper, 0, 2);
        place(helper, 1, 2, RotaryBlocks.GEARBOXES.get(4).get());
        be(helper, 1, 2, GearboxBlockEntity.class).toggleMode();
        place(helper, 2, 2, RotaryBlocks.DYNAMOMETER.get());
        helper.succeedWhen(() -> {
            DynamometerBlockEntity dyn = be(helper, 2, 2, DynamometerBlockEntity.class);
            helper.assertTrue(dyn.getTorque() == 1 && dyn.getOmega() == 1024, "4x acceleration gave " + dyn.getTorque() + " N*m " + dyn.getOmega() + " rad/s");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void overloadedWoodShaftBreaks(GameTestHelper helper) {
        // 4 N*m x 16 x 16 = 1024 N*m, far over a wood shaft's ~278 N*m
        poweredEngine(helper, 0, 2);
        place(helper, 1, 2, RotaryBlocks.GEARBOXES.get(16).get());
        place(helper, 2, 2, RotaryBlocks.GEARBOXES.get(16).get());
        place(helper, 3, 2, RotaryBlocks.SHAFTS.get(ShaftMaterial.WOOD).get());
        helper.succeedWhen(() -> helper.assertBlockNotPresent(RotaryBlocks.SHAFTS.get(ShaftMaterial.WOOD).get(), new BlockPos(3, 1, 2)));
    }

    @GameTest(template = TEMPLATE)
    public static void steelShaftCarriesTheSameLoad(GameTestHelper helper) {
        poweredEngine(helper, 0, 2);
        place(helper, 1, 2, RotaryBlocks.GEARBOXES.get(16).get());
        place(helper, 2, 2, RotaryBlocks.GEARBOXES.get(16).get());
        place(helper, 3, 2, RotaryBlocks.SHAFTS.get(ShaftMaterial.STEEL).get());
        helper.runAfterDelay(20, () -> {
            helper.assertBlockPresent(RotaryBlocks.SHAFTS.get(ShaftMaterial.STEEL).get(), new BlockPos(3, 1, 2));
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void motorRunsOnFe(GameTestHelper helper) {
        place(helper, 1, 2, RotaryBlocks.ELECTRIC_MOTOR.get());
        place(helper, 2, 2, RotaryBlocks.DYNAMOMETER.get());
        IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(new BlockPos(1, 1, 2)), Direction.UP);
        helper.assertTrue(energy != null && energy.receiveEnergy(50_000, false) == 50_000, "motor did not accept FE");
        helper.succeedWhen(() -> {
            DynamometerBlockEntity dyn = be(helper, 2, 2, DynamometerBlockEntity.class);
            helper.assertTrue(dyn.getTorque() == 16 && dyn.getOmega() == 256, "motor gave " + dyn.getTorque() + " N*m " + dyn.getOmega() + " rad/s");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void dynamometerComparatorSignal(GameTestHelper helper) {
        helper.assertTrue(DynamometerBlockEntity.signalFor(0) == 0, "no power should give 0");
        helper.assertTrue(DynamometerBlockEntity.signalFor(1024) == 6, "1 kW gave " + DynamometerBlockEntity.signalFor(1024));
        helper.assertTrue(DynamometerBlockEntity.signalFor(Long.MAX_VALUE) == 15, "huge power should cap at 15");
        helper.succeed();
    }

    // ---- phase 2 ------------------------------------------------------------------------------------------------------

    /** A wood flywheel behind a 16:1 reduction spins up to the input speed, then coasts down when the input is cut. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void flywheelSpinsUpAndCoasts(GameTestHelper helper) {
        poweredEngine(helper, 0, 2);
        place(helper, 1, 2, RotaryBlocks.GEARBOXES.get(16).get());
        place(helper, 2, 2, RotaryBlocks.FLYWHEELS.get(FlywheelType.WOOD).get());
        helper.startSequence()
                .thenWaitUntil(() -> {
                    FlywheelBlockEntity fw = be(helper, 2, 2, FlywheelBlockEntity.class);
                    helper.assertTrue(fw.getOmega() == 16 && fw.getTorque() == 16,
                            "flywheel at " + fw.getTorque() + " N*m " + fw.getOmega() + " rad/s, expected 16/16 (torque capped at the wood rating)");
                })
                .thenExecute(() -> helper.setBlock(new BlockPos(1, 1, 2), Blocks.AIR))
                .thenExecuteAfter(6, () -> {
                    int w = be(helper, 2, 2, FlywheelBlockEntity.class).getOmega();
                    helper.assertTrue(w > 0 && w < 16, "flywheel should be coasting down, speed " + w);
                })
                .thenWaitUntil(() -> helper.assertTrue(be(helper, 2, 2, FlywheelBlockEntity.class).getOmega() == 0, "flywheel never stopped"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void flywheelBurstsWhenOverspun(GameTestHelper helper) {
        // wood: 800 kg/m^3, 20 MPa x100 -> bursts above ~2980 rad/s
        helper.assertTrue(!FlywheelType.WOOD.fails(2900) && FlywheelType.WOOD.fails(3100), "wood flywheel burst speed is off");
        helper.assertTrue(!FlywheelType.BEDROCK.fails(Integer.MAX_VALUE), "bedrock flywheel can burst");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void clutchTransmitsOnlyWithRedstone(GameTestHelper helper) {
        poweredEngine(helper, 0, 2);
        place(helper, 1, 2, RotaryBlocks.CLUTCH.get());
        place(helper, 2, 2, RotaryBlocks.DYNAMOMETER.get());
        helper.startSequence()
                .thenExecuteAfter(20, () -> helper.assertTrue(be(helper, 2, 2, DynamometerBlockEntity.class).getPower() == 0,
                        "unpowered clutch let power through"))
                .thenExecute(() -> helper.setBlock(new BlockPos(1, 1, 1), Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> helper.assertTrue(be(helper, 2, 2, DynamometerBlockEntity.class).getPower() == 1024,
                        "powered clutch should pass the engine's 1 kW"))
                .thenExecute(() -> be(helper, 1, 2, ClutchBlockEntity.class).toggleMode())
                .thenWaitUntil(() -> helper.assertTrue(be(helper, 2, 2, DynamometerBlockEntity.class).getPower() == 0,
                        "inverted clutch still transmits while powered"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void bevelGearTurnsPowerNinetyDegrees(GameTestHelper helper) {
        poweredEngine(helper, 0, 2);
        helper.setBlock(new BlockPos(1, 1, 2), RotaryBlocks.BEVEL_GEAR.get().defaultBlockState()
                .setValue(MachineBlock.FACING, Direction.NORTH).setValue(BevelGearBlock.INPUT, Direction.WEST));
        helper.setBlock(new BlockPos(1, 1, 1), RotaryBlocks.DYNAMOMETER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        helper.succeedWhen(() -> {
            DynamometerBlockEntity dyn = (DynamometerBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 1));
            helper.assertTrue(dyn.getTorque() == 4 && dyn.getOmega() == 256, "bevel output " + dyn.getTorque() + " N*m " + dyn.getOmega() + " rad/s");
        });
    }

    // Wind in clear air is checked in the real game: GameTest worlds pack other test structures right behind the blades.

    @GameTest(template = TEMPLATE)
    public static void windEngineStopsWhenBladesBlocked(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 2), RotaryBlocks.WIND_ENGINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(0, 2, 2), Blocks.STONE);
        helper.runAfterDelay(40, () -> {
            WindEngineBlockEntity wind = (WindEngineBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 2));
            helper.assertTrue(wind.getPower() == 0, "wind engine runs with its blades blocked");
            helper.succeed();
        });
    }

    /** Over lava the steam engine heats 2 C a second; at 100 C with water it runs at 32 N*m. */
    @GameTest(template = TEMPLATE, timeoutTicks = 1600)
    public static void steamEngineRunsOverLava(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.LAVA);
        helper.setBlock(new BlockPos(2, 2, 2), RotaryBlocks.STEAM_ENGINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        SteamEngineBlockEntity steam = (SteamEngineBlockEntity) helper.getBlockEntity(new BlockPos(2, 2, 2));
        steam.fillWater(20_000);
        helper.succeedWhen(() -> {
            SteamEngineBlockEntity s = (SteamEngineBlockEntity) helper.getBlockEntity(new BlockPos(2, 2, 2));
            helper.assertTrue(s.temperature() >= 100, "steam engine at " + s.temperature() + " C");
            helper.assertTrue(s.getTorque() == SteamEngineBlockEntity.TORQUE && s.getOmega() > 0, "steam engine not running");
            helper.assertTrue(s.water().getFluidAmount() < 20_000, "no water used");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void steamEngineNeedsWater(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.LAVA);
        helper.setBlock(new BlockPos(2, 2, 2), RotaryBlocks.STEAM_ENGINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(((SteamEngineBlockEntity) helper.getBlockEntity(new BlockPos(2, 2, 2))).getPower() == 0, "dry steam engine runs");
            helper.succeed();
        });
    }

    /** Fire below heats it 1 C a second (net of losses at low temperature). */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void steamEngineHeatsOverFire(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.NETHERRACK);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.FIRE);
        helper.setBlock(new BlockPos(2, 2, 2), RotaryBlocks.STEAM_ENGINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.runAfterDelay(300, () -> {
            SteamEngineBlockEntity s = (SteamEngineBlockEntity) helper.getBlockEntity(new BlockPos(2, 2, 2));
            helper.assertBlockPresent(Blocks.FIRE, new BlockPos(2, 1, 2));
            helper.assertTrue(s.temperature() >= 30, "steam engine over fire only reached " + s.temperature() + " C after 15 s");
            helper.succeed();
        });
    }
}
