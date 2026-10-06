package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
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
import net.scwunge.rotarycraft.blockentity.PulseFurnaceBlockEntity;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryItems;

/** Pulse Furnace checks. The "pulse" batch lets the Electric Motor reach the 131072 rad/s the furnace's compressor needs. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class PulseFurnaceGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final String BATCH = "pulse";
    static final BlockPos AT = new BlockPos(2, 1, 2);
    private static int savedTorque;
    private static int savedOmega;
    private static int savedWattsPerFe;

    @BeforeBatch(batch = BATCH)
    public static void boostMotor(ServerLevel level) {
        savedTorque = RotaryConfig.MOTOR_TORQUE.get();
        savedOmega = RotaryConfig.MOTOR_OMEGA.get();
        savedWattsPerFe = RotaryConfig.WATTS_PER_FE.get();
        RotaryConfig.MOTOR_TORQUE.set(16);
        RotaryConfig.MOTOR_OMEGA.set(131072);
        RotaryConfig.WATTS_PER_FE.set(1_000_000);
    }

    @AfterBatch(batch = BATCH)
    public static void restoreMotor(ServerLevel level) {
        RotaryConfig.MOTOR_TORQUE.set(savedTorque);
        RotaryConfig.MOTOR_OMEGA.set(savedOmega);
        RotaryConfig.WATTS_PER_FE.set(savedWattsPerFe);
    }

    static PulseFurnaceBlockEntity furnace(GameTestHelper helper, int temperature, int fuel, int oxygen) {
        helper.setBlock(new BlockPos(1, 1, 2), RotaryBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        IEnergyStorage e = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(new BlockPos(1, 1, 2)), Direction.UP);
        e.receiveEnergy(1_000_000, false);
        helper.setBlock(AT, RotaryBlocks.PULSE_FURNACE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        PulseFurnaceBlockEntity f = at(helper);
        f.setTemperature(temperature);
        if (fuel > 0) {
            f.fuel().fill(new FluidStack(RotaryFluids.JET_FUEL.get(), fuel), IFluidHandler.FluidAction.EXECUTE);
        }
        if (oxygen > 0) {
            f.accelerant().fill(new FluidStack(RotaryFluids.OXYGEN.get(), oxygen), IFluidHandler.FluidAction.EXECUTE);
        }
        return f;
    }

    static PulseFurnaceBlockEntity at(GameTestHelper helper) {
        return (PulseFurnaceBlockEntity) helper.getBlockEntity(AT);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void obsidianBecomesBlastGlass(GameTestHelper helper) {
        PulseFurnaceBlockEntity f = furnace(helper, 880, 4000, 0);
        f.items().setStackInSlot(PulseFurnaceBlockEntity.SLOT_INPUT, new ItemStack(Items.OBSIDIAN, 2));
        helper.succeedWhen(() -> {
            PulseFurnaceBlockEntity b = at(helper);
            helper.assertTrue(b.items().getStackInSlot(PulseFurnaceBlockEntity.SLOT_OUTPUT).is(RotaryBlocks.BLAST_GLASS.get().asItem()),
                    "no blast glass yet at " + b.getTemperature() + " C");
            helper.assertTrue(b.items().getStackInSlot(PulseFurnaceBlockEntity.SLOT_INPUT).getCount() < 2, "the obsidian wasn't used");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void ironIngotsBecomeTwoSteel(GameTestHelper helper) {
        PulseFurnaceBlockEntity f = furnace(helper, 920, 4000, 0);
        f.items().setStackInSlot(PulseFurnaceBlockEntity.SLOT_INPUT, new ItemStack(Items.IRON_INGOT));
        helper.succeedWhen(() -> {
            ItemStack out = at(helper).items().getStackInSlot(PulseFurnaceBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(out.is(RotaryItems.HSLA_STEEL_INGOT.get()) && out.getCount() == 2, "got " + out);
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void aColdFurnaceHeatsBeforeItSmelts(GameTestHelper helper) {
        PulseFurnaceBlockEntity f = furnace(helper, 25, 4000, 0);
        f.items().setStackInSlot(PulseFurnaceBlockEntity.SLOT_INPUT, new ItemStack(Items.OBSIDIAN));
        helper.runAfterDelay(120, () -> {
            PulseFurnaceBlockEntity b = at(helper);
            helper.assertTrue(b.getTemperature() > 25 + 30, "the burner should be warming it, at " + b.getTemperature());
            helper.assertTrue(b.items().getStackInSlot(PulseFurnaceBlockEntity.SLOT_OUTPUT).isEmpty(), "smelted before 850 C");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void noFuelNoSmelting(GameTestHelper helper) {
        PulseFurnaceBlockEntity f = furnace(helper, 900, 0, 0);
        f.items().setStackInSlot(PulseFurnaceBlockEntity.SLOT_INPUT, new ItemStack(Items.IRON_INGOT));
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(at(helper).items().getStackInSlot(PulseFurnaceBlockEntity.SLOT_OUTPUT).isEmpty(), "smelted without fuel");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void oxygenMakesItFourTimesAsFast(GameTestHelper helper) {
        PulseFurnaceBlockEntity f = furnace(helper, 800, 4000, 4000);
        f.items().setStackInSlot(PulseFurnaceBlockEntity.SLOT_INPUT, new ItemStack(Items.IRON_BOOTS));
        // 100 ticks of heating plus 20 of cooking is 120 ticks without oxygen; with it about 30
        helper.runAfterDelay(70, () -> {
            PulseFurnaceBlockEntity b = at(helper);
            helper.assertTrue(b.items().getStackInSlot(PulseFurnaceBlockEntity.SLOT_OUTPUT).is(Items.IRON_INGOT), "iron boots should be recycled by now");
            helper.assertTrue(b.items().getStackInSlot(PulseFurnaceBlockEntity.SLOT_OUTPUT).getCount() == 4, "four ingots from boots");
            helper.assertTrue(b.accelerant().getFluidAmount() < 4000, "oxygen should have been burned");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void pastAThousandDegreesItBlowsUp(GameTestHelper helper) {
        furnace(helper, 1005, 4000, 0);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(helper.getBlockState(AT).is(Blocks.AIR) || !helper.getBlockState(AT).is(RotaryBlocks.PULSE_FURNACE.get()), "it should have destroyed itself");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void slowShaftLeavesItDead(GameTestHelper helper) {
        // the default motor only manages 256 rad/s, nowhere near the 131072 the compressor needs
        helper.setBlock(new BlockPos(1, 1, 2), RotaryBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(new BlockPos(1, 1, 2)), Direction.UP).receiveEnergy(100_000, false);
        helper.setBlock(AT, RotaryBlocks.PULSE_FURNACE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        PulseFurnaceBlockEntity f = at(helper);
        f.fuel().fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 4000), IFluidHandler.FluidAction.EXECUTE);
        int start = f.getTemperature();
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(at(helper).getTemperature() <= start, "no compressor power, so no heating: " + start + " -> " + at(helper).getTemperature());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void fluidsGoInFromTheSidesOnly(GameTestHelper helper) {
        furnace(helper, 25, 0, 0);
        var level = helper.getLevel();
        BlockPos abs = helper.absolutePos(AT);
        helper.assertTrue(level.getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.UP) == null, "no fluid access from above");
        helper.assertTrue(level.getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.DOWN) == null, "no fluid access from below");
        IFluidHandler side = level.getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.NORTH);
        helper.assertTrue(side.fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE) == 500, "water");
        helper.assertTrue(side.fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 500), IFluidHandler.FluidAction.EXECUTE) == 500, "jet fuel");
        helper.assertTrue(side.fill(new FluidStack(RotaryFluids.OXYGEN.get(), 500), IFluidHandler.FluidAction.EXECUTE) == 500, "oxygen");
        helper.assertTrue(side.fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 500), IFluidHandler.FluidAction.EXECUTE) == 0, "lubricant isn't wanted");
        helper.assertTrue(side.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "nothing comes back out");
        PulseFurnaceBlockEntity f = at(helper);
        helper.assertTrue(f.water().getFluidAmount() == 500 && f.fuel().getFluidAmount() == 500 && f.accelerant().getFluidAmount() == 500, "each fluid in its own tank");
        helper.succeed();
    }
}
