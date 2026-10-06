package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.DryerBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class DryerGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final BlockPos AT = new BlockPos(2, 1, 2);

    static DryerBlockEntity dryer(GameTestHelper helper) {
        helper.setBlock(AT, RotaryBlocks.DRYER.get().defaultBlockState());
        return at(helper);
    }

    static DryerBlockEntity at(GameTestHelper helper) {
        return (DryerBlockEntity) helper.getBlockEntity(AT);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void waterDriesIntoSalt(GameTestHelper helper) {
        DryerBlockEntity d = dryer(helper);
        d.tank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(DryerBlockEntity.PERIOD + 20, () -> {
            DryerBlockEntity b = at(helper);
            helper.assertTrue(b.items().getStackInSlot(0).is(RotaryItems.SALT.get()) && b.items().getStackInSlot(0).getCount() == 4,
                    "1000 mB of water is four batches of salt, got " + b.items().getStackInSlot(0));
            helper.assertTrue(b.tank().getFluidAmount() == 0, "the water should be used up");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void lessThanABatchWaitsForMore(GameTestHelper helper) {
        DryerBlockEntity d = dryer(helper);
        d.tank().fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(DryerBlockEntity.PERIOD + 20, () -> {
            helper.assertTrue(at(helper).items().getStackInSlot(0).isEmpty(), "100 mB isn't a batch of 250");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void lavaDriesIntoGold(GameTestHelper helper) {
        DryerBlockEntity d = dryer(helper);
        d.tank().fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(DryerBlockEntity.PERIOD + 20, () -> {
            helper.assertTrue(at(helper).items().getStackInSlot(0).is(Items.GOLD_NUGGET), "no gold nugget yet");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void fluidInFromTheSidesNotTheBottom(GameTestHelper helper) {
        dryer(helper);
        var level = helper.getLevel();
        BlockPos abs = helper.absolutePos(AT);
        helper.assertTrue(level.getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.DOWN) == null, "no fluid access from below");
        IFluidHandler side = level.getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.EAST);
        helper.assertTrue(side.fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE) == 500, "water goes in");
        helper.assertTrue(side.fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 500), IFluidHandler.FluidAction.EXECUTE) == 0, "lubricant dries into nothing");
        helper.assertTrue(side.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "nothing comes back out");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void onlyTheOutputCanBeTaken(GameTestHelper helper) {
        DryerBlockEntity d = dryer(helper);
        d.items().setStackInSlot(0, new net.minecraft.world.item.ItemStack(RotaryItems.SALT.get(), 3));
        helper.assertTrue(d.outputItems().insertItem(0, new net.minecraft.world.item.ItemStack(Items.DIRT), false).getCount() == 1, "nothing goes in");
        helper.assertTrue(d.outputItems().extractItem(0, 2, false).getCount() == 2, "salt comes out");
        helper.succeed();
    }
}
