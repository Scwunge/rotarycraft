package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.ComposterBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ComposterGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final BlockPos AT = new BlockPos(2, 1, 2);

    static ComposterBlockEntity composter(GameTestHelper helper, int temperature, ItemStack matter, int yeast) {
        helper.setBlock(AT, RotaryBlocks.COMPOSTER.get().defaultBlockState());
        ComposterBlockEntity c = at(helper);
        c.setTemperature(temperature);
        c.items().setStackInSlot(ComposterBlockEntity.SLOT_INPUT, matter);
        if (yeast > 0) {
            c.items().setStackInSlot(ComposterBlockEntity.SLOT_YEAST, new ItemStack(RotaryItems.YEAST.get(), yeast));
        }
        return c;
    }

    static ComposterBlockEntity at(GameTestHelper helper) {
        return (ComposterBlockEntity) helper.getBlockEntity(AT);
    }

    /** The temperature drifts a degree a second towards its surroundings, so the tests hold it with a fire beside it. */
    static void warm(GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 1, 2), net.minecraft.world.level.block.Blocks.MAGMA_BLOCK);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void warmMatterWithYeastBecomesCompost(GameTestHelper helper) {
        warm(helper);
        composter(helper, 60, new ItemStack(Items.ROTTEN_FLESH, 3), 4);
        helper.succeedWhen(() -> {
            ItemStack out = at(helper).items().getStackInSlot(ComposterBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(out.is(RotaryItems.COMPOST.get()) && out.getCount() >= 3, "rotten flesh is worth 3 compost, got " + out);
            helper.assertTrue(at(helper).items().getStackInSlot(ComposterBlockEntity.SLOT_INPUT).getCount() == 2, "one flesh used");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void coldItDoesNothing(GameTestHelper helper) {
        composter(helper, 10, new ItemStack(Items.ROTTEN_FLESH, 3), 4);
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(at(helper).items().getStackInSlot(ComposterBlockEntity.SLOT_OUTPUT).isEmpty(), "composted while cold");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void noYeastNoCompost(GameTestHelper helper) {
        warm(helper);
        composter(helper, 60, new ItemStack(Items.ROTTEN_FLESH, 3), 0);
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(at(helper).items().getStackInSlot(ComposterBlockEntity.SLOT_OUTPUT).isEmpty(), "composted without yeast");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void valuesFollowTheOriginal(GameTestHelper helper) {
        ComposterBlockEntity c = composter(helper, 25, ItemStack.EMPTY, 0);
        helper.assertTrue(c.compostValue(new ItemStack(Items.WHEAT)) == 1, "wheat");
        helper.assertTrue(c.compostValue(new ItemStack(Items.BEEF)) == 4, "beef");
        helper.assertTrue(c.compostValue(new ItemStack(Items.COD)) == 3, "fish");
        helper.assertTrue(c.compostValue(new ItemStack(Items.OAK_LEAVES)) == 2, "leaves");
        helper.assertTrue(c.compostValue(new ItemStack(Items.DIAMOND)) == 0, "diamond doesn't rot");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void onlyMatterAndYeastGoIn(GameTestHelper helper) {
        ComposterBlockEntity c = composter(helper, 25, ItemStack.EMPTY, 0);
        helper.assertTrue(c.automationItems().insertItem(ComposterBlockEntity.SLOT_INPUT, new ItemStack(Items.DIAMOND), false).getCount() == 1, "diamond refused");
        helper.assertTrue(c.automationItems().insertItem(ComposterBlockEntity.SLOT_INPUT, new ItemStack(Items.WHEAT, 4), false).isEmpty(), "wheat accepted");
        helper.assertTrue(c.automationItems().insertItem(ComposterBlockEntity.SLOT_YEAST, new ItemStack(Items.WHEAT), false).getCount() == 1, "only yeast in the yeast slot");
        helper.assertTrue(c.automationItems().insertItem(ComposterBlockEntity.SLOT_OUTPUT, new ItemStack(RotaryItems.COMPOST.get()), false).getCount() == 1, "no inserting output");
        helper.succeed();
    }
}
