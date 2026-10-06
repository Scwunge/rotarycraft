package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.block.PipeBlock;
import net.scwunge.rotarycraft.blockentity.GasEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.PipeBlockEntity;
import net.scwunge.rotarycraft.pipe.PipeType;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryFluids;

/** Pipes: what each kind carries, how they join, flow between pipes, intake from below and delivery to the sides. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class PipeGameTests {
    static final String TEMPLATE = "empty5x4x5";

    static PipeBlockEntity pipe(GameTestHelper helper, BlockPos pos) {
        return (PipeBlockEntity) helper.getBlockEntity(pos);
    }

    static void place(GameTestHelper helper, BlockPos pos, PipeType t) {
        helper.setBlock(pos, RotaryBlocks.PIPES.get(t).get().defaultBlockState());
        helper.getLevel().scheduleTick(helper.absolutePos(pos), RotaryBlocks.PIPES.get(t).get(), 1);
    }

    @GameTest(template = TEMPLATE)
    public static void eachKindCarriesItsFluids(GameTestHelper helper) {
        FluidStack water = new FluidStack(Fluids.WATER, 1);
        FluidStack ethanol = new FluidStack(RotaryFluids.ETHANOL.get(), 1);
        FluidStack jet = new FluidStack(RotaryFluids.JET_FUEL.get(), 1);
        FluidStack lube = new FluidStack(RotaryFluids.LUBRICANT.get(), 1);
        helper.assertTrue(PipeType.PIPE.carries(water) && !PipeType.PIPE.carries(ethanol) && !PipeType.PIPE.carries(lube), "pipe: liquids but not fuel or lubricant");
        helper.assertTrue(PipeType.FUEL_LINE.carries(ethanol) && PipeType.FUEL_LINE.carries(jet) && !PipeType.FUEL_LINE.carries(water), "fuel line: fuels only");
        helper.assertTrue(PipeType.HOSE.carries(lube) && !PipeType.HOSE.carries(water), "hose: lubricant only");
        helper.assertTrue(PipeType.BEDROCK.carries(water) && PipeType.BEDROCK.carries(jet) && PipeType.BEDROCK.carries(lube), "bedrock pipe: anything");
        helper.assertTrue(PipeType.PIPE.connectsTo(PipeType.BEDROCK) && !PipeType.PIPE.connectsTo(PipeType.HOSE) && !PipeType.FUEL_LINE.connectsTo(PipeType.PIPE),
                "pipe joins bedrock pipe only");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void fluidEvensOutAlongARun(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) {
            place(helper, new BlockPos(x, 1, 2), PipeType.PIPE);
        }
        helper.runAfterDelay(3, () -> pipe(helper, new BlockPos(0, 1, 2)).input().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE));
        helper.succeedWhen(() -> {
            int far = pipe(helper, new BlockPos(4, 1, 2)).amount();
            int near = pipe(helper, new BlockPos(0, 1, 2)).amount();
            helper.assertTrue(far > 500 && Math.abs(near - far) < 300, "water should spread along the run: " + near + " ... " + far);
            helper.assertTrue(helper.getBlockState(new BlockPos(1, 1, 2)).getValue(PipeBlock.PROPERTIES.get(Direction.WEST)), "pipes didn't join");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void pipesDontMixKinds(GameTestHelper helper) {
        place(helper, new BlockPos(1, 1, 2), PipeType.PIPE);
        place(helper, new BlockPos(2, 1, 2), PipeType.HOSE);
        helper.runAfterDelay(5, () -> {
            helper.assertFalse(helper.getBlockState(new BlockPos(1, 1, 2)).getValue(PipeBlock.PROPERTIES.get(Direction.EAST)), "pipe joined a hose");
            helper.assertTrue(pipe(helper, new BlockPos(2, 1, 2)).input().fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE) == 0,
                    "hose took water");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void fuelLineFeedsAnEngineBesideIt(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), RotaryBlocks.GAS_ENGINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        place(helper, new BlockPos(1, 1, 2), PipeType.FUEL_LINE);
        helper.runAfterDelay(3, () -> pipe(helper, new BlockPos(1, 1, 2)).input().fill(new FluidStack(RotaryFluids.ETHANOL.get(), 3000), IFluidHandler.FluidAction.EXECUTE));
        helper.succeedWhen(() -> {
            int fuel = ((GasEngineBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2))).fuel().getFluidAmount();
            helper.assertTrue(fuel > 1000, "the engine should fill from the fuel line beside it, has " + fuel);
        });
    }

    /** Pipes draw from machines above and below them: a fuel line on top of a Fractionation Unit takes its jet fuel. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void fuelLineDrawsFromTheMachineBelow(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), RotaryBlocks.FRACTIONATOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        var unit = (net.scwunge.rotarycraft.blockentity.FractionatorBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
        unit.fuel().fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 8000), IFluidHandler.FluidAction.EXECUTE);
        place(helper, new BlockPos(2, 2, 2), PipeType.FUEL_LINE);
        helper.succeedWhen(() -> {
            PipeBlockEntity line = pipe(helper, new BlockPos(2, 2, 2));
            helper.assertTrue(line.amount() > 500 && line.contents().is(RotaryFluids.JET_FUEL.get()), "fuel line drew " + line.amount() + " mB");
        });
    }

    /** ...but not from machines beside them (those are what they deliver to). */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void pipeBesideAMachineDoesNotDrawFromIt(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), RotaryBlocks.ROCK_MELTER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        var melter = (net.scwunge.rotarycraft.blockentity.RockMelterBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
        melter.tank().fill(new FluidStack(Fluids.LAVA, 8000), IFluidHandler.FluidAction.EXECUTE);
        place(helper, new BlockPos(1, 1, 2), PipeType.PIPE);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(pipe(helper, new BlockPos(1, 1, 2)).amount() == 0, "a pipe pulled from the machine beside it");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void overpressureBurstsAPipe(GameTestHelper helper) {
        place(helper, new BlockPos(2, 1, 2), PipeType.PIPE);
        helper.runAfterDelay(3, () -> pipe(helper, new BlockPos(2, 1, 2)).input().fill(new FluidStack(Fluids.WATER, 100_000), IFluidHandler.FluidAction.EXECUTE));
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.WATER, new BlockPos(2, 1, 2)));
    }
}
