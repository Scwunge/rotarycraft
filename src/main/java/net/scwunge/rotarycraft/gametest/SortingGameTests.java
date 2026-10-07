package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.logistics.SortingBlockEntity;
import net.scwunge.rotarycraft.machine.LayoutMenu;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;

/** The Sorting Machine: what it sends where, how fast, and its pattern slots. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SortingGameTests {
    static final String WIDE = LogisticsGameTests.WIDE;
    static final BlockPos AT = LogisticsGameTests.AT;

    /** Facing north it takes power from the south, and its rows are north, west and east. */
    static SortingBlockEntity sorter(GameTestHelper helper, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, AT.south(), torque, omega, Direction.NORTH);
        helper.setBlock(AT, LogisticsRegistry.SORTING.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        SortingBlockEntity sorter = helper.getBlockEntity(AT);
        sorter.items().setStackInSlot(0, new ItemStack(Items.DIRT));
        sorter.items().setStackInSlot(9, new ItemStack(Items.STONE));
        sorter.items().setStackInSlot(18, new ItemStack(Items.GLASS));
        for (BlockPos at : new BlockPos[] {AT.north(), AT.west(), AT.east(), AT.below()}) {
            helper.setBlock(at, Blocks.CHEST);
        }
        return sorter;
    }

    static int count(GameTestHelper helper, BlockPos at, Item item) {
        Container chest = helper.getBlockEntity(at);
        int n = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (chest.getItem(i).is(item)) {
                n += chest.getItem(i).getCount();
            }
        }
        return n;
    }

    @GameTest(template = WIDE, batch = "sort_inventory", timeoutTicks = 200)
    public static void sortingMachineSendsItemsOutOfTheirSides(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("sortingMachine");
        SortingBlockEntity sorter = sorter(helper, 8, 256);
        helper.setBlock(AT.above(), Blocks.CHEST);
        ChestBlockEntity source = helper.getBlockEntity(AT.above());
        source.setItem(0, new ItemStack(Items.DIRT, 3));
        source.setItem(1, new ItemStack(Items.STONE, 2));
        source.setItem(2, new ItemStack(Items.GLASS, 4));
        source.setItem(3, new ItemStack(Items.SAND, 5));
        helper.succeedWhen(() -> {
            helper.assertTrue(sorter.rowSide(0) == Direction.NORTH && sorter.rowSide(9) == Direction.WEST && sorter.rowSide(18) == Direction.EAST, "the rows are on the wrong sides");
            helper.assertTrue(count(helper, AT.north(), Items.DIRT) == 3, "dirt north: " + count(helper, AT.north(), Items.DIRT));
            helper.assertTrue(count(helper, AT.west(), Items.STONE) == 2, "stone west: " + count(helper, AT.west(), Items.STONE));
            helper.assertTrue(count(helper, AT.east(), Items.GLASS) == 4, "glass east: " + count(helper, AT.east(), Items.GLASS));
            helper.assertTrue(count(helper, AT.below(), Items.SAND) == 5, "sand below: " + count(helper, AT.below(), Items.SAND));
            helper.assertTrue(source.isEmpty(), "the source was not emptied");
            restore.run();
        });
    }

    @GameTest(template = WIDE, batch = "sort_entities", timeoutTicks = 200)
    public static void sortingMachineSortsItemsLyingOnIt(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("sortingMachine");
        sorter(helper, 8, 256);
        helper.spawnItem(Items.DIRT, new Vec3(2.5, 3.0, 2.5)).setItem(new ItemStack(Items.DIRT, 3));
        helper.succeedWhen(() -> {
            helper.assertTrue(count(helper, AT.north(), Items.DIRT) == 3, "dirt north: " + count(helper, AT.north(), Items.DIRT));
            restore.run();
        });
    }

    @GameTest(template = WIDE, batch = "sort_unpowered", timeoutTicks = 60)
    public static void sortingMachineNeedsAKilowatt(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("sortingMachine");
        sorter(helper, 1, 1000);
        helper.setBlock(AT.above(), Blocks.CHEST);
        ChestBlockEntity source = helper.getBlockEntity(AT.above());
        source.setItem(0, new ItemStack(Items.DIRT, 3));
        helper.runAfterDelay(30, () -> {
            restore.run();
            helper.assertTrue(source.getItem(0).getCount() == 3, "it sorted on 1000 W");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "sort_off", timeoutTicks = 60)
    public static void aSwitchedOffSortingMachineDoesNothing(GameTestHelper helper) {
        Runnable restore = DecorGameTests.disable("sortingMachine");
        sorter(helper, 8, 256);
        helper.setBlock(AT.above(), Blocks.CHEST);
        ChestBlockEntity source = helper.getBlockEntity(AT.above());
        source.setItem(0, new ItemStack(Items.DIRT, 3));
        helper.runAfterDelay(30, () -> {
            restore.run();
            helper.assertTrue(source.getItem(0).getCount() == 3, "it sorted while switched off");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "sort_speed", timeoutTicks = 60)
    public static void sortingSpeedFollowsThePower(GameTestHelper helper) {
        SortingBlockEntity sorter = sorter(helper, 1, 1024);
        int[][] cases = {{1024, 1}, {2048, 2}, {4096, 4}, {8192, 16}, {12288, 32}, {16384, 64}, {1_000_000, 64}};
        for (int i = 0; i < cases.length; i++) {
            int[] c = cases[i];
            helper.runAfterDelay(3 + 3 * i, () -> {
                CompoundTagHelper.drive(helper, AT.south(), 1, c[0]);
            });
            helper.runAfterDelay(5 + 3 * i, () -> helper.assertTrue(sorter.cyclesPerTick() == c[1], c[0] + " W sorts " + sorter.cyclesPerTick() + " but should sort " + c[1]));
        }
        helper.runAfterDelay(30, helper::succeed);
    }

    @GameTest(template = WIDE, batch = "sort_patterns", timeoutTicks = 60)
    public static void patternSlotsTakeACopyAndRefuseRepeats(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("sortingMachine");
        SortingBlockEntity sorter = sorter(helper, 8, 256);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(3, () -> {
            LayoutMenu menu = (LayoutMenu) sorter.createMenu(1, player.getInventory(), player);
            menu.setCarried(new ItemStack(Items.SAND, 32));
            menu.clicked(1, 0, ClickType.PICKUP, player);
            helper.assertTrue(sorter.items().getStackInSlot(1).is(Items.SAND) && sorter.items().getStackInSlot(1).getCount() == 1, "no copy of sand went in");
            helper.assertTrue(menu.getCarried().getCount() == 32, "the held sand was used up");
            menu.clicked(2, 0, ClickType.PICKUP, player);
            helper.assertTrue(sorter.items().getStackInSlot(2).isEmpty(), "sand went in twice");
            menu.setCarried(ItemStack.EMPTY);
            menu.clicked(1, 0, ClickType.PICKUP, player);
            helper.assertTrue(sorter.items().getStackInSlot(1).isEmpty(), "an empty hand did not clear the slot");
            restore.run();
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "sort_break", timeoutTicks = 60)
    public static void brokenSortingMachineDropsNoPatterns(GameTestHelper helper) {
        sorter(helper, 8, 256);
        helper.runAfterDelay(3, () -> helper.getLevel().destroyBlock(helper.absolutePos(AT), true));
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(2),
                    e -> e.getItem().is(Items.DIRT) || e.getItem().is(Items.STONE) || e.getItem().is(Items.GLASS)).isEmpty(), "a pattern was dropped as an item");
            helper.succeed();
        });
    }

    /** Drives the flywheel the tests put under a machine. */
    static final class CompoundTagHelper {
        static void drive(GameTestHelper helper, BlockPos at, int torque, int omega) {
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.putInt("torque", torque);
            tag.putInt("omega", omega);
            helper.getBlockEntity(at).loadCustomOnly(tag, helper.getLevel().registryAccess());
        }
    }
}
