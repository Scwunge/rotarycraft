package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.logistics.AggregatorBlockEntity;
import net.scwunge.rotarycraft.logistics.BucketFillerBlockEntity;
import net.scwunge.rotarycraft.logistics.FillingStationBlockEntity;
import net.scwunge.rotarycraft.logistics.GrindstoneBlockEntity;
import net.scwunge.rotarycraft.logistics.ItemRefresherBlockEntity;
import net.scwunge.rotarycraft.logistics.PurifierBlockEntity;
import net.scwunge.rotarycraft.logistics.WetterBlockEntity;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryItems;

import java.util.function.Predicate;

/** The wetter, aggregator, grindstone, purifier, bucket filler, filling station and item refresher. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class FluidLogisticsGameTests {
    static final String WIDE = LogisticsGameTests.WIDE;
    static final BlockPos AT = LogisticsGameTests.AT;

    static <T extends net.minecraft.world.level.block.entity.BlockEntity> T machine(GameTestHelper helper, net.minecraft.world.level.block.Block block, int torque, int omega) {
        return machine(helper, block.defaultBlockState(), torque, omega);
    }

    static <T extends net.minecraft.world.level.block.entity.BlockEntity> T machine(GameTestHelper helper, BlockState state, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, AT.below(), torque, omega);
        helper.setBlock(AT, state);
        return helper.getBlockEntity(AT);
    }

    // ---- Wetter ----

    @GameTest(template = WIDE, batch = "fluid_wetter", timeoutTicks = 200)
    public static void wetterSoaksSandInLubricantIntoSoulSand(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("wetter");
        WetterBlockEntity wetter = machine(helper, LogisticsRegistry.WETTER.block().get(), 8, 2048);
        wetter.items().setStackInSlot(0, new ItemStack(Items.SAND));
        helper.assertTrue(wetter.fluidHandler(Direction.NORTH).fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 500), IFluidHandler.FluidAction.EXECUTE) == 500,
                "it would not take lubricant");
        helper.succeedWhen(() -> {
            helper.assertTrue(wetter.items().getStackInSlot(0).is(Items.SOUL_SAND), "item is " + wetter.items().getStackInSlot(0) + ", soaked " + wetter.soaked());
            helper.assertTrue(wetter.tank().isEmpty(), "the lubricant was not used up");
            restore.run();
        });
    }

    @GameTest(template = WIDE, batch = "fluid_wetterwrong", timeoutTicks = 60)
    public static void wetterTakesOnlyFluidsItCanUseAndOneItem(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("wetter");
        WetterBlockEntity wetter = machine(helper, LogisticsRegistry.WETTER.block().get(), 8, 2048);
        helper.assertTrue(wetter.fluidHandler(Direction.EAST).fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE) == 0, "it took water");
        helper.assertTrue(wetter.fluidHandler(Direction.UP) == null, "it takes fluid from above");
        ItemStack left = wetter.items().insertItem(0, new ItemStack(Items.SAND, 5), false);
        helper.assertTrue(wetter.items().getStackInSlot(0).getCount() == 1 && left.getCount() == 4, "it took " + wetter.items().getStackInSlot(0).getCount());
        helper.assertTrue(wetter.items().insertItem(0, new ItemStack(Items.DIRT), true).getCount() == 1, "it took dirt");
        restore.run();
        helper.succeed();
    }

    @GameTest(template = WIDE, batch = "fluid_wetteroff", timeoutTicks = 100)
    public static void aSwitchedOffWetterDoesNothing(GameTestHelper helper) {
        Runnable restore = DecorGameTests.disable("wetter");
        WetterBlockEntity wetter = machine(helper, LogisticsRegistry.WETTER.block().get(), 8, 2048);
        wetter.items().setStackInSlot(0, new ItemStack(Items.SAND));
        wetter.tank().fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 500), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(60, () -> {
            restore.run();
            helper.assertTrue(wetter.items().getStackInSlot(0).is(Items.SAND), "it worked while switched off");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "fluid_wetterslow", timeoutTicks = 100)
    public static void wetterIsIdleBelowItsMinimumSpeed(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("wetter");
        WetterBlockEntity wetter = machine(helper, LogisticsRegistry.WETTER.block().get(), 8, 512);
        wetter.items().setStackInSlot(0, new ItemStack(Items.SAND));
        wetter.tank().fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 500), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(60, () -> {
            restore.run();
            helper.assertTrue(wetter.items().getStackInSlot(0).is(Items.SAND) && wetter.soaked() == 0, "it worked too slowly turned");
            helper.succeed();
        });
    }

    // ---- Aggregator ----

    @GameTest(template = WIDE, batch = "fluid_aggregator", timeoutTicks = 200)
    public static void aggregatorCondensesWaterWhileItIsColderThanTheAir(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("aggregator");
        AggregatorBlockEntity aggregator = machine(helper, LogisticsRegistry.AGGREGATOR.block().get(), 100, 4096);
        aggregator.setCurrentTemperature(1);
        helper.succeedWhen(() -> {
            helper.assertTrue(aggregator.tank().getFluidAmount() > 0 && aggregator.tank().getFluid().getFluid() == Fluids.WATER.getSource(),
                    "it made " + aggregator.tank().getFluidAmount());
            restore.run();
        });
    }

    @GameTest(template = WIDE, batch = "fluid_aggregatorhot", timeoutTicks = 60)
    public static void aggregatorMakesNothingWhenWarmerThanTheAir(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("aggregator");
        AggregatorBlockEntity aggregator = machine(helper, LogisticsRegistry.AGGREGATOR.block().get(), 100, 4096);
        aggregator.setCurrentTemperature(99);
        helper.runAfterDelay(15, () -> {
            restore.run();
            helper.assertTrue(aggregator.tank().isEmpty(), "it made " + aggregator.tank().getFluidAmount() + " while hot");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "fluid_aggregatorslow", timeoutTicks = 60)
    public static void aggregatorHasMinimumSpeedAndGivesWaterToItsNeighbours(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("aggregator");
        AggregatorBlockEntity aggregator = machine(helper, LogisticsRegistry.AGGREGATOR.block().get(), 100, 1024);
        helper.assertTrue(aggregator.perTick(1F) == 0, "it made water too slowly turned");
        aggregator.tank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.setBlock(AT.east(), LogisticsRegistry.WETTER.block().get().defaultBlockState());
        WetterBlockEntity next = helper.getBlockEntity(AT.east());
        helper.runAfterDelay(5, () -> {
            restore.run();
            helper.assertTrue(aggregator.tank().getFluidAmount() == 1000, "it gave away water that the wetter cannot use");
            helper.assertTrue(next.tank().isEmpty(), "the wetter took water");
            helper.succeed();
        });
    }

    // ---- Grindstone ----

    static ItemStack worn(int damage) {
        ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
        stack.setDamageValue(damage);
        return stack;
    }

    static GrindstoneBlockEntity grindstone(GameTestHelper helper) {
        return machine(helper, LogisticsRegistry.GRINDSTONE.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.DOWN), 256, 4096);
    }

    @GameTest(template = WIDE, batch = "fluid_grindstone", timeoutTicks = 400)
    public static void grindstoneRepairsToolsUsingWater(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("grindstone");
        GrindstoneBlockEntity stone = grindstone(helper);
        stone.items().setStackInSlot(0, worn(5));
        stone.tank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.succeedWhen(() -> {
            helper.assertTrue(stone.items().getStackInSlot(0).getDamageValue() == 0, "damage " + stone.items().getStackInSlot(0).getDamageValue());
            helper.assertTrue(stone.tank().getFluidAmount() == 500, "it used " + (1000 - stone.tank().getFluidAmount()) + " mB of water");
            restore.run();
        });
    }

    @GameTest(template = WIDE, batch = "fluid_grindstonedry", timeoutTicks = 100)
    public static void grindstoneNeedsWaterAndOnlyTakesTools(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("grindstone");
        GrindstoneBlockEntity stone = grindstone(helper);
        helper.assertTrue(stone.items().insertItem(0, new ItemStack(Items.DIRT), true).getCount() == 1, "it took dirt");
        helper.assertTrue(stone.items().insertItem(0, new ItemStack(Items.IRON_SWORD), true).isEmpty(), "it refused a sword");
        stone.items().setStackInSlot(0, worn(5));
        helper.runAfterDelay(60, () -> {
            restore.run();
            helper.assertTrue(stone.items().getStackInSlot(0).getDamageValue() == 5, "it repaired with no water");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "fluid_grindstonespent", timeoutTicks = 100)
    public static void grindstoneCannotRepairAToolItHasWornOut(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("grindstone");
        GrindstoneBlockEntity stone = grindstone(helper);
        ItemStack tool = worn(5);
        tool.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(spent()));
        stone.items().setStackInSlot(0, tool);
        stone.tank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(60, () -> {
            restore.run();
            helper.assertTrue(stone.items().getStackInSlot(0).getDamageValue() == 5 && stone.tank().getFluidAmount() == 1000, "it ground a worn-out tool");
            helper.succeed();
        });
    }

    static net.minecraft.nbt.CompoundTag spent() {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt(GrindstoneBlockEntity.REPAIRS, 0);
        return tag;
    }

    // ---- Purifier ----

    static PurifierBlockEntity purifier(GameTestHelper helper) {
        return machine(helper, LogisticsRegistry.PURIFIER.block().get(), 64, 131072);
    }

    @GameTest(template = WIDE, batch = "fluid_purifier", timeoutTicks = 400)
    public static void purifierMakesSteelFromAnotherModsSteel(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("purifier");
        Predicate<ItemStack> before = PurifierBlockEntity.foreignSteel;
        PurifierBlockEntity.foreignSteel = stack -> stack.is(Items.COPPER_INGOT);
        PurifierBlockEntity purifier = purifier(helper);
        purifier.setCurrentTemperature(800);
        purifier.items().setStackInSlot(PurifierBlockEntity.GUNPOWDER, new ItemStack(Items.GUNPOWDER, 4));
        purifier.items().setStackInSlot(PurifierBlockEntity.SAND, new ItemStack(Items.SAND, 4));
        purifier.items().setStackInSlot(1, new ItemStack(Items.COPPER_INGOT, 3));
        purifier.items().setStackInSlot(2, new ItemStack(Items.COPPER_INGOT, 1));
        helper.succeedWhen(() -> {
            // the heat comes from the air round it, so keep it up
            purifier.setCurrentTemperature(800);
            ItemStack out = purifier.items().getStackInSlot(PurifierBlockEntity.OUTPUT);
            helper.assertTrue(out.is(RotaryItems.HSLA_STEEL_INGOT.get()) && out.getCount() == 2, "output " + out + ", " + purifier.getTemperature() + " C, power " + purifier.getTorque() + "x" + purifier.getOmega() + ", smelt " + purifier.canSmelt());
            PurifierBlockEntity.foreignSteel = before;
            restore.run();
        });
    }

    @GameTest(template = WIDE, batch = "fluid_purifiercold", timeoutTicks = 100)
    public static void purifierNeedsHeatAndRefusesItsOwnSteel(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("purifier");
        PurifierBlockEntity purifier = purifier(helper);
        helper.assertTrue(purifier.items().insertItem(1, new ItemStack(RotaryItems.HSLA_STEEL_INGOT.get()), true).getCount() == 1, "it took its own steel");
        purifier.items().setStackInSlot(PurifierBlockEntity.GUNPOWDER, new ItemStack(Items.GUNPOWDER));
        purifier.items().setStackInSlot(PurifierBlockEntity.SAND, new ItemStack(Items.SAND));
        helper.runAfterDelay(30, () -> {
            restore.run();
            helper.assertFalse(purifier.canSmelt(), "it would smelt cold");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "fluid_purifierburn", timeoutTicks = 100)
    public static void purifierBurnsUpWhenTooHot(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("purifier");
        PurifierBlockEntity purifier = purifier(helper);
        helper.setBlock(AT.north(), Blocks.LAVA);
        purifier.setCurrentTemperature(PurifierBlockEntity.MAX_TEMPERATURE + 10);
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(AT).isAir(), "it survived " + purifier.getTemperature() + " C");
            restore.run();
        });
    }

    // ---- Bucket Filler ----

    static BucketFillerBlockEntity bucketFiller(GameTestHelper helper) {
        return machine(helper, LogisticsRegistry.BUCKET_FILLER.block().get(), 4, 1024);
    }

    @GameTest(template = WIDE, batch = "fluid_bucketfill", timeoutTicks = 400)
    public static void bucketFillerFillsEmptyBuckets(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("bucketFiller");
        BucketFillerBlockEntity filler = bucketFiller(helper);
        filler.items().setStackInSlot(0, new ItemStack(Items.BUCKET, 2));
        filler.tank().fill(new FluidStack(Fluids.WATER, 3000), IFluidHandler.FluidAction.EXECUTE);
        helper.succeedWhen(() -> {
            int water = 0;
            for (int i = 0; i < BucketFillerBlockEntity.SLOTS; i++) {
                if (filler.items().getStackInSlot(i).is(Items.WATER_BUCKET)) {
                    water += filler.items().getStackInSlot(i).getCount();
                }
            }
            helper.assertTrue(water == 2 && filler.tank().getFluidAmount() == 1000, water + " buckets, " + filler.tank().getFluidAmount() + " mB left");
            restore.run();
        });
    }

    @GameTest(template = WIDE, batch = "fluid_bucketempty", timeoutTicks = 400)
    public static void bucketFillerEmptiesFullBucketsWhenSwitched(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("bucketFiller");
        BucketFillerBlockEntity filler = bucketFiller(helper);
        filler.menuButton(null, 0);
        helper.assertFalse(filler.filling(), "the button did not switch it");
        filler.items().setStackInSlot(0, new ItemStack(Items.WATER_BUCKET));
        helper.succeedWhen(() -> {
            helper.assertTrue(filler.tank().getFluidAmount() == 1000, "tank " + filler.tank().getFluidAmount());
            boolean bucket = false;
            for (int i = 0; i < BucketFillerBlockEntity.SLOTS; i++) {
                bucket |= filler.items().getStackInSlot(i).is(Items.BUCKET);
            }
            helper.assertTrue(bucket, "no empty bucket came back");
            restore.run();
        });
    }

    // ---- Filling Station ----

    @GameTest(template = WIDE, batch = "fluid_filling", timeoutTicks = 300)
    public static void fillingStationFillsTheItemInItsInput(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("fillingStation");
        FillingStationBlockEntity station = machine(helper, LogisticsRegistry.FILLING_STATION.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.UP), 1, 1024);
        station.tank().fill(new FluidStack(Fluids.LAVA, 5000), IFluidHandler.FluidAction.EXECUTE);
        station.items().setStackInSlot(FillingStationBlockEntity.INPUT, new ItemStack(Items.BUCKET));
        helper.succeedWhen(() -> {
            helper.assertTrue(station.items().getStackInSlot(FillingStationBlockEntity.OUTPUT).is(Items.LAVA_BUCKET), "output " + station.items().getStackInSlot(FillingStationBlockEntity.OUTPUT));
            helper.assertTrue(station.tank().getFluidAmount() == 4000, "tank " + station.tank().getFluidAmount());
            restore.run();
        });
    }

    @GameTest(template = WIDE, batch = "fluid_fillingfuel", timeoutTicks = 100)
    public static void fillingStationEmptiesAContainerIntoItsTank(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("fillingStation");
        FillingStationBlockEntity station = machine(helper, LogisticsRegistry.FILLING_STATION.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.UP), 1, 1024);
        station.items().setStackInSlot(FillingStationBlockEntity.FUEL, new ItemStack(Items.WATER_BUCKET));
        helper.succeedWhen(() -> {
            helper.assertTrue(station.tank().getFluidAmount() == 1000, "tank " + station.tank().getFluidAmount());
            helper.assertTrue(station.items().getStackInSlot(FillingStationBlockEntity.FUEL).is(Items.BUCKET), "the bucket was not left");
            restore.run();
        });
    }

    // ---- Item Refresher ----

    @GameTest(template = WIDE, batch = "fluid_refresher", timeoutTicks = 100)
    public static void itemRefresherKeepsItemsFromDespawning(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("itemRefresher");
        ItemRefresherBlockEntity refresher = machine(helper, LogisticsRegistry.ITEM_REFRESHER.block().get(), 16, 1024);
        helper.assertTrue(refresher.range() == 4, "range " + refresher.range());
        ItemEntity item = helper.spawnItem(Items.DIRT, new Vec3(4.5, 2, 2.5));
        item.lifespan = 30;
        helper.runAfterDelay(40, () -> {
            restore.run();
            helper.assertTrue(item.isAlive(), "the item despawned");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "fluid_refresherstale", timeoutTicks = 100)
    public static void itemsOutOfReachOfARefresherDespawnAsUsual(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("itemRefresher");
        machine(helper, LogisticsRegistry.ITEM_REFRESHER.block().get(), 16, 1024);
        ItemEntity item = helper.spawnItem(Items.DIRT, new Vec3(16.5, 2, 2.5));
        item.lifespan = 30;
        helper.runAfterDelay(40, () -> {
            restore.run();
            helper.assertFalse(item.isAlive(), "it kept an item twelve blocks away");
            helper.succeed();
        });
    }
}
