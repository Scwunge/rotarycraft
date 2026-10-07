package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.logistics.DropProcessorBlockEntity;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;

/** The Drop Processor: what it makes of a block, its Fortune and Efficiency from books, and what it keeps. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class DropProcessorGameTests {
    static final String WIDE = LogisticsGameTests.WIDE;
    static final BlockPos AT = LogisticsGameTests.AT;

    /** Facing north it takes power from the south. */
    static DropProcessorBlockEntity processor(GameTestHelper helper, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, AT.south(), torque, omega, net.minecraft.core.Direction.NORTH);
        helper.setBlock(AT, LogisticsRegistry.DROP_PROCESSOR.block().get().defaultBlockState().setValue(net.scwunge.rotarycraft.block.MachineBlock.FACING, net.minecraft.core.Direction.NORTH));
        return helper.getBlockEntity(AT);
    }

    static ItemStack book(GameTestHelper helper, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantment, int level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment), level);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        return book;
    }

    @GameTest(template = WIDE, batch = "drops_basic", timeoutTicks = 200)
    public static void dropProcessorTurnsBlocksIntoTheirDrops(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("dropProcessor");
        DropProcessorBlockEntity drops = processor(helper, 32, 16384);
        drops.items().setStackInSlot(DropProcessorBlockEntity.INPUT, new ItemStack(Items.STONE, 2));
        helper.succeedWhen(() -> {
            helper.assertTrue(drops.items().getStackInSlot(DropProcessorBlockEntity.OUTPUT).is(Items.COBBLESTONE), "output " + drops.items().getStackInSlot(DropProcessorBlockEntity.OUTPUT));
            helper.assertTrue(drops.items().getStackInSlot(DropProcessorBlockEntity.INPUT).getCount() == 1, "input " + drops.items().getStackInSlot(DropProcessorBlockEntity.INPUT));
            restore.run();
        });
    }

    @GameTest(template = WIDE, batch = "drops_ore", timeoutTicks = 200)
    public static void dropProcessorMakesDiamondsFromDiamondOre(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("dropProcessor");
        DropProcessorBlockEntity drops = processor(helper, 32, 16384);
        drops.items().setStackInSlot(DropProcessorBlockEntity.INPUT, new ItemStack(Items.DIAMOND_ORE));
        helper.succeedWhen(() -> {
            helper.assertTrue(drops.items().getStackInSlot(DropProcessorBlockEntity.OUTPUT).is(Items.DIAMOND), "output " + drops.items().getStackInSlot(DropProcessorBlockEntity.OUTPUT));
            restore.run();
        });
    }

    @GameTest(template = WIDE, batch = "drops_weak", timeoutTicks = 100)
    public static void dropProcessorNeedsTorqueAndPowerAndTakesOnlyBlocks(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("dropProcessor");
        DropProcessorBlockEntity drops = processor(helper, 31, 16384);
        helper.assertTrue(drops.automationItems().insertItem(DropProcessorBlockEntity.INPUT, new ItemStack(Items.STICK), true).getCount() == 1, "it took a stick");
        helper.assertTrue(drops.automationItems().insertItem(DropProcessorBlockEntity.INPUT, new ItemStack(Items.DIRT), true).isEmpty(), "it refused a block");
        helper.assertTrue(drops.automationItems().extractItem(DropProcessorBlockEntity.INPUT, 1, true).isEmpty(), "pipes took the input");
        drops.items().setStackInSlot(DropProcessorBlockEntity.INPUT, new ItemStack(Items.STONE));
        helper.runAfterDelay(60, () -> {
            restore.run();
            helper.assertTrue(drops.items().getStackInSlot(DropProcessorBlockEntity.OUTPUT).isEmpty(), "it worked on 31 N*m");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "drops_off", timeoutTicks = 100)
    public static void aSwitchedOffDropProcessorDoesNothing(GameTestHelper helper) {
        Runnable restore = DecorGameTests.disable("dropProcessor");
        DropProcessorBlockEntity drops = processor(helper, 32, 16384);
        drops.items().setStackInSlot(DropProcessorBlockEntity.INPUT, new ItemStack(Items.STONE));
        helper.runAfterDelay(60, () -> {
            restore.run();
            helper.assertTrue(drops.items().getStackInSlot(DropProcessorBlockEntity.OUTPUT).isEmpty(), "it worked while switched off");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "drops_books", timeoutTicks = 100)
    public static void booksGiveFortuneAndEfficiency(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("dropProcessor");
        DropProcessorBlockEntity drops = processor(helper, 32, 4096);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        int slow = drops.operationTime();
        ItemStack efficiency = book(helper, Enchantments.EFFICIENCY, 3);
        ItemStack fortune = book(helper, Enchantments.FORTUNE, 3);
        ItemStack sharpness = book(helper, Enchantments.SHARPNESS, 5);
        helper.assertTrue(drops.onItemUse(efficiency, player, InteractionHand.MAIN_HAND) && efficiency.isEmpty(), "the efficiency book was not taken");
        helper.assertTrue(drops.operationTime() < slow, "it is no faster: " + drops.operationTime() + " against " + slow);
        helper.assertTrue(drops.onItemUse(fortune, player, InteractionHand.MAIN_HAND) && drops.enchantments().level(Enchantments.FORTUNE) == 3, "fortune was not taken");
        helper.assertTrue(drops.onItemUse(sharpness, player, InteractionHand.MAIN_HAND) && drops.enchantments().level(Enchantments.SHARPNESS) == 0, "it took sharpness");
        ServerLevel server = helper.getLevel();
        int with = 0;
        for (int i = 0; i < 40; i++) {
            with += drops.dropsOf(server, new ItemStack(Items.DIAMOND_ORE)).stream().mapToInt(ItemStack::getCount).sum();
        }
        restore.run();
        helper.assertTrue(with > 40, "fortune made no difference: " + with + " diamonds from 40 ores");
        helper.succeed();
    }

    @GameTest(template = WIDE, batch = "drops_overflow", timeoutTicks = 100)
    public static void whatDidNotFitInTheOutputWaitsAndSurvivesSaving(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("dropProcessor");
        DropProcessorBlockEntity drops = processor(helper, 32, 16384);
        CompoundTag tag = drops.saveWithoutMetadata(helper.getLevel().registryAccess());
        ListTag overflow = new ListTag();
        overflow.add(new ItemStack(Items.APPLE, 2).save(helper.getLevel().registryAccess()));
        tag.put("overflow", overflow);
        drops.loadCustomOnly(tag, helper.getLevel().registryAccess());
        helper.assertTrue(drops.overflowCount() == 1, "the overflow was not loaded");
        CompoundTag saved = drops.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(saved.getList("overflow", 10).size() == 1, "the overflow was not saved");
        helper.runAfterDelay(5, () -> {
            restore.run();
            helper.assertTrue(drops.items().getStackInSlot(DropProcessorBlockEntity.OUTPUT).is(Items.APPLE) && drops.overflowCount() == 0, "the waiting stack did not move to the output");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "drops_broken", timeoutTicks = 100)
    public static void brokenDropProcessorDropsWhatWasWaiting(GameTestHelper helper) {
        Runnable restore = DecorGameTests.disable("dropProcessor");
        DropProcessorBlockEntity drops = processor(helper, 1, 1);
        CompoundTag tag = drops.saveWithoutMetadata(helper.getLevel().registryAccess());
        ListTag overflow = new ListTag();
        overflow.add(new ItemStack(Items.APPLE, 2).save(helper.getLevel().registryAccess()));
        tag.put("overflow", overflow);
        drops.loadCustomOnly(tag, helper.getLevel().registryAccess());
        drops.items().setStackInSlot(DropProcessorBlockEntity.OUTPUT, new ItemStack(Items.COAL));
        helper.runAfterDelay(3, () -> helper.getLevel().destroyBlock(helper.absolutePos(AT), false));
        helper.runAfterDelay(8, () -> {
            restore.run();
            helper.assertFalse(helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(2), e -> e.getItem().is(Items.APPLE)).isEmpty(), "the waiting stack was lost");
            helper.assertFalse(helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(2), e -> e.getItem().is(Items.COAL)).isEmpty(), "the output was lost");
            helper.succeed();
        });
    }
}
