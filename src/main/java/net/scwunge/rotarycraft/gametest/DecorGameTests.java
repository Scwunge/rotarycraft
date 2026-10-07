package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.ObsidianMakerBlockEntity;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.registry.DecorRegistry;

/** The world and decoration machines. Each machine is driven by a flywheel set spinning underneath it. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class DecorGameTests {
    static final String TEMPLATE = RotaryGameTests.TEMPLATE;
    static final BlockPos MACHINE = new BlockPos(2, 2, 2);

    /** Turns a machine's config switch on for a test; the returned runnable puts it back. */
    static Runnable enable(String name) {
        ModConfigSpec.BooleanValue value = MachineConfig.SWITCHES.get(name);
        MachineConfig.override(value, true);
        return () -> MachineConfig.clearOverride(value);
    }

    static Runnable disable(String name) {
        ModConfigSpec.BooleanValue value = MachineConfig.SWITCHES.get(name);
        MachineConfig.override(value, false);
        return () -> MachineConfig.clearOverride(value);
    }

    static ObsidianMakerBlockEntity obsidianMaker(GameTestHelper helper, int temperature) {
        WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), 1, 65536);
        helper.setBlock(MACHINE, DecorRegistry.OBSIDIAN_MAKER.block().get().defaultBlockState());
        ObsidianMakerBlockEntity maker = helper.getBlockEntity(MACHINE);
        maker.waterTank().fill(new FluidStack(Fluids.WATER, 10_000), IFluidHandler.FluidAction.EXECUTE);
        maker.lavaTank().fill(new FluidStack(Fluids.LAVA, 2_000), IFluidHandler.FluidAction.EXECUTE);
        maker.addTemperature(temperature - maker.temperature());
        return maker;
    }

    @GameTest(template = TEMPLATE, batch = "decor_obsidianmakermakesobsidian", timeoutTicks = 100)
    public static void obsidianMakerMakesObsidianInItsWindow(GameTestHelper helper) {
        ObsidianMakerBlockEntity maker = obsidianMaker(helper, 650);
        helper.succeedWhen(() -> {
            int obsidian = 0;
            for (int i = 0; i < maker.items().getSlots(); i++) {
                if (maker.items().getStackInSlot(i).is(Items.OBSIDIAN)) {
                    obsidian += maker.items().getStackInSlot(i).getCount();
                }
            }
            helper.assertTrue(obsidian >= 1, "no obsidian yet at " + maker.temperature() + " C");
            helper.assertTrue(maker.lavaTank().getFluidAmount() < 2_000 && maker.waterTank().getFluidAmount() < 10_000, "it should have used lava and water");
        });
    }

    @GameTest(template = TEMPLATE, batch = "decor_obsidianmakermakescobble", timeoutTicks = 100)
    public static void obsidianMakerMakesCobblestoneWhenCold(GameTestHelper helper) {
        ObsidianMakerBlockEntity maker = obsidianMaker(helper, 100);
        helper.succeedWhen(() -> {
            helper.assertTrue(maker.items().getStackInSlot(0).is(Items.COBBLESTONE) || maker.items().getStackInSlot(1).is(Items.COBBLESTONE),
                    "no cobblestone yet at " + maker.temperature() + " C");
            helper.assertTrue(maker.lavaTank().getFluidAmount() == 2_000, "cobblestone should use no lava, but " + maker.lavaTank().getFluidAmount() + " is left");
        });
    }

    @GameTest(template = TEMPLATE, batch = "decor_obsidianmakerswitch", timeoutTicks = 60)
    public static void obsidianMakerDoesNothingWhenSwitchedOff(GameTestHelper helper) {
        Runnable restore = disable("obsidianMaker");
        ObsidianMakerBlockEntity maker = obsidianMaker(helper, 650);
        helper.runAfterDelay(40, () -> {
            restore.run();
            helper.assertTrue(maker.items().getStackInSlot(0).isEmpty(), "a switched-off maker made something");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "decor_obsidianmakerfluids", timeoutTicks = 40)
    public static void obsidianMakerTakesFluidFromTheSidesOnly(GameTestHelper helper) {
        ObsidianMakerBlockEntity maker = obsidianMaker(helper, 100);
        maker.waterTank().drain(10_000, IFluidHandler.FluidAction.EXECUTE);
        BlockPos abs = helper.absolutePos(MACHINE);
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.UP) == null, "top shouldn't take fluid");
        IFluidHandler side = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.EAST);
        helper.assertTrue(side != null && side.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "side should take water");
        helper.assertTrue(side.fill(new FluidStack(Fluids.LAVA, 500), IFluidHandler.FluidAction.EXECUTE) == 500, "and lava");
        helper.assertTrue(side.fill(new FluidStack(net.minecraft.world.level.material.Fluids.EMPTY, 1), IFluidHandler.FluidAction.EXECUTE) == 0, "and nothing else");
        helper.assertTrue(side.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "but nothing comes out");
        helper.assertTrue(!maker.items().isItemValid(0, new ItemStack(Items.STONE)), "players cannot put things in");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "decor_obsidianmakeroverheats", timeoutTicks = 60)
    public static void obsidianMakerOverheatsIntoLava(GameTestHelper helper) {
        WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), 1, 65536);
        helper.setBlock(MACHINE, DecorRegistry.OBSIDIAN_MAKER.block().get().defaultBlockState());
        ObsidianMakerBlockEntity maker = helper.getBlockEntity(MACHINE);
        maker.addTemperature(ObsidianMakerBlockEntity.MAX_TEMPERATURE + 100 - maker.temperature());
        helper.succeedWhen(() -> helper.assertBlock(MACHINE, b -> b == Blocks.LAVA, () -> "it should have burst into lava"));
    }

    // ---- Line Builder ----

    /** Stops machines owned by nobody (they act as an anonymous fake player) from changing blocks in {@code box}, as a claim mod would. */
    static Runnable claim(net.minecraft.world.phys.AABB box) {
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> listener = e -> {
            if (box.contains(e.getPos().getX() + 0.5, e.getPos().getY() + 0.5, e.getPos().getZ() + 0.5)) {
                e.setCanceled(true);
            }
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        return () -> net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
    }

    static net.scwunge.rotarycraft.blockentity.LineBuilderBlockEntity lineBuilder(GameTestHelper helper) {
        WeaponGameTests.spinningFlywheel(helper, MACHINE.west(), 1024, 256, Direction.EAST);
        helper.setBlock(MACHINE, DecorRegistry.LINE_BUILDER.block().get().defaultBlockState()
                .setValue(net.scwunge.rotarycraft.block.MachineBlock.FACING, Direction.EAST));
        net.scwunge.rotarycraft.blockentity.LineBuilderBlockEntity builder = helper.getBlockEntity(MACHINE);
        builder.items().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 8));
        return builder;
    }

    @GameTest(template = TEMPLATE, batch = "decor_linebuilderpushes", timeoutTicks = 100)
    public static void lineBuilderPushesTheLineAndAddsABlock(GameTestHelper helper) {
        Runnable restore = enable("lineBuilder");
        net.scwunge.rotarycraft.blockentity.LineBuilderBlockEntity builder = lineBuilder(helper);
        helper.setBlock(MACHINE.east(), Blocks.STONE);
        helper.runAfterDelay(60, () -> {
            restore.run();
            helper.assertBlock(MACHINE.east(2), b -> b == Blocks.STONE, () -> "the stone should have been pushed along");
            helper.assertBlock(MACHINE.east(), b -> b == Blocks.COBBLESTONE, () -> "and a block added behind it");
            helper.assertTrue(builder.items().getStackInSlot(0).getCount() < 8, "from its slots");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "decor_linebuilderoff", timeoutTicks = 100)
    public static void lineBuilderDoesNothingUnlessSwitchedOn(GameTestHelper helper) {
        lineBuilder(helper);
        helper.setBlock(MACHINE.east(), Blocks.STONE);
        helper.runAfterDelay(60, () -> {
            helper.assertBlock(MACHINE.east(), b -> b == Blocks.STONE, () -> "it should be switched off by default");
            helper.assertBlock(MACHINE.east(2), b -> b == Blocks.AIR, () -> "so nothing moves");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "decor_linebuilderblockentity", timeoutTicks = 100)
    public static void lineBuilderWillNotPushABlockEntity(GameTestHelper helper) {
        Runnable restore = enable("lineBuilder");
        lineBuilder(helper);
        helper.setBlock(MACHINE.east(), Blocks.CHEST);
        helper.runAfterDelay(60, () -> {
            restore.run();
            helper.assertBlock(MACHINE.east(), b -> b == Blocks.CHEST, () -> "the chest must stay put");
            helper.assertBlock(MACHINE.east(2), b -> b == Blocks.AIR, () -> "and nothing is pushed");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "decor_linebuilderclaim", timeoutTicks = 100)
    public static void lineBuilderRespectsClaims(GameTestHelper helper) {
        Runnable restore = enable("lineBuilder");
        BlockPos abs = helper.absolutePos(MACHINE.east());
        Runnable release = claim(new net.minecraft.world.phys.AABB(abs).inflate(0.1));
        lineBuilder(helper);
        helper.runAfterDelay(60, () -> {
            restore.run();
            release.run();
            helper.assertBlock(MACHINE.east(), b -> b == Blocks.AIR, () -> "a claimed block must not be built on");
            helper.succeed();
        });
    }

    // ---- Block Filler and Spiller ----

    static final BlockPos PIT = new BlockPos(2, 3, 2);

    /** A one-block-wide pit two deep beneath {@code PIT}, walled in with stone. */
    static void pit(GameTestHelper helper) {
        for (BlockPos p : new BlockPos[] {new BlockPos(2, 0, 2), new BlockPos(1, 1, 2), new BlockPos(3, 1, 2), new BlockPos(2, 1, 1), new BlockPos(2, 1, 3),
                new BlockPos(1, 2, 2), new BlockPos(3, 2, 2), new BlockPos(2, 2, 1), new BlockPos(2, 2, 3)}) {
            helper.setBlock(p, Blocks.STONE);
        }
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.AIR);
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
    }

    static net.scwunge.rotarycraft.blockentity.BlockFillerBlockEntity blockFiller(GameTestHelper helper, ItemStack stack) {
        pit(helper);
        WeaponGameTests.spinningFlywheel(helper, PIT.west(), 1, 2048, Direction.EAST);
        helper.setBlock(PIT, DecorRegistry.BLOCK_FILLER.block().get().defaultBlockState().setValue(net.scwunge.rotarycraft.block.MachineBlock.FACING, Direction.EAST));
        net.scwunge.rotarycraft.blockentity.BlockFillerBlockEntity filler = helper.getBlockEntity(PIT);
        filler.items().setStackInSlot(0, stack);
        return filler;
    }

    @GameTest(template = TEMPLATE, batch = "decor_blockfillerfills", timeoutTicks = 100)
    public static void blockFillerFillsThePitLowestFirst(GameTestHelper helper) {
        Runnable restore = enable("blockFiller");
        net.scwunge.rotarycraft.blockentity.BlockFillerBlockEntity filler = blockFiller(helper, new ItemStack(Items.COBBLESTONE, 5));
        helper.runAfterDelay(60, () -> {
            restore.run();
            helper.assertBlock(new BlockPos(2, 1, 2), b -> b == Blocks.COBBLESTONE, () -> "the bottom of the pit should be filled");
            helper.assertBlock(new BlockPos(2, 2, 2), b -> b == Blocks.COBBLESTONE, () -> "then the next layer");
            helper.assertTrue(filler.items().getStackInSlot(0).getCount() == 3, "using one block each: " + filler.items().getStackInSlot(0).getCount() + " left");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "decor_blockfilleroff", timeoutTicks = 100)
    public static void blockFillerDoesNothingUnlessSwitchedOn(GameTestHelper helper) {
        blockFiller(helper, new ItemStack(Items.COBBLESTONE, 5));
        helper.runAfterDelay(60, () -> {
            helper.assertBlock(new BlockPos(2, 1, 2), b -> b == Blocks.AIR, () -> "it should be switched off by default");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "decor_blockfillerclaim", timeoutTicks = 100)
    public static void blockFillerRespectsClaims(GameTestHelper helper) {
        Runnable restore = enable("blockFiller");
        Runnable release = claim(new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(2, 1, 2))).inflate(1.1));
        blockFiller(helper, new ItemStack(Items.COBBLESTONE, 5));
        helper.runAfterDelay(60, () -> {
            restore.run();
            release.run();
            helper.assertBlock(new BlockPos(2, 1, 2), b -> b == Blocks.AIR, () -> "a claimed space must not be filled");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "decor_blockfillerpower", timeoutTicks = 100)
    public static void blockFillerNeedsMorePowerForStone(GameTestHelper helper) {
        Runnable restore = enable("blockFiller");
        pit(helper);
        WeaponGameTests.spinningFlywheel(helper, PIT.west(), 1, 1500, Direction.EAST);
        helper.setBlock(PIT, DecorRegistry.BLOCK_FILLER.block().get().defaultBlockState().setValue(net.scwunge.rotarycraft.block.MachineBlock.FACING, Direction.EAST));
        net.scwunge.rotarycraft.blockentity.BlockFillerBlockEntity filler = helper.getBlockEntity(PIT);
        filler.items().setStackInSlot(0, new ItemStack(Items.STONE, 2));
        helper.runAfterDelay(60, () -> {
            restore.run();
            helper.assertBlock(new BlockPos(2, 1, 2), b -> b == Blocks.AIR, () -> "1500 W is not enough to place stone (2048 W)");
            helper.assertTrue(!filler.items().isItemValid(0, new ItemStack(Items.DIAMOND)) && filler.items().isItemValid(0, new ItemStack(Items.STONE)), "only blocks go in");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "decor_spillerfills", timeoutTicks = 100)
    public static void spillerPlacesSourceBlocksFromItsTank(GameTestHelper helper) {
        Runnable restore = enable("spiller");
        pit(helper);
        WeaponGameTests.spinningFlywheel(helper, PIT.west(), 1, 1024, Direction.EAST);
        helper.setBlock(PIT, DecorRegistry.SPILLER.block().get().defaultBlockState().setValue(net.scwunge.rotarycraft.block.MachineBlock.FACING, Direction.EAST));
        net.scwunge.rotarycraft.blockentity.SpillerBlockEntity spiller = helper.getBlockEntity(PIT);
        spiller.tank().fill(new FluidStack(Fluids.WATER, 2500), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(80, () -> {
            restore.run();
            helper.assertBlock(new BlockPos(2, 1, 2), b -> b == Blocks.WATER, () -> "the pit bottom should be water");
            helper.assertBlock(new BlockPos(2, 2, 2), b -> b == Blocks.WATER, () -> "and the next");
            helper.assertTrue(spiller.tank().getFluidAmount() == 500, "a bucket each: " + spiller.tank().getFluidAmount() + " mB left");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "decor_spillersides", timeoutTicks = 40)
    public static void spillerTakesFluidFromTheTopAndSidesNotTheBottom(GameTestHelper helper) {
        helper.setBlock(PIT, DecorRegistry.SPILLER.block().get().defaultBlockState());
        BlockPos abs = helper.absolutePos(PIT);
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.DOWN) == null, "the bottom takes nothing");
        IFluidHandler top = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.UP);
        helper.assertTrue(top != null && top.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "the top takes lava");
        helper.succeed();
    }
}
