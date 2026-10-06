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
import net.scwunge.rotarycraft.block.SplitterBlock;
import net.scwunge.rotarycraft.blockentity.SplitterBlockEntity;
import net.scwunge.rotarycraft.blockentity.ClutchBlockEntity;
import net.scwunge.rotarycraft.blockentity.GrinderBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
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
        place(helper, 1, 2, RotaryBlocks.gearbox(ShaftMaterial.STEEL, 4).get());
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
        place(helper, 1, 2, RotaryBlocks.gearbox(ShaftMaterial.STEEL, 4).get());
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
        place(helper, 1, 2, RotaryBlocks.gearbox(ShaftMaterial.STEEL, 16).get());
        place(helper, 2, 2, RotaryBlocks.gearbox(ShaftMaterial.STEEL, 16).get());
        place(helper, 3, 2, RotaryBlocks.SHAFTS.get(ShaftMaterial.WOOD).get());
        helper.succeedWhen(() -> helper.assertBlockNotPresent(RotaryBlocks.SHAFTS.get(ShaftMaterial.WOOD).get(), new BlockPos(3, 1, 2)));
    }

    @GameTest(template = TEMPLATE)
    public static void steelShaftCarriesTheSameLoad(GameTestHelper helper) {
        poweredEngine(helper, 0, 2);
        place(helper, 1, 2, RotaryBlocks.gearbox(ShaftMaterial.STEEL, 16).get());
        place(helper, 2, 2, RotaryBlocks.gearbox(ShaftMaterial.STEEL, 16).get());
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
        place(helper, 1, 2, RotaryBlocks.gearbox(ShaftMaterial.STEEL, 16).get());
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

    static void splitter(GameTestHelper helper, int x, int z, Direction facing, Direction bent) {
        helper.setBlock(new BlockPos(x, 1, z), RotaryBlocks.SPLITTER.get().defaultBlockState()
                .setValue(MachineBlock.FACING, facing).setValue(SplitterBlock.BENT, bent));
    }

    /** Split 1:1: the front and the branch each get half the torque at full speed. */
    @GameTest(template = TEMPLATE)
    public static void splitterSplitsEvenly(GameTestHelper helper) {
        poweredEngine(helper, 0, 2);
        place(helper, 1, 2, RotaryBlocks.gearbox(ShaftMaterial.STEEL, 4).get()); // 16 N*m, 64 rad/s
        splitter(helper, 2, 2, Direction.EAST, Direction.NORTH);
        ((SplitterBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2))).toggleMode();
        place(helper, 3, 2, RotaryBlocks.DYNAMOMETER.get());
        helper.setBlock(new BlockPos(2, 1, 1), RotaryBlocks.DYNAMOMETER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        helper.succeedWhen(() -> {
            DynamometerBlockEntity front = be(helper, 3, 2, DynamometerBlockEntity.class);
            DynamometerBlockEntity branch = (DynamometerBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 1));
            helper.assertTrue(front.getTorque() == 8 && front.getOmega() == 64, "front got " + front.getTorque() + " N*m " + front.getOmega() + " rad/s");
            helper.assertTrue(branch.getTorque() == 8 && branch.getOmega() == 64, "branch got " + branch.getTorque() + " N*m " + branch.getOmega() + " rad/s");
        });
    }

    /** Split 4: 3/4 of the torque to the front, 1/4 to the branch. */
    @GameTest(template = TEMPLATE)
    public static void splitterSplitsByRatio(GameTestHelper helper) {
        poweredEngine(helper, 0, 2);
        place(helper, 1, 2, RotaryBlocks.gearbox(ShaftMaterial.STEEL, 4).get());
        splitter(helper, 2, 2, Direction.EAST, Direction.NORTH);
        SplitterBlockEntity spl = (SplitterBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
        spl.toggleMode();
        spl.cycleRatio(); // 2
        spl.cycleRatio(); // 4
        place(helper, 3, 2, RotaryBlocks.DYNAMOMETER.get());
        helper.setBlock(new BlockPos(2, 1, 1), RotaryBlocks.DYNAMOMETER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        helper.succeedWhen(() -> {
            helper.assertTrue(be(helper, 3, 2, DynamometerBlockEntity.class).getTorque() == 12, "front should get 12 of 16 N*m");
            helper.assertTrue(((DynamometerBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 1))).getTorque() == 4, "branch should get 4 of 16 N*m");
        });
    }

    /** Merge: two engines at the same speed add their torque. */
    @GameTest(template = TEMPLATE)
    public static void splitterMergesMatchingInputs(GameTestHelper helper) {
        poweredEngine(helper, 0, 2);
        splitter(helper, 1, 2, Direction.EAST, Direction.NORTH);
        helper.setBlock(new BlockPos(1, 1, 1), RotaryBlocks.DC_ENGINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.REDSTONE_BLOCK);
        place(helper, 2, 2, RotaryBlocks.DYNAMOMETER.get());
        helper.succeedWhen(() -> {
            DynamometerBlockEntity dyn = be(helper, 2, 2, DynamometerBlockEntity.class);
            helper.assertTrue(dyn.getTorque() == 8 && dyn.getOmega() == 256, "merged " + dyn.getTorque() + " N*m " + dyn.getOmega() + " rad/s, expected 8 at 256");
        });
    }

    // ---- phase 3: grinder ---------------------------------------------------------------------------------------------

    static void chargeMotor(GameTestHelper helper, int x, int z) {
        IEnergyStorage e = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(new BlockPos(x, 1, z)), Direction.UP);
        e.receiveEnergy(100_000, false);
    }

    /** Electric motor (16 N*m, 256 rad/s) through 8:1 reduction = 128 N*m at 32 rad/s = 4 kW: exactly what the grinder needs. */
    static void poweredGrinder(GameTestHelper helper) {
        place(helper, 0, 2, RotaryBlocks.ELECTRIC_MOTOR.get());
        chargeMotor(helper, 0, 2);
        place(helper, 1, 2, RotaryBlocks.gearbox(ShaftMaterial.STEEL, 8).get());
        // a dry steel gearbox wears, and 128 N*m is exactly the grinder's minimum, so lubricate it as a player would
        be(helper, 1, 2, net.scwunge.rotarycraft.blockentity.GearboxBlockEntity.class).lubricant().fill(
                new net.neoforged.neoforge.fluids.FluidStack(net.scwunge.rotarycraft.registry.RotaryFluids.LUBRICANT.get(), 24000),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        place(helper, 2, 2, RotaryBlocks.GRINDER.get());
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void grinderGrindsCobbleToGravel(GameTestHelper helper) {
        poweredGrinder(helper);
        GrinderBlockEntity grinder = be(helper, 2, 2, GrinderBlockEntity.class);
        grinder.items().setStackInSlot(GrinderBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE, 2));
        helper.startSequence()
                .thenExecuteAfter(300, () -> chargeMotor(helper, 0, 2))
                .thenWaitUntil(() -> {
                    GrinderBlockEntity g = be(helper, 2, 2, GrinderBlockEntity.class);
                    helper.assertTrue(g.hasEnoughPower(), "grinder underpowered: " + g.getTorque() + " N*m " + g.getOmega() + " rad/s");
                    helper.assertTrue(g.items().getStackInSlot(GrinderBlockEntity.SLOT_OUTPUT).is(Items.GRAVEL), "no gravel yet, progress " + g.progress());
                })
                .thenExecute(() -> helper.assertTrue(be(helper, 2, 2, GrinderBlockEntity.class).items().getStackInSlot(GrinderBlockEntity.SLOT_INPUT).getCount() == 1,
                        "one cobblestone should have been used"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void grinderWontRunUnderpowered(GameTestHelper helper) {
        poweredEngine(helper, 0, 2); // 1 kW, 4 N*m: far below 128 N*m and 4 kW
        place(helper, 1, 2, RotaryBlocks.GRINDER.get());
        be(helper, 1, 2, GrinderBlockEntity.class).items().setStackInSlot(GrinderBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE));
        helper.runAfterDelay(60, () -> {
            GrinderBlockEntity g = be(helper, 1, 2, GrinderBlockEntity.class);
            helper.assertTrue(!g.hasEnoughPower() && g.progress() == 0, "underpowered grinder made progress");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void grinderTimeFollowsTheOriginalFormula(GameTestHelper helper) {
        // 840 - 60 x log2(omega + 1): 32 rad/s -> 537 ticks, 1024 rad/s -> 240 ticks
        helper.assertTrue(net.scwunge.rotarycraft.power.PowerRequirement.operationTime(840, 60, 32) == 537, "32 rad/s");
        helper.assertTrue(net.scwunge.rotarycraft.power.PowerRequirement.operationTime(840, 60, 1024) == 239, "1024 rad/s gave "
                + net.scwunge.rotarycraft.power.PowerRequirement.operationTime(840, 60, 1024));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void grinderAutomationOnlyFillsInputAndEmptiesOutput(GameTestHelper helper) {
        place(helper, 1, 2, RotaryBlocks.GRINDER.get());
        IItemHandler h = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(new BlockPos(1, 1, 2)), Direction.UP);
        helper.assertTrue(h != null, "grinder exposes no item handler");
        helper.assertTrue(h.insertItem(GrinderBlockEntity.SLOT_INPUT, new ItemStack(Items.COBBLESTONE), false).isEmpty(), "can't insert into the input");
        helper.assertTrue(!h.insertItem(GrinderBlockEntity.SLOT_OUTPUT, new ItemStack(Items.GRAVEL), false).isEmpty(), "inserted into the output");
        helper.assertTrue(h.extractItem(GrinderBlockEntity.SLOT_INPUT, 1, false).isEmpty(), "extracted from the input");
        be(helper, 1, 2, GrinderBlockEntity.class).items().setStackInSlot(GrinderBlockEntity.SLOT_OUTPUT, new ItemStack(Items.GRAVEL));
        helper.assertTrue(h.extractItem(GrinderBlockEntity.SLOT_OUTPUT, 1, false).is(Items.GRAVEL), "can't extract the output");
        helper.succeed();
    }
}
