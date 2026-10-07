package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.logistics.ChestContents;
import net.scwunge.rotarycraft.logistics.ScaleChestBlockEntity;
import net.scwunge.rotarycraft.logistics.ScaleChestMenu;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;

/** The Scale-able Chest: its size with its power, its pages, its refusal to open unpowered, its contents kept in the item, and what switching its power does. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ScaleChestGameTests {
    static final String WIDE = LogisticsGameTests.WIDE;
    static final BlockPos AT = LogisticsGameTests.AT;

    static ScaleChestBlockEntity chest(GameTestHelper helper, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, AT.below(), torque, omega);
        helper.setBlock(AT, LogisticsRegistry.SCALE_CHEST.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.UP));
        return helper.getBlockEntity(AT);
    }

    static void drive(GameTestHelper helper, int torque, int omega) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("torque", torque);
        tag.putInt("omega", omega);
        helper.getBlockEntity(AT.below()).loadCustomOnly(tag, helper.getLevel().registryAccess());
    }

    @GameTest(template = WIDE, batch = "chest_size", timeoutTicks = 60)
    public static void chestHasASlotForEveryHundredAndTwentyEightWatts(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("scaleChest");
        ScaleChestBlockEntity chest = chest(helper, 1, 5376);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(chest.numberSlots() == 19, "slots " + chest.numberSlots());
            drive(helper, 1, 4000);
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(chest.numberSlots() == 9, "slots unpowered " + chest.numberSlots());
            drive(helper, 1, 4_000_000);
        });
        helper.runAfterDelay(20, () -> {
            restore.run();
            helper.assertTrue(chest.numberSlots() == ScaleChestBlockEntity.MAX_SIZE && chest.numberPages() == 18, "slots " + chest.numberSlots() + ", pages " + chest.numberPages());
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "chest_open", timeoutTicks = 80)
    public static void chestOpensOnlyWhilePowered(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("scaleChest");
        ScaleChestBlockEntity chest = chest(helper, 1, 5376);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(5, () -> {
            ScaleChestMenu menu = (ScaleChestMenu) chest.createMenu(1, player.getInventory(), player);
            helper.assertTrue(menu != null && menu.openSlots() == 19 && chest.users() == 1, "it would not open powered");
            menu.removed(player);
            helper.assertTrue(chest.users() == 0, "it was left open");
            drive(helper, 1, 100);
        });
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(chest.createMenu(2, player.getInventory(), player) == null, "it opened with no power");
        });
        helper.runAfterDelay(35, () -> {
            drive(helper, 1, 5376);
        });
        helper.runAfterDelay(70, () -> {
            restore.run();
            helper.assertTrue(chest.createMenu(3, player.getInventory(), player) != null, "it stayed shut once the power settled");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "chest_off", timeoutTicks = 40)
    public static void aSwitchedOffChestStaysShut(GameTestHelper helper) {
        Runnable restore = DecorGameTests.disable("scaleChest");
        ScaleChestBlockEntity chest = chest(helper, 1, 5376);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(5, () -> {
            boolean opened = chest.createMenu(1, player.getInventory(), player) != null;
            restore.run();
            helper.assertFalse(opened, "it opened while switched off");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "chest_pages", timeoutTicks = 60)
    public static void chestShowsItsSlotsAPageAtATime(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("scaleChest");
        ScaleChestBlockEntity chest = chest(helper, 1, 4_000_000);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        chest.items().setStackInSlot(0, new ItemStack(Items.DIRT));
        chest.items().setStackInSlot(54, new ItemStack(Items.STONE, 3));
        helper.runAfterDelay(5, () -> {
            ScaleChestMenu menu = (ScaleChestMenu) chest.createMenu(1, player.getInventory(), player);
            helper.assertTrue(menu != null && menu.slots.get(0).getItem().is(Items.DIRT), "page one is wrong");
            helper.assertTrue(menu.clickMenuButton(player, 1) && menu.page() == 1, "it would not turn the page");
            helper.assertTrue(menu.slots.get(0).getItem().is(Items.STONE) && menu.slots.get(0).getItem().getCount() == 3, "page two is wrong");
            menu.slots.get(1).set(new ItemStack(Items.GLASS));
            helper.assertTrue(chest.items().getStackInSlot(55).is(Items.GLASS), "a slot of the second page did not write to the chest");
            helper.assertTrue(menu.clickMenuButton(player, 0) && menu.page() == 0, "it would not go back");
            helper.assertTrue(menu.page() == 0, "it went before the first page");
            // a stack shift-clicked in lands on the page shown
            player.getInventory().setItem(0, new ItemStack(Items.GOLD_INGOT, 5));
            menu.quickMoveStack(player, ScaleChestMenu.CHEST_SLOTS + 27);
            helper.assertTrue(chest.items().getStackInSlot(1).is(Items.GOLD_INGOT), "shift-click did not fill the first free slot");
            menu.removed(player);
            restore.run();
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "chest_shut", timeoutTicks = 60)
    public static void slotsBeyondThePowerTakeNothing(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("scaleChest");
        ScaleChestBlockEntity chest = chest(helper, 1, 5376);
        helper.runAfterDelay(5, () -> {
            restore.run();
            helper.assertTrue(chest.items().insertItem(18, new ItemStack(Items.DIRT), true).isEmpty(), "the last open slot took nothing");
            helper.assertFalse(chest.items().insertItem(19, new ItemStack(Items.DIRT), true).isEmpty(), "a shut slot took an item");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "chest_item", timeoutTicks = 80)
    public static void brokenChestKeepsItsContentsInItsItem(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("scaleChest");
        ScaleChestBlockEntity chest = chest(helper, 1, 4_000_000);
        chest.items().setStackInSlot(3, new ItemStack(Items.DIAMOND, 7));
        chest.items().setStackInSlot(700, new ItemStack(Items.APPLE, 2));
        helper.runAfterDelay(5, () -> {
            helper.getLevel().destroyBlock(helper.absolutePos(AT), true);
        });
        helper.runAfterDelay(10, () -> {
            restore.run();
            ItemEntity drop = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(2),
                    e -> e.getItem().is(LogisticsRegistry.SCALE_CHEST.block().get().asItem())).stream().findFirst().orElse(null);
            helper.assertTrue(drop != null, "it dropped no chest");
            ChestContents held = drop.getItem().get(LogisticsRegistry.CHEST_CONTENTS.get());
            helper.assertTrue(held != null && held.entries().size() == 2, "the item held " + held);
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "chest_restore", timeoutTicks = 60)
    public static void chestPlacedFromItsItemHasItsContentsBack(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("scaleChest");
        ItemStack stack = new ItemStack(LogisticsRegistry.SCALE_CHEST.block().get().asItem());
        stack.set(LogisticsRegistry.CHEST_CONTENTS.get(), new ChestContents(java.util.List.of(new ChestContents.Entry(5, new ItemStack(Items.EMERALD, 4)),
                new ChestContents.Entry(800, new ItemStack(Items.BREAD, 9)))));
        helper.setBlock(AT, LogisticsRegistry.SCALE_CHEST.block().get().defaultBlockState());
        ScaleChestBlockEntity chest = helper.getBlockEntity(AT);
        chest.applyComponentsFromItemStack(stack);
        restore.run();
        helper.assertTrue(chest.items().getStackInSlot(5).is(Items.EMERALD) && chest.items().getStackInSlot(800).getCount() == 9, "contents are not back");
        helper.succeed();
    }

    @GameTest(template = WIDE, batch = "chest_flicker", timeoutTicks = 100)
    public static void switchingTheChestsPowerTooOftenSmokesAndThenBlowsItUp(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("scaleChest");
        GameRules.BooleanValue rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean before = rule.get();
        rule.set(false, helper.getLevel().getServer());
        ScaleChestBlockEntity chest = chest(helper, 1, 5376);
        chest.items().setStackInSlot(0, new ItemStack(Items.DIAMOND));
        for (int i = 0; i < 14; i++) {
            int n = i;
            helper.runAfterDelay(5 + i, () -> drive(helper, 1, n % 2 == 0 ? 100 : 5376));
        }
        int[] seen = {0};
        helper.runAfterDelay(12, () -> seen[0] = chest.powerChanges());
        helper.runAfterDelay(50, () -> {
            rule.set(before, helper.getLevel().getServer());
            restore.run();
            helper.assertTrue(seen[0] > 3 && seen[0] <= 8, "changes counted by then: " + seen[0]);
            helper.assertTrue(helper.getBlockState(AT).isAir(), "it did not blow up");
            helper.assertFalse(helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(3),
                    e -> e.getItem().is(LogisticsRegistry.SCALE_CHEST.block().get().asItem())).isEmpty(), "it did not drop itself");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "chest_codec", timeoutTicks = 20)
    public static void chestItemContentsSurviveSavingTheItem(GameTestHelper helper) {
        ItemStack stack = new ItemStack(LogisticsRegistry.SCALE_CHEST.block().get().asItem());
        stack.set(LogisticsRegistry.CHEST_CONTENTS.get(), new ChestContents(java.util.List.of(new ChestContents.Entry(5, new ItemStack(Items.EMERALD, 4)),
                new ChestContents.Entry(800, new ItemStack(Items.BREAD, 9)))));
        var ops = net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, helper.getLevel().registryAccess());
        net.minecraft.nbt.Tag tag = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        ItemStack back = ItemStack.CODEC.parse(ops, tag).getOrThrow();
        ChestContents held = back.get(LogisticsRegistry.CHEST_CONTENTS.get());
        helper.assertTrue(held != null && held.entries().size() == 2 && held.entries().get(0).stack().is(Items.EMERALD) && held.entries().get(1).slot() == 800, "contents " + held);
        helper.succeed();
    }
}
