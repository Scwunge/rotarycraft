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
}
