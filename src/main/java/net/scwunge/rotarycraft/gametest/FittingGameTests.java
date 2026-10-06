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
import net.scwunge.rotarycraft.block.PipeBlock;
import net.scwunge.rotarycraft.blockentity.PipeBlockEntity;
import net.scwunge.rotarycraft.blockentity.RockMelterBlockEntity;
import net.scwunge.rotarycraft.pipe.PipeType;
import net.scwunge.rotarycraft.registry.RotaryBlocks;

/** Valve, separator, bypass and suction pipe. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class FittingGameTests {
    static final String TEMPLATE = "empty5x4x5";

    static void place(GameTestHelper helper, BlockPos pos, PipeType t) {
        helper.setBlock(pos, RotaryBlocks.PIPES.get(t).get().defaultBlockState());
        helper.getLevel().scheduleTick(helper.absolutePos(pos), RotaryBlocks.PIPES.get(t).get(), 1);
    }

    static PipeBlockEntity at(GameTestHelper helper, BlockPos pos) {
        return (PipeBlockEntity) helper.getBlockEntity(pos);
    }

    static void lavaMelter(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, RotaryBlocks.ROCK_MELTER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        ((RockMelterBlockEntity) helper.getBlockEntity(pos)).tank().fill(new FluidStack(Fluids.LAVA, 8000), IFluidHandler.FluidAction.EXECUTE);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void valveDrawsOnlyWhenPowered(GameTestHelper helper) {
        lavaMelter(helper, new BlockPos(2, 1, 2));
        place(helper, new BlockPos(1, 1, 2), PipeType.VALVE);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(at(helper, new BlockPos(1, 1, 2)).amount() == 0, "an unpowered valve drew lava");
            helper.setBlock(new BlockPos(0, 1, 2), Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(at(helper, new BlockPos(1, 1, 2)).contents().is(Fluids.LAVA), "a powered valve should draw lava");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void separatorLetsFluidOutDownwardsOnly(GameTestHelper helper) {
        place(helper, new BlockPos(1, 2, 2), PipeType.PIPE);
        place(helper, new BlockPos(2, 2, 2), PipeType.SEPARATOR);
        place(helper, new BlockPos(2, 1, 2), PipeType.PIPE);
        place(helper, new BlockPos(2, 3, 2), PipeType.PIPE);
        helper.runAfterDelay(3, () -> at(helper, new BlockPos(1, 2, 2)).input().fill(new FluidStack(Fluids.WATER, 8000), IFluidHandler.FluidAction.EXECUTE));
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(at(helper, new BlockPos(2, 1, 2)).amount() > 500, "nothing came out of the bottom");
            helper.assertTrue(at(helper, new BlockPos(2, 3, 2)).amount() == 0, "fluid came out of the top");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bypassesOnlyJoinWhenPlacedAgainstEachOther(GameTestHelper helper) {
        place(helper, new BlockPos(1, 1, 2), PipeType.BYPASS);
        place(helper, new BlockPos(2, 1, 2), PipeType.BYPASS);
        // placed against the first: the joint is stored in the state, as a player's placement would
        helper.setBlock(new BlockPos(2, 1, 3), RotaryBlocks.PIPES.get(PipeType.BYPASS).get().defaultBlockState()
                .setValue(PipeBlock.PROPERTIES.get(Direction.WEST), false).setValue(PipeBlock.PROPERTIES.get(Direction.NORTH), true));
        helper.runAfterDelay(10, () -> {
            helper.assertFalse(helper.getBlockState(new BlockPos(1, 1, 2)).getValue(PipeBlock.PROPERTIES.get(Direction.EAST)), "side-by-side bypasses joined");
            helper.assertTrue(helper.getBlockState(new BlockPos(2, 1, 2)).getValue(PipeBlock.PROPERTIES.get(Direction.SOUTH)), "the bypass placed against it should join");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void suctionPipeDrawsFromAnySide(GameTestHelper helper) {
        lavaMelter(helper, new BlockPos(2, 1, 2));
        place(helper, new BlockPos(1, 1, 2), PipeType.SUCTION);
        helper.succeedWhen(() -> helper.assertTrue(at(helper, new BlockPos(1, 1, 2)).contents().is(Fluids.LAVA), "the suction pipe drew nothing"));
    }

    @GameTest(template = TEMPLATE)
    public static void fittingJoinRules(GameTestHelper helper) {
        helper.assertTrue(PipeType.VALVE.connectsTo(PipeType.HOSE) && PipeType.VALVE.connectsTo(PipeType.FUEL_LINE), "valve joins any pipe");
        helper.assertFalse(PipeType.BYPASS.connectsTo(PipeType.HOSE), "the original's hose never joins a bypass");
        helper.assertFalse(PipeType.SUCTION.connectsTo(PipeType.VALVE), "suction pipe doesn't join a valve");
        helper.succeed();
    }
}
