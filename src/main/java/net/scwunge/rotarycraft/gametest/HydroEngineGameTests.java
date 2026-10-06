package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.HydroEngineBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryFluids;

/** Hydrokinetic Engine checks. Real waterfalls are tested in game; here the formula and the conditions. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class HydroEngineGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final BlockPos ENGINE = new BlockPos(2, 1, 2);

    @GameTest(template = TEMPLATE)
    public static void fallPowerFollowsTheOriginal(GameTestHelper helper) {
        int[] tall = HydroEngineBlockEntity.fallPower(64, 1000, 1000);
        helper.assertTrue(tall[0] == 32 && tall[1] == 16384, "a 64-block water fall should max out at 32 rad/s, 16384 N*m: " + tall[0] + ", " + tall[1]);
        int[] small = HydroEngineBlockEntity.fallPower(8, 1000, 1000);
        helper.assertTrue(small[0] == 6 && Math.abs(small[1] - 733) <= 2, "an 8-block fall: 6 rad/s, ~733 N*m, got " + small[0] + ", " + small[1]);
        int[] thick = HydroEngineBlockEntity.fallPower(8, 1000, 6000);
        helper.assertTrue(thick[0] < small[0], "a thicker liquid falls slower");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void needsLubricantAndAColumn(GameTestHelper helper) {
        helper.setBlock(ENGINE, RotaryBlocks.HYDRO_ENGINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        HydroEngineBlockEntity h = (HydroEngineBlockEntity) helper.getBlockEntity(ENGINE);
        h.lubricant().fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(((HydroEngineBlockEntity) helper.getBlockEntity(ENGINE)).getOmega() == 0, "ran without falling water beside it");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void bedrockRodUpgradesAndMends(GameTestHelper helper) {
        helper.setBlock(ENGINE, RotaryBlocks.HYDRO_ENGINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        HydroEngineBlockEntity h = (HydroEngineBlockEntity) helper.getBlockEntity(ENGINE);
        helper.assertTrue(h.makeBedrock() && h.isBedrock() && !h.isFailed(), "bedrock upgrade");
        helper.assertFalse(h.makeBedrock(), "upgraded twice");
        helper.assertTrue(h.columnSide() == Direction.WEST, "facing north, the column is on the west");
        helper.succeed();
    }
}
