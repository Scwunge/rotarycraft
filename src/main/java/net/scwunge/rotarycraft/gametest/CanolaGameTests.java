package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.CanolaBlock;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CanolaGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final BlockPos SOIL = new BlockPos(2, 1, 2);
    static final BlockPos CROP = SOIL.above();

    @GameTest(template = TEMPLATE)
    public static void growsToFullOnWetFarmland(GameTestHelper helper) {
        helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        helper.setBlock(CROP, RotaryBlocks.CANOLA.get().defaultBlockState());
        helper.setBlock(CROP.east(), Blocks.GLOWSTONE);
        var level = helper.getLevel();
        BlockPos abs = helper.absolutePos(CROP);
        for (int i = 0; i < 300; i++) {
            BlockState s = level.getBlockState(abs);
            if (s.is(RotaryBlocks.CANOLA.get())) {
                s.randomTick(level, abs, level.random);
            }
        }
        BlockState s = level.getBlockState(abs);
        helper.assertTrue(s.is(RotaryBlocks.CANOLA.get()) && s.getValue(CanolaBlock.AGE) == CanolaBlock.GROWN,
                "canola should be grown after many random ticks, is " + s);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void needsHydratedFarmland(GameTestHelper helper) {
        helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
        BlockState canola = RotaryBlocks.CANOLA.get().defaultBlockState();
        helper.assertFalse(canola.canSurvive(helper.getLevel(), helper.absolutePos(CROP)), "survives on dry farmland");
        helper.setBlock(SOIL, Blocks.DIRT);
        helper.assertFalse(canola.canSurvive(helper.getLevel(), helper.absolutePos(CROP)), "survives on dirt");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void grownDropsFollowTheOriginal(GameTestHelper helper) {
        RandomSource r = RandomSource.create(1);
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (int i = 0; i < 2000; i++) {
            int n = CanolaBlock.grownDrops(0, r);
            min = Math.min(min, n);
            max = Math.max(max, n);
        }
        helper.assertTrue(min == 2 && max <= 26 && max >= 20, "drops should run 2..26, saw " + min + ".." + max);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void huskChainToLubricant(GameTestHelper helper) {
        var level = helper.getLevel();
        var grind = level.getRecipeManager().getRecipeFor(RotaryRecipes.GRINDING.get(), new SingleRecipeInput(new ItemStack(RotaryItems.CANOLA_SEEDS.get())), level);
        helper.assertTrue(grind.isPresent() && grind.get().value().getResultItem(level.registryAccess()).is(RotaryItems.CANOLA_HUSKS.get()), "seeds don't grind into husks");
        var spin = level.getRecipeManager().getRecipeFor(RotaryRecipes.CENTRIFUGE.get(), new SingleRecipeInput(new ItemStack(RotaryItems.CANOLA_HUSKS.get())), level);
        helper.assertTrue(spin.isPresent() && spin.get().value().fluid().is(RotaryFluids.LUBRICANT.get()) && spin.get().value().fluid().getAmount() == 90,
                "husks should spin into 90 mB of lubricant");
        helper.succeed();
    }
}
