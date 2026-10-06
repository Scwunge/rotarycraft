package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.block.ReservoirBlock;
import net.scwunge.rotarycraft.blockentity.PumpBlockEntity;
import net.scwunge.rotarycraft.blockentity.ReservoirBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryFluids;

/** Pump and Reservoir checks. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class PumpGameTests {
    static final String TEMPLATE = "empty5x4x5";

    /** A walled 3x3 water pool at y=1 with the pump over its middle, driven by the default motor (4 kW, 16 N*m). */
    static void pool(GameTestHelper helper) {
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                boolean inside = x >= 1 && x <= 3 && z >= 1 && z <= 3;
                helper.setBlock(new BlockPos(x, 1, z), inside ? Blocks.WATER : Blocks.STONE);
            }
        }
        helper.setBlock(new BlockPos(1, 2, 2), RotaryBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        IEnergyStorage e = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(new BlockPos(1, 2, 2)), Direction.UP);
        e.receiveEnergy(100_000, false);
        helper.setBlock(new BlockPos(2, 2, 2), RotaryBlocks.PUMP.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void pumpsWaterOutOfAPool(GameTestHelper helper) {
        pool(helper);
        helper.succeedWhen(() -> {
            PumpBlockEntity p = (PumpBlockEntity) helper.getBlockEntity(new BlockPos(2, 2, 2));
            helper.assertTrue(p.tank().getFluid().is(Fluids.WATER) && p.tank().getFluidAmount() >= 1000, "no water pumped yet");
            int sources = 0;
            for (int x = 1; x <= 3; x++) {
                for (int z = 1; z <= 3; z++) {
                    if (helper.getLevel().getFluidState(helper.absolutePos(new BlockPos(x, 1, z))).isSource()) {
                        sources++;
                    }
                }
            }
            helper.assertTrue(sources < 9, "a source block should have been taken");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void pumpGivesFromItsSidesOnly(GameTestHelper helper) {
        pool(helper);
        BlockPos abs = helper.absolutePos(new BlockPos(2, 2, 2));
        var level = helper.getLevel();
        helper.assertTrue(level.getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.UP) == null
                && level.getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.DOWN) == null, "top/bottom expose the tank");
        IFluidHandler side = level.getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.NORTH);
        helper.assertTrue(side != null && side.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE) == 0, "the pump took fluid in");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void reservoirsShareAndDrainFromTheBottom(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 2), RotaryBlocks.RESERVOIR.get().defaultBlockState());
        helper.setBlock(new BlockPos(2, 1, 2), RotaryBlocks.RESERVOIR.get().defaultBlockState());
        var level = helper.getLevel();
        IFluidHandler top = level.getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(new BlockPos(1, 1, 2)), Direction.UP);
        helper.assertTrue(top.fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 20_000), IFluidHandler.FluidAction.EXECUTE) == 20_000, "didn't fill");
        helper.assertTrue(top.drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "drained from the top");
        helper.succeedWhen(() -> {
            int a = ((ReservoirBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 2))).tank().getFluidAmount();
            int b = ((ReservoirBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2))).tank().getFluidAmount();
            helper.assertTrue(Math.abs(a - b) <= 2 && a + b == 20_000, "neighbouring reservoirs should even out: " + a + " / " + b);
            IFluidHandler bottom = level.getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(new BlockPos(2, 1, 2)), Direction.DOWN);
            helper.assertTrue(bottom.drain(1000, IFluidHandler.FluidAction.SIMULATE).getAmount() == 1000, "can't drain from the bottom");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void coveredReservoirIsClosed(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), RotaryBlocks.RESERVOIR.get().defaultBlockState().setValue(ReservoirBlock.COVERED, true));
        helper.assertTrue(((ReservoirBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2))).covered(), "not covered");
        helper.succeed();
    }
}
