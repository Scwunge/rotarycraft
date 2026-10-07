package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.config.FarmConfig;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.process.AirCompressorBlockEntity;
import net.scwunge.rotarycraft.process.BoilerBlockEntity;
import net.scwunge.rotarycraft.process.BigFurnaceBlockEntity;
import net.scwunge.rotarycraft.process.DistillerBlockEntity;
import net.scwunge.rotarycraft.process.DynamoBlockEntity;
import net.scwunge.rotarycraft.process.FuelEnhancerBlockEntity;
import net.scwunge.rotarycraft.process.GasTankBlockEntity;
import net.scwunge.rotarycraft.blockentity.PipeBlockEntity;
import net.scwunge.rotarycraft.pipe.PipeType;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.scwunge.rotarycraft.process.MagneticMotorBlockEntity;
import net.scwunge.rotarycraft.process.PneumaticEngineBlockEntity;
import net.scwunge.rotarycraft.process.SteamTurbineBlockEntity;
import net.scwunge.rotarycraft.registry.ProcessRegistry;
import net.scwunge.rotarycraft.registry.RotaryFluids;

/** Tests of the fluid and process machines. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ProcessGameTests {
    static final String TEMPLATE = RotaryGameTests.TEMPLATE;
    static final BlockPos AT = new BlockPos(2, 2, 2);

    // ---- the engines that turn an energy into shaft power ----

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = "process")
    public static void aMagneticMotorSpinsUpOnFeAndCoastsDownWithout(GameTestHelper helper) {
        helper.setBlock(AT, ProcessRegistry.MAGNETIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        MagneticMotorBlockEntity motor = helper.getBlockEntity(AT);
        helper.assertTrue(motor.energy().receiveEnergy(1_000_000, false) > 0, "took no energy");
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(motor.getOmega() == motor.maxSpeed() && motor.getTorque() == motor.ratedTorque(), "speed " + motor.getOmega() + " at " + motor.getTorque());
            helper.assertTrue(motor.stored() < motor.maxStorage(), "used no energy");
        });
        helper.runAfterDelay(190, () -> {
            helper.assertTrue(motor.getOmega() < motor.maxSpeed(), "kept speed without energy: " + motor.getOmega());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = "process")
    public static void theConvertersTierSetsTheirTorqueAndSpeed(GameTestHelper helper) {
        helper.setBlock(AT, ProcessRegistry.MAGNETIC_MOTOR.get());
        MagneticMotorBlockEntity motor = helper.getBlockEntity(AT);
        RotaryConfig.override(FarmConfig.CONVERTER_TIER, 3);
        int torque = motor.ratedTorque(), speed = motor.maxSpeed();
        double efficiency = motor.efficiency();
        RotaryConfig.override(FarmConfig.CONVERTER_TIER, 0);
        int torque0 = motor.ratedTorque(), speed0 = motor.maxSpeed();
        double efficiency0 = motor.efficiency();
        RotaryConfig.clearOverride(FarmConfig.CONVERTER_TIER);
        helper.assertTrue(torque == 512 && speed == 2048, "tier 3: " + torque + " N*m at " + speed);
        helper.assertTrue(torque0 == 8 && speed0 == 256, "tier 0: " + torque0 + " N*m at " + speed0);
        helper.assertTrue(Math.abs(efficiency - 0.66) < 1e-9 && Math.abs(efficiency0 - 0.9) < 1e-9, "efficiency " + efficiency + ", " + efficiency0);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = "process")
    public static void aSteamTurbineRunsOnSteamAndNothingElse(GameTestHelper helper) {
        helper.setBlock(AT, ProcessRegistry.STEAM_TURBINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        SteamTurbineBlockEntity turbine = helper.getBlockEntity(AT);
        IFluidHandler steam = turbine.steam();
        helper.assertTrue(steam.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "took water");
        helper.assertTrue(steam.fill(new FluidStack(ProcessRegistry.steam(), 100_000), IFluidHandler.FluidAction.EXECUTE) == 100_000, "did not take steam");
        helper.assertTrue(steam.drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "gave steam back");
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(turbine.getOmega() == turbine.maxSpeed(), "speed " + turbine.getOmega());
            helper.assertTrue(turbine.stored() < 100_000, "used no steam");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = "process")
    public static void aPneumaticEngineRunsOnCompressedAir(GameTestHelper helper) {
        helper.setBlock(AT, ProcessRegistry.PNEUMATIC_ENGINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        PneumaticEngineBlockEntity engine = helper.getBlockEntity(AT);
        helper.assertTrue(engine.air().fill(new FluidStack(ProcessRegistry.steam(), 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "took steam");
        helper.assertTrue(engine.air().fill(new FluidStack(ProcessRegistry.compressedAir(), 30_000), IFluidHandler.FluidAction.EXECUTE) == 30_000, "did not take air");
        helper.runAfterDelay(18, () -> {
            helper.assertTrue(engine.getOmega() == engine.maxSpeed(), "speed " + engine.getOmega());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60, batch = "process")
    public static void anEngineWithTooLittleEnergyDoesNotStart(GameTestHelper helper) {
        helper.setBlock(AT, ProcessRegistry.MAGNETIC_MOTOR.get());
        MagneticMotorBlockEntity motor = helper.getBlockEntity(AT);
        motor.energy().receiveEnergy(5, false);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(motor.getOmega() == 0, "started on 5 FE");
            helper.succeed();
        });
    }

    // ---- Boiler ----

    static BoilerBlockEntity boiler(GameTestHelper helper, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, AT.below(), torque, omega);
        helper.setBlock(AT, ProcessRegistry.BOILER.get());
        BoilerBlockEntity boiler = helper.getBlockEntity(AT);
        boiler.water().fill(new FluidStack(Fluids.WATER, 10_000), IFluidHandler.FluidAction.EXECUTE);
        return boiler;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = "process")
    public static void aHotBoilerMakesEightMillibucketsOfSteamFromOneOfWater(GameTestHelper helper) {
        var boiler = boiler(helper, 64, 1024);
        boiler.setTemperature(150);
        helper.succeedWhen(() -> {
            int steam = boiler.steam().getFluidAmount();
            helper.assertTrue(steam > 0, "no steam yet");
            helper.assertTrue(steam % BoilerBlockEntity.STEAM_PER_WATER == 0, "steam " + steam);
            helper.assertTrue(10_000 - boiler.water().getFluidAmount() == steam / BoilerBlockEntity.STEAM_PER_WATER, "water used " + (10_000 - boiler.water().getFluidAmount()) + " for " + steam);
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80, batch = "process")
    public static void aColdBoilerMakesNoSteam(GameTestHelper helper) {
        var boiler = boiler(helper, 64, 1024);
        boiler.setTemperature(60);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(boiler.steam().getFluidAmount() == 0 && boiler.getTemperature() <= 100, "steam below 100 C: " + boiler.steam().getFluidAmount() + " at " + boiler.getTemperature());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60, batch = "process")
    public static void aBoilerTakesOnlyWaterAndGivesOnlySteam(GameTestHelper helper) {
        var boiler = boiler(helper, 64, 1024);
        IFluidHandler pipes = boiler.handler();
        helper.assertTrue(pipes.fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE) == 0, "took lava");
        helper.assertTrue(pipes.fill(new FluidStack(ProcessRegistry.steam(), 100), IFluidHandler.FluidAction.EXECUTE) == 0, "took steam");
        helper.assertTrue(pipes.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "gave water");
        boiler.steam().fill(new FluidStack(ProcessRegistry.steam(), 400), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(pipes.drain(100, IFluidHandler.FluidAction.EXECUTE).getFluid() == ProcessRegistry.steam(), "did not give steam");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = "process_boiler_blast")
    public static void aBoilerHeatedPastFiveHundredDegreesBlowsUp(GameTestHelper helper) {
        var boiler = boiler(helper, 64, 1024);
        boiler.setTemperature(600);
        helper.succeedWhen(() -> helper.assertBlock(AT, b -> b == Blocks.AIR, () -> "it is still there at " + boiler.getTemperature()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = "process_boiler_off")
    public static void aBoilerSwitchedOffDoesNothing(GameTestHelper helper) {
        RotaryConfig.override(FarmConfig.MACHINES.get("boiler"), false);
        var boiler = boiler(helper, 64, 1024);
        boiler.setTemperature(150);
        helper.runAfterDelay(40, () -> {
            RotaryConfig.clearOverride(FarmConfig.MACHINES.get("boiler"));
            helper.assertTrue(boiler.steam().getFluidAmount() == 0, "made steam while off");
            helper.succeed();
        });
    }

    /** A boiler under a turbine: the steam goes up into it and spins it. */
    @GameTest(template = TEMPLATE, timeoutTicks = 300, batch = "process")
    public static void aBoilerFeedsASteamTurbineAboveIt(GameTestHelper helper) {
        var boiler = boiler(helper, 64, 1024);
        boiler.setTemperature(150);
        helper.setBlock(AT.above(), ProcessRegistry.STEAM_TURBINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        SteamTurbineBlockEntity turbine = helper.getBlockEntity(AT.above());
        helper.succeedWhen(() -> helper.assertTrue(turbine.getOmega() > 0, "the turbine is still"));
    }

    // ---- Air Compressor ----

    @GameTest(template = TEMPLATE, timeoutTicks = 300, batch = "process")
    public static void anAirCompressorFeedsAPneumaticEngineInFrontOfIt(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 2);
        WeaponGameTests.spinningFlywheel(helper, at.west(), 64, 1024, Direction.EAST);
        helper.setBlock(at, ProcessRegistry.AIR_COMPRESSOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(at.east(), ProcessRegistry.PNEUMATIC_ENGINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        PneumaticEngineBlockEntity engine = helper.getBlockEntity(at.east());
        helper.succeedWhen(() -> helper.assertTrue(engine.stored() > 0 && engine.getOmega() > 0, "stored " + engine.stored() + ", speed " + engine.getOmega()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = "process")
    public static void anAirCompressorsOutputFollowsThePower(GameTestHelper helper) {
        WeaponGameTests.spinningFlywheel(helper, AT.below(), 4, 1000, Direction.UP);
        helper.setBlock(AT, ProcessRegistry.AIR_COMPRESSOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.UP));
        AirCompressorBlockEntity compressor = helper.getBlockEntity(AT);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(compressor.generated() == 4000 / 20, "made " + compressor.generated() + " a tick on 4 kW");
            helper.succeed();
        });
    }

    // ---- Dynamo ----

    @GameTest(template = TEMPLATE, timeoutTicks = 300, batch = "process")
    public static void aDynamoMakesEnergyFromShaftPowerAndPassesItOnFromItsFront(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 2);
        WeaponGameTests.spinningFlywheel(helper, at.west(), 4, 1000, Direction.EAST);
        helper.setBlock(at, ProcessRegistry.DYNAMO.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(at.east(), ProcessRegistry.MAGNETIC_MOTOR.get());
        DynamoBlockEntity dynamo = helper.getBlockEntity(at);
        MagneticMotorBlockEntity motor = helper.getBlockEntity(at.east());
        helper.succeedWhen(() -> {
            helper.assertTrue(dynamo.generated() == 4000 / RotaryConfig.get(RotaryConfig.WATTS_PER_FE), "makes " + dynamo.generated());
            helper.assertTrue(motor.stored() > 0, "nothing arrived");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60, batch = "process")
    public static void aDynamoCountsOnlyUpToItsTorqueAndSpeedLimits(GameTestHelper helper) {
        WeaponGameTests.spinningFlywheel(helper, AT.south(), 4096, 16384, Direction.NORTH);
        helper.setBlock(AT, ProcessRegistry.DYNAMO.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        DynamoBlockEntity dynamo = helper.getBlockEntity(AT);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(dynamo.generated() == (int) ((long) DynamoBlockEntity.MAX_TORQUE * DynamoBlockEntity.MAX_OMEGA / RotaryConfig.get(RotaryConfig.WATTS_PER_FE)),
                    "makes " + dynamo.generated());
            helper.succeed();
        });
    }

    // ---- Gas Tank ----

    static GasTankBlockEntity gasTank(GameTestHelper helper, int torque) {
        WeaponGameTests.spinningFlywheel(helper, AT.below(), torque, 64, Direction.UP);
        helper.setBlock(AT, ProcessRegistry.GAS_TANK.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.UP));
        return helper.getBlockEntity(AT);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "process")
    public static void aGasTanksCapacityFollowsTheTorqueAndIsEightTimesForAGas(GameTestHelper helper) {
        helper.assertTrue(GasTankBlockEntity.capacityAt(1, false) == 0 && GasTankBlockEntity.capacityAt(0, true) == 0, "held fluid without torque");
        helper.assertTrue(GasTankBlockEntity.capacityAt(4096, false) == 25_000, "liquid at 4096: " + GasTankBlockEntity.capacityAt(4096, false));
        helper.assertTrue(GasTankBlockEntity.capacityAt(4096, true) == 200_000, "gas at 4096: " + GasTankBlockEntity.capacityAt(4096, true));
        helper.assertTrue(GasTankBlockEntity.capacityAt(Integer.MAX_VALUE, true) == GasTankBlockEntity.LIMIT, "not capped");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60, batch = "process")
    public static void aGasTankHoldsOneFluidUpToItsCapacityAndGivesItFromTheTop(GameTestHelper helper) {
        var tank = gasTank(helper, 4096);
        helper.runAfterDelay(5, () -> {
            IFluidHandler top = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(AT), Direction.UP);
            IFluidHandler side = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(AT), Direction.EAST);
            helper.assertTrue(side.fill(new FluidStack(Fluids.WATER, 30_000), IFluidHandler.FluidAction.EXECUTE) == 25_000, "water held " + tank.contents().getAmount());
            helper.assertTrue(side.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "took a second fluid");
            helper.assertTrue(side.drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "gave from the side");
            helper.assertTrue(top.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "took at the top");
            helper.assertTrue(top.drain(10_000, IFluidHandler.FluidAction.EXECUTE).getAmount() == 10_000, "gave nothing from the top");
            helper.assertTrue(tank.contents().getAmount() == 15_000, "has " + tank.contents().getAmount());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60, batch = "process")
    public static void aGasTankHoldsEightTimesAsMuchOfAGas(GameTestHelper helper) {
        var tank = gasTank(helper, 4096);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(tank.handler().fill(new FluidStack(ProcessRegistry.steam(), 1_000_000), IFluidHandler.FluidAction.EXECUTE) == 200_000, "steam held " + tank.contents().getAmount());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60, batch = "process")
    public static void anUnturnedGasTankTakesNothing(GameTestHelper helper) {
        helper.setBlock(AT, ProcessRegistry.GAS_TANK.get());
        GasTankBlockEntity tank = helper.getBlockEntity(AT);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(tank.handler().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "took water unturned");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60, batch = "process")
    public static void aGasTankKeepsWhatItHeldInItsItem(GameTestHelper helper) {
        var tank = gasTank(helper, 4096);
        helper.runAfterDelay(5, () -> {
            tank.handler().fill(new FluidStack(Fluids.WATER, 7000), IFluidHandler.FluidAction.EXECUTE);
            var components = tank.collectComponents();
            BlockPos other = new BlockPos(4, 2, 4);
            helper.setBlock(other, ProcessRegistry.GAS_TANK.get());
            GasTankBlockEntity placed = helper.getBlockEntity(other);
            placed.applyComponents(components, net.minecraft.core.component.DataComponentPatch.EMPTY);
            helper.assertTrue(placed.contents().getFluid() == Fluids.WATER && placed.contents().getAmount() == 7000, "carried " + placed.contents());
            helper.succeed();
        });
    }

    // ---- Pipe Pump ----

    static PipeBlockEntity pipe(GameTestHelper helper, BlockPos at) {
        helper.setBlock(at, RotaryBlocks.PIPES.get(PipeType.PIPE).get().defaultBlockState());
        helper.getLevel().scheduleTick(helper.absolutePos(at), RotaryBlocks.PIPES.get(PipeType.PIPE).get(), 1);
        return helper.getBlockEntity(at);
    }

    static PipeBlockEntity[] pumped(GameTestHelper helper, boolean turned) {
        BlockPos pump = new BlockPos(2, 2, 2);
        helper.setBlock(pump, ProcessRegistry.PIPE_PUMP.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        if (turned) {
            WeaponGameTests.spinningFlywheel(helper, pump.above(), 64, 1024, Direction.DOWN);
        }
        var from = pipe(helper, pump.west());
        var to = pipe(helper, pump.east());
        from.input().fill(new FluidStack(Fluids.WATER, 5000), IFluidHandler.FluidAction.EXECUTE);
        return new PipeBlockEntity[] {from, to};
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = "process")
    public static void aPipePumpMovesFluidFromThePipeBehindIntoThePipeInFront(GameTestHelper helper) {
        var pipes = pumped(helper, true);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(pipes[1].amount() > 0, "nothing arrived");
            helper.assertTrue(pipes[0].amount() + pipes[1].amount() == 5000, "lost fluid: " + pipes[0].amount() + " + " + pipes[1].amount());
            helper.assertTrue(pipes[1].amount() == 5000 && pipes[0].amount() == 0, "moved " + pipes[1].amount());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = "process")
    public static void aPipePumpMovesAQuarterOfItsSpeedEachTick(GameTestHelper helper) {
        var pipes = pumped(helper, true);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(pipes[1].amount() >= 256 && pipes[1].amount() <= 768, "moved " + pipes[1].amount() + " in three ticks");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = "process")
    public static void anUnturnedPipePumpMovesNothingAndTheBlockedPipesStayApart(GameTestHelper helper) {
        var pipes = pumped(helper, false);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(pipes[1].amount() == 0 && pipes[0].amount() == 5000, "moved " + pipes[1].amount());
            helper.succeed();
        });
    }

    // ---- Distiller ----

    /** Water stands in for the oil other mods bring: 1 mB becomes 6 of lubricant at 2048 N*m and 8192 W. */
    static synchronized void distillWater() {
        for (var c : DistillerBlockEntity.CONVERSIONS) {
            if (c.input() == net.minecraft.tags.FluidTags.WATER) {
                return;
            }
        }
        DistillerBlockEntity.addConversion(new DistillerBlockEntity.Conversion(net.minecraft.tags.FluidTags.WATER, () -> RotaryFluids.LUBRICANT.get(), 1, 6, 2048, 8192));
    }

    static DistillerBlockEntity distiller(GameTestHelper helper, int torque, int omega) {
        distillWater();
        WeaponGameTests.spinningFlywheel(helper, AT.below(), torque, omega, Direction.UP);
        helper.setBlock(AT, ProcessRegistry.DISTILLER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.UP));
        DistillerBlockEntity still = helper.getBlockEntity(AT);
        still.input().fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE);
        return still;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = "process")
    public static void aDistillerTurnsOneMillibucketIntoSixEverySixTicks(GameTestHelper helper) {
        var still = distiller(helper, 4096, 64);
        helper.runAfterDelay(14, () -> {
            helper.assertTrue(still.output().getFluidAmount() == 12 && still.output().getFluid().getFluid() == RotaryFluids.LUBRICANT.get(), "made " + still.output().getFluid());
            helper.assertTrue(still.input().getFluidAmount() == 98, "left " + still.input().getFluidAmount());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = "process")
    public static void aDistillerNeedsTheTorqueOfItsConversion(GameTestHelper helper) {
        var still = distiller(helper, 1024, 512);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(still.output().isEmpty() && still.input().getFluidAmount() == 100, "made " + still.output().getFluid());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = "process")
    public static void aDistillerWithAFullOutputKeepsItsInput(GameTestHelper helper) {
        var still = distiller(helper, 4096, 64);
        still.output().fill(new FluidStack(RotaryFluids.LUBRICANT.get(), DistillerBlockEntity.CAPACITY - 3), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(still.input().getFluidAmount() == 100, "used input with no room: " + still.input().getFluidAmount());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = "process")
    public static void aDistillerTakesOnlyFluidsItCanMakeSomethingOf(GameTestHelper helper) {
        var still = distiller(helper, 4096, 64);
        IFluidHandler side = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(AT), Direction.EAST);
        IFluidHandler top = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(AT), Direction.UP);
        helper.assertTrue(side.fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE) == 0, "took lava");
        helper.assertTrue(side.drain(10, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "gave from the side");
        helper.assertTrue(top.fill(new FluidStack(Fluids.WATER, 10), IFluidHandler.FluidAction.EXECUTE) == 0, "took at the top");
        still.output().fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 50), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(top.drain(30, IFluidHandler.FluidAction.EXECUTE).getAmount() == 30, "gave nothing at the top");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "process")
    public static void theDistillersOwnConversionsAreTheOriginals(GameTestHelper helper) {
        var oil = DistillerBlockEntity.CONVERSIONS.get(0);
        helper.assertTrue(oil.input() == DistillerBlockEntity.CRUDE_OIL && oil.consumed() == 1 && oil.produced() == 6 && oil.minTorque() == 2048 && oil.minPower() == 8192, "oil: " + oil);
        var biofuel = DistillerBlockEntity.CONVERSIONS.get(2);
        helper.assertTrue(biofuel.consumed() == 2 && biofuel.produced() == 1 && biofuel.minPower() == 131072, "biofuel: " + biofuel);
        helper.succeed();
    }

    // ---- Fuel Enhancer ----

    static synchronized void enhanceWater() {
        for (var c : FuelEnhancerBlockEntity.CONVERSIONS) {
            if (c.input() == net.minecraft.tags.FluidTags.WATER) {
                return;
            }
        }
        FuelEnhancerBlockEntity.addConversion(new FuelEnhancerBlockEntity.Conversion(net.minecraft.tags.FluidTags.WATER, 1, 4, 1));
    }

    static FuelEnhancerBlockEntity enhancer(GameTestHelper helper, boolean everything) {
        enhanceWater();
        WeaponGameTests.spinningFlywheel(helper, AT.below(), 64, 1024, Direction.UP);
        helper.setBlock(AT, ProcessRegistry.FUEL_ENHANCER.get());
        FuelEnhancerBlockEntity enhancer = helper.getBlockEntity(AT);
        enhancer.input().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        var needs = FuelEnhancerBlockEntity.ingredients();
        for (int i = 0; i < needs.size() - (everything ? 0 : 1); i++) {
            enhancer.items().setStackInSlot(i, needs.get(i).copy());
        }
        return enhancer;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60, batch = "process")
    public static void aFuelEnhancerMakesJetFuelFromFourTimesAsMuchFuel(GameTestHelper helper) {
        var enhancer = enhancer(helper, true);
        helper.runAfterDelay(4, () -> {
            int made = enhancer.output().getFluidAmount();
            helper.assertTrue(made > 0 && made % 6 == 0 && enhancer.output().getFluid().getFluid() == RotaryFluids.JET_FUEL.get(), "made " + enhancer.output().getFluid());
            helper.assertTrue(1000 - enhancer.input().getFluidAmount() == 4 * made, "used " + (1000 - enhancer.input().getFluidAmount()) + " for " + made);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60, batch = "process")
    public static void aFuelEnhancerMissingAnIngredientMakesNothing(GameTestHelper helper) {
        var enhancer = enhancer(helper, false);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(enhancer.output().isEmpty(), "made fuel without pink dye");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60, batch = "process_fuel_items")
    public static void aFuelEnhancerUsesUpItsIngredientsByChance(GameTestHelper helper) {
        RotaryConfig.override(FarmConfig.FUEL_ENHANCER_ITEM_CHANCE, 1.0);
        var enhancer = enhancer(helper, true);
        helper.runAfterDelay(4, () -> {
            RotaryConfig.clearOverride(FarmConfig.FUEL_ENHANCER_ITEM_CHANCE);
            int left = 0;
            for (int i = 0; i < FuelEnhancerBlockEntity.SLOTS; i++) {
                left += enhancer.items().getStackInSlot(i).getCount();
            }
            helper.assertTrue(left == 0, "kept " + left + " items");
            helper.assertTrue(enhancer.output().getFluidAmount() == 6, "made " + enhancer.output().getFluidAmount() + " before running out");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60, batch = "process")
    public static void aFuelEnhancerTakesFuelFromAboveAndGivesItFromItsSides(GameTestHelper helper) {
        var enhancer = enhancer(helper, true);
        IFluidHandler top = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(AT), Direction.UP);
        IFluidHandler side = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(AT), Direction.NORTH);
        helper.assertTrue(top.fill(new FluidStack(Fluids.WATER, 10), IFluidHandler.FluidAction.EXECUTE) == 10, "took no fuel from above");
        helper.assertTrue(top.fill(new FluidStack(Fluids.LAVA, 10), IFluidHandler.FluidAction.EXECUTE) == 0, "took lava");
        helper.assertTrue(side.fill(new FluidStack(Fluids.WATER, 10), IFluidHandler.FluidAction.EXECUTE) == 0, "took fuel from the side");
        enhancer.output().fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 100), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(side.drain(40, IFluidHandler.FluidAction.EXECUTE).getAmount() == 40, "gave nothing from the side");
        helper.assertTrue(top.drain(40, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "gave from the top");
        helper.succeed();
    }

    // ---- Big Furnace ----

    static BigFurnaceBlockEntity bigFurnace(GameTestHelper helper, int temperature) {
        WeaponGameTests.spinningFlywheel(helper, AT.below(), 4, 64, Direction.UP);
        helper.setBlock(AT, ProcessRegistry.BIG_FURNACE.get());
        BigFurnaceBlockEntity furnace = helper.getBlockEntity(AT);
        furnace.setTemperature(temperature);
        furnace.items().setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON, 5));
        furnace.items().setStackInSlot(17, new net.minecraft.world.item.ItemStack(Blocks.SAND, 2));
        return furnace;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = "process_big_furnace")
    public static void aHotBigFurnaceSmeltsEverySlotAtOnce(GameTestHelper helper) {
        var furnace = bigFurnace(helper, 1100);
        helper.succeedWhen(() -> {
            helper.assertTrue(furnace.items().getStackInSlot(BigFurnaceBlockEntity.INPUTS).is(net.minecraft.world.item.Items.IRON_INGOT)
                    && furnace.items().getStackInSlot(2 * BigFurnaceBlockEntity.INPUTS - 1).is(net.minecraft.world.item.Items.GLASS), "nothing smelted yet");
            helper.assertTrue(furnace.items().getStackInSlot(0).getCount() == 4 && furnace.items().getStackInSlot(17).getCount() == 1, "took " + furnace.items().getStackInSlot(0));
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120, batch = "process_big_furnace")
    public static void aColdBigFurnaceSmeltsNothing(GameTestHelper helper) {
        var furnace = bigFurnace(helper, 300);
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(furnace.items().getStackInSlot(BigFurnaceBlockEntity.INPUTS).isEmpty() && furnace.items().getStackInSlot(0).getCount() == 5, "smelted at " + furnace.getTemperature());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60, batch = "process_big_furnace")
    public static void aBigFurnaceGoesFasterWhenHotter(GameTestHelper helper) {
        var furnace = bigFurnace(helper, 300);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(furnace.operationTime() == 200, "cold " + furnace.operationTime());
            furnace.setTemperature(700);
            helper.assertTrue(furnace.operationTime() == 150, "700: " + furnace.operationTime());
            furnace.setTemperature(1100);
            helper.assertTrue(furnace.operationTime() == 75, "1100: " + furnace.operationTime());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80, batch = "process_big_furnace")
    public static void aBigFurnaceTakesLavaFromItsSidesAndBurnsItToHeat(GameTestHelper helper) {
        var furnace = bigFurnace(helper, 300);
        IFluidHandler side = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(AT), Direction.EAST);
        helper.assertTrue(side.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE) == 0, "took water");
        helper.assertTrue(side.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "took no lava");
        helper.assertTrue(side.drain(10, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "gave lava back");
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(furnace.lava().getFluidAmount() < 1000 && furnace.lava().getFluidAmount() >= 1000 - 15 * 3, "lava " + furnace.lava().getFluidAmount());
            helper.assertTrue(furnace.getTemperature() > 300, "not heated: " + furnace.getTemperature());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "process_big_furnace")
    public static void aBigFurnaceTakesSmeltablesInAndGivesOnlyProductsOut(GameTestHelper helper) {
        var furnace = bigFurnace(helper, 300);
        var items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(AT), Direction.NORTH);
        helper.assertTrue(items.insertItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_COPPER, 1), false).isEmpty(), "refused smeltable");
        helper.assertTrue(items.insertItem(2, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK, 1), false).getCount() == 1, "took a stick");
        helper.assertTrue(items.insertItem(BigFurnaceBlockEntity.INPUTS + 1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_COPPER, 1), false).getCount() == 1, "took into an output slot");
        helper.assertTrue(items.extractItem(0, 1, false).isEmpty(), "gave an input back");
        furnace.items().setStackInSlot(BigFurnaceBlockEntity.INPUTS + 3, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 4));
        helper.assertTrue(items.extractItem(BigFurnaceBlockEntity.INPUTS + 3, 4, false).getCount() == 4, "kept a product");
        helper.succeed();
    }
}
