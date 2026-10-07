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
import net.scwunge.rotarycraft.process.DynamoBlockEntity;
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
}
