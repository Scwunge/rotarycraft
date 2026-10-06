package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.ACEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.MagnetizerBlockEntity;
import net.scwunge.rotarycraft.item.ShaftCoreItem;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryItems;

/** AC Engine and Magnetizer checks. A redstone block toggled every two ticks next to the machine is the AC signal. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ACEngineGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final BlockPos MACHINE = new BlockPos(2, 1, 2);
    static final BlockPos CLOCK = new BlockPos(2, 1, 1);

    static void alternate(GameTestHelper helper) {
        int[] tick = new int[1];
        helper.onEachTick(() -> {
            if (tick[0]++ % 2 == 0) {
                boolean on = helper.getBlockState(CLOCK).is(Blocks.REDSTONE_BLOCK);
                helper.setBlock(CLOCK, on ? Blocks.AIR : Blocks.REDSTONE_BLOCK);
            }
        });
    }

    static ItemStack core(int uT) {
        ItemStack s = new ItemStack(RotaryItems.SHAFT_CORE.get());
        ShaftCoreItem.setMagnetization(s, uT);
        return s;
    }

    static ACEngineBlockEntity engine(GameTestHelper helper, ItemStack core) {
        helper.setBlock(MACHINE, RotaryBlocks.AC_ENGINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        ACEngineBlockEntity e = (ACEngineBlockEntity) helper.getBlockEntity(MACHINE);
        e.items().setStackInSlot(0, core);
        return e;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void runsOnAlternatingRedstoneWithACore(GameTestHelper helper) {
        engine(helper, core(100));
        alternate(helper);
        helper.succeedWhen(() -> {
            ACEngineBlockEntity e = (ACEngineBlockEntity) helper.getBlockEntity(MACHINE);
            helper.assertTrue(e.getOmega() == ACEngineBlockEntity.SPEED && e.getTorque() == ACEngineBlockEntity.TORQUE,
                    "expected 512 N*m at 256 rad/s, got " + e.getTorque() + " at " + e.getOmega());
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void steadyRedstoneIsNotEnough(GameTestHelper helper) {
        engine(helper, core(100));
        helper.setBlock(CLOCK, Blocks.REDSTONE_BLOCK);
        // the signal's first three ticks count as a change (as in the original), so it twitches and then coasts to a stop
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(((ACEngineBlockEntity) helper.getBlockEntity(MACHINE)).getOmega() == 0, "ran on a steady (DC) signal");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void unmagnetizedCoreDoesNothing(GameTestHelper helper) {
        engine(helper, new ItemStack(RotaryItems.SHAFT_CORE.get()));
        alternate(helper);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(((ACEngineBlockEntity) helper.getBlockEntity(MACHINE)).getOmega() == 0, "ran with a plain core");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 700)
    public static void runningUsesUpTheCore(GameTestHelper helper) {
        engine(helper, core(5));
        alternate(helper);
        helper.succeedWhen(() -> {
            int m = ShaftCoreItem.magnetization(((ACEngineBlockEntity) helper.getBlockEntity(MACHINE)).items().getStackInSlot(0));
            helper.assertTrue(m == 4, "600 ticks of running should take 1 uT, core at " + m);
        });
    }

    /** 8192 rad/s from the boosted motor: cycles of 140 ticks; a tungsten core charges every cycle. */
    @GameTest(template = TEMPLATE, batch = ExtractorGameTests.BATCH, timeoutTicks = 400)
    public static void magnetizerChargesACore(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 2), RotaryBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        IEnergyStorage e = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(new BlockPos(1, 1, 2)), Direction.UP);
        e.receiveEnergy(100_000, false);
        helper.setBlock(MACHINE, RotaryBlocks.MAGNETIZER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        ((MagnetizerBlockEntity) helper.getBlockEntity(MACHINE)).items().setStackInSlot(0, new ItemStack(RotaryItems.TUNGSTEN_SHAFT_CORE.get()));
        alternate(helper);
        helper.succeedWhen(() -> {
            int m = ShaftCoreItem.magnetization(((MagnetizerBlockEntity) helper.getBlockEntity(MACHINE)).items().getStackInSlot(0));
            helper.assertTrue(m >= 2, "core at " + m + " uT");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void magnetizerNeedsItsSpeed(GameTestHelper helper) {
        // the default motor's 256 rad/s is under the 2048 needed
        helper.setBlock(new BlockPos(1, 1, 2), RotaryBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        IEnergyStorage e = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(new BlockPos(1, 1, 2)), Direction.UP);
        e.receiveEnergy(100_000, false);
        helper.setBlock(MACHINE, RotaryBlocks.MAGNETIZER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        ((MagnetizerBlockEntity) helper.getBlockEntity(MACHINE)).items().setStackInSlot(0, new ItemStack(RotaryItems.TUNGSTEN_SHAFT_CORE.get()));
        alternate(helper);
        helper.runAfterDelay(150, () -> {
            MagnetizerBlockEntity m = (MagnetizerBlockEntity) helper.getBlockEntity(MACHINE);
            helper.assertTrue(m.getOmega() > 0 && ShaftCoreItem.magnetization(m.items().getStackInSlot(0)) == 0, "charged below 2048 rad/s");
            helper.succeed();
        });
    }
}
