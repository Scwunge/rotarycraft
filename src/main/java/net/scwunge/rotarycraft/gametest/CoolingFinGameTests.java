package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.CoolingFinBlock;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.CompactorBlockEntity;
import net.scwunge.rotarycraft.blockentity.CoolingFinBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CoolingFinGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final BlockPos MACHINE = new BlockPos(2, 1, 2);
    static final BlockPos FIN = new BlockPos(3, 1, 2);

    static CompactorBlockEntity compactorWithFin(GameTestHelper helper, int temperature) {
        helper.setBlock(MACHINE, RotaryBlocks.COMPACTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        helper.setBlock(FIN, RotaryBlocks.COOLING_FIN.get().defaultBlockState().setValue(CoolingFinBlock.FACING, Direction.WEST));
        CompactorBlockEntity c = (CompactorBlockEntity) helper.getBlockEntity(MACHINE);
        c.setTemperature(temperature);
        return c;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void finDrawsHeatOutOfAMachine(GameTestHelper helper) {
        compactorWithFin(helper, 400);
        helper.runAfterDelay(300, () -> {
            int t = ((CompactorBlockEntity) helper.getBlockEntity(MACHINE)).temperature();
            // 15 fin steps of one degree on top of the compactor's own slow cooling (about 1 C a second)
            helper.assertTrue(t <= 400 - 15 - 10, "the fin should have drawn about 15 C out, compactor at " + t);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void quarterSettingIsFourTimesSlower(GameTestHelper helper) {
        compactorWithFin(helper, 400);
        ((CoolingFinBlockEntity) helper.getBlockEntity(FIN)).cycleSetting();
        ((CoolingFinBlockEntity) helper.getBlockEntity(FIN)).cycleSetting();
        helper.assertTrue(((CoolingFinBlockEntity) helper.getBlockEntity(FIN)).setting() == CoolingFinBlockEntity.Setting.QUARTER, "setting didn't cycle");
        helper.runAfterDelay(320, () -> {
            int t = ((CompactorBlockEntity) helper.getBlockEntity(MACHINE)).temperature();
            // 4 fin steps instead of 16, on top of the compactor's own cooling (about 1 C a second for 16 s)
            helper.assertTrue(t >= 400 - 4 - 16 - 2 && t <= 400 - 4 - 16 + 2, "a quarter-rate fin should only take ~4 C, compactor at " + t);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void finBesideLavaWarmsUp(GameTestHelper helper) {
        helper.setBlock(FIN, RotaryBlocks.COOLING_FIN.get().defaultBlockState().setValue(CoolingFinBlock.FACING, Direction.WEST));
        helper.setBlock(FIN.above(), Blocks.LAVA);
        int start = ((CoolingFinBlockEntity) helper.getBlockEntity(FIN)).getTemperature();
        helper.runAfterDelay(90, () -> {
            int now = ((CoolingFinBlockEntity) helper.getBlockEntity(FIN)).getTemperature();
            helper.assertTrue(now >= start + 3, "a fin beside lava should warm a degree a step: " + start + " -> " + now);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void notEveryMachineCanBeFinCooled(GameTestHelper helper) {
        helper.setBlock(MACHINE, RotaryBlocks.BLAST_FURNACE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        var blast = (net.scwunge.rotarycraft.power.Heatable) helper.getBlockEntity(MACHINE);
        helper.assertFalse(blast.canBeCooledWithFins(), "the Blast Furnace isn't fin-cooled in the original");
        helper.setBlock(MACHINE, RotaryBlocks.STEAM_ENGINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        var steam = (net.scwunge.rotarycraft.power.Heatable) helper.getBlockEntity(MACHINE);
        helper.assertTrue(steam.canBeCooledWithFins() && !steam.canBeFrictionHeated(), "the steam engine is fin-cooled, not friction-heated");
        helper.succeed();
    }
}
