package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.FermenterBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class FermenterGameTests {
    static final String TEMPLATE = "empty5x4x5";

    /** Electric motor (256 rad/s, 4 kW) into a fermenter held at the given temperature, optionally with water. */
    static FermenterBlockEntity fermenter(GameTestHelper helper, int temperature, boolean water) {
        helper.setBlock(new BlockPos(1, 1, 2), RotaryBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        IEnergyStorage e = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(new BlockPos(1, 1, 2)), Direction.UP);
        e.receiveEnergy(100_000, false);
        helper.setBlock(new BlockPos(2, 1, 2), RotaryBlocks.FERMENTER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        FermenterBlockEntity f = (FermenterBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
        f.setTemperature(temperature);
        if (water) {
            IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(new BlockPos(2, 1, 2)), Direction.UP);
            tank.fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
        }
        return f;
    }

    static FermenterBlockEntity at(GameTestHelper helper) {
        return (FermenterBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void sugarAndDirtMakeYeast(GameTestHelper helper) {
        FermenterBlockEntity f = fermenter(helper, 25, true);
        f.items().setStackInSlot(FermenterBlockEntity.SLOT_A, new ItemStack(Items.SUGAR, 4));
        f.items().setStackInSlot(FermenterBlockEntity.SLOT_B, new ItemStack(Items.DIRT, 4));
        helper.succeedWhen(() -> {
            FermenterBlockEntity b = at(helper);
            helper.assertTrue(b.items().getStackInSlot(FermenterBlockEntity.SLOT_OUT).is(RotaryItems.YEAST.get()), "no yeast yet at " + b.temperature() + " C");
            helper.assertTrue(b.water().getFluidAmount() < 4000, "no water used");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void yeastAndLeavesMakeSludge(GameTestHelper helper) {
        FermenterBlockEntity f = fermenter(helper, 35, true);
        f.items().setStackInSlot(FermenterBlockEntity.SLOT_A, new ItemStack(RotaryItems.YEAST.get(), 4));
        f.items().setStackInSlot(FermenterBlockEntity.SLOT_B, new ItemStack(Items.OAK_LEAVES, 4));
        helper.succeedWhen(() -> {
            ItemStack out = at(helper).items().getStackInSlot(FermenterBlockEntity.SLOT_OUT);
            helper.assertTrue(out.is(RotaryItems.SLUDGE.get()) && out.getCount() >= 2, "leaves (worth 2) should make 2 sludge, got " + out);
        });
    }

    @GameTest(template = TEMPLATE)
    public static void heatKillsYeast(GameTestHelper helper) {
        FermenterBlockEntity f = fermenter(helper, 70, true);
        f.items().setStackInSlot(FermenterBlockEntity.SLOT_A, new ItemStack(RotaryItems.YEAST.get(), 10));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(at(helper).items().getStackInSlot(FermenterBlockEntity.SLOT_A).getCount() < 10, "yeast survived 70 C");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void noWaterNoFermenting(GameTestHelper helper) {
        FermenterBlockEntity f = fermenter(helper, 25, false);
        f.items().setStackInSlot(FermenterBlockEntity.SLOT_A, new ItemStack(Items.SUGAR, 4));
        f.items().setStackInSlot(FermenterBlockEntity.SLOT_B, new ItemStack(Items.DIRT, 4));
        helper.runAfterDelay(250, () -> {
            helper.assertTrue(at(helper).items().getStackInSlot(FermenterBlockEntity.SLOT_OUT).isEmpty(), "fermented without water");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void fermentRateFollowsTheOriginal(GameTestHelper helper) {
        helper.assertTrue(FermenterBlockEntity.fermentRate(25, true) == 1F, "yeast optimum");
        helper.assertTrue(FermenterBlockEntity.fermentRate(35, false) == 1F, "sludge optimum");
        helper.assertTrue(Math.abs(FermenterBlockEntity.fermentRate(10, true) - 0.1F) < 1e-6, "cold: 1/(20 - T)");
        helper.assertTrue(Math.abs(FermenterBlockEntity.fermentRate(50, false) - 0.1F) < 1e-6, "hot: 1/(T - 40)");
        helper.assertTrue(FermenterBlockEntity.mulchValue(new ItemStack(Items.OAK_LEAVES)) == 2 && FermenterBlockEntity.mulchValue(new ItemStack(Items.SUGAR_CANE)) == 1,
                "plant values from the mulch tags");
        helper.succeed();
    }
}
