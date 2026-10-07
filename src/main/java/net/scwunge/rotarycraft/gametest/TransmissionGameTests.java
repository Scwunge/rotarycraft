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
import net.scwunge.rotarycraft.transmission.AdvancedGearBlock;
import net.scwunge.rotarycraft.transmission.AdvancedGearBlockEntity;
import net.scwunge.rotarycraft.menu.CvtMenu;
import net.minecraft.server.level.ServerLevel;
import net.scwunge.rotarycraft.transmission.PortalShafts;
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

    // ---- portal shafts ----

    /** A shaft facing east against a nether portal, fed by a flywheel, with the portal and a shaft and a meter set up beyond it in the nether; returns the meter there. */
    static DynamometerBlockEntity portalLink(GameTestHelper helper, Direction farFacing) {
        BlockPos a = new BlockPos(5, 2, 3);
        WeaponGameTests.spinningFlywheel(helper, a.west(), 64, 64, Direction.EAST);
        helper.setBlock(a, RotaryBlocks.SHAFTS.get(ShaftMaterial.STEEL).get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        ServerLevel level = helper.getLevel();
        ServerLevel nether = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        BlockPos p0 = helper.absolutePos(a.east());
        level.setBlock(p0, Blocks.NETHER_PORTAL.defaultBlockState().setValue(net.minecraft.world.level.block.NetherPortalBlock.AXIS, Direction.Axis.Z), 18);
        BlockPos p1 = PortalShafts.across(p0, level, nether);
        // the chunks round the far side are made first: the test runs far faster than the world is generated in play, where this takes a moment
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                nether.getChunk((p1.getX() >> 4) + dx, (p1.getZ() >> 4) + dz);
            }
        }
        nether.setBlock(p1, Blocks.NETHER_PORTAL.defaultBlockState().setValue(net.minecraft.world.level.block.NetherPortalBlock.AXIS, Direction.Axis.Z), 18);
        nether.setBlock(p1.east(), RotaryBlocks.SHAFTS.get(ShaftMaterial.STEEL).get().defaultBlockState().setValue(MachineBlock.FACING, farFacing), 3);
        nether.setBlock(p1.east(2), RotaryBlocks.DYNAMOMETER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST), 3);
        return (DynamometerBlockEntity) nether.getBlockEntity(p1.east(2));
    }

    @GameTest(template = ROOM, timeoutTicks = 400)
    public static void aShaftAtAPortalHandsItsPowerToTheShaftBeyondIt(GameTestHelper helper) {
        DynamometerBlockEntity meter = portalLink(helper, Direction.EAST);
        helper.succeedWhen(() -> helper.assertTrue(meter.getTorque() == 64 && meter.getOmega() == 64, "beyond the portal there is " + meter.getTorque() + " N*m " + meter.getOmega() + " rad/s"));
    }

    @GameTest(template = ROOM, timeoutTicks = 60)
    public static void theShaftBeyondAPortalMustPointBackAtIt(GameTestHelper helper) {
        DynamometerBlockEntity meter = portalLink(helper, Direction.WEST);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(meter.getTorque() == 0, "a shaft pointing the wrong way was fed: " + meter.getTorque());
            helper.succeed();
        });
    }

    @GameTest(template = ROOM, timeoutTicks = 60)
    public static void withNoPortalInFrontNothingCrosses(GameTestHelper helper) {
        DynamometerBlockEntity meter = portalLink(helper, Direction.EAST);
        helper.setBlock(new BlockPos(6, 2, 3), Blocks.AIR);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(meter.getTorque() == 0, "power crossed with no portal: " + meter.getTorque());
            helper.succeed();
        });
    }

    @GameTest(template = ROOM, timeoutTicks = 20)
    public static void aPortalLinkScalesTheNetherByEightAndKeepsTheHeight(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel nether = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        helper.assertTrue(PortalShafts.across(new BlockPos(80, 64, -17), level, nether).equals(new BlockPos(10, 64, -3)), "to the nether: " + PortalShafts.across(new BlockPos(80, 64, -17), level, nether));
        helper.assertTrue(PortalShafts.across(new BlockPos(10, 64, -3), nether, level).equals(new BlockPos(80, 64, -24)), "back from the nether");
        helper.assertTrue(PortalShafts.across(new BlockPos(0, -58, 0), level, nether).getY() == nether.getMinBuildHeight(), "the height is kept within the level");
        helper.assertTrue(PortalShafts.otherSide(level, Blocks.NETHER_PORTAL.defaultBlockState()) == nether && PortalShafts.otherSide(nether, Blocks.NETHER_PORTAL.defaultBlockState()) == level, "nether portals join the overworld and the nether");
        helper.assertTrue(PortalShafts.otherSide(level, Blocks.END_PORTAL.defaultBlockState()).dimension() == net.minecraft.world.level.Level.END, "end portals lead to the end");
        helper.assertTrue(PortalShafts.otherSide(level, Blocks.STONE.defaultBlockState()) == null, "stone is no portal");
        helper.succeed();
    }

    // ---- advanced gears ----

    static AdvancedGearBlockEntity advancedGear(GameTestHelper helper, DeferredBlock<AdvancedGearBlock> block, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, SOURCE, torque, omega, Direction.EAST);
        helper.setBlock(CLUTCH, block.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        return helper.getBlockEntity(CLUTCH);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void aWormGearTradesSpeedForTorqueAndLosesSomeToTheWorm(GameTestHelper helper) {
        advancedGear(helper, TransmissionRegistry.WORM_DRIVE, 2, 4096);
        DynamometerBlockEntity meter = meter(helper, CLUTCH.east(), Direction.EAST);
        helper.succeedWhen(() -> {
            helper.assertTrue(meter.getTorque() == 128, "torque " + meter.getTorque());
            helper.assertTrue(meter.getOmega() == AdvancedGearBlockEntity.wormSpeed(4096) && meter.getOmega() == 51, "speed " + meter.getOmega());
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void aWormGearNeverGivesMoreThanTheLimit(GameTestHelper helper) {
        AdvancedGearBlockEntity gear = advancedGear(helper, TransmissionRegistry.WORM_DRIVE, 40_000_000, 4096);
        helper.succeedWhen(() -> helper.assertTrue(gear.getTorque() == AdvancedGearBlockEntity.LIMIT, "torque " + gear.getTorque()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void theWormGearsLossFollowsTheOriginalsFormula(GameTestHelper helper) {
        helper.assertTrue(Math.abs(AdvancedGearBlockEntity.wormLoss(1024) - 0.88) < 1.0E-9, "at 1024 rad/s " + AdvancedGearBlockEntity.wormLoss(1024));
        helper.assertTrue(Math.abs(AdvancedGearBlockEntity.wormLoss(4096) - 0.80) < 1.0E-9, "at 4096 rad/s " + AdvancedGearBlockEntity.wormLoss(4096));
        helper.assertTrue(AdvancedGearBlockEntity.wormSpeed(63) == 0, "below 64 rad/s nothing comes out");
        helper.succeed();
    }

    // ---- the CVT ----

    /** A CVT fed 64 N*m at 64 rad/s, lubricated, with its last belt in and {@code row} belts in a row from the first slot. */
    static AdvancedGearBlockEntity cvt(GameTestHelper helper, int row, boolean lubricated) {
        AdvancedGearBlockEntity gear = advancedGear(helper, TransmissionRegistry.CVT, 64, 64);
        ItemStack belt = new ItemStack(RotaryParts.part("belt").get());
        for (int i = 0; i < row; i++) {
            gear.belts().setStackInSlot(i, belt.copy());
        }
        gear.belts().setStackInSlot(AdvancedGearBlockEntity.BELT_SLOTS - 1, belt.copy());
        if (lubricated) {
            gear.lubricant().fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        }
        return gear;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void aCvtNeedsLubricantAndABeltInItsLastSlot(GameTestHelper helper) {
        AdvancedGearBlockEntity dry = cvt(helper, 3, false);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(dry.getTorque() == 0 && dry.getOmega() == 0, "a dry CVT passed power");
            dry.lubricant().fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 100), IFluidHandler.FluidAction.EXECUTE);
            dry.belts().setStackInSlot(AdvancedGearBlockEntity.BELT_SLOTS - 1, ItemStack.EMPTY);
        });
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(dry.getTorque() == 0, "a CVT with no belt in its last slot passed power");
            dry.belts().setStackInSlot(AdvancedGearBlockEntity.BELT_SLOTS - 1, new ItemStack(RotaryParts.part("belt").get()));
        });
        helper.succeedWhen(() -> helper.assertTrue(dry.getTorque() > 0, "it should run now"));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void aCvtTradesTorqueForSpeedOrTheOtherWayByItsRatio(GameTestHelper helper) {
        AdvancedGearBlockEntity gear = cvt(helper, 3, true);
        gear.setRatio(4);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(gear.getTorque() == 16 && gear.getOmega() == 256, "speed mode gave " + gear.getTorque() + " N*m " + gear.getOmega() + " rad/s");
            gear.setRatio(-4);
        });
        helper.succeedWhen(() -> helper.assertTrue(gear.getTorque() == 256 && gear.getOmega() == 16, "torque mode gave " + gear.getTorque() + " N*m " + gear.getOmega() + " rad/s"));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void theBeltsInARowSetTheCvtsTopRatioInPowersOfTwo(GameTestHelper helper) {
        AdvancedGearBlockEntity gear = cvt(helper, 0, true);
        helper.assertTrue(gear.maxRatio() == 1, "no belts: " + gear.maxRatio());
        ItemStack belt = new ItemStack(RotaryParts.part("belt").get());
        gear.belts().setStackInSlot(0, belt.copy());
        helper.assertTrue(gear.maxRatio() == 2, "one belt: " + gear.maxRatio());
        gear.belts().setStackInSlot(1, belt.copy());
        helper.assertTrue(gear.maxRatio() == 2, "two belts: " + gear.maxRatio());
        gear.belts().setStackInSlot(2, belt.copy());
        helper.assertTrue(gear.maxRatio() == 4, "three belts: " + gear.maxRatio());
        gear.belts().setStackInSlot(4, belt.copy());
        helper.assertTrue(gear.maxRatio() == 4, "a gap stops the count: " + gear.maxRatio());
        for (int i = 3; i < 31; i++) {
            gear.belts().setStackInSlot(i, belt.copy());
        }
        helper.assertTrue(gear.maxRatio() == 32, "thirty belts: " + gear.maxRatio());
        gear.setRatio(100);
        helper.assertTrue(gear.ratio() == 32, "the ratio should be held to the top one: " + gear.ratio());
        gear.setRatio(0);
        helper.assertTrue(gear.ratio() == 1, "zero is one");
        gear.setRatio(-50);
        helper.assertTrue(gear.ratio() == -32, "torque too: " + gear.ratio());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void aCvtInRedstoneModeUsesTheRatioForTheSignal(GameTestHelper helper) {
        AdvancedGearBlockEntity gear = cvt(helper, 7, true);
        gear.stepMode();
        gear.stepMode();
        helper.assertTrue(gear.mode() == AdvancedGearBlockEntity.CvtMode.AUTO, "mode " + gear.mode());
        gear.stepMode();
        helper.assertTrue(gear.mode() == AdvancedGearBlockEntity.CvtMode.MANUAL, "the modes go round");
        gear.stepMode();
        helper.assertTrue(gear.mode() == AdvancedGearBlockEntity.CvtMode.REDSTONE, "mode " + gear.mode());
        gear.stepState(true);
        gear.stepState(true);
        helper.assertTrue(gear.stateRatio(false) == 1 && gear.stateRatio(true) == 4, "states " + gear.stateRatio(false) + ", " + gear.stateRatio(true));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(gear.getTorque() == 64 && gear.getOmega() == 64, "no signal should be 1x: " + gear.getTorque() + ", " + gear.getOmega());
            helper.setBlock(CLUTCH.above(), Blocks.REDSTONE_BLOCK);
        });
        helper.succeedWhen(() -> helper.assertTrue(gear.getTorque() == 16 && gear.getOmega() == 256, "a signal should be 4x speed: " + gear.getTorque() + ", " + gear.getOmega()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void anAutomaticCvtPicksTheRatioThatKeepsTheTorqueNearItsTarget(GameTestHelper helper) {
        AdvancedGearBlockEntity gear = cvt(helper, 7, true);
        gear.stepMode();
        gear.stepMode();
        gear.setTargetTorque(100);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(gear.ratio() == -2 && gear.getTorque() == 128, "short of the target: ratio " + gear.ratio() + ", " + gear.getTorque() + " N*m");
            gear.setTargetTorque(20);
        });
        helper.succeedWhen(() -> helper.assertTrue(gear.ratio() == 3 && gear.getTorque() == 21 && gear.getOmega() == 192, "over the target: ratio " + gear.ratio() + ", " + gear.getTorque() + " N*m " + gear.getOmega() + " rad/s"));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void aCvtNeverGivesMoreThanTheLimit(GameTestHelper helper) {
        AdvancedGearBlockEntity gear = advancedGear(helper, TransmissionRegistry.CVT, 4, 1_000_000_000);
        ItemStack belt = new ItemStack(RotaryParts.part("belt").get());
        gear.belts().setStackInSlot(0, belt.copy());
        gear.belts().setStackInSlot(1, belt.copy());
        gear.belts().setStackInSlot(AdvancedGearBlockEntity.BELT_SLOTS - 1, belt.copy());
        gear.lubricant().fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        gear.setRatio(4);
        helper.succeedWhen(() -> helper.assertTrue(gear.getOmega() == AdvancedGearBlockEntity.LIMIT, "speed " + gear.getOmega()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void theCvtScreenButtonsAndNumbersWorkAndTheCvtKeepsItsThingsWhenSaved(GameTestHelper helper) {
        AdvancedGearBlockEntity gear = cvt(helper, 7, true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        CvtMenu menu = new CvtMenu(1, player.getInventory(), gear);
        gear.setRatio(4);
        helper.assertTrue(menu.clickMenuButton(player, CvtMenu.FLIP) && gear.ratio() == -4, "flip should make it torque");
        helper.assertTrue(menu.clickMenuButton(player, CvtMenu.MODE) && gear.mode() == AdvancedGearBlockEntity.CvtMode.REDSTONE, "the mode button");
        helper.assertFalse(menu.clickMenuButton(player, CvtMenu.FLIP), "flipping is for manual mode");
        helper.assertTrue(menu.clickMenuButton(player, CvtMenu.STATE_ON) && gear.stateRatio(true) == 2, "state on");
        helper.assertTrue(menu.clickMenuButton(player, CvtMenu.STATE_OFF) && gear.stateRatio(false) == 2, "state off");
        gear.setTargetTorque(77);
        helper.assertTrue(menu.slots.size() == 32 + 36 && menu.getSlot(31).container != null, "the belts and the inventory are in the menu");
        var registries = helper.getLevel().registryAccess();
        var saved = gear.saveWithFullMetadata(registries);
        helper.setBlock(CLUTCH, Blocks.AIR);
        helper.setBlock(CLUTCH, TransmissionRegistry.CVT.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        AdvancedGearBlockEntity other = helper.getBlockEntity(CLUTCH);
        other.loadWithComponents(saved, registries);
        helper.assertTrue(other.ratio() == -4 && other.mode() == AdvancedGearBlockEntity.CvtMode.REDSTONE && other.stateRatio(true) == 2 && other.targetTorque() == 77, "settings lost");
        helper.assertTrue(other.maxRatio() == 8 && other.hasRequiredBelt() && other.lubricant().getFluidAmount() == 1000, "belts or lubricant lost: " + other.maxRatio());
        helper.succeed();
    }

    // ---- the energy coil ----

    static AdvancedGearBlockEntity coil(GameTestHelper helper, DeferredBlock<AdvancedGearBlock> block, int torque, int omega) {
        return advancedGear(helper, block, torque, omega);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void aCoilsChargingAndReleaseLimitsRiseWithWhatItHolds(GameTestHelper helper) {
        AdvancedGearBlockEntity coil = coil(helper, TransmissionRegistry.ENERGY_COIL, 0, 0);
        helper.assertTrue(coil.chargingPower() == 1 && coil.chargingTorque() == 1, "an empty coil takes anything");
        coil.setEnergy(20L * 1_000_000);
        helper.assertTrue(coil.chargingPower() == 131072, "power to charge at a megajoule " + coil.chargingPower());
        helper.assertTrue(coil.chargingTorque() == 4096, "torque to charge at a megajoule " + coil.chargingTorque());
        helper.assertTrue(coil.torqueCap() == 256, "torque it can give at a megajoule " + coil.torqueCap());
        AdvancedGearBlockEntity bedrock = coil(helper, TransmissionRegistry.BEDROCK_ENERGY_COIL, 0, 0);
        bedrock.setEnergy(20L * 1_000_000);
        helper.assertTrue(bedrock.chargingTorque() == 65536, "a bedrock coil asks more torque: " + bedrock.chargingTorque());
        helper.assertTrue(bedrock.maxEmission() == 4096 && coil.maxEmission() == 1024, "emission limits");
        helper.assertTrue(AdvancedGearBlockEntity.ceilPseudoPow2(5) == 6 && AdvancedGearBlockEntity.ceilPseudoPow2(7) == 8 && AdvancedGearBlockEntity.ceilPseudoPow2(9) == 12
                && AdvancedGearBlockEntity.ceilPseudoPow2(3) == 3 && AdvancedGearBlockEntity.ceilPseudoPow2(1) == 1, "the in-between steps");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void aCoilChargesFromTheShaftUntilItsOwnLimitsStopIt(GameTestHelper helper) {
        AdvancedGearBlockEntity coil = coil(helper, TransmissionRegistry.ENERGY_COIL, 2, 8);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(coil.energy() > 0 && coil.energy() < 400, "energy " + coil.energy());
            helper.assertTrue(coil.chargingTorque() > 2 || coil.chargingPower() > 16, "a fuller coil asks more than the 2 N*m and 16 W it gets: " + coil.chargingTorque() + ", " + coil.chargingPower());
            long held = coil.energy();
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(coil.energy() == held, "it went on charging against its own limit");
                helper.succeed();
            });
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void aCoilGivesBackWhatItHoldsAtTheSetSpeedAndTorqueWhileItHasASignal(GameTestHelper helper) {
        AdvancedGearBlockEntity coil = coil(helper, TransmissionRegistry.ENERGY_COIL, 0, 0);
        coil.setEnergy(20L * 1_000_000);
        coil.setReleaseOmega(100);
        coil.setReleaseTorque(50);
        DynamometerBlockEntity meter = meter(helper, CLUTCH.east(), Direction.EAST);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(meter.getTorque() == 0, "it gave power with no signal");
            helper.setBlock(CLUTCH.above(), Blocks.REDSTONE_BLOCK);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(meter.getTorque() == 50 && meter.getOmega() == 100, "the coil gave " + meter.getTorque() + " N*m " + meter.getOmega() + " rad/s");
            helper.assertTrue(coil.energy() < 20L * 1_000_000, "its energy should be going down: " + coil.energy());
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void aCoilThatRunsOutStopsGivingPower(GameTestHelper helper) {
        AdvancedGearBlockEntity coil = coil(helper, TransmissionRegistry.ENERGY_COIL, 0, 0);
        coil.setEnergy(60);
        coil.setReleaseOmega(10);
        coil.setReleaseTorque(1);
        helper.setBlock(CLUTCH.above(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> helper.assertTrue(coil.energy() == 0 && coil.getTorque() == 0, "energy " + coil.energy() + ", giving " + coil.getTorque()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void theReleaseSettingsStayWithinWhatTheCoilCanGive(GameTestHelper helper) {
        AdvancedGearBlockEntity coil = coil(helper, TransmissionRegistry.ENERGY_COIL, 0, 0);
        coil.setEnergy(20L * 1_000_000);
        coil.setReleaseOmega(5000);
        coil.setReleaseTorque(5000);
        helper.assertTrue(coil.releaseOmega() == 1024, "speed " + coil.releaseOmega());
        helper.assertTrue(coil.releaseTorque() == 256, "torque is held to what the energy allows: " + coil.releaseTorque());
        AdvancedGearBlockEntity bedrock = coil(helper, TransmissionRegistry.BEDROCK_ENERGY_COIL, 0, 0);
        bedrock.setEnergy(20L * 1_000_000_000L);
        bedrock.setReleaseOmega(100000);
        helper.assertTrue(bedrock.releaseOmega() == 4096, "bedrock speed " + bedrock.releaseOmega());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void anOverchargedCoilBlowsUp(GameTestHelper helper) {
        var breaks = net.scwunge.rotarycraft.config.RotaryConfig.EXPLOSIONS_BREAK_BLOCKS;
        net.scwunge.rotarycraft.config.RotaryConfig.override(breaks, false);
        AdvancedGearBlockEntity coil = coil(helper, TransmissionRegistry.ENERGY_COIL, 0, 0);
        coil.setEnergy(AdvancedGearBlockEntity.CAPACITY * 20);
        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(TransmissionRegistry.ENERGY_COIL.get(), CLUTCH);
            net.scwunge.rotarycraft.config.RotaryConfig.clearOverride(breaks);
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void aCoilKeepsItsEnergyInItsItemAndWhenSaved(GameTestHelper helper) {
        AdvancedGearBlockEntity coil = coil(helper, TransmissionRegistry.ENERGY_COIL, 0, 0);
        coil.setEnergy(123456);
        coil.setReleaseOmega(77);
        var components = coil.collectComponents();
        helper.assertTrue(components.getOrDefault(net.scwunge.rotarycraft.registry.RotaryComponents.COIL_ENERGY.get(), 0L) == 123456L, "the item should carry the energy");
        ItemStack item = new ItemStack(TransmissionRegistry.ENERGY_COIL.get());
        item.set(net.scwunge.rotarycraft.registry.RotaryComponents.COIL_ENERGY.get(), 999L);
        helper.setBlock(CLUTCH, Blocks.AIR);
        helper.setBlock(CLUTCH, TransmissionRegistry.ENERGY_COIL.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        AdvancedGearBlockEntity placed = helper.getBlockEntity(CLUTCH);
        placed.applyComponentsFromItemStack(item);
        helper.assertTrue(placed.energy() == 999, "a placed coil starts with its item's energy: " + placed.energy());
        placed.setEnergy(555);
        placed.setReleaseOmega(33);
        var registries = helper.getLevel().registryAccess();
        var saved = placed.saveWithFullMetadata(registries);
        helper.setBlock(CLUTCH, Blocks.AIR);
        helper.setBlock(CLUTCH, TransmissionRegistry.ENERGY_COIL.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        AdvancedGearBlockEntity other = helper.getBlockEntity(CLUTCH);
        other.loadWithComponents(saved, registries);
        helper.assertTrue(other.energy() == 555 && other.releaseOmega() == 33, "saved energy " + other.energy());
        helper.succeed();
    }

    // ---- the 256x gear ----

    static AdvancedGearBlockEntity highGear(GameTestHelper helper, int torque, int omega, boolean lubricated) {
        AdvancedGearBlockEntity gear = advancedGear(helper, TransmissionRegistry.HIGH_GEAR, torque, omega);
        if (lubricated) {
            gear.lubricant().fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 5000), IFluidHandler.FluidAction.EXECUTE);
        }
        return gear;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void aHighGearTradesSpeedForTorqueByTwoHundredAndFiftySix(GameTestHelper helper) {
        AdvancedGearBlockEntity gear = highGear(helper, 2, 1024, true);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(gear.getTorque() == 512 && gear.getOmega() == 4, "torque mode gave " + gear.getTorque() + " N*m " + gear.getOmega() + " rad/s");
            gear.setTorqueMode(false);
            WeaponGameTests.spinningFlywheel(helper, SOURCE, 1024, 8, Direction.EAST);
        });
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(gear.getTorque() == 4 && gear.getOmega() == 2048, "speed mode gave " + gear.getTorque() + " N*m " + gear.getOmega() + " rad/s");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void aHighGearWithNoLubricantPassesNothingAndOneThatRunsUsesSome(GameTestHelper helper) {
        AdvancedGearBlockEntity gear = highGear(helper, 2, 1024, false);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(gear.getTorque() == 0 && gear.getOmega() == 0, "a dry gear passed power");
            gear.lubricant().fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 5000), IFluidHandler.FluidAction.EXECUTE);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(gear.getTorque() == 512, "it should run: " + gear.getTorque());
            helper.assertTrue(gear.lubricant().getFluidAmount() < 5000, "it should be using lubricant: " + gear.lubricant().getFluidAmount());
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void aHighGearNeverGivesMoreThanTheLimit(GameTestHelper helper) {
        AdvancedGearBlockEntity gear = highGear(helper, 40_000_000, 1024, true);
        helper.succeedWhen(() -> helper.assertTrue(gear.getTorque() == AdvancedGearBlockEntity.LIMIT, "torque " + gear.getTorque()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void theScrewdriverTurnsAdvancedGearsLevelAndSneakingSwitchesTheHighGearsMode(GameTestHelper helper) {
        AdvancedGearBlockEntity gear = highGear(helper, 2, 1024, true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack tool = new ItemStack(RotaryItems.SCREWDRIVER.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tool);
        net.minecraft.world.item.context.UseOnContext use = new net.minecraft.world.item.context.UseOnContext(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, tool,
                new net.minecraft.world.phys.BlockHitResult(helper.absolutePos(CLUTCH).getCenter(), Direction.UP, helper.absolutePos(CLUTCH), false));
        RotaryItems.SCREWDRIVER.get().useOn(use);
        helper.assertTrue(helper.getBlockState(CLUTCH).getValue(MachineBlock.FACING) == Direction.SOUTH, "it should turn a quarter: " + helper.getBlockState(CLUTCH).getValue(MachineBlock.FACING));
        for (int i = 0; i < 3; i++) {
            RotaryItems.SCREWDRIVER.get().useOn(use);
        }
        helper.assertTrue(helper.getBlockState(CLUTCH).getValue(MachineBlock.FACING) == Direction.EAST, "four turns make a full one");
        player.setShiftKeyDown(true);
        RotaryItems.SCREWDRIVER.get().useOn(use);
        AdvancedGearBlockEntity after = helper.getBlockEntity(CLUTCH);
        helper.assertFalse(after.isTorqueMode(), "sneaking should switch to speed mode");
        var registries = helper.getLevel().registryAccess();
        var saved = after.saveWithFullMetadata(registries);
        helper.setBlock(CLUTCH, Blocks.AIR);
        helper.setBlock(CLUTCH, TransmissionRegistry.HIGH_GEAR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        AdvancedGearBlockEntity other = helper.getBlockEntity(CLUTCH);
        other.loadWithComponents(saved, registries);
        helper.assertFalse(other.isTorqueMode(), "the mode should be saved");
        helper.succeed();
    }
}
