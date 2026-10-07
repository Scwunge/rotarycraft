package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.logistics.ItemFilterBlockEntity;
import net.scwunge.rotarycraft.logistics.MatchData;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;

/** The Item Filter: what it lets in, how each setting changes that, and what survives saving. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ItemFilterGameTests {
    static final String WIDE = LogisticsGameTests.WIDE;
    static final BlockPos AT = LogisticsGameTests.AT;

    static ItemFilterBlockEntity filter(GameTestHelper helper, ItemStack template) {
        WeaponGameTests.spinningFlywheel(helper, AT.below(), 8, 256);
        helper.setBlock(AT, LogisticsRegistry.ITEM_FILTER.block().get().defaultBlockState());
        ItemFilterBlockEntity filter = helper.getBlockEntity(AT);
        filter.items().setStackInSlot(ItemFilterBlockEntity.TEMPLATE, template);
        return filter;
    }

    static boolean takes(ItemFilterBlockEntity filter, ItemStack stack) {
        return filter.automationItems().insertItem(ItemFilterBlockEntity.BUFFER, stack, true).isEmpty();
    }

    static void set(ItemFilterBlockEntity filter, MatchData.Page page, int row, MatchData.MatchType type) {
        filter.matchData().set(page, row, type);
    }

    @GameTest(template = WIDE, batch = "filter_basic", timeoutTicks = 60)
    public static void filterTakesOnlyWhatMatchesItsTemplate(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("itemFilter");
        ItemFilterBlockEntity filter = filter(helper, new ItemStack(Items.IRON_INGOT));
        helper.runAfterDelay(5, () -> {
            restore.run();
            helper.assertTrue(takes(filter, new ItemStack(Items.IRON_INGOT, 5)), "it refused iron");
            helper.assertFalse(takes(filter, new ItemStack(Items.GOLD_INGOT)), "it took gold");
            helper.assertFalse(takes(filter, new ItemStack(Items.IRON_NUGGET)), "it took a nugget");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "filter_ignore", timeoutTicks = 60)
    public static void ignoringIdAndTagsLetsOtherIngotsThrough(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("itemFilter");
        ItemFilterBlockEntity filter = filter(helper, new ItemStack(Items.IRON_INGOT));
        helper.runAfterDelay(5, () -> {
            set(filter, MatchData.Page.BASIC, 0, MatchData.MatchType.IGNORE);
            set(filter, MatchData.Page.BASIC, 4, MatchData.MatchType.IGNORE);
            boolean gold = takes(filter, new ItemStack(Items.GOLD_INGOT));
            boolean nugget = takes(filter, new ItemStack(Items.IRON_NUGGET));
            boolean sword = takes(filter, new ItemStack(Items.IRON_SWORD));
            restore.run();
            helper.assertTrue(gold, "it refused gold with the id ignored");
            helper.assertTrue(nugget, "it refused a nugget: same class, same damage, same mod");
            helper.assertFalse(sword, "it took a sword, which is another class");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "filter_mismatch", timeoutTicks = 60)
    public static void mismatchTakesEverythingButTheItem(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("itemFilter");
        ItemFilterBlockEntity filter = filter(helper, new ItemStack(Items.IRON_INGOT));
        helper.runAfterDelay(5, () -> {
            for (int i = 0; i < 5; i++) {
                set(filter, MatchData.Page.BASIC, i, i == 0 ? MatchData.MatchType.MISMATCH : MatchData.MatchType.IGNORE);
            }
            filter.matchData().setAll(MatchData.Page.CLASS, MatchData.MatchType.IGNORE);
            boolean iron = takes(filter, new ItemStack(Items.IRON_INGOT));
            boolean dirt = takes(filter, new ItemStack(Items.DIRT));
            restore.run();
            helper.assertFalse(iron, "it took the item set to mismatch");
            helper.assertTrue(dirt, "it refused dirt");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "filter_redstone", timeoutTicks = 60)
    public static void aRedstoneSignalTurnsTheFilterRoundAndTheBlacklistStillHolds(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("itemFilter");
        ItemFilterBlockEntity filter = filter(helper, new ItemStack(Items.IRON_INGOT));
        helper.setBlock(AT.north(), Blocks.REDSTONE_BLOCK);
        filter.items().setStackInSlot(ItemFilterBlockEntity.BLACKLIST + 3, new ItemStack(Items.DIRT));
        helper.runAfterDelay(5, () -> {
            boolean iron = takes(filter, new ItemStack(Items.IRON_INGOT));
            boolean gold = takes(filter, new ItemStack(Items.GOLD_INGOT));
            boolean dirt = takes(filter, new ItemStack(Items.DIRT));
            restore.run();
            helper.assertFalse(iron, "it took iron with a signal on");
            helper.assertTrue(gold, "it refused gold with a signal on");
            helper.assertFalse(dirt, "it took a blacklisted item");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "filter_black", timeoutTicks = 60)
    public static void blacklistedItemsAreRefusedEvenWhenTheyMatch(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("itemFilter");
        ItemFilterBlockEntity filter = filter(helper, new ItemStack(Items.IRON_INGOT));
        filter.items().setStackInSlot(ItemFilterBlockEntity.BLACKLIST, new ItemStack(Items.IRON_INGOT));
        helper.runAfterDelay(5, () -> {
            restore.run();
            helper.assertFalse(takes(filter, new ItemStack(Items.IRON_INGOT)), "it took a blacklisted item");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "filter_components", timeoutTicks = 60)
    public static void componentsMustMatchUnlessTheyAreIgnored(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("itemFilter");
        ItemStack named = new ItemStack(Items.STICK);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Wand"));
        ItemFilterBlockEntity filter = filter(helper, named);
        helper.runAfterDelay(5, () -> {
            ItemStack same = new ItemStack(Items.STICK);
            same.set(DataComponents.CUSTOM_NAME, Component.literal("Wand"));
            ItemStack other = new ItemStack(Items.STICK);
            other.set(DataComponents.CUSTOM_NAME, Component.literal("Staff"));
            boolean sameOk = takes(filter, same);
            boolean otherOk = takes(filter, other);
            boolean plainOk = takes(filter, new ItemStack(Items.STICK));
            filter.matchData().set(MatchData.Page.BASIC, 3, MatchData.MatchType.IGNORE);
            boolean plainIgnored = takes(filter, new ItemStack(Items.STICK));
            restore.run();
            helper.assertTrue(sameOk, "it refused the same name");
            helper.assertFalse(otherOk, "it took another name");
            helper.assertFalse(plainOk, "it took an unnamed stick");
            helper.assertTrue(plainIgnored, "it refused an unnamed stick with components ignored");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "filter_power", timeoutTicks = 60)
    public static void filterNeedsAKilowattAndATemplate(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("itemFilter");
        WeaponGameTests.spinningFlywheel(helper, AT.below(), 1, 1000);
        helper.setBlock(AT, LogisticsRegistry.ITEM_FILTER.block().get().defaultBlockState());
        ItemFilterBlockEntity filter = helper.getBlockEntity(AT);
        filter.items().setStackInSlot(ItemFilterBlockEntity.TEMPLATE, new ItemStack(Items.IRON_INGOT));
        helper.runAfterDelay(5, () -> {
            boolean weak = takes(filter, new ItemStack(Items.IRON_INGOT));
            ScaleChestGameTests.drive(helper, 8, 256);
            filter.items().setStackInSlot(ItemFilterBlockEntity.TEMPLATE, ItemStack.EMPTY);
            helper.runAfterDelay(5, () -> {
                boolean none = takes(filter, new ItemStack(Items.IRON_INGOT));
                restore.run();
                helper.assertFalse(weak, "it took an item on 1000 W");
                helper.assertFalse(none, "it took an item with no template");
                helper.succeed();
            });
        });
    }

    @GameTest(template = WIDE, batch = "filter_off", timeoutTicks = 60)
    public static void aSwitchedOffFilterTakesNothing(GameTestHelper helper) {
        Runnable restore = DecorGameTests.disable("itemFilter");
        ItemFilterBlockEntity filter = filter(helper, new ItemStack(Items.IRON_INGOT));
        helper.runAfterDelay(5, () -> {
            boolean took = takes(filter, new ItemStack(Items.IRON_INGOT));
            restore.run();
            helper.assertFalse(took, "it took an item while switched off");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "filter_buttons", timeoutTicks = 60)
    public static void screenButtonsStepSettingsAndTheyAreSavedAndSynced(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("itemFilter");
        ItemFilterBlockEntity filter = filter(helper, new ItemStack(Items.IRON_INGOT));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(5, () -> {
            int idRow = MatchData.Page.BASIC.ordinal() * 10000;
            helper.assertTrue(filter.menuButton(player, idRow) && filter.matchData().rows(MatchData.Page.BASIC).get(0).setting() == MatchData.MatchType.MISMATCH, "one click should be mismatch");
            helper.assertTrue(filter.menuButton(player, idRow) && filter.matchData().rows(MatchData.Page.BASIC).get(0).setting() == MatchData.MatchType.IGNORE, "two clicks should be ignore");
            helper.assertTrue(filter.menuButton(player, 200000 + MatchData.Page.TAGS.ordinal() * 10 + 2), "set-all was refused");
            CompoundTag saved = filter.saveWithoutMetadata(helper.getLevel().registryAccess());
            CompoundTag update = filter.getUpdateTag(helper.getLevel().registryAccess());
            helper.setBlock(AT, Blocks.AIR);
            helper.setBlock(AT, LogisticsRegistry.ITEM_FILTER.block().get().defaultBlockState());
            ItemFilterBlockEntity again = helper.getBlockEntity(AT);
            again.loadCustomOnly(saved, helper.getLevel().registryAccess());
            helper.assertTrue(again.matchData() != null && again.matchData().rows(MatchData.Page.BASIC).get(0).setting() == MatchData.MatchType.IGNORE, "the setting was not saved");
            helper.setBlock(AT, Blocks.AIR);
            helper.setBlock(AT, LogisticsRegistry.ITEM_FILTER.block().get().defaultBlockState());
            ItemFilterBlockEntity client = helper.getBlockEntity(AT);
            client.handleUpdateTag(update, helper.getLevel().registryAccess());
            restore.run();
            helper.assertTrue(client.matchData() != null && client.matchData().rows(MatchData.Page.BASIC).get(0).setting() == MatchData.MatchType.IGNORE, "the setting was not sent to the client");
            helper.assertTrue(client.matchData().rows(MatchData.Page.BASIC).get(0).value().equals("minecraft:iron_ingot"), "the client does not know the item");
            helper.succeed();
        });
    }
}
