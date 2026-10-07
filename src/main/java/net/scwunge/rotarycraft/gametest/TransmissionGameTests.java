package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.DynamometerBlockEntity;
import net.scwunge.rotarycraft.menu.DistributionClutchMenu;
import net.scwunge.rotarycraft.menu.MultiClutchMenu;
import net.scwunge.rotarycraft.menu.PowerBusMenu;
import net.scwunge.rotarycraft.power.ShaftMaterial;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryParts;
import net.scwunge.rotarycraft.transmission.BusControllerBlockEntity;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.scwunge.rotarycraft.item.BeltItem;
import net.scwunge.rotarycraft.registry.RotaryComponents;
import net.scwunge.rotarycraft.transmission.BeltHubBlock;
import net.scwunge.rotarycraft.transmission.BeltHubBlockEntity;
import net.scwunge.rotarycraft.blockentity.GasEngineBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.transmission.EngineControllerBlockEntity;
import net.scwunge.rotarycraft.transmission.PowerBusBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;
import net.scwunge.rotarycraft.transmission.DistributionClutchBlockEntity;
import net.scwunge.rotarycraft.transmission.MultiClutchBlockEntity;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class TransmissionGameTests {
    static final String TEMPLATE = RotaryGameTests.TEMPLATE;
    static final BlockPos CLUTCH = new BlockPos(2, 2, 2);
    static final BlockPos SOURCE = CLUTCH.west();

    /** A multi-clutch fed from the west by a spinning flywheel (64 N*m at 64 rad/s), power coming in at its back. */
    static MultiClutchBlockEntity clutch(GameTestHelper helper) {
        WeaponGameTests.spinningFlywheel(helper, SOURCE, 64, 64, Direction.EAST);
        helper.setBlock(CLUTCH, TransmissionRegistry.MULTI_CLUTCH.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        return helper.getBlockEntity(CLUTCH);
    }

    static DynamometerBlockEntity meter(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.setBlock(pos, RotaryBlocks.DYNAMOMETER.get().defaultBlockState().setValue(MachineBlock.FACING, facing));
        return helper.getBlockEntity(pos);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void aMultiClutchSendsPowerOutTheSideItsRedstoneStrengthPicks(GameTestHelper helper) {
        MultiClutchBlockEntity clutch = clutch(helper);
        clutch.setSideOfState(0, Direction.NORTH.ordinal());
        clutch.setSideOfState(15, Direction.SOUTH.ordinal());
        DynamometerBlockEntity north = meter(helper, CLUTCH.north(), Direction.NORTH);
        DynamometerBlockEntity south = meter(helper, CLUTCH.south(), Direction.SOUTH);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(clutch.outputSide() == Direction.NORTH, "output " + clutch.outputSide());
            helper.assertTrue(north.getTorque() == 64 && north.getOmega() == 64, "north got " + north.getTorque() + " N*m " + north.getOmega() + " rad/s");
            helper.assertTrue(south.getTorque() == 0, "south got power it should not have");
            helper.setBlock(CLUTCH.above(), Blocks.REDSTONE_BLOCK);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(clutch.redstoneLevel() == 15, "redstone level " + clutch.redstoneLevel());
            helper.assertTrue(south.getTorque() == 64 && south.getOmega() == 64, "south got " + south.getTorque() + " N*m " + south.getOmega() + " rad/s");
            helper.assertTrue(north.getTorque() == 0, "north still has power");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void aMultiClutchSendsNothingBackOutTheInputSide(GameTestHelper helper) {
        MultiClutchBlockEntity clutch = clutch(helper);
        clutch.setSideOfState(0, Direction.WEST.ordinal());
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(clutch.outputSide() == null, "output " + clutch.outputSide());
            for (Direction side : Direction.values()) {
                helper.assertTrue(clutch.getTorqueOut(side) == 0 && clutch.getOmegaOut(side) == 0, "power out of " + side);
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void anUnfedMultiClutchHasNoPower(GameTestHelper helper) {
        helper.setBlock(CLUTCH, TransmissionRegistry.MULTI_CLUTCH.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        MultiClutchBlockEntity clutch = helper.getBlockEntity(CLUTCH);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(clutch.getTorque() == 0 && clutch.getOmega() == 0, "an unfed clutch has power");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void theMultiClutchScreenCyclesSidesAndTheyAreKept(GameTestHelper helper) {
        MultiClutchBlockEntity clutch = clutch(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        MultiClutchMenu menu = new MultiClutchMenu(1, player.getInventory(), clutch);
        helper.assertTrue(clutch.sideOfState(3) == 0, "starts down");
        menu.clickMenuButton(player, 3);
        menu.clickMenuButton(player, 3);
        helper.assertTrue(clutch.sideOfState(3) == Direction.NORTH.ordinal(), "two clicks should reach north, got " + clutch.sideOfState(3));
        for (int i = 0; i < 4; i++) {
            menu.clickMenuButton(player, 3);
        }
        helper.assertTrue(clutch.sideOfState(3) == 0, "the sides should go round");
        helper.assertFalse(menu.clickMenuButton(player, 16), "there are only sixteen states");
        clutch.setSideOfState(7, Direction.EAST.ordinal());
        var registries = helper.getLevel().registryAccess();
        var saved = clutch.saveWithFullMetadata(registries);
        helper.setBlock(CLUTCH, Blocks.AIR);
        helper.setBlock(CLUTCH, TransmissionRegistry.MULTI_CLUTCH.get().defaultBlockState());
        MultiClutchBlockEntity other = helper.getBlockEntity(CLUTCH);
        other.loadWithComponents(saved, registries);
        helper.assertTrue(other.sideOfState(7) == Direction.EAST.ordinal(), "the setting was lost");
        helper.succeed();
    }

    // ---- distribution clutch ----

    static DistributionClutchBlockEntity distribution(GameTestHelper helper) {
        WeaponGameTests.spinningFlywheel(helper, SOURCE, 64, 64, Direction.EAST);
        helper.setBlock(CLUTCH, TransmissionRegistry.DISTRIBUTION_CLUTCH.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        return helper.getBlockEntity(CLUTCH);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void aDistributionClutchGivesEachSideWhatItAsksForAndTheFrontTheRest(GameTestHelper helper) {
        DistributionClutchBlockEntity clutch = distribution(helper);
        clutch.setSideEnabled(Direction.NORTH, true);
        clutch.setSideEnabled(Direction.SOUTH, true);
        clutch.setTorqueRequests(new int[] {20, 30, 0, 0});
        DynamometerBlockEntity north = meter(helper, CLUTCH.north(), Direction.NORTH);
        DynamometerBlockEntity south = meter(helper, CLUTCH.south(), Direction.SOUTH);
        DynamometerBlockEntity front = meter(helper, CLUTCH.east(), Direction.EAST);
        helper.succeedWhen(() -> {
            helper.assertTrue(north.getTorque() == 20 && north.getOmega() == 64, "north got " + north.getTorque() + " N*m " + north.getOmega() + " rad/s");
            helper.assertTrue(south.getTorque() == 30 && south.getOmega() == 64, "south got " + south.getTorque() + " N*m " + south.getOmega() + " rad/s");
            helper.assertTrue(front.getTorque() == 14 && front.getOmega() == 64, "the front got " + front.getTorque() + " N*m " + front.getOmega() + " rad/s");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void aDistributionClutchServesTheSidesInOrderAndGivesOutNoMoreThanItHas(GameTestHelper helper) {
        DistributionClutchBlockEntity clutch = distribution(helper);
        clutch.setSideEnabled(Direction.NORTH, true);
        clutch.setSideEnabled(Direction.SOUTH, true);
        clutch.setTorqueRequests(new int[] {50, 50, 0, 0});
        DynamometerBlockEntity north = meter(helper, CLUTCH.north(), Direction.NORTH);
        DynamometerBlockEntity south = meter(helper, CLUTCH.south(), Direction.SOUTH);
        DynamometerBlockEntity front = meter(helper, CLUTCH.east(), Direction.EAST);
        helper.succeedWhen(() -> {
            helper.assertTrue(north.getTorque() == 50, "north got " + north.getTorque());
            helper.assertTrue(south.getTorque() == 14, "south should get what is left, got " + south.getTorque());
            helper.assertTrue(front.getTorque() == 0, "the front got " + front.getTorque());
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void aSideThatIsOffGetsNothingAndNothingGoesBackTheInput(GameTestHelper helper) {
        DistributionClutchBlockEntity clutch = distribution(helper);
        clutch.setTorqueRequests(new int[] {20, 20, 20, 20});
        clutch.setSideEnabled(Direction.WEST, true);
        helper.runAfterDelay(10, () -> {
            helper.assertFalse(clutch.isSideEnabled(Direction.NORTH), "north should be off");
            helper.assertTrue(clutch.getTorqueOut(Direction.NORTH) == 0 && clutch.getOmegaOut(Direction.NORTH) == 0, "power out of an off side");
            helper.assertTrue(clutch.getTorqueOut(Direction.WEST) == 0, "power back out the input");
            helper.assertTrue(clutch.getTorqueOut(Direction.EAST) == 64 && clutch.getOmegaOut(Direction.EAST) == 64, "the front should take it all");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void redstoneModeTurnsSidesOnByTheBitsOfTheSignal(GameTestHelper helper) {
        DistributionClutchBlockEntity clutch = distribution(helper);
        clutch.setTorqueRequests(new int[] {10, 10, 0, 0});
        helper.assertTrue(clutch.control() == DistributionClutchBlockEntity.Control.GUI, "starts under screen control");
        clutch.stepControl();
        helper.assertTrue(clutch.control() == DistributionClutchBlockEntity.Control.REDSTONE, "control " + clutch.control());
        helper.runAfterDelay(5, () -> {
            helper.assertFalse(clutch.isSideEnabled(Direction.NORTH), "no signal, north should be off");
            helper.setBlock(CLUTCH.above(), Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(clutch.isSideEnabled(Direction.NORTH) && clutch.isSideEnabled(Direction.SOUTH) && clutch.isSideEnabled(Direction.EAST), "strength 15 should turn the sides on");
            helper.assertTrue(clutch.getTorqueOut(Direction.NORTH) == 10 && clutch.getTorqueOut(Direction.SOUTH) == 10, "north " + clutch.getTorqueOut(Direction.NORTH));
            helper.assertTrue(clutch.getTorqueOut(Direction.WEST) == 0, "never out of the input side");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void theDistributionClutchScreenTogglesSidesAndKeepsItsSettingsWhenSaved(GameTestHelper helper) {
        DistributionClutchBlockEntity clutch = distribution(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        DistributionClutchMenu menu = new DistributionClutchMenu(1, player.getInventory(), clutch);
        helper.assertTrue(menu.clickMenuButton(player, 0) && clutch.isSideEnabled(Direction.NORTH), "button 0 should turn north on");
        menu.clickMenuButton(player, 0);
        helper.assertFalse(clutch.isSideEnabled(Direction.NORTH), "a second click turns it off");
        menu.clickMenuButton(player, 3);
        clutch.setTorqueRequests(new int[] {5, 6, 7, 800000});
        var registries = helper.getLevel().registryAccess();
        var saved = clutch.saveWithFullMetadata(registries);
        helper.setBlock(CLUTCH, Blocks.AIR);
        helper.setBlock(CLUTCH, TransmissionRegistry.DISTRIBUTION_CLUTCH.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        DistributionClutchBlockEntity other = helper.getBlockEntity(CLUTCH);
        other.loadWithComponents(saved, registries);
        helper.assertTrue(other.isSideEnabled(Direction.EAST) && other.torqueRequest(Direction.EAST) == 800000 && other.torqueRequest(Direction.SOUTH) == 6, "the settings were lost");
        helper.assertFalse(menu.clickMenuButton(player, 9), "no such button");
        helper.succeed();
    }

    // ---- power bus and bus controller ----

    static final BlockPos BUS = CLUTCH.east();

    static BusControllerBlockEntity controller(GameTestHelper helper, boolean lubricated) {
        WeaponGameTests.spinningFlywheel(helper, SOURCE, 64, 64, Direction.EAST);
        helper.setBlock(CLUTCH, TransmissionRegistry.BUS_CONTROLLER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        BusControllerBlockEntity hub = helper.getBlockEntity(CLUTCH);
        if (lubricated) {
            hub.tank().fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 8000), IFluidHandler.FluidAction.EXECUTE);
        }
        return hub;
    }

    static PowerBusBlockEntity bus(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, TransmissionRegistry.POWER_BUS.get().defaultBlockState());
        return helper.getBlockEntity(pos);
    }

    static ItemStack unit(ShaftMaterial material, int ratio) {
        return new ItemStack(RotaryParts.GEAR_UNITS.get(material).get(ratio).get());
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void aPowerBusSharesItsPowerAndEachSideChangesItByItsGearUnit(GameTestHelper helper) {
        BusControllerBlockEntity hub = controller(helper, true);
        PowerBusBlockEntity bus = bus(helper, BUS);
        bus.items().setStackInSlot(0, unit(ShaftMaterial.STEEL, 2));
        bus.items().setStackInSlot(1, unit(ShaftMaterial.STEEL, 4));
        bus.setSideSpeedMode(Direction.SOUTH, true);
        DynamometerBlockEntity north = meter(helper, BUS.north(), Direction.NORTH);
        DynamometerBlockEntity south = meter(helper, BUS.south(), Direction.SOUTH);
        helper.succeedWhen(() -> {
            helper.assertTrue(hub.sides() == 2 && hub.busSize() == 1, "sides " + hub.sides() + ", blocks " + hub.busSize());
            // 64 N*m shared over two sides is 32 each: x2 and /2 on the north side, /4 and x4 on the south side
            helper.assertTrue(north.getTorque() == 64 && north.getOmega() == 32, "north got " + north.getTorque() + " N*m " + north.getOmega() + " rad/s");
            helper.assertTrue(south.getTorque() == 8 && south.getOmega() == 256, "south got " + south.getTorque() + " N*m " + south.getOmega() + " rad/s");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void aBusControllerWithNoLubricantPassesNothingOn(GameTestHelper helper) {
        BusControllerBlockEntity hub = controller(helper, false);
        PowerBusBlockEntity bus = bus(helper, BUS);
        bus.items().setStackInSlot(0, unit(ShaftMaterial.STEEL, 2));
        DynamometerBlockEntity north = meter(helper, BUS.north(), Direction.NORTH);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(hub.getTorque() == 0 && hub.getOmega() == 0, "the controller has power with no lubricant");
            helper.assertTrue(north.getTorque() == 0, "the bus gave power with no lubricant");
            hub.tank().fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        });
        helper.succeedWhen(() -> helper.assertTrue(north.getTorque() == 128 && north.getOmega() == 32, "no power after lubricating: " + north.getTorque()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void aGearUnitThatCannotTakeTheLoadBreaks(GameTestHelper helper) {
        controller(helper, true);
        PowerBusBlockEntity bus = bus(helper, BUS);
        bus.items().setStackInSlot(0, unit(ShaftMaterial.WOOD, 16));
        helper.succeedWhen(() -> helper.assertTrue(bus.items().getStackInSlot(0).isEmpty(), "the wooden 16:1 gear unit should break under 1024 N*m"));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void busBlocksJoinInAChainAndShareTheSameInput(GameTestHelper helper) {
        BusControllerBlockEntity hub = controller(helper, true);
        PowerBusBlockEntity first = bus(helper, BUS);
        PowerBusBlockEntity second = bus(helper, BUS.east());
        first.items().setStackInSlot(0, unit(ShaftMaterial.STEEL, 2));
        second.items().setStackInSlot(0, unit(ShaftMaterial.STEEL, 2));
        DynamometerBlockEntity far = meter(helper, BUS.east().north(), Direction.NORTH);
        helper.succeedWhen(() -> {
            helper.assertTrue(hub.busSize() == 2 && hub.sides() == 2, "blocks " + hub.busSize() + ", sides " + hub.sides());
            helper.assertTrue(second.isOnBus() && second.inputSide() == Direction.WEST, "the second block should be fed from the west");
            helper.assertTrue(far.getTorque() == 64 && far.getOmega() == 32, "the far side got " + far.getTorque() + " N*m " + far.getOmega() + " rad/s");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void takingABusBlockAwayLeavesTheOthersWithTheirShare(GameTestHelper helper) {
        BusControllerBlockEntity hub = controller(helper, true);
        PowerBusBlockEntity first = bus(helper, BUS);
        bus(helper, BUS.east()).items().setStackInSlot(0, unit(ShaftMaterial.STEEL, 2));
        first.items().setStackInSlot(0, unit(ShaftMaterial.STEEL, 2));
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(hub.sides() == 2, "sides " + hub.sides());
            helper.setBlock(BUS.east(), Blocks.AIR);
        });
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(hub.busSize() == 1 && hub.sides() == 1, "after: blocks " + hub.busSize() + ", sides " + hub.sides());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void aBusControllerUsesLubricantWhilePowerFlows(GameTestHelper helper) {
        BusControllerBlockEntity hub = controller(helper, false);
        hub.tank().fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        PowerBusBlockEntity bus = bus(helper, BUS);
        bus.items().setStackInSlot(0, unit(ShaftMaterial.STEEL, 2));
        helper.succeedWhen(() -> helper.assertTrue(hub.tank().getFluidAmount() < 1000, "no lubricant used"));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void theBusScreenTogglesModesAndSlotsFollowWhatIsBesideThem(GameTestHelper helper) {
        controller(helper, true);
        PowerBusBlockEntity bus = bus(helper, BUS);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        PowerBusMenu menu = new PowerBusMenu(1, player.getInventory(), bus);
        helper.assertFalse(menu.speedMode(0), "starts in torque mode");
        helper.assertTrue(menu.clickMenuButton(player, 0) && bus.isSideSpeedMode(Direction.NORTH), "button 0 should switch north to speed mode");
        helper.assertTrue(menu.hasSlot(0) && menu.hasSlot(1) && menu.hasSlot(3), "north, south and east are free");
        helper.assertFalse(menu.hasSlot(2), "the west side faces the controller and has no slot");
        helper.assertFalse(bus.items().insertItem(2, unit(ShaftMaterial.STEEL, 2), false).isEmpty(), "a gear unit went in a side with no slot");
        helper.assertFalse(bus.items().insertItem(0, new ItemStack(net.minecraft.world.item.Items.DIRT), false).isEmpty(), "dirt went in");
        helper.assertTrue(bus.items().insertItem(0, unit(ShaftMaterial.STEEL, 2), false).isEmpty(), "a gear unit should go in");
        helper.assertFalse(menu.clickMenuButton(player, 5), "no such button");
        var registries = helper.getLevel().registryAccess();
        var saved = bus.saveWithFullMetadata(registries);
        helper.setBlock(BUS, Blocks.AIR);
        PowerBusBlockEntity other = bus(helper, BUS);
        other.loadWithComponents(saved, registries);
        helper.assertTrue(other.isSideSpeedMode(Direction.NORTH) && other.ratio(Direction.NORTH) == 2, "the settings were lost");
        helper.succeed();
    }

    // ---- engine control unit ----

    static EngineControllerBlockEntity ecu(GameTestHelper helper, int ethanol) {
        helper.setBlock(CLUTCH.below(), TransmissionRegistry.ENGINE_CONTROLLER.get().defaultBlockState());
        helper.setBlock(CLUTCH, RotaryBlocks.GAS_ENGINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        EngineControllerBlockEntity ecu = helper.getBlockEntity(CLUTCH.below());
        ecu.tank().fill(new FluidStack(RotaryFluids.ETHANOL.get(), ethanol), IFluidHandler.FluidAction.EXECUTE);
        return ecu;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void anEnginesSpeedFollowsItsControlUnitSetting(GameTestHelper helper) {
        EngineControllerBlockEntity ecu = ecu(helper, 3000);
        GasEngineBlockEntity engine = helper.getBlockEntity(CLUTCH);
        ecu.setSetting(EngineControllerBlockEntity.Setting.MEDIUM);
        helper.succeedWhen(() -> {
            if (ecu.setting() == EngineControllerBlockEntity.Setting.MEDIUM) {
                helper.assertTrue(engine.getOmega() == GasEngineBlockEntity.SPEED / 2, "medium: " + engine.getOmega());
                ecu.setSetting(EngineControllerBlockEntity.Setting.FULL);
            }
            if (ecu.setting() == EngineControllerBlockEntity.Setting.FULL) {
                helper.assertTrue(engine.getOmega() == GasEngineBlockEntity.SPEED, "full: " + engine.getOmega());
                ecu.setSetting(EngineControllerBlockEntity.Setting.SHUTDOWN);
            }
            helper.assertTrue(engine.getOmega() < GasEngineBlockEntity.SPEED / 2, "shut down, should be slowing: " + engine.getOmega());
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void aControlUnitFeedsTheEngineAndAShutDownEngineBurnsNothing(GameTestHelper helper) {
        EngineControllerBlockEntity ecu = ecu(helper, 2000);
        GasEngineBlockEntity engine = helper.getBlockEntity(CLUTCH);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(engine.fuel().getFluidAmount() > 0, "the engine got no fuel");
            helper.assertTrue(ecu.tank().getFluidAmount() < 2000, "the control unit did not give any up");
            ecu.setSetting(EngineControllerBlockEntity.Setting.SHUTDOWN);
        });
        helper.runAfterDelay(25, () -> {
            int before = engine.fuel().getFluidAmount() + ecu.tank().getFluidAmount();
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(engine.fuel().getFluidAmount() + ecu.tank().getFluidAmount() == before, "fuel was burnt while shut down");
                helper.succeed();
            });
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void runningSlowBurnsFuelMoreThriftilyAndTurbinesCountAnEighth(GameTestHelper helper) {
        EngineControllerBlockEntity ecu = ecu(helper, 0);
        ecu.setSetting(EngineControllerBlockEntity.Setting.LOW);
        helper.assertTrue(ecu.fuelIntervalFactor(false) == 8 && ecu.speedMultiplier() == 0.25F, "low: " + ecu.fuelIntervalFactor(false) + ", " + ecu.speedMultiplier());
        helper.assertTrue(ecu.fuelIntervalFactor(true) == 1, "a turbine counts an eighth of 8");
        ecu.setSetting(EngineControllerBlockEntity.Setting.STANDBY);
        helper.assertTrue(ecu.fuelIntervalFactor(false) == 64 && ecu.fuelIntervalFactor(true) == 8 && ecu.speedMultiplier() == 1F / 16, "standby");
        ecu.setSetting(EngineControllerBlockEntity.Setting.SHUTDOWN);
        helper.assertTrue(!ecu.canProducePower() && !ecu.consumesFuel() && ecu.speedMultiplier() == 0, "shutdown");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void redstoneModePicksTheSettingFromTheSignalStrength(GameTestHelper helper) {
        EngineControllerBlockEntity ecu = ecu(helper, 0);
        ecu.setRedstoneMode(true);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(ecu.setting() == EngineControllerBlockEntity.Setting.FULL, "no signal should mean full, got " + ecu.setting());
            helper.setBlock(CLUTCH.below().west(), Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(ecu.setting() == EngineControllerBlockEntity.Setting.SHUTDOWN, "full strength should shut it down, got " + ecu.setting());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void theScrewdriverStepsTheControlUnitAndSneakTogglesRedstoneMode(GameTestHelper helper) {
        EngineControllerBlockEntity ecu = ecu(helper, 0);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(CLUTCH.below());
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(at.getCenter(), Direction.UP, at, false);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(RotaryItems.SCREWDRIVER.get()));
        net.minecraft.world.item.context.UseOnContext use = new net.minecraft.world.item.context.UseOnContext(helper.getLevel(), player,
                net.minecraft.world.InteractionHand.MAIN_HAND, player.getMainHandItem(), hit);
        helper.assertTrue(RotaryItems.SCREWDRIVER.get().useOn(use).consumesAction(), "the screwdriver should work on the unit");
        helper.assertTrue(ecu.setting() == EngineControllerBlockEntity.Setting.SHUTDOWN, "full steps on to shutdown, got " + ecu.setting());
        player.setShiftKeyDown(true);
        RotaryItems.SCREWDRIVER.get().useOn(use);
        helper.assertTrue(ecu.redstoneMode(), "sneaking should turn redstone mode on");
        var registries = helper.getLevel().registryAccess();
        var saved = ecu.saveWithFullMetadata(registries);
        helper.setBlock(CLUTCH.below(), Blocks.AIR);
        helper.setBlock(CLUTCH.below(), TransmissionRegistry.ENGINE_CONTROLLER.get().defaultBlockState());
        EngineControllerBlockEntity other = helper.getBlockEntity(CLUTCH.below());
        other.loadWithComponents(saved, registries);
        helper.assertTrue(other.redstoneMode() && other.setting() == EngineControllerBlockEntity.Setting.SHUTDOWN, "the settings were lost");
        helper.succeed();
    }

    // ---- belt, chain and split belt pulleys ----

    static final String ROOM = SolarGameTests.ROOM;
    static final BlockPos DRIVER = new BlockPos(2, 2, 3);
    static final BlockPos RECEIVER = new BlockPos(8, 2, 3);

    /** A driver at the west end, fed from the north by a spinning flywheel, and a receiving end at the east, with a meter on its shaft side. */
    static BeltHubBlockEntity[] belt(GameTestHelper helper, DeferredBlock<BeltHubBlock> block, int torque, int omega, boolean connect) {
        WeaponGameTests.spinningFlywheel(helper, DRIVER.north(), torque, omega, Direction.SOUTH);
        helper.setBlock(DRIVER, block.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        helper.setBlock(RECEIVER, block.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        BeltHubBlockEntity driver = helper.getBlockEntity(DRIVER);
        BeltHubBlockEntity receiver = helper.getBlockEntity(RECEIVER);
        receiver.setReceivingEnd(true);
        if (connect) {
            helper.assertTrue(driver.tryConnect(helper.absolutePos(RECEIVER)) && receiver.tryConnect(helper.absolutePos(DRIVER)), "the pulleys would not join");
        }
        return new BeltHubBlockEntity[] {driver, receiver};
    }

    @GameTest(template = ROOM, timeoutTicks = 60)
    public static void aBeltCarriesPowerBetweenTwoPulleys(GameTestHelper helper) {
        belt(helper, TransmissionRegistry.BELT_HUB, 64, 64, true);
        DynamometerBlockEntity meter = meter(helper, RECEIVER.north(), Direction.NORTH);
        helper.succeedWhen(() -> helper.assertTrue(meter.getTorque() == 64 && meter.getOmega() == 64, "the far end gave " + meter.getTorque() + " N*m " + meter.getOmega() + " rad/s"));
    }

    @GameTest(template = ROOM, timeoutTicks = 60)
    public static void aBeltSlipsAboveItsLimitAndAWetOneTakesAQuarter(GameTestHelper helper) {
        BeltHubBlockEntity[] hubs = belt(helper, TransmissionRegistry.BELT_HUB, 10000, 64, true);
        DynamometerBlockEntity meter = meter(helper, RECEIVER.north(), Direction.NORTH);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(meter.getTorque() == 8192, "a dry belt should cap at 8192, gave " + meter.getTorque());
            helper.assertTrue(hubs[1].isSlipping(), "it should be slipping");
            hubs[1].makeWet(1);
        });
        helper.succeedWhen(() -> helper.assertTrue(meter.getTorque() == 2048, "a wet belt should cap at 2048, gave " + meter.getTorque()));
    }

    @GameTest(template = ROOM, timeoutTicks = 60)
    public static void aBeltSmoothsSpeedAboveItsLimit(GameTestHelper helper) {
        belt(helper, TransmissionRegistry.BELT_HUB, 4, 8192 + 100, true);
        DynamometerBlockEntity meter = meter(helper, RECEIVER.north(), Direction.NORTH);
        helper.succeedWhen(() -> helper.assertTrue(meter.getOmega() == 8192 + 10, "the speed over the limit passes on as its square root: " + meter.getOmega()));
    }

    @GameTest(template = ROOM, timeoutTicks = 60)
    public static void aChainTakesMoreButTearsItselfApartAboveItsSpeed(GameTestHelper helper) {
        belt(helper, TransmissionRegistry.CHAIN_DRIVE, 12000, 64, true);
        DynamometerBlockEntity meter = meter(helper, RECEIVER.north(), Direction.NORTH);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(meter.getTorque() == 12000, "a chain should take 12000 N*m, gave " + meter.getTorque());
            WeaponGameTests.spinningFlywheel(helper, DRIVER.north(), 4, 70000, Direction.SOUTH);
        });
        helper.succeedWhen(() -> helper.assertBlockNotPresent(TransmissionRegistry.CHAIN_DRIVE.get(), RECEIVER));
    }

    @GameTest(template = ROOM, timeoutTicks = 60)
    public static void aSplitBeltTakesOffAFixedTorqueAndTheShaftCarriesOn(GameTestHelper helper) {
        belt(helper, TransmissionRegistry.SPLIT_BELT, 200, 64, true);
        DynamometerBlockEntity through = meter(helper, DRIVER.south(), Direction.SOUTH);
        DynamometerBlockEntity taken = meter(helper, RECEIVER.south(), Direction.SOUTH);
        helper.succeedWhen(() -> {
            helper.assertTrue(through.getTorque() == 200 - BeltHubBlockEntity.TAKEOFF_TORQUE && through.getOmega() == 64, "the shaft carries " + through.getTorque() + " N*m " + through.getOmega() + " rad/s");
            helper.assertTrue(taken.getTorque() == BeltHubBlockEntity.TAKEOFF_TORQUE && taken.getOmega() == 64, "the belt delivers " + taken.getTorque() + " N*m " + taken.getOmega() + " rad/s");
        });
    }

    @GameTest(template = ROOM, timeoutTicks = 60)
    public static void aSplitBeltsReceivingEndAddsTheBeltsTorqueToItsOwnShaft(GameTestHelper helper) {
        belt(helper, TransmissionRegistry.SPLIT_BELT, 200, 64, true);
        WeaponGameTests.spinningFlywheel(helper, RECEIVER.north(), 100, 64, Direction.SOUTH);
        DynamometerBlockEntity taken = meter(helper, RECEIVER.south(), Direction.SOUTH);
        helper.succeedWhen(() -> helper.assertTrue(taken.getTorque() == 100 + BeltHubBlockEntity.TAKEOFF_TORQUE && taken.getOmega() == 64, "the line carries " + taken.getTorque() + " N*m " + taken.getOmega() + " rad/s"));
    }

    @GameTest(template = ROOM, timeoutTicks = 60)
    public static void pulleysOnlyJoinInAStraightClearLineAcrossParallelShafts(GameTestHelper helper) {
        BeltHubBlockEntity[] hubs = belt(helper, TransmissionRegistry.BELT_HUB, 4, 4, false);
        BlockPos driverAt = helper.absolutePos(DRIVER);
        BlockPos receiverAt = helper.absolutePos(RECEIVER);
        helper.setBlock(DRIVER.east(2), Blocks.STONE);
        helper.assertFalse(hubs[0].canConnect(receiverAt), "a belt cannot pass through stone");
        helper.setBlock(DRIVER.east(2), Blocks.AIR);
        helper.assertTrue(hubs[0].canConnect(receiverAt) && hubs[1].canConnect(driverAt), "it is clear now");
        hubs[1].setReceivingEnd(false);
        helper.assertFalse(hubs[0].canConnect(receiverAt), "two drivers cannot be joined");
        hubs[1].setReceivingEnd(true);
        helper.setBlock(RECEIVER.above(), Blocks.AIR);
        BlockPos off = RECEIVER.above(2);
        helper.setBlock(off, TransmissionRegistry.BELT_HUB.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        ((BeltHubBlockEntity) helper.getBlockEntity(off)).setReceivingEnd(true);
        helper.assertFalse(hubs[0].canConnect(helper.absolutePos(off)), "not in a straight line");
        helper.setBlock(RECEIVER, TransmissionRegistry.BELT_HUB.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.UP));
        BeltHubBlockEntity turned = helper.getBlockEntity(RECEIVER);
        turned.setReceivingEnd(true);
        helper.assertFalse(hubs[0].canConnect(receiverAt), "the shafts must be parallel");
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 60)
    public static void theBeltItemJoinsTwoPulleysAndSpendsABeltForEveryBlockBetween(GameTestHelper helper) {
        BeltHubBlockEntity[] hubs = belt(helper, TransmissionRegistry.BELT_HUB, 4, 4, false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack belts = new ItemStack(RotaryParts.part("belt").get(), 8);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, belts);
        BeltItem item = (BeltItem) belts.getItem();
        item.useOn(new UseOnContext(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, belts, new BlockHitResult(helper.absolutePos(DRIVER).getCenter(), Direction.UP, helper.absolutePos(DRIVER), false)));
        helper.assertTrue(belts.has(RotaryComponents.BELT_END.get()), "the first pulley should be remembered");
        item.useOn(new UseOnContext(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, belts, new BlockHitResult(helper.absolutePos(RECEIVER).getCenter(), Direction.UP, helper.absolutePos(RECEIVER), false)));
        helper.assertTrue(hubs[0].hasValidConnection() && hubs[1].hasValidConnection(), "the belt should be on");
        helper.assertTrue(belts.getCount() == 3, "five of the eight belts go between the pulleys six apart, left " + belts.getCount());
        helper.assertFalse(belts.has(RotaryComponents.BELT_END.get()), "the memory should be wiped");
        // too short a belt
        hubs[0].resetOther();
        hubs[0].reset();
        ItemStack few = new ItemStack(RotaryParts.part("belt").get(), 3);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, few);
        item.useOn(new UseOnContext(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, few, new BlockHitResult(helper.absolutePos(DRIVER).getCenter(), Direction.UP, helper.absolutePos(DRIVER), false)));
        item.useOn(new UseOnContext(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, few, new BlockHitResult(helper.absolutePos(RECEIVER).getCenter(), Direction.UP, helper.absolutePos(RECEIVER), false)));
        helper.assertFalse(hubs[0].hasValidConnection(), "three belts should not reach");
        helper.assertTrue(few.getCount() == 3, "nothing should be spent");
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 60)
    public static void breakingAPulleyDropsTheBeltAndFreesTheOtherEnd(GameTestHelper helper) {
        BeltHubBlockEntity[] hubs = belt(helper, TransmissionRegistry.BELT_HUB, 4, 4, true);
        helper.setBlock(DRIVER, Blocks.AIR);
        helper.assertFalse(hubs[1].hasValidConnection(), "the other end should be free");
        int dropped = 0;
        for (var drop : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(DRIVER)).inflate(3))) {
            if (drop.getItem().is(RotaryParts.part("belt").get())) {
                dropped += drop.getItem().getCount();
            }
        }
        helper.assertTrue(dropped == 5, "five belts should drop, got " + dropped);
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 60)
    public static void theScrewdriverSwapsWhichPulleyReceivesAndSavedPulleysKeepTheirBelt(GameTestHelper helper) {
        BeltHubBlockEntity[] hubs = belt(helper, TransmissionRegistry.BELT_HUB, 4, 4, true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        ItemStack driver = new ItemStack(RotaryItems.SCREWDRIVER.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, driver);
        RotaryItems.SCREWDRIVER.get().useOn(new UseOnContext(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, driver,
                new BlockHitResult(helper.absolutePos(DRIVER).getCenter(), Direction.UP, helper.absolutePos(DRIVER), false)));
        helper.assertTrue(hubs[0].isReceivingEnd(), "sneaking should make it the receiving end");
        helper.assertFalse(hubs[0].hasValidConnection() || hubs[1].hasValidConnection(), "the belt should come off");
        hubs[0].setReceivingEnd(false);
        helper.assertTrue(hubs[0].tryConnect(helper.absolutePos(RECEIVER)) && hubs[1].tryConnect(helper.absolutePos(DRIVER)), "it joins again");
        var registries = helper.getLevel().registryAccess();
        var saved = hubs[0].saveWithFullMetadata(registries);
        helper.setBlock(DRIVER, Blocks.AIR);
        helper.setBlock(DRIVER, TransmissionRegistry.BELT_HUB.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        BeltHubBlockEntity other = helper.getBlockEntity(DRIVER);
        other.loadWithComponents(saved, registries);
        helper.assertTrue(other.otherEnd() != null && other.otherEnd().equals(helper.absolutePos(RECEIVER)) && !other.isReceivingEnd(), "the belt was lost");
        helper.succeed();
    }
}
