package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.CrystallizerBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryItems;

/** Crystallizer checks. They share the Extractor's batch so the Electric Motor can reach the 1024 rad/s the machine needs. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CrystallizerGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final BlockPos AT = new BlockPos(2, 1, 2);

    static CrystallizerBlockEntity crystallizer(GameTestHelper helper, int temperature, Fluid fluid, int amount, int dryIce) {
        helper.setBlock(new BlockPos(1, 1, 2), RotaryBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        IEnergyStorage e = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(new BlockPos(1, 1, 2)), Direction.UP);
        e.receiveEnergy(1_000_000, false);
        helper.setBlock(AT, RotaryBlocks.CRYSTALLIZER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        CrystallizerBlockEntity c = at(helper);
        c.setTemperature(temperature);
        if (amount > 0) {
            c.tank().fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE);
        }
        if (dryIce > 0) {
            c.items().setStackInSlot(CrystallizerBlockEntity.SLOT_DRY_ICE, new ItemStack(RotaryItems.DRY_ICE.get(), dryIce));
        }
        return c;
    }

    static CrystallizerBlockEntity at(GameTestHelper helper) {
        return (CrystallizerBlockEntity) helper.getBlockEntity(AT);
    }

    @GameTest(template = TEMPLATE, batch = ExtractorGameTests.BATCH, timeoutTicks = 1200)
    public static void lavaSetsIntoStone(GameTestHelper helper) {
        crystallizer(helper, 25, Fluids.LAVA, 1000, 0);
        helper.succeedWhen(() -> {
            CrystallizerBlockEntity c = at(helper);
            helper.assertTrue(c.items().getStackInSlot(CrystallizerBlockEntity.SLOT_OUTPUT).is(Items.STONE), "no stone yet");
            helper.assertTrue(c.tank().getFluidAmount() == 0, "the lava should be used up");
        });
    }

    @GameTest(template = TEMPLATE, batch = ExtractorGameTests.BATCH, timeoutTicks = 1200)
    public static void coldWaterFreezesIntoIce(GameTestHelper helper) {
        crystallizer(helper, -30, Fluids.WATER, 1000, 16);
        helper.succeedWhen(() -> {
            CrystallizerBlockEntity c = at(helper);
            helper.assertTrue(c.items().getStackInSlot(CrystallizerBlockEntity.SLOT_OUTPUT).is(Items.ICE),
                    "no ice yet at " + c.getTemperature() + " C, freezing point " + c.freezingPoint());
        });
    }

    @GameTest(template = TEMPLATE, batch = ExtractorGameTests.BATCH, timeoutTicks = 1200)
    public static void aSmallerBatchMakesSnowballs(GameTestHelper helper) {
        crystallizer(helper, -30, Fluids.WATER, 200, 16);
        helper.succeedWhen(() -> {
            CrystallizerBlockEntity c = at(helper);
            helper.assertTrue(c.items().getStackInSlot(CrystallizerBlockEntity.SLOT_OUTPUT).is(Items.SNOWBALL), "no snowball yet");
        });
    }

    @GameTest(template = TEMPLATE, batch = ExtractorGameTests.BATCH, timeoutTicks = 400)
    public static void warmWaterDoesNotFreeze(GameTestHelper helper) {
        crystallizer(helper, 25, Fluids.WATER, 1000, 0);
        helper.runAfterDelay(380, () -> {
            CrystallizerBlockEntity c = at(helper);
            helper.assertTrue(c.items().getStackInSlot(CrystallizerBlockEntity.SLOT_OUTPUT).isEmpty(), "water froze at " + c.getTemperature() + " C");
            helper.assertTrue(c.tank().getFluidAmount() == 1000, "water was used up");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void dryIceIsConsumedAndChills(GameTestHelper helper) {
        CrystallizerBlockEntity c = crystallizer(helper, 25, Fluids.WATER, 1000, 8);
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(at(helper).getTemperature() < 25 - 5, "dry ice should chill the machine, at " + at(helper).getTemperature());
            helper.assertTrue(at(helper).items().getStackInSlot(CrystallizerBlockEntity.SLOT_DRY_ICE).getCount() < 8, "dry ice should be used up while warming");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void onlyFreezableFluidsAndDryIceGoIn(GameTestHelper helper) {
        CrystallizerBlockEntity c = crystallizer(helper, 25, Fluids.WATER, 0, 0);
        helper.assertTrue(c.tank().fill(new FluidStack(RotaryFluids.LUBRICANT.source.get(), 1000), IFluidHandler.FluidAction.EXECUTE) == 0,
                "lubricant has no freezing recipe");
        helper.assertTrue(c.tank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "water should go in");
        helper.assertTrue(c.automationItems().insertItem(CrystallizerBlockEntity.SLOT_DRY_ICE, new ItemStack(Items.DIRT), false).getCount() == 1, "dirt isn't dry ice");
        helper.assertTrue(c.automationItems().insertItem(CrystallizerBlockEntity.SLOT_OUTPUT, new ItemStack(RotaryItems.DRY_ICE.get()), false).getCount() == 1,
                "nothing goes into the output slot");
        helper.assertTrue(c.automationItems().insertItem(CrystallizerBlockEntity.SLOT_DRY_ICE, new ItemStack(RotaryItems.DRY_ICE.get(), 4), false).isEmpty(), "dry ice goes in");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void freezingPointFollowsTheFluid(GameTestHelper helper) {
        CrystallizerBlockEntity c = crystallizer(helper, 25, Fluids.LAVA, 1000, 0);
        helper.assertTrue(c.freezingPoint() == -273 + (int) (0.9 * Fluids.LAVA.getFluidType().getTemperature()), "lava: " + c.freezingPoint());
        c.tank().drain(1000, IFluidHandler.FluidAction.EXECUTE);
        c.tank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(c.freezingPoint() == -273 + (int) (0.9 * Fluids.WATER.getFluidType().getTemperature()), "water: " + c.freezingPoint());
        helper.succeed();
    }
}
