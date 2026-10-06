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
import net.scwunge.rotarycraft.blockentity.RockMelterBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryItems;

/** Rock Melter checks, driven by the default Electric Motor underneath (16 N*m at 256 rad/s, 4 kW). */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class RockMelterGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final BlockPos MOTOR = new BlockPos(2, 1, 2);
    static final BlockPos MELTER = new BlockPos(2, 2, 2);

    static RockMelterBlockEntity melter(GameTestHelper helper, int temperature, ItemStack input) {
        helper.setBlock(MOTOR, RotaryBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.UP));
        IEnergyStorage e = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(MOTOR), Direction.NORTH);
        e.receiveEnergy(100_000, false);
        helper.setBlock(MELTER, RotaryBlocks.ROCK_MELTER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        RockMelterBlockEntity m = at(helper);
        m.setTemperature(temperature);
        m.items().setStackInSlot(0, input);
        return m;
    }

    static RockMelterBlockEntity at(GameTestHelper helper) {
        return (RockMelterBlockEntity) helper.getBlockEntity(MELTER);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void cleanSludgeMeltsIntoEthanol(GameTestHelper helper) {
        // 9000 J needs 180000 watt-ticks: about 44 ticks at 4 kW once above 180 C
        melter(helper, 200, new ItemStack(RotaryItems.CLEAN_SLUDGE.get(), 4));
        helper.succeedWhen(() -> {
            RockMelterBlockEntity m = at(helper);
            helper.assertTrue(m.tank().getFluid().is(RotaryFluids.ETHANOL.get()) && m.tank().getFluidAmount() >= 1000,
                    "no ethanol yet: " + m.tank().getFluidAmount() + " mB at " + m.temperature() + " C");
            helper.assertTrue(m.items().getStackInSlot(0).getCount() < 4, "sludge wasn't used");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void stoneNeedsAThousandDegrees(GameTestHelper helper) {
        // 4 kW settles the melter about 64 x 12 = 768 C above ambient, under stone's 1000 C
        melter(helper, 25, new ItemStack(Items.STONE, 4));
        helper.runAfterDelay(250, () -> {
            RockMelterBlockEntity m = at(helper);
            helper.assertTrue(m.tank().isEmpty() && m.items().getStackInSlot(0).getCount() == 4, "stone melted below 1000 C");
            helper.assertTrue(m.energy() == 0, "energy built up while too cold");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void powerHeatsTheMelter(GameTestHelper helper) {
        melter(helper, 25, ItemStack.EMPTY);
        helper.runAfterDelay(110, () -> {
            int t = at(helper).temperature();
            // five one-second steps of +12 (log2 of 4096 W), minus a little settling
            helper.assertTrue(t >= 60 && t <= 85, "expected about 25 + 5 x 12 C, got " + t);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void tankDrainsFromTheSidesOnly(GameTestHelper helper) {
        RockMelterBlockEntity m = melter(helper, 25, ItemStack.EMPTY);
        m.tank().fill(new FluidStack(Fluids.LAVA, 4000), IFluidHandler.FluidAction.EXECUTE);
        var level = helper.getLevel();
        BlockPos abs = helper.absolutePos(MELTER);
        helper.assertTrue(level.getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.UP) == null, "top shouldn't expose the tank");
        IFluidHandler side = level.getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.EAST);
        helper.assertTrue(side != null && side.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "side accepted fluid");
        helper.assertTrue(side.drain(1000, IFluidHandler.FluidAction.EXECUTE).getAmount() == 1000 && m.tank().getFluidAmount() == 3000, "side didn't drain");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fluidsHaveBuckets(GameTestHelper helper) {
        for (RotaryFluids.Entry f : RotaryFluids.ALL) {
            helper.assertTrue(f.bucket.get().content == f.get(), f.name + " bucket holds the wrong fluid");
            helper.assertTrue(f.get().getFluidType().getTemperature() > 0, f.name + " has no temperature");
        }
        helper.assertTrue(RotaryFluids.LIQUID_NITROGEN.get().getFluidType().getTemperature() == 77, "liquid nitrogen is 77 K");
        helper.succeed();
    }
}
