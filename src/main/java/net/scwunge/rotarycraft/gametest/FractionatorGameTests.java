package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.FractionatorBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

/** Fractionation Unit checks. Powered ones use the boosted motor (512 N*m at 8192 rad/s) underneath. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class FractionatorGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final BlockPos MOTOR = new BlockPos(2, 1, 2);
    static final BlockPos UNIT = new BlockPos(2, 2, 2);

    static FractionatorBlockEntity unit(GameTestHelper helper, boolean solvent) {
        helper.setBlock(MOTOR, RotaryBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.UP));
        IEnergyStorage e = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(MOTOR), Direction.NORTH);
        e.receiveEnergy(100_000, false);
        helper.setBlock(UNIT, RotaryBlocks.FRACTIONATOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        FractionatorBlockEntity f = at(helper);
        ItemStack[] ingredients = {new ItemStack(Items.BLAZE_POWDER, 8), new ItemStack(RotaryItems.COAL_DUST.get(), 8),
                new ItemStack(Items.MAGMA_CREAM, 8), new ItemStack(Items.PINK_DYE, 8), new ItemStack(RotaryItems.NETHERRACK_DUST.get(), 8),
                new ItemStack(RotaryItems.TAR.get(), 8)};
        for (int i = 0; i < ingredients.length; i++) {
            f.items().setStackInSlot(i, ingredients[i]);
        }
        if (solvent) {
            f.items().setStackInSlot(FractionatorBlockEntity.SLOT_SOLVENT, new ItemStack(Items.GHAST_TEAR));
        }
        f.ethanol().fill(new FluidStack(RotaryFluids.ETHANOL.get(), 4000), IFluidHandler.FluidAction.EXECUTE);
        return f;
    }

    static FractionatorBlockEntity at(GameTestHelper helper) {
        return (FractionatorBlockEntity) helper.getBlockEntity(UNIT);
    }

    @GameTest(template = TEMPLATE)
    public static void yieldFollowsTheOriginalCurve(GameTestHelper helper) {
        helper.assertTrue(Math.abs(FractionatorBlockEntity.yieldAt(0) - 0.01) < 1e-9, "0 kPa");
        helper.assertTrue(Math.abs(FractionatorBlockEntity.yieldAt(720) - 1) < 1e-9, "break-even at 720 kPa");
        helper.assertTrue(Math.abs(FractionatorBlockEntity.yieldAt(610) - 0.7) < 1e-9, "halfway between 500 and 720");
        helper.assertTrue(Math.abs(FractionatorBlockEntity.yieldAt(1000) - 2.5) < 1e-9, "max");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = ExtractorGameTests.BATCH, timeoutTicks = 400)
    public static void makesJetFuelFromEthanol(GameTestHelper helper) {
        unit(helper, true).setPressure(720);
        helper.succeedWhen(() -> {
            FractionatorBlockEntity f = at(helper);
            helper.assertTrue(f.fuel().getFluid().is(RotaryFluids.JET_FUEL.get()) && f.fuel().getFluidAmount() >= 500,
                    "no jet fuel yet at " + f.pressure() + " kPa");
            helper.assertTrue(f.ethanol().getFluidAmount() == 4000 - FractionatorBlockEntity.ETHANOL_PER_BATCH, "a batch takes 250 mB of ethanol");
            helper.assertTrue(f.items().getStackInSlot(FractionatorBlockEntity.SLOT_SOLVENT).is(Items.GHAST_TEAR), "the ghast tear was used up");
            int used = 0;
            for (int i = 0; i < FractionatorBlockEntity.SLOT_SOLVENT; i++) {
                used += 8 - f.items().getStackInSlot(i).getCount();
            }
            helper.assertTrue(used == 1 || used == 2, "a batch uses one or two ingredients, used " + used);
        });
    }

    @GameTest(template = TEMPLATE, batch = ExtractorGameTests.BATCH, timeoutTicks = 400)
    public static void noGhastTearNoFuel(GameTestHelper helper) {
        unit(helper, false).setPressure(720);
        helper.runAfterDelay(330, () -> {
            helper.assertTrue(at(helper).fuel().isEmpty() && at(helper).ethanol().getFluidAmount() == 4000, "worked without a ghast tear");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = ExtractorGameTests.BATCH, timeoutTicks = 300)
    public static void torqueBuildsPressure(GameTestHelper helper) {
        unit(helper, false);
        // pressure climbs slowly and unevenly, as in the original (a random step of up to ~19 kPa a second), so check it keeps rising
        int[] earlier = new int[1];
        helper.runAfterDelay(100, () -> earlier[0] = at(helper).pressure());
        helper.runAfterDelay(220, () -> {
            int p = at(helper).pressure();
            helper.assertTrue(earlier[0] > 0 && p > earlier[0], "512 N*m should keep pushing the pressure up: " + earlier[0] + " then " + p + " kPa");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void eachIngredientOnceAndFluidsBySide(GameTestHelper helper) {
        FractionatorBlockEntity f = unit(helper, true);
        ItemStack extra = new ItemStack(Items.BLAZE_POWDER);
        f.items().setStackInSlot(0, ItemStack.EMPTY);
        f.items().setStackInSlot(0, new ItemStack(Items.BLAZE_POWDER));
        helper.assertTrue(f.automationItems().insertItem(1, extra, true).getCount() == 1, "blaze powder went into a second slot");
        var level = helper.getLevel();
        BlockPos abs = helper.absolutePos(UNIT);
        IFluidHandler side = level.getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.EAST);
        IFluidHandler top = level.getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.UP);
        helper.assertTrue(side.fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 100), IFluidHandler.FluidAction.EXECUTE) == 0, "side took jet fuel");
        helper.assertTrue(side.fill(new FluidStack(RotaryFluids.ETHANOL.get(), 100), IFluidHandler.FluidAction.EXECUTE) == 100, "side refused ethanol");
        helper.assertTrue(top.fill(new FluidStack(RotaryFluids.ETHANOL.get(), 100), IFluidHandler.FluidAction.EXECUTE) == 0, "top took ethanol");
        f.fuel().fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(top.drain(400, IFluidHandler.FluidAction.EXECUTE).getAmount() == 400, "top didn't give jet fuel");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void grinderMakesCoalDust(GameTestHelper helper) {
        var level = helper.getLevel();
        var r = level.getRecipeManager().getRecipeFor(RotaryRecipes.GRINDING.get(), new SingleRecipeInput(new ItemStack(Items.COAL)), level);
        helper.assertTrue(r.isPresent() && r.get().value().getResultItem(level.registryAccess()).is(RotaryItems.COAL_DUST.get()), "no coal -> coal dust");
        var pink = level.getRecipeManager().getRecipeFor(RotaryRecipes.GRINDING.get(), new SingleRecipeInput(new ItemStack(Items.PINK_TULIP)), level);
        helper.assertTrue(pink.isPresent() && pink.get().value().getResultItem(level.registryAccess()).getCount() == 6, "flowers give 6 dye");
        helper.succeed();
    }
}
