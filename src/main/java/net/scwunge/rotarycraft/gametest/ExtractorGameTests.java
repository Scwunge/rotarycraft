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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.ExtractorBlockEntity;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.item.OreProduct;
import net.scwunge.rotarycraft.item.OreProductItem;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryComponents;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

/**
 * Extractor checks. The "extractor" batch boosts the Electric Motor to 512 N*m at 8192 rad/s (enough for all four
 * stages at once) and restores the config afterwards, so the other tests are unaffected.
 */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ExtractorGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final String BATCH = "extractor";
    private static int savedTorque;
    private static int savedOmega;
    private static int savedWattsPerFe;

    @BeforeBatch(batch = BATCH)
    public static void boostMotor(ServerLevel level) {
        savedTorque = RotaryConfig.MOTOR_TORQUE.get();
        savedOmega = RotaryConfig.MOTOR_OMEGA.get();
        savedWattsPerFe = RotaryConfig.WATTS_PER_FE.get();
        RotaryConfig.MOTOR_TORQUE.set(512);
        RotaryConfig.MOTOR_OMEGA.set(8192);
        RotaryConfig.WATTS_PER_FE.set(1_000_000);
    }

    @AfterBatch(batch = BATCH)
    public static void restoreMotor(ServerLevel level) {
        RotaryConfig.MOTOR_TORQUE.set(savedTorque);
        RotaryConfig.MOTOR_OMEGA.set(savedOmega);
        RotaryConfig.WATTS_PER_FE.set(savedWattsPerFe);
    }

    static ExtractorBlockEntity poweredExtractor(GameTestHelper helper, boolean water) {
        helper.setBlock(new BlockPos(1, 1, 2), RotaryBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        IEnergyStorage e = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(new BlockPos(1, 1, 2)), Direction.UP);
        e.receiveEnergy(100_000, false);
        helper.setBlock(new BlockPos(2, 1, 2), RotaryBlocks.EXTRACTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        if (water) {
            IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(new BlockPos(2, 1, 2)), Direction.UP);
            tank.fill(new FluidStack(Fluids.WATER, 16_000), IFluidHandler.FluidAction.EXECUTE);
        }
        return (ExtractorBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
    }

    @GameTest(template = TEMPLATE)
    public static void oreTypesLoadFromData(GameTestHelper helper) {
        var level = helper.getLevel();
        var iron = level.getRecipeManager().getRecipeFor(RotaryRecipes.EXTRACTION.get(), new SingleRecipeInput(new ItemStack(Items.IRON_ORE)), level);
        var deepslateGold = level.getRecipeManager().getRecipeFor(RotaryRecipes.EXTRACTION.get(), new SingleRecipeInput(new ItemStack(Items.DEEPSLATE_GOLD_ORE)), level);
        var emerald = level.getRecipeManager().getRecipeFor(RotaryRecipes.EXTRACTION.get(), new SingleRecipeInput(new ItemStack(Items.EMERALD_ORE)), level);
        helper.assertTrue(iron.isPresent() && iron.get().value().type().equals("iron"), "iron ore isn't an extractor ore");
        helper.assertTrue(deepslateGold.isPresent() && deepslateGold.get().value().type().equals("gold"), "deepslate gold ore isn't recognised");
        helper.assertTrue(emerald.isPresent() && emerald.get().value().doublingChance() == 0.9, "emerald should be rare (90% doubling)");
        helper.assertTrue(iron.get().value().doublingChance() == 0.5, "iron should double 50% of the time");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void flakesSmeltIntoTheMetal(GameTestHelper helper) {
        var level = helper.getLevel();
        ItemStack ironFlakes = OreProductItem.of(RotaryItems.ORE_FLAKES.get(), new OreProduct("iron", 0xD8AF93), 1);
        var iron = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(ironFlakes), level);
        helper.assertTrue(iron.isPresent(), "iron flakes have no furnace recipe");
        helper.assertTrue(iron.get().value().assemble(new SingleRecipeInput(ironFlakes), level.registryAccess()).is(Items.IRON_INGOT),
                "iron flakes smelt into " + iron.get().value().assemble(new SingleRecipeInput(ironFlakes), level.registryAccess()));
        ItemStack tungsten = OreProductItem.of(RotaryItems.ORE_FLAKES.get(), new OreProduct("tungsten", 0x5A6470), 1);
        var t = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(tungsten), level);
        helper.assertTrue(t.isPresent() && t.get().value().assemble(new SingleRecipeInput(tungsten), level.registryAccess()).is(RotaryItems.TUNGSTEN_INGOT.get()),
                "tungsten flakes should smelt into this mod's tungsten ingot");
        helper.succeed();
    }

    /** One iron ore goes all the way through the four stages and comes out as iron flakes (1 to 16 of them). */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 2000)
    public static void ironOreBecomesFlakes(GameTestHelper helper) {
        ExtractorBlockEntity ex = poweredExtractor(helper, true);
        ex.items().setStackInSlot(0, new ItemStack(Items.IRON_ORE));
        helper.succeedWhen(() -> {
            ExtractorBlockEntity e = (ExtractorBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
            for (int s = 0; s < 4; s++) {
                helper.assertTrue(e.stagePowered(s), "stage " + (s + 1) + " unpowered at " + e.getTorque() + " N*m " + e.getOmega() + " rad/s");
            }
            ItemStack flakes = e.items().getStackInSlot(7);
            helper.assertTrue(flakes.is(RotaryItems.ORE_FLAKES.get()), "no flakes yet; stage inputs: "
                    + e.items().getStackInSlot(0) + " " + e.items().getStackInSlot(1) + " " + e.items().getStackInSlot(2) + " " + e.items().getStackInSlot(3));
            OreProduct p = flakes.get(RotaryComponents.ORE_PRODUCT.get());
            helper.assertTrue(p != null && p.type().equals("iron"), "flakes are of " + p);
            boolean pipelineEmpty = e.items().getStackInSlot(1).isEmpty() && e.items().getStackInSlot(2).isEmpty() && e.items().getStackInSlot(3).isEmpty()
                    && e.items().getStackInSlot(4).isEmpty() && e.items().getStackInSlot(5).isEmpty() && e.items().getStackInSlot(6).isEmpty();
            helper.assertTrue(pipelineEmpty, "products still moving through the stages");
            helper.assertTrue(flakes.getCount() >= 1 && flakes.getCount() <= 16, "impossible flake count " + flakes.getCount());
            helper.assertTrue(e.water().getFluidAmount() < 16_000, "the water stages used no water");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 600)
    public static void slurryStageNeedsWater(GameTestHelper helper) {
        ExtractorBlockEntity ex = poweredExtractor(helper, false);
        ex.items().setStackInSlot(1, OreProductItem.of(RotaryItems.ORE_DUST.get(), new OreProduct("iron", 0xD8AF93), 1));
        helper.runAfterDelay(400, () -> {
            ExtractorBlockEntity e = (ExtractorBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
            helper.assertTrue(e.stagePowered(1), "stage 2 should be powered");
            helper.assertTrue(e.items().getStackInSlot(1).is(RotaryItems.ORE_DUST.get()) && e.progress(1) == 0, "dust was processed without water");
            helper.succeed();
        });
    }
}
