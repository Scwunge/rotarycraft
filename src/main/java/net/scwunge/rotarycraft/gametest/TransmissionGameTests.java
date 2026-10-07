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
}
