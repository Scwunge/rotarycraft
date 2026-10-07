package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.BlastFurnaceBlockEntity;
import net.scwunge.rotarycraft.blockentity.JetEngineBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryFluids;

/** Jet Engine checks. The engine faces east: exhaust and shaft east, intake west. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class JetEngineGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final BlockPos JET = new BlockPos(2, 1, 2);

    static JetEngineBlockEntity jet(GameTestHelper helper) {
        helper.setBlock(JET, RotaryBlocks.JET_ENGINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        JetEngineBlockEntity j = at(helper);
        j.fuel().fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 200_000), IFluidHandler.FluidAction.EXECUTE);
        return j;
    }

    static JetEngineBlockEntity at(GameTestHelper helper) {
        return (JetEngineBlockEntity) helper.getBlockEntity(JET);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 1500)
    public static void reachesFullPowerOnJetFuel(GameTestHelper helper) {
        jet(helper);
        helper.succeedWhen(() -> {
            JetEngineBlockEntity j = at(helper);
            helper.assertTrue(j.getOmega() == JetEngineBlockEntity.SPEED && j.getTorque() == JetEngineBlockEntity.TORQUE,
                    "expected 1024 N*m at 65536 rad/s, got " + j.getTorque() + " at " + j.getOmega());
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void blockedIntakeChokesIt(GameTestHelper helper) {
        helper.setBlock(JET.west(), Blocks.STONE);
        jet(helper);
        helper.runAfterDelay(60, () -> {
            JetEngineBlockEntity j = at(helper);
            helper.assertTrue(j.chokedFraction() == 0 && j.getOmega() == 0, "ran with a stone block over its intake");
            helper.setBlock(JET.west(), Blocks.OAK_FENCE);
            helper.assertTrue(at(helper).chokedFraction() == 0.75F, "a fence lets 3/4 of the air through");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void damageHalvesTorqueAndRepairsFixIt(GameTestHelper helper) {
        JetEngineBlockEntity j = jet(helper);
        j.setFod(3);
        helper.assertTrue(j.repairPartly() && j.fod() == 2, "a compressor takes off one point");
        helper.assertTrue(j.repairFully() && j.fod() == 0 && !j.isFailing(), "a turbine repairs it fully");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void fodHalvesTorque(GameTestHelper helper) {
        jet(helper).setFod(2);
        helper.succeedWhen(() -> helper.assertTrue(at(helper).getTorque() == JetEngineBlockEntity.TORQUE / 4,
                "two points of FOD should quarter the torque, got " + at(helper).getTorque()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void swallowedCobblestoneComesOutGravel(GameTestHelper helper) {
        jet(helper);
        helper.runAfterDelay(20, () -> {
            Vec3 p = helper.absoluteVec(new Vec3(0.5, 1.2, 2.5));
            ItemEntity item = new ItemEntity(helper.getLevel(), p.x, p.y, p.z, new ItemStack(Items.COBBLESTONE));
            item.setDeltaMovement(Vec3.ZERO);
            helper.getLevel().addFreshEntity(item);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(at(helper).fod() >= 2, "cobblestone should chip the blades (FOD 2), FOD " + at(helper).fod());
            boolean gravel = helper.getEntities(EntityType.ITEM).stream().anyMatch(i -> i.getItem().is(Items.GRAVEL));
            helper.assertTrue(gravel, "no gravel blown out of the exhaust");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = "jet_swallow")
    public static void swallowsMobs(GameTestHelper helper) {
        jet(helper);
        // a no-AI mob ignores velocity, so use an ordinary one
        Zombie z = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 1, 2));
        helper.succeedWhen(() -> {
            helper.assertTrue(!z.isAlive(), "the zombie wasn't sucked in");
            helper.assertTrue(at(helper).fod() >= 1, "a zombie should damage the engine");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 1600)
    public static void exhaustHeatsMachinesBehindIt(GameTestHelper helper) {
        jet(helper);
        helper.setBlock(JET.east(), RotaryBlocks.BLAST_FURNACE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.succeedWhen(() -> {
            int t = ((BlastFurnaceBlockEntity) helper.getBlockEntity(JET.east())).getTemperature();
            helper.assertTrue(t >= 300, "the exhaust should heat the blast furnace, at " + t + " C");
        });
    }
}
