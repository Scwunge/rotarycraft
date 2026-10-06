package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.CentrifugeBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

/**
 * Centrifuge checks. The powered ones run in the Extractor's boosted batch (8192 rad/s), since the Centrifuge needs
 * 4096 rad/s; at that speed one spin takes 1200 - 60 x 13 = 420 ticks.
 */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CentrifugeGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final BlockPos MOTOR = new BlockPos(2, 1, 2);
    static final BlockPos CENTRIFUGE = new BlockPos(2, 2, 2);

    /** A motor underneath driving a centrifuge, which takes its power from below. */
    static CentrifugeBlockEntity centrifuge(GameTestHelper helper, ItemStack input) {
        helper.setBlock(MOTOR, RotaryBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.UP));
        IEnergyStorage e = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(MOTOR), Direction.NORTH);
        e.receiveEnergy(100_000, false);
        helper.setBlock(CENTRIFUGE, RotaryBlocks.CENTRIFUGE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        CentrifugeBlockEntity c = (CentrifugeBlockEntity) helper.getBlockEntity(CENTRIFUGE);
        c.items().setStackInSlot(CentrifugeBlockEntity.SLOT_INPUT, input);
        return c;
    }

    static int count(GameTestHelper helper, Item item) {
        CentrifugeBlockEntity c = (CentrifugeBlockEntity) helper.getBlockEntity(CENTRIFUGE);
        int n = 0;
        for (int s = 1; s < CentrifugeBlockEntity.SLOTS; s++) {
            if (c.items().getStackInSlot(s).is(item)) {
                n += c.items().getStackInSlot(s).getCount();
            }
        }
        return n;
    }

    @GameTest(template = TEMPLATE, batch = ExtractorGameTests.BATCH, timeoutTicks = 600)
    public static void magmaCreamSplitsIntoSlimeAndBlaze(GameTestHelper helper) {
        centrifuge(helper, new ItemStack(Items.MAGMA_CREAM, 4));
        helper.succeedWhen(() -> {
            helper.assertTrue(count(helper, Items.SLIME_BALL) >= 1 && count(helper, Items.BLAZE_POWDER) >= 1, "no slime and blaze powder yet");
            helper.assertTrue(((CentrifugeBlockEntity) helper.getBlockEntity(CENTRIFUGE)).items().getStackInSlot(0).getCount() == 3, "should use one cream per spin");
        });
    }

    @GameTest(template = TEMPLATE, batch = ExtractorGameTests.BATCH, timeoutTicks = 600)
    public static void sludgeSpinsTwoAtATime(GameTestHelper helper) {
        centrifuge(helper, new ItemStack(RotaryItems.SLUDGE.get(), 8));
        helper.succeedWhen(() -> {
            CentrifugeBlockEntity c = (CentrifugeBlockEntity) helper.getBlockEntity(CENTRIFUGE);
            helper.assertTrue(c.items().getStackInSlot(0).getCount() == 6, "one spin should take two sludge, " + c.items().getStackInSlot(0).getCount() + " left");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void tooSlowDoesNothing(GameTestHelper helper) {
        // the default motor gives 256 rad/s, far under the 4096 the centrifuge needs
        centrifuge(helper, new ItemStack(Items.MAGMA_CREAM, 4));
        helper.runAfterDelay(200, () -> {
            CentrifugeBlockEntity c = (CentrifugeBlockEntity) helper.getBlockEntity(CENTRIFUGE);
            helper.assertTrue(c.getOmega() > 0, "motor isn't turning the centrifuge");
            helper.assertTrue(!c.hasEnoughPower() && c.progress() == 0, "ran without enough speed");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void recipesLoadWithOriginalChances(GameTestHelper helper) {
        var level = helper.getLevel();
        var dirt = level.getRecipeManager().getRecipeFor(RotaryRecipes.CENTRIFUGE.get(), new SingleRecipeInput(new ItemStack(Items.DIRT)), level);
        helper.assertTrue(dirt.isPresent() && dirt.get().value().outputs().size() == 7, "dirt should have 7 outputs");
        var clay = level.getRecipeManager().getRecipeFor(RotaryRecipes.CENTRIFUGE.get(), new SingleRecipeInput(new ItemStack(Items.CLAY)), level);
        helper.assertTrue(clay.isPresent() && clay.get().value().fluidChance() == 40F && clay.get().value().fluid().getAmount() == 20, "clay gives 20 mB water 40% of the time");
        var nether = level.getRecipeManager().getRecipeFor(RotaryRecipes.CENTRIFUGE.get(), new SingleRecipeInput(new ItemStack(RotaryItems.NETHERRACK_DUST.get())), level);
        helper.assertTrue(nether.isPresent(), "netherrack dust has a recipe with or without Mekanism");
        helper.succeed();
    }
}
