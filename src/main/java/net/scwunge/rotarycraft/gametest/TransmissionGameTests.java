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
import net.scwunge.rotarycraft.menu.MultiClutchMenu;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;
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
}
