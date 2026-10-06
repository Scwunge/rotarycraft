package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.VanDeGraffBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class VanDeGraffGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final BlockPos AT = new BlockPos(2, 2, 2);

    /** A motor under the generator (256 rad/s, 16 N*m: 4 kW, so 256 charge a tick). */
    static VanDeGraffBlockEntity generator(GameTestHelper helper, boolean powered) {
        if (powered) {
            helper.setBlock(new BlockPos(2, 1, 2), RotaryBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.UP));
            helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(new BlockPos(2, 1, 2)), Direction.DOWN)
                    .receiveEnergy(100_000, false);
        }
        helper.setBlock(AT, RotaryBlocks.VAN_DE_GRAAFF.get().defaultBlockState());
        return at(helper);
    }

    static VanDeGraffBlockEntity at(GameTestHelper helper) {
        return (VanDeGraffBlockEntity) helper.getBlockEntity(AT);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void shaftPowerBuildsCharge(GameTestHelper helper) {
        generator(helper, true);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(at(helper).charge() >= 1000, "charge after a second: " + at(helper).charge());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void unpoweredItStaysEmpty(GameTestHelper helper) {
        generator(helper, false);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(at(helper).charge() == 0, "charge " + at(helper).charge());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void zapsAMobInRange(GameTestHelper helper) {
        generator(helper, true).setCharge(4000);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 4));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(pig.getHealth() < pig.getMaxHealth() || !pig.isAlive(), "the pig should be hurt");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void lightsAdjacentTnt(GameTestHelper helper) {
        generator(helper, true).setCharge(4000);
        helper.setBlock(new BlockPos(2, 2, 1), Blocks.TNT);
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 1)).isAir(), "the TNT should have been lit");
            helper.assertEntityPresent(EntityType.TNT, new BlockPos(2, 2, 1), 2.0);
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void tooMuchChargeBlowsItUp(GameTestHelper helper) {
        generator(helper, false).setCharge(VanDeGraffBlockEntity.EXPLODE_CHARGE + 10);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(helper.getBlockState(AT).isAir(), "it should have destroyed itself");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void rangeGrowsWithCharge(GameTestHelper helper) {
        VanDeGraffBlockEntity g = generator(helper, false);
        g.setCharge(0);
        helper.assertTrue(g.range() == 0, "no charge, no range");
        g.setCharge(5 * 1024);
        helper.assertTrue(g.range() == 5, "5k charge is 5 blocks: " + g.range());
        g.setCharge(1_000_000);
        helper.assertTrue(g.range() == VanDeGraffBlockEntity.MAX_RANGE, "range is capped at 16");
        helper.succeed();
    }
}
