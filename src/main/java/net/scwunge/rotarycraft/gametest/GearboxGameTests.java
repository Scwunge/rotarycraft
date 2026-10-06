package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.GearboxBlockEntity;
import net.scwunge.rotarycraft.power.ShaftMaterial;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryComponents;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryItems;

/** Gearbox materials: wear, lubricant, heat and failure. Driven by the default motor (16 N*m, 256 rad/s) unless boosted. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class GearboxGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final BlockPos MOTOR = new BlockPos(1, 1, 2);
    static final BlockPos BOX = new BlockPos(2, 1, 2);

    static GearboxBlockEntity gearbox(GameTestHelper helper, ShaftMaterial m, int ratio) {
        helper.setBlock(MOTOR, RotaryBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        IEnergyStorage e = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(MOTOR), Direction.UP);
        e.receiveEnergy(100_000, false);
        helper.setBlock(BOX, RotaryBlocks.gearbox(m, ratio).get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        return at(helper);
    }

    static GearboxBlockEntity at(GameTestHelper helper) {
        return (GearboxBlockEntity) helper.getBlockEntity(BOX);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void wearCostsTorque(GameTestHelper helper) {
        gearbox(helper, ShaftMaterial.BEDROCK, 2).setDamage(100);
        helper.succeedWhen(() -> {
            GearboxBlockEntity g = at(helper);
            int expected = (int) (32 * Math.pow(0.99, 100));
            helper.assertTrue(g.getOmega() == 128 && g.getTorque() == expected, "100 wear should leave " + expected + " N*m, got " + g.getTorque());
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void dryGearboxWears(GameTestHelper helper) {
        gearbox(helper, ShaftMaterial.STEEL, 2);
        helper.runAfterDelay(800, () -> {
            int d = at(helper).damage();
            helper.assertTrue(d >= 5, "a dry steel gearbox should wear about 1 in 40 ticks after the first 100, wear " + d);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 500)
    public static void lubricatedGearboxUsesLubricantNotItsGears(GameTestHelper helper) {
        gearbox(helper, ShaftMaterial.STEEL, 2);
        IFluidHandler side = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(BOX), Direction.NORTH);
        helper.assertTrue(side.fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "no lubricant taken");
        helper.runAfterDelay(400, () -> {
            GearboxBlockEntity g = at(helper);
            helper.assertTrue(g.damage() == 0, "wore with lubricant");
            helper.assertTrue(g.lubricant().getFluidAmount() < 1000, "used no lubricant");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void diamondKeepsItsLubricantAndWoodTakesNone(GameTestHelper helper) {
        helper.assertTrue(GearboxBlockEntity.maxLubricant(ShaftMaterial.WOOD) == 0 && GearboxBlockEntity.maxLubricant(ShaftMaterial.BEDROCK) == 0, "wood/bedrock hold none");
        helper.assertTrue(GearboxBlockEntity.maxLubricant(ShaftMaterial.STONE) == 8000 && GearboxBlockEntity.maxLubricant(ShaftMaterial.STEEL) == 24000
                && GearboxBlockEntity.maxLubricant(ShaftMaterial.DIAMOND) == 1000, "capacities from the original");
        gearbox(helper, ShaftMaterial.WOOD, 2);
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(BOX), Direction.NORTH) == null, "wood takes lubricant");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void woodWarmsWhileTurning(GameTestHelper helper) {
        GearboxBlockEntity g = gearbox(helper, ShaftMaterial.WOOD, 2);
        g.setTemperature(20);
        helper.runAfterDelay(220, () -> {
            int t = at(helper).temperature();
            helper.assertTrue(t >= 20, "a turning wooden gearbox shouldn't cool below ambient, at " + t);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void repairAndBearings(GameTestHelper helper) {
        GearboxBlockEntity g = gearbox(helper, ShaftMaterial.STEEL, 16);
        g.setDamage(300);
        helper.assertTrue(g.repairWithGear() && g.damage() < 300, "gear didn't repair");
        // up to two tiers above its own material: steel takes stone to diamond, never wood or bedrock
        helper.assertTrue(g.acceptsBearing(ShaftMaterial.STONE) && g.acceptsBearing(ShaftMaterial.DIAMOND), "steel refused a stone or diamond bearing");
        helper.assertFalse(g.acceptsBearing(ShaftMaterial.WOOD) || g.acceptsBearing(ShaftMaterial.BEDROCK), "steel took a wood or bedrock bearing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void brokenGearboxKeepsItsState(GameTestHelper helper) {
        GearboxBlockEntity g = gearbox(helper, ShaftMaterial.STEEL, 4);
        g.setDamage(42);
        g.lubricant().fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 500), IFluidHandler.FluidAction.EXECUTE);
        var state = g.collectComponents().get(RotaryComponents.GEARBOX_STATE.get());
        helper.assertTrue(state != null && state.damage() == 42 && state.lubricant() == 500, "item state lost: " + state);
        helper.succeed();
    }

    /** The boosted motor's 512 N*m tears a wooden gearbox apart (wood carries about 278 N*m). */
    @GameTest(template = TEMPLATE, batch = ExtractorGameTests.BATCH, timeoutTicks = 200)
    public static void overloadedWoodenGearboxBreaks(GameTestHelper helper) {
        gearbox(helper, ShaftMaterial.WOOD, 2);
        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(RotaryBlocks.gearbox(ShaftMaterial.WOOD, 2).get(), BOX);
            boolean sawdust = helper.getEntities(EntityType.ITEM).stream().anyMatch(i -> i.getItem().is(RotaryItems.SAWDUST.get()));
            helper.assertTrue(sawdust, "no sawdust from the broken gearbox");
        });
    }
}
