package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.FrictionHeaterBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryItems;

/** The Friction Heater driving a vanilla furnace in front of it. A pre-heated heater is fed by the motor only to stay "powered". */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class FrictionFurnaceGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final BlockPos HEATER = new BlockPos(2, 1, 2);
    static final BlockPos FURNACE = new BlockPos(3, 1, 2);

    /** 32 N*m is the heater's minimum torque; the default motor gives 16, so use the boosted batch (512 N*m at 8192 rad/s). */
    static FrictionHeaterBlockEntity setup(GameTestHelper helper, int temperature) {
        helper.setBlock(new BlockPos(1, 1, 2), RotaryBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        IEnergyStorage e = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(new BlockPos(1, 1, 2)), Direction.UP);
        e.receiveEnergy(100_000, false);
        helper.setBlock(HEATER, RotaryBlocks.FRICTION_HEATER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(FURNACE, Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.WEST));
        FrictionHeaterBlockEntity h = (FrictionHeaterBlockEntity) helper.getBlockEntity(HEATER);
        h.setTemperature(temperature);
        return h;
    }

    static AbstractFurnaceBlockEntity furnace(GameTestHelper helper) {
        return (AbstractFurnaceBlockEntity) helper.getBlockEntity(FURNACE);
    }

    @GameTest(template = TEMPLATE, batch = ExtractorGameTests.BATCH, timeoutTicks = 200)
    public static void keepsTheFurnaceLitAndSmeltsFast(GameTestHelper helper) {
        setup(helper, 1000);
        furnace(helper).setItem(0, new ItemStack(Items.RAW_IRON, 8));
        helper.succeedWhen(() -> {
            AbstractFurnaceBlockEntity f = furnace(helper);
            helper.assertTrue(helper.getBlockState(FURNACE).getValue(AbstractFurnaceBlock.LIT), "the furnace isn't lit");
            helper.assertTrue(f.getItem(2).is(Items.IRON_INGOT) && f.getItem(2).getCount() >= 3, "a 1000 C heater should smelt fast, ingots: " + f.getItem(2));
        });
    }

    @GameTest(template = TEMPLATE, batch = ExtractorGameTests.BATCH, timeoutTicks = 300)
    public static void doesTheSpecialSmeltsOnlyHotEnough(GameTestHelper helper) {
        setup(helper, 1000);
        furnace(helper).setItem(0, new ItemStack(RotaryItems.SILICON_DUST.get(), 4));
        helper.succeedWhen(() -> helper.assertTrue(furnace(helper).getItem(2).is(net.scwunge.rotarycraft.registry.RotaryParts.part("silicon").get()),
                "silicon dust should become silicon in a 1000 C furnace (needs 800 C)"));
    }

    @GameTest(template = TEMPLATE)
    public static void speedFactorFollowsTheOriginal(GameTestHelper helper) {
        helper.assertTrue(FrictionHeaterBlockEntity.speedFactor(400) == 1, "under 500 C");
        helper.assertTrue(FrictionHeaterBlockEntity.speedFactor(700) == 1 + (int) Math.sqrt(4), "700 C: 1 + sqrt(2^2)");
        helper.assertTrue(FrictionHeaterBlockEntity.speedFactor(2000) == 2000, "instant at the maximum");
        helper.succeed();
    }
}
