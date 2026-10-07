package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.AirGunBlockEntity;
import net.scwunge.rotarycraft.blockentity.ArrowGunBlockEntity;
import net.scwunge.rotarycraft.blockentity.ItemCannonBlockEntity;
import net.scwunge.rotarycraft.machine.LayoutMenu;
import net.scwunge.rotarycraft.machine.LayoutMenus;
import net.scwunge.rotarycraft.registry.DecorRegistry;

/** The item cannon, the arrow gun and the air gun. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CannonGameTests {
    static final String WIDE = "empty20x8x7";
    static final BlockPos A = new BlockPos(2, 2, 2);
    static final BlockPos B = new BlockPos(8, 2, 2);

    /** Waits for {@code done} to hold, then puts the switch back and passes; the switch is put back at the end of the time allowed too. */
    static void waitFor(GameTestHelper helper, Runnable restore, int ticks, java.util.function.BooleanSupplier done) {
        helper.runAfterDelay(ticks - 2, restore);
        helper.onEachTick(() -> {
            if (done.getAsBoolean()) {
                restore.run();
                helper.succeed();
            }
        });
    }

    // ---- Item Cannon ----

    static ItemCannonBlockEntity itemCannon(GameTestHelper helper, BlockPos at, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, at.below(), torque, omega);
        helper.setBlock(at, DecorRegistry.ITEM_CANNON.block().get().defaultBlockState());
        return helper.getBlockEntity(at);
    }

    static void aim(GameTestHelper helper, ItemCannonBlockEntity from, BlockPos to) {
        BlockPos abs = helper.absolutePos(to);
        from.setTarget(0, abs.getX(), abs.getY(), abs.getZ());
    }

    @GameTest(template = WIDE, batch = "cannon_itemsend", timeoutTicks = 100)
    public static void itemCannonSendsItemsToItsTargetOneAtATime(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("itemCannon");
        ItemCannonBlockEntity from = itemCannon(helper, A, 256, 128);
        ItemCannonBlockEntity to = itemCannon(helper, B, 1, 1);
        aim(helper, from, B);
        from.items().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 10));
        helper.runAfterDelay(30, () -> {
            restore.run();
            int got = to.items().getStackInSlot(0).getCount();
            helper.assertTrue(got >= 2 && got <= 4, "one item every eight ticks, but it sent " + got);
            helper.assertTrue(from.items().getStackInSlot(0).getCount() == 10 - got, "what it sent should be gone from it");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "cannon_itemstack", timeoutTicks = 100)
    public static void itemCannonSendsAWholeStackWithEnoughPower(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("itemCannon");
        ItemCannonBlockEntity from = itemCannon(helper, A, 1024, 512);
        ItemCannonBlockEntity to = itemCannon(helper, B, 1, 1);
        aim(helper, from, B);
        from.items().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 40));
        helper.runAfterDelay(15, () -> {
            restore.run();
            helper.assertTrue(to.items().getStackInSlot(0).getCount() == 40 && from.items().getStackInSlot(0).isEmpty(), "the whole stack should go at 512 kW");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "cannon_itemfull", timeoutTicks = 100)
    public static void itemCannonLosesNothingWhenTheTargetIsFull(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("itemCannon");
        ItemCannonBlockEntity from = itemCannon(helper, A, 256, 128);
        ItemCannonBlockEntity to = itemCannon(helper, B, 1, 1);
        aim(helper, from, B);
        for (int i = 0; i < ItemCannonBlockEntity.SLOTS; i++) {
            to.items().setStackInSlot(i, new ItemStack(Items.STICK, 64));
        }
        from.items().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 10));
        helper.runAfterDelay(30, () -> {
            restore.run();
            helper.assertTrue(from.items().getStackInSlot(0).getCount() == 10, "items vanished into a full cannon");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "cannon_itemnotarget", timeoutTicks = 100)
    public static void itemCannonNeedsPowerAndATargetCannon(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("itemCannon");
        ItemCannonBlockEntity from = itemCannon(helper, A, 127, 256);
        from.items().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 10));
        ItemCannonBlockEntity to = itemCannon(helper, B, 1, 1);
        aim(helper, from, B);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(to.items().getStackInSlot(0).isEmpty(), "it sent on 127 N*m");
            WeaponGameTests.spinningFlywheel(helper, A.below(), 256, 128);
            // aimed at a block with no cannon in it
            from.setTarget(0, helper.absolutePos(A.above(2)).getX(), helper.absolutePos(A.above(2)).getY(), helper.absolutePos(A.above(2)).getZ());
        });
        helper.runAfterDelay(55, () -> {
            restore.run();
            helper.assertTrue(from.items().getStackInSlot(0).getCount() == 10, "it sent to nothing");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "cannon_itemscreen", timeoutTicks = 40)
    public static void itemCannonScreenSetsTheTargetFromItsBoxes(GameTestHelper helper) {
        ItemCannonBlockEntity cannon = itemCannon(helper, A, 1, 1);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        LayoutMenu menu = LayoutMenus.create(ItemCannonBlockEntity.NAME, 1, player.getInventory(), cannon);
        helper.assertTrue(cannon.setField(player, 0, -120) && cannon.setField(player, 1, 64) && cannon.setField(player, 2, 33) && cannon.setField(player, 3, -1), "boxes refused");
        helper.assertTrue(cannon.target().equals(new BlockPos(-120, 64, 33)) && cannon.targetDimension() == -1, "target " + cannon.target());
        helper.assertTrue(menu.extra(0) == -120 && menu.extra(3) == -1, "the screen is not told the target");
        helper.assertTrue(menu.layout().fields().size() == 4, "four boxes");
        helper.assertFalse(cannon.setField(player, 9, 1), "a box that is not there took a number");
        helper.succeed();
    }

    @GameTest(template = WIDE, batch = "cannon_itemswitch", timeoutTicks = 100)
    public static void switchedOffItemCannonSendsNothing(GameTestHelper helper) {
        Runnable restore = DecorGameTests.disable("itemCannon");
        ItemCannonBlockEntity from = itemCannon(helper, A, 256, 128);
        ItemCannonBlockEntity to = itemCannon(helper, B, 1, 1);
        aim(helper, from, B);
        from.items().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 10));
        helper.runAfterDelay(30, () -> {
            restore.run();
            helper.assertTrue(to.items().getStackInSlot(0).isEmpty(), "a switched-off cannon sent");
            helper.succeed();
        });
    }

    // ---- Arrow Gun ----

    static ArrowGunBlockEntity arrowGun(GameTestHelper helper, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, A.west(), torque, omega, Direction.EAST);
        helper.setBlock(A, DecorRegistry.ARROW_GUN.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        ArrowGunBlockEntity gun = helper.getBlockEntity(A);
        gun.items().setStackInSlot(0, new ItemStack(Items.ARROW, 4));
        return gun;
    }

    static Pig pigOnStone(GameTestHelper helper, int x) {
        helper.setBlock(new BlockPos(x, 1, 2), Blocks.STONE);
        return helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(x + 0.5, 2, 2.5));
    }

    @GameTest(template = WIDE, batch = "cannon_arrowfire", timeoutTicks = 150)
    public static void arrowGunShootsAnArrowAtWhatIsInFront(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("arrowGun");
        ArrowGunBlockEntity gun = arrowGun(helper, 64, 16);
        Pig pig = pigOnStone(helper, 10);
        helper.runAfterDelay(3, () -> helper.assertTrue(gun.range() == 22 && gun.operationTime() == 12, "range " + gun.range() + ", time " + gun.operationTime()));
        waitFor(helper, restore, 150, () -> gun.items().getStackInSlot(0).getCount() < 4 && (pig.getHealth() < pig.getMaxHealth() || !pig.isAlive()));
    }

    @GameTest(template = WIDE, batch = "cannon_arrowwall", timeoutTicks = 80)
    public static void arrowGunHoldsFireWithAWallBetween(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("arrowGun");
        ArrowGunBlockEntity gun = arrowGun(helper, 64, 16);
        pigOnStone(helper, 10);
        for (int y = 1; y <= 4; y++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(5, y, z), Blocks.STONE);
            }
        }
        helper.runAfterDelay(50, () -> {
            restore.run();
            helper.assertTrue(gun.items().getStackInSlot(0).getCount() == 4, "it shot through a wall");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "cannon_arrowfilter", timeoutTicks = 40)
    public static void arrowGunTakesOnlyArrowsAndGivesNone(GameTestHelper helper) {
        ArrowGunBlockEntity gun = arrowGun(helper, 1, 1);
        helper.assertFalse(gun.automationItems().isItemValid(1, new ItemStack(Items.STICK)), "a stick went in");
        helper.assertTrue(gun.automationItems().isItemValid(1, new ItemStack(Items.ARROW)), "arrows are the ammunition");
        helper.assertTrue(gun.automationItems().extractItem(0, 1, true).isEmpty(), "pipes may not take arrows out");
        helper.assertTrue(gun.comparatorSignal() > 0, "a comparator should read the arrows");
        helper.succeed();
    }

    @GameTest(template = WIDE, batch = "cannon_arrowoff", timeoutTicks = 80)
    public static void switchedOffArrowGunHoldsFire(GameTestHelper helper) {
        Runnable restore = DecorGameTests.disable("arrowGun");
        ArrowGunBlockEntity gun = arrowGun(helper, 64, 16);
        pigOnStone(helper, 10);
        helper.runAfterDelay(50, () -> {
            restore.run();
            helper.assertTrue(gun.items().getStackInSlot(0).getCount() == 4, "a switched-off gun fired");
            helper.succeed();
        });
    }

    // ---- Air Gun ----

    static AirGunBlockEntity airGun(GameTestHelper helper, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, A.west(), torque, omega, Direction.EAST);
        helper.setBlock(A, DecorRegistry.AIR_GUN.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        return helper.getBlockEntity(A);
    }

    @GameTest(template = WIDE, batch = "cannon_airblast", timeoutTicks = 100)
    public static void airGunThrowsWhatStandsInFrontOfIt(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("airGun");
        AirGunBlockEntity gun = airGun(helper, 512, 32);
        helper.runAfterDelay(3, () -> helper.assertTrue(gun.range() == 28 && gun.operationTime() == 11, "range " + gun.range() + ", time " + gun.operationTime()));
        helper.setBlock(new BlockPos(6, 1, 2), Blocks.STONE);
        // an ordinary pig, not a no-AI one, which ignores being thrown
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(6, 2, 2));
        double start = pig.getX();
        waitFor(helper, restore, 100, () -> pig.getX() > start + 4);
    }

    @GameTest(template = WIDE, batch = "cannon_airweak", timeoutTicks = 80)
    public static void airGunNeedsFiveHundredAndTwelveNewtonMetres(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("airGun");
        airGun(helper, 511, 64);
        helper.setBlock(new BlockPos(6, 1, 2), Blocks.STONE);
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(6, 2, 2));
        double start = pig.getX();
        helper.runAfterDelay(40, () -> {
            restore.run();
            helper.assertTrue(Math.abs(pig.getX() - start) < 2.5, "it threw the pig on 511 N*m");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "cannon_airoff", timeoutTicks = 80)
    public static void switchedOffAirGunDoesNothing(GameTestHelper helper) {
        Runnable restore = DecorGameTests.disable("airGun");
        airGun(helper, 512, 32);
        helper.setBlock(new BlockPos(6, 1, 2), Blocks.STONE);
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(6, 2, 2));
        double start = pig.getX();
        helper.runAfterDelay(40, () -> {
            restore.run();
            helper.assertTrue(Math.abs(pig.getX() - start) < 2.5, "a switched-off air gun threw the pig");
            helper.succeed();
        });
    }
}
