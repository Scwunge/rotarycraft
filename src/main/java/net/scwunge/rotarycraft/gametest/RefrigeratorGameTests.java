package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.RefrigeratorBlockEntity;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryFluids;

/** Refrigerator checks. The "fridge" batch gives the Electric Motor the 2048 N*m the machine needs. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class RefrigeratorGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final String BATCH = "fridge";
    static final BlockPos AT = new BlockPos(2, 1, 2);
    private static int savedTorque;
    private static int savedOmega;
    private static int savedWattsPerFe;

    @BeforeBatch(batch = BATCH)
    public static void boostMotor(ServerLevel level) {
        savedTorque = RotaryConfig.MOTOR_TORQUE.get();
        savedOmega = RotaryConfig.MOTOR_OMEGA.get();
        savedWattsPerFe = RotaryConfig.WATTS_PER_FE.get();
        RotaryConfig.MOTOR_TORQUE.set(2048);
        RotaryConfig.MOTOR_OMEGA.set(8192);
        RotaryConfig.WATTS_PER_FE.set(1_000_000);
    }

    @AfterBatch(batch = BATCH)
    public static void restoreMotor(ServerLevel level) {
        RotaryConfig.MOTOR_TORQUE.set(savedTorque);
        RotaryConfig.MOTOR_OMEGA.set(savedOmega);
        RotaryConfig.WATTS_PER_FE.set(savedWattsPerFe);
    }

    static RefrigeratorBlockEntity fridge(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 2), RotaryBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(new BlockPos(1, 1, 2)), Direction.UP).receiveEnergy(1_000_000, false);
        helper.setBlock(AT, RotaryBlocks.REFRIGERATOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        return at(helper);
    }

    static RefrigeratorBlockEntity at(GameTestHelper helper) {
        return (RefrigeratorBlockEntity) helper.getBlockEntity(AT);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 800)
    public static void iceBecomesLiquidNitrogen(GameTestHelper helper) {
        RefrigeratorBlockEntity f = fridge(helper);
        f.items().setStackInSlot(RefrigeratorBlockEntity.SLOT_ICE, new ItemStack(Items.ICE, 2));
        helper.succeedWhen(() -> {
            RefrigeratorBlockEntity b = at(helper);
            helper.assertTrue(b.tank().getFluid().getFluid().isSame(RotaryFluids.LIQUID_NITROGEN.get()) && b.tank().getFluidAmount() >= 100,
                    "no liquid nitrogen yet (" + b.tank().getFluidAmount() + " mB)");
            helper.assertTrue(b.items().getStackInSlot(RefrigeratorBlockEntity.SLOT_ICE).getCount() < 2, "the ice wasn't used");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void aFullTankStopsIt(GameTestHelper helper) {
        RefrigeratorBlockEntity f = fridge(helper);
        f.tank().fill(new FluidStack(RotaryFluids.LIQUID_NITROGEN.get(), RefrigeratorBlockEntity.CAPACITY), IFluidHandler.FluidAction.EXECUTE);
        f.items().setStackInSlot(RefrigeratorBlockEntity.SLOT_ICE, new ItemStack(Items.ICE, 2));
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(at(helper).items().getStackInSlot(RefrigeratorBlockEntity.SLOT_ICE).getCount() == 2, "it ran with a full tank");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aWeakShaftDoesNothing(GameTestHelper helper) {
        // the default motor delivers 16 N*m, nowhere near 2048
        RefrigeratorBlockEntity f = fridge(helper);
        f.items().setStackInSlot(RefrigeratorBlockEntity.SLOT_ICE, new ItemStack(Items.ICE, 2));
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(at(helper).items().getStackInSlot(RefrigeratorBlockEntity.SLOT_ICE).getCount() == 2, "ran without the torque");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void brokenWithAFullTankItFreezesThingsNearby(GameTestHelper helper) {
        RefrigeratorBlockEntity f = fridge(helper);
        f.tank().fill(new FluidStack(RotaryFluids.LIQUID_NITROGEN.get(), RefrigeratorBlockEntity.CAPACITY), IFluidHandler.FluidAction.EXECUTE);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 1, 4));
        f.onBroken();
        helper.assertTrue(pig.getHealth() < pig.getMaxHealth() || !pig.isAlive(), "the cold should have hurt the pig");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void onlyIceInAndOnlyNitrogenAndDryIceOut(GameTestHelper helper) {
        RefrigeratorBlockEntity f = fridge(helper);
        helper.assertTrue(f.automationItems().insertItem(RefrigeratorBlockEntity.SLOT_ICE, new ItemStack(Items.DIRT), false).getCount() == 1, "dirt isn't ice");
        helper.assertTrue(f.automationItems().insertItem(RefrigeratorBlockEntity.SLOT_ICE, new ItemStack(Items.ICE, 4), false).isEmpty(), "ice goes in");
        helper.assertTrue(f.automationItems().extractItem(RefrigeratorBlockEntity.SLOT_ICE, 1, false).isEmpty(), "the ice can't be pulled back out");
        f.tank().fill(new FluidStack(RotaryFluids.LIQUID_NITROGEN.get(), 500), IFluidHandler.FluidAction.EXECUTE);
        IFluidHandler out = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(AT), Direction.NORTH);
        helper.assertTrue(out.fill(new FluidStack(RotaryFluids.LIQUID_NITROGEN.get(), 100), IFluidHandler.FluidAction.EXECUTE) == 0, "nothing goes in");
        helper.assertTrue(out.drain(200, IFluidHandler.FluidAction.EXECUTE).getAmount() == 200, "nitrogen comes out");
        helper.succeed();
    }
}
