package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.CompactorBlockEntity;
import net.scwunge.rotarycraft.power.ShaftMaterial;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

/**
 * Compactor checks. The boosted motor (512 N*m, 8192 rad/s) goes through a bedrock 16:1 reduction for 8192 N*m at
 * 512 rad/s: one coal stage then takes 300 - 15 x 9 = 165 ticks.
 */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CompactorGameTests {
    static final String TEMPLATE = "empty5x4x5";
    static final BlockPos COMPACTOR = new BlockPos(3, 1, 2);

    static CompactorBlockEntity compactor(GameTestHelper helper, int pressure, int temperature) {
        helper.setBlock(new BlockPos(1, 1, 2), RotaryBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        IEnergyStorage e = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(new BlockPos(1, 1, 2)), Direction.UP);
        e.receiveEnergy(100_000, false);
        helper.setBlock(new BlockPos(2, 1, 2), RotaryBlocks.gearbox(ShaftMaterial.BEDROCK, 16).get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(COMPACTOR, RotaryBlocks.COMPACTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        CompactorBlockEntity c = (CompactorBlockEntity) helper.getBlockEntity(COMPACTOR);
        for (int i = 0; i < 4; i++) {
            c.items().setStackInSlot(i, new ItemStack(Items.COAL, 4));
        }
        c.setPressure(pressure);
        c.setTemperature(temperature);
        return c;
    }

    @GameTest(template = TEMPLATE, batch = ExtractorGameTests.BATCH, timeoutTicks = 400)
    public static void squeezesCoalIntoAnthracite(GameTestHelper helper) {
        // pressure leaks ~1100 kPa a second net at this torque, so start well above the 550 MPa needed
        compactor(helper, 585_000, 860);
        helper.succeedWhen(() -> {
            CompactorBlockEntity c = (CompactorBlockEntity) helper.getBlockEntity(COMPACTOR);
            ItemStack out = c.items().getStackInSlot(CompactorBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(out.is(RotaryItems.ANTHRACITE.get()) && out.getCount() == 2, "four coal should make two anthracite, got " + out
                    + " at " + c.pressure() + " kPa, " + c.temperature() + " C");
            helper.assertTrue(c.items().getStackInSlot(0).getCount() == 3, "one of each coal used");
        });
    }

    @GameTest(template = TEMPLATE, batch = ExtractorGameTests.BATCH, timeoutTicks = 300)
    public static void tooLittlePressureDoesNothing(GameTestHelper helper) {
        compactor(helper, 300_000, 860);
        helper.runAfterDelay(250, () -> {
            CompactorBlockEntity c = (CompactorBlockEntity) helper.getBlockEntity(COMPACTOR);
            helper.assertTrue(c.items().getStackInSlot(CompactorBlockEntity.SLOT_OUTPUT).isEmpty(), "compacted at " + c.pressure() + " kPa");
            helper.assertTrue(c.pressure() > 300_000, "8192 N*m should keep raising the pressure");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void coalStagesFollowTheOriginal(GameTestHelper helper) {
        var level = helper.getLevel();
        var rm = level.getRecipeManager();
        var lons = rm.getRecipeFor(RotaryRecipes.COMPACTING.get(), new SingleRecipeInput(new ItemStack(RotaryItems.LONSDALEITE.get())), level);
        helper.assertTrue(lons.isPresent() && lons.get().value().result().is(Items.DIAMOND) && lons.get().value().stage() == 4, "lonsdaleite -> diamond, stage 4");
        var charcoal = rm.getRecipeFor(RotaryRecipes.COMPACTING.get(), new SingleRecipeInput(new ItemStack(Items.CHARCOAL)), level);
        helper.assertTrue(charcoal.isPresent() && charcoal.get().value().result().getCount() == 3, "charcoal gives 3 anthracite");
        helper.assertTrue(new ItemStack(RotaryItems.ANTHRACITE.get()).getBurnTime(null) == 4800, "anthracite burns 24 items");
        helper.succeed();
    }
}
