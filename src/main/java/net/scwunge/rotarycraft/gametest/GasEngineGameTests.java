package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.GasEngineBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class GasEngineGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final BlockPos ENGINE = new BlockPos(2, 1, 2);

    static GasEngineBlockEntity engine(GameTestHelper helper) {
        helper.setBlock(ENGINE, RotaryBlocks.GAS_ENGINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        return at(helper);
    }

    static GasEngineBlockEntity at(GameTestHelper helper) {
        return (GasEngineBlockEntity) helper.getBlockEntity(ENGINE);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void runsOnEthanolAtFullPower(GameTestHelper helper) {
        GasEngineBlockEntity e = engine(helper);
        IFluidHandler in = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(ENGINE), Direction.WEST);
        helper.assertTrue(in.fill(new FluidStack(RotaryFluids.ETHANOL.get(), 2000), IFluidHandler.FluidAction.EXECUTE) == 2000, "didn't take ethanol");
        helper.succeedWhen(() -> {
            GasEngineBlockEntity b = at(helper);
            helper.assertTrue(b.getOmega() == GasEngineBlockEntity.SPEED && b.getTorque() == GasEngineBlockEntity.TORQUE,
                    "expected 128 N*m at 512 rad/s, got " + b.getTorque() + " at " + b.getOmega());
            helper.assertTrue(b.fuel().getFluidAmount() < 2000, "no fuel burned");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void refusesOtherFluidsAndTheFront(GameTestHelper helper) {
        engine(helper);
        var level = helper.getLevel();
        BlockPos abs = helper.absolutePos(ENGINE);
        IFluidHandler back = level.getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.WEST);
        helper.assertTrue(back.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "took water");
        helper.assertTrue(level.getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.EAST) == null, "output side exposes the tank");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void ethanolCrystalsAddABucket(GameTestHelper helper) {
        GasEngineBlockEntity e = engine(helper);
        e.items().setStackInSlot(0, new ItemStack(RotaryItems.ETHANOL_CRYSTALS.get(), 3));
        helper.succeedWhen(() -> helper.assertTrue(at(helper).items().getStackInSlot(0).isEmpty() && at(helper).fuel().getFluidAmount() > 2900,
                "crystals not turned into fuel: " + at(helper).fuel().getFluidAmount()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void drownedEngineStops(GameTestHelper helper) {
        // fill the space around it with water, so no side is open
        for (Direction d : Direction.values()) {
            helper.setBlock(ENGINE.relative(d), Blocks.WATER.defaultBlockState());
        }
        GasEngineBlockEntity e = engine(helper);
        e.fuel().fill(new FluidStack(RotaryFluids.ETHANOL.get(), 2000), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(at(helper).isDrowned() && at(helper).getOmega() == 0, "a drowned engine ran");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void burnsTenMillibucketsEveryTwelveTicksAtSpeed(GameTestHelper helper) {
        GasEngineBlockEntity e = engine(helper);
        e.fuel().fill(new FluidStack(RotaryFluids.ETHANOL.get(), 10_000), IFluidHandler.FluidAction.EXECUTE);
        int[] mark = new int[1];
        helper.runAfterDelay(60, () -> mark[0] = at(helper).fuel().getFluidAmount());
        helper.runAfterDelay(180, () -> {
            int used = mark[0] - at(helper).fuel().getFluidAmount();
            helper.assertTrue(used == 100, "120 ticks at speed should burn 100 mB, burned " + used);
            helper.succeed();
        });
    }
}
