package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.BlastFurnaceBlockEntity;
import net.scwunge.rotarycraft.blockentity.FrictionHeaterBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class BlastFurnaceGameTests {
    static final String TEMPLATE = "empty5x4x5";

    static BlastFurnaceBlockEntity furnace(GameTestHelper helper, int x, int z) {
        helper.setBlock(new BlockPos(x, 1, z), RotaryBlocks.BLAST_FURNACE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        return (BlastFurnaceBlockEntity) helper.getBlockEntity(new BlockPos(x, 1, z));
    }

    static void loadSteelRecipe(BlastFurnaceBlockEntity bf, int iron) {
        bf.items().setStackInSlot(BlastFurnaceBlockEntity.SLOT_CENTER_ADDITIVE, new ItemStack(Items.COAL, 16));
        bf.items().setStackInSlot(BlastFurnaceBlockEntity.SLOT_LOWER_ADDITIVE, new ItemStack(Items.GUNPOWDER, 16));
        bf.items().setStackInSlot(BlastFurnaceBlockEntity.SLOT_UPPER_ADDITIVE, new ItemStack(Items.SAND, 16));
        for (int i = 0; i < iron; i++) {
            bf.items().setStackInSlot(1 + i, new ItemStack(Items.IRON_INGOT));
        }
    }

    static int steel(BlastFurnaceBlockEntity bf) {
        int n = 0;
        for (int slot : new int[]{BlastFurnaceBlockEntity.SLOT_OUTPUT_CENTER, BlastFurnaceBlockEntity.SLOT_OUTPUT_UPPER, BlastFurnaceBlockEntity.SLOT_OUTPUT_LOWER}) {
            ItemStack s = bf.items().getStackInSlot(slot);
            if (s.is(RotaryItems.HSLA_STEEL_INGOT.get())) {
                n += s.getCount();
            }
        }
        return n;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void hotBlastFurnaceMakesSteel(GameTestHelper helper) {
        BlastFurnaceBlockEntity bf = furnace(helper, 2, 2);
        bf.setTemperature(650);
        loadSteelRecipe(bf, 4);
        helper.succeedWhen(() -> {
            BlastFurnaceBlockEntity b = (BlastFurnaceBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
            helper.assertTrue(steel(b) >= 4, "made " + steel(b) + " steel at " + b.getTemperature() + " C");
            for (int s = 1; s <= 4; s++) {
                helper.assertTrue(b.items().getStackInSlot(s).isEmpty(), "iron left in grid slot " + s);
            }
            helper.assertTrue(b.items().getStackInSlot(BlastFurnaceBlockEntity.SLOT_CENTER_ADDITIVE).getCount() < 16, "coal is always used");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void coldBlastFurnaceMakesNothing(GameTestHelper helper) {
        BlastFurnaceBlockEntity bf = furnace(helper, 2, 2);
        loadSteelRecipe(bf, 4);
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(steel((BlastFurnaceBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2))) == 0, "steel made below 600 C");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void steelTimeFollowsTheOriginalFormula(GameTestHelper helper) {
        // 2 x (1500 - (T - 600)) / 12
        helper.assertTrue(BlastFurnaceBlockEntity.operationTime(600) == 250, "600 C gave " + BlastFurnaceBlockEntity.operationTime(600));
        helper.assertTrue(BlastFurnaceBlockEntity.operationTime(1200) == 150, "1200 C gave " + BlastFurnaceBlockEntity.operationTime(1200));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 500)
    public static void lavaHeatsTheBlastFurnace(GameTestHelper helper) {
        BlastFurnaceBlockEntity bf = furnace(helper, 2, 2);
        int start = bf.getTemperature();
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.LAVA);
        helper.runAfterDelay(400, () -> {
            int t = ((BlastFurnaceBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2))).getTemperature();
            // far below the lava target it climbs 2 C a second
            helper.assertTrue(t >= start + 30, "lava raised it from " + start + " only to " + t + " C in 20 s");
            helper.succeed();
        });
    }

    /** A Friction Heater driven hard enough (boosted motor) brings the furnace above steel temperature. */
    @GameTest(template = TEMPLATE, batch = ExtractorGameTests.BATCH, timeoutTicks = 400)
    public static void frictionHeaterBringsFurnaceToSteelHeat(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 2), RotaryBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        IEnergyStorage e = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(new BlockPos(1, 1, 2)), Direction.UP);
        e.receiveEnergy(100_000, false);
        helper.setBlock(new BlockPos(2, 1, 2), RotaryBlocks.FRICTION_HEATER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        furnace(helper, 3, 2);
        helper.succeedWhen(() -> {
            FrictionHeaterBlockEntity heater = (FrictionHeaterBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
            BlastFurnaceBlockEntity b = (BlastFurnaceBlockEntity) helper.getBlockEntity(new BlockPos(3, 1, 2));
            helper.assertTrue(heater.hasEnoughPower(), "heater underpowered: " + heater.getTorque() + " N*m " + heater.getOmega() + " rad/s");
            helper.assertTrue(b.getTemperature() >= BlastFurnaceBlockEntity.SMELT_TEMPERATURE,
                    "furnace at " + b.getTemperature() + " C, heater at " + heater.temperature() + " C");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void frictionHeaterEquilibriumMatchesTheOriginal(GameTestHelper helper) {
        // add 3*log2(w)*log2(t), then lose (T-30)/5: settles where (T + inc - 30) / 5 = inc
        int inc = (int) (3 * 8 * 5); // 32 N*m at 256 rad/s
        int t = 30;
        for (int i = 0; i < 200; i++) {
            t += inc;
            t -= (t - 30) / 5;
        }
        helper.assertTrue(Math.abs(t - 510) <= 5, "8 kW heater settles at " + t + " C, expected about 510");
        helper.succeed();
    }
}
