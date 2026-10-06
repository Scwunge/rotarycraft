package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.GasEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.PerformanceEngineBlockEntity;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryItems;

/** Performance Engine checks. The "explosions" batch turns block damage off so a burst can't wreck other tests. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class PerformanceEngineGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final String EXPLOSIONS = "explosions";
    static final BlockPos ENGINE = new BlockPos(2, 1, 2);
    private static boolean savedBreakBlocks;

    @BeforeBatch(batch = EXPLOSIONS)
    public static void noBlockDamage(ServerLevel level) {
        savedBreakBlocks = RotaryConfig.EXPLOSIONS_BREAK_BLOCKS.get();
        RotaryConfig.EXPLOSIONS_BREAK_BLOCKS.set(false);
    }

    @AfterBatch(batch = EXPLOSIONS)
    public static void restoreBlockDamage(ServerLevel level) {
        RotaryConfig.EXPLOSIONS_BREAK_BLOCKS.set(savedBreakBlocks);
    }

    static PerformanceEngineBlockEntity engine(GameTestHelper helper, int ethanol, ItemStack additive) {
        helper.setBlock(ENGINE, RotaryBlocks.PERFORMANCE_ENGINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        PerformanceEngineBlockEntity e = at(helper);
        e.fuel().fill(new FluidStack(RotaryFluids.ETHANOL.get(), ethanol), IFluidHandler.FluidAction.EXECUTE);
        e.items().setStackInSlot(PerformanceEngineBlockEntity.SLOT_ADDITIVE, additive);
        return e;
    }

    static PerformanceEngineBlockEntity at(GameTestHelper helper) {
        return (PerformanceEngineBlockEntity) helper.getBlockEntity(ENGINE);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void additivesGiveFullPower(GameTestHelper helper) {
        engine(helper, 10_000, new ItemStack(Items.BLAZE_POWDER, 4));
        helper.succeedWhen(() -> {
            PerformanceEngineBlockEntity e = at(helper);
            helper.assertTrue(e.getOmega() == PerformanceEngineBlockEntity.SPEED && e.getTorque() == PerformanceEngineBlockEntity.TORQUE,
                    "expected 256 N*m at 1024 rad/s, got " + e.getTorque() + " at " + e.getOmega());
            helper.assertTrue(e.additives() > 0 && e.additives() <= 16, "4 blaze powder are 16 additive units, have " + e.additives());
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void withoutAdditivesItIsAGasEngine(GameTestHelper helper) {
        engine(helper, 10_000, ItemStack.EMPTY);
        helper.succeedWhen(() -> {
            PerformanceEngineBlockEntity e = at(helper);
            helper.assertTrue(e.getOmega() == GasEngineBlockEntity.SPEED && e.getTorque() == GasEngineBlockEntity.TORQUE,
                    "a starved engine should give 128 N*m at 512 rad/s, got " + e.getTorque() + " at " + e.getOmega());
        });
    }

    @GameTest(template = TEMPLATE)
    public static void sidesTakeEthanolAndWater(GameTestHelper helper) {
        engine(helper, 0, ItemStack.EMPTY);
        IFluidHandler side = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(ENGINE), Direction.NORTH);
        helper.assertTrue(side.fill(new FluidStack(Fluids.WATER, 5000), IFluidHandler.FluidAction.EXECUTE) == 5000, "no water taken");
        helper.assertTrue(side.fill(new FluidStack(RotaryFluids.ETHANOL.get(), 3000), IFluidHandler.FluidAction.EXECUTE) == 3000, "no ethanol taken");
        helper.assertTrue(side.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "took lava");
        PerformanceEngineBlockEntity e = at(helper);
        helper.assertTrue(e.water().getFluidAmount() == 5000 && e.fuel().getFluidAmount() == 3000, "fluids went to the wrong tanks");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void waterHoldsItsTemperature(GameTestHelper helper) {
        PerformanceEngineBlockEntity e = engine(helper, 10_000, new ItemStack(Items.REDSTONE, 8));
        e.water().fill(new FluidStack(Fluids.WATER, 10_000), IFluidHandler.FluidAction.EXECUTE);
        e.setTemperature(100);
        helper.runAfterDelay(130, () -> {
            PerformanceEngineBlockEntity b = at(helper);
            helper.assertTrue(b.temperature() == 100, "cooled engine should hold 100 C, at " + b.temperature());
            helper.assertTrue(b.water().getFluidAmount() < 10_000, "no cooling water used");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = EXPLOSIONS, timeoutTicks = 200)
    public static void overheatingBlowsItIntoScrap(GameTestHelper helper) {
        PerformanceEngineBlockEntity e = engine(helper, 10_000, new ItemStack(Items.REDSTONE, 8));
        e.setTemperature(PerformanceEngineBlockEntity.MAX_TEMPERATURE - 1);
        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(RotaryBlocks.PERFORMANCE_ENGINE.get(), ENGINE);
            boolean scrap = helper.getEntities(EntityType.ITEM).stream().allMatch(i -> i.getItem().is(RotaryItems.SCRAP.get()));
            helper.assertTrue(scrap, "dropped something other than scrap");
        });
    }
}
