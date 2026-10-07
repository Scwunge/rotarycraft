package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.config.FarmConfig;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.farm.FanBlockEntity;
import net.scwunge.rotarycraft.farm.FertilizerBlockEntity;
import net.scwunge.rotarycraft.farm.GroundHydratorBlockEntity;
import net.scwunge.rotarycraft.farm.LawnSprinklerBlockEntity;
import net.scwunge.rotarycraft.farm.SprinklerBlockEntity;
import net.scwunge.rotarycraft.pipe.PipeType;
import net.scwunge.rotarycraft.registry.FarmRegistry;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.RotaryParts;

import java.util.List;

/** Tests of the farming and automation machines. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class FarmGameTests {
    static final String SMALL = RotaryGameTests.TEMPLATE;
    static final String LONG = "empty20x8x7";

    /** A fan on the ground at (3, 2, 3) blowing east, with a spinning flywheel feeding it from the west. */
    static FanBlockEntity fan(GameTestHelper helper, int torque, int omega) {
        BlockPos at = new BlockPos(3, 2, 3);
        WeaponGameTests.spinningFlywheel(helper, at.west(), torque, omega, Direction.EAST);
        helper.setBlock(at, FarmRegistry.FAN.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        return helper.getBlockEntity(at);
    }

    /** 8 blocks at the minimum power (1 kW), and one more for each 1024 W above it, to the configured limit. */
    @GameTest(template = LONG, timeoutTicks = 60)
    public static void fanRangeFollowsPower(GameTestHelper helper) {
        var fan = fan(helper, 8, 128);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(fan.range() == 8, "range " + fan.range() + " on 1 kW");
            WeaponGameTests.spinningFlywheel(helper, new BlockPos(2, 2, 3), 64, 128, Direction.EAST);
        });
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(fan.range() == 8 + (8192 - 1024) / 1024, "range " + fan.range() + " on 8 kW");
            WeaponGameTests.spinningFlywheel(helper, new BlockPos(2, 2, 3), 4096, 1024, Direction.EAST);
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(fan.range() == 32, "range " + fan.range() + " not capped at 32");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60)
    public static void fanBelowItsMinimumPowerBlowsNothing(GameTestHelper helper) {
        var fan = fan(helper, 1, 512);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(fan.range() == 0 && fan.clippedRange() == 0, "blew on 512 W: " + fan.range());
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60)
    public static void aSolidBlockStopsTheBeam(GameTestHelper helper) {
        var fan = fan(helper, 64, 128);
        helper.setBlock(new BlockPos(8, 2, 3), Blocks.STONE);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(fan.clippedRange() == 5, "beam reaches " + fan.clippedRange() + ", not 5");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60)
    public static void cropsDoNotStopTheBeam(GameTestHelper helper) {
        var fan = fan(helper, 64, 128);
        helper.setBlock(new BlockPos(5, 2, 3), Blocks.WHEAT);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(fan.clippedRange() == fan.range(), "crops stopped the beam");
            helper.succeed();
        });
    }

    /** What is in the beam is flung along it. */
    @GameTest(template = LONG, timeoutTicks = 60)
    public static void fanBlowsWhatIsInTheBeam(GameTestHelper helper) {
        fan(helper, 64, 128);
        var cow = helper.spawn(EntityType.COW, new BlockPos(6, 2, 3));
        double x = cow.getX();
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(cow.getX() > x + 0.5, "the cow did not move: " + (cow.getX() - x));
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60)
    public static void fanDoesNotBlowOutsideItsBeam(GameTestHelper helper) {
        fan(helper, 64, 128);
        var cow = helper.spawn(EntityType.COW, new BlockPos(6, 2, 6));
        double x = cow.getX();
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(Math.abs(cow.getX() - x) < 0.3, "the cow beside the beam was moved");
            helper.succeed();
        });
    }

    /** Webs come away on a fast enough shaft, and are left alone on a slow one. */
    @GameTest(template = LONG, timeoutTicks = 80)
    public static void fanTearsWebsOnlyAtSpeed(GameTestHelper helper) {
        var fan = fan(helper, 64, 128);
        BlockPos web = new BlockPos(5, 2, 3);
        helper.setBlock(web, Blocks.COBWEB);
        helper.runAfterDelay(5, () -> {
            for (int i = 0; i < 4000; i++) {
                fan.rip(helper.getLevel(), helper.absolutePos(web));
            }
            helper.assertBlock(web, b -> b == Blocks.COBWEB, () -> "torn at 128 rad/s");
            WeaponGameTests.spinningFlywheel(helper, new BlockPos(2, 2, 3), 64, 512, Direction.EAST);
        });
        helper.runAfterDelay(15, () -> {
            for (int i = 0; i < 4000; i++) {
                fan.rip(helper.getLevel(), helper.absolutePos(web));
            }
            helper.assertBlock(web, b -> b == Blocks.AIR, () -> "not torn at 512 rad/s");
            helper.succeed();
        });
    }

    /** Ripe wheat is harvested (its seed kept for the next crop) and left to grow again. */
    @GameTest(template = LONG, timeoutTicks = 80)
    public static void fanHarvestsRipeWheat(GameTestHelper helper) {
        var fan = fan(helper, 64, 1024);
        BlockPos crop = new BlockPos(5, 2, 3);
        helper.setBlock(crop.below(), Blocks.FARMLAND);
        helper.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        helper.setBlock(new BlockPos(6, 1, 3), Blocks.FARMLAND);
        helper.setBlock(new BlockPos(6, 2, 3), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 2));
        helper.runAfterDelay(5, () -> {
            for (int i = 0; i < 4000; i++) {
                fan.rip(helper.getLevel(), helper.absolutePos(crop));
                fan.rip(helper.getLevel(), helper.absolutePos(new BlockPos(6, 2, 3)));
            }
            BlockState state = helper.getBlockState(crop);
            helper.assertTrue(state.is(Blocks.WHEAT) && state.getValue(CropBlock.AGE) == 0, "wheat not harvested and replanted: " + state);
            helper.assertTrue(helper.getBlockState(new BlockPos(6, 2, 3)).getValue(CropBlock.AGE) == 2, "unripe wheat was harvested");
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds());
            helper.assertTrue(drops.stream().anyMatch(i -> i.getItem().is(Items.WHEAT)), "no wheat dropped");
            helper.succeed();
        });
    }

    /** The mobGriefing rule, and with it any claim, stops the fan breaking things. */
    @GameTest(template = LONG, timeoutTicks = 80)
    public static void fanLeavesBlocksAloneWhereGriefingIsOff(GameTestHelper helper) {
        var fan = fan(helper, 64, 128);
        BlockPos web = new BlockPos(5, 2, 3);
        helper.setBlock(web, Blocks.COBWEB);
        boolean was = helper.getLevel().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, helper.getLevel().getServer());
        helper.runAfterDelay(5, () -> {
            for (int i = 0; i < 4000; i++) {
                fan.rip(helper.getLevel(), helper.absolutePos(web));
            }
            helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(was, helper.getLevel().getServer());
            helper.assertBlock(web, b -> b == Blocks.COBWEB, () -> "torn with mobGriefing off");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60)
    public static void fanDoesNothingWhenSwitchedOff(GameTestHelper helper) {
        RotaryConfig.override(FarmConfig.MACHINES.get("fan"), false);
        fan(helper, 64, 128);
        var cow = helper.spawn(EntityType.COW, new BlockPos(6, 2, 3));
        double x = cow.getX();
        helper.runAfterDelay(10, () -> {
            RotaryConfig.clearOverride(FarmConfig.MACHINES.get("fan"));
            helper.assertTrue(Math.abs(cow.getX() - x) < 0.3, "the fan blew while switched off");
            helper.succeed();
        });
    }

    /** A diffuser, right-clicked on, widens the beam and halves what each watt adds to its length. */
    @GameTest(template = LONG, timeoutTicks = 60)
    public static void aDiffuserWidensTheBeam(GameTestHelper helper) {
        var fan = fan(helper, 64, 128);
        var before = fan.beamBox(10);
        ItemStack diffuser = new ItemStack(RotaryParts.part("diffuser").get());
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        helper.assertTrue(fan.interact(player, diffuser), "the diffuser was not taken");
        helper.assertTrue(fan.isWide() && fan.beamBox(10).getYsize() > before.getYsize(), "beam not wider");
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(fan.range() == 8 + (8192 - 1024) / 2048, "range " + fan.range() + " with a diffuser");
            helper.succeed();
        });
    }

    // ---- Sprinkler and Lawn Sprinkler ----

    /** A pipe full of water above (Sprinkler) or below (Lawn Sprinkler): kept full each tick, as a pump would. */
    static void feed(GameTestHelper helper, BlockPos pipe, int amount) {
        helper.setBlock(pipe, RotaryBlocks.PIPES.get(PipeType.PIPE).get());
        helper.onEachTick(() -> {
            CompoundTag tag = new CompoundTag();
            tag.put("fluid", new FluidStack(Fluids.WATER, 1).save(helper.getLevel().registryAccess()));
            tag.putInt("amount", amount);
            helper.getBlockEntity(pipe).loadCustomOnly(tag, helper.getLevel().registryAccess());
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100)
    public static void aSprinklerWithWaterAndPressureReaches8Blocks(GameTestHelper helper) {
        BlockPos at = new BlockPos(8, 3, 3);
        helper.setBlock(at, FarmRegistry.SPRINKLER.get());
        feed(helper, at.above(), 500);
        SprinklerBlockEntity sprinkler = helper.getBlockEntity(at);
        helper.succeedWhen(() -> {
            helper.assertTrue(sprinkler.water() > 0 && sprinkler.range() == 8, "water " + sprinkler.water() + ", range " + sprinkler.range());
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60)
    public static void aSprinklerWithoutPressureDoesNothing(GameTestHelper helper) {
        BlockPos at = new BlockPos(8, 3, 3);
        helper.setBlock(at, FarmRegistry.SPRINKLER.get());
        SprinklerBlockEntity sprinkler = helper.getBlockEntity(at);
        sprinkler.tank().fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE);
        BlockPos farm = at.below(2).east();
        helper.setBlock(farm, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(sprinkler.range() == 0 && !sprinkler.canPerformEffects(), "it works without pressure");
            helper.assertTrue(helper.getBlockState(farm).getValue(FarmBlock.MOISTURE) == 0 || helper.getBlockState(farm).getValue(FarmBlock.MOISTURE) < 7,
                    "it wet the ground");
            helper.succeed();
        });
    }

    /** It wets farmland in its square, and puts out a fire there, and uses water doing it. */
    @GameTest(template = LONG, timeoutTicks = 400)
    public static void aSprinklerWetsFarmlandAndPutsOutFires(GameTestHelper helper) {
        BlockPos at = new BlockPos(8, 4, 3);
        helper.setBlock(at, FarmRegistry.SPRINKLER.get());
        feed(helper, at.above(), 500);
        BlockPos farm = new BlockPos(10, 2, 3);
        BlockPos fire = new BlockPos(6, 3, 4);
        BlockPos far = new BlockPos(17, 2, 3);
        helper.setBlock(new BlockPos(6, 2, 4), Blocks.OAK_PLANKS);
        helper.setBlock(farm, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
        helper.setBlock(far, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
        helper.setBlock(fire, Blocks.FIRE);
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(farm).getValue(FarmBlock.MOISTURE) == 7, "farmland still dry");
            helper.assertBlock(fire, b -> b == Blocks.AIR, () -> "fire still burning");
        });
    }

    @GameTest(template = LONG, timeoutTicks = 400)
    public static void aLawnSprinklerWetsFarmlandAndPutsOutFires(GameTestHelper helper) {
        BlockPos at = new BlockPos(8, 3, 3);
        helper.setBlock(at, FarmRegistry.LAWN_SPRINKLER.get());
        feed(helper, at.below(), 500);
        helper.setBlock(at.below(), RotaryBlocks.PIPES.get(PipeType.PIPE).get());
        BlockPos farm = new BlockPos(9, 2, 3);
        BlockPos fire = new BlockPos(8, 2, 5);
        helper.setBlock(farm, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
        helper.setBlock(fire.below(), Blocks.OAK_PLANKS);
        helper.setBlock(fire, Blocks.FIRE);
        helper.succeedWhen(() -> {
            LawnSprinklerBlockEntity lawn = helper.getBlockEntity(at);
            helper.assertTrue(lawn.range() > 0, "no pressure yet");
            helper.assertTrue(helper.getBlockState(farm).getValue(FarmBlock.MOISTURE) == 7, "farmland still dry");
            helper.assertBlock(fire, b -> b == Blocks.AIR, () -> "fire still burning");
        });
    }

    /** Only past 0.8 MPa does the lawn sprinkler's spray hurt, as the original, and the config can turn that off. */
    @GameTest(template = LONG, timeoutTicks = 100)
    public static void aLawnSprinklerHurtsOnlyUnderHighPressure(GameTestHelper helper) {
        BlockPos at = new BlockPos(8, 3, 3);
        helper.setBlock(at, FarmRegistry.LAWN_SPRINKLER.get());
        var cow = helper.spawn(EntityType.COW, new BlockPos(9, 3, 3));
        LawnSprinklerBlockEntity lawn = helper.getBlockEntity(at);
        helper.onEachTick(() -> {
            CompoundTag tag = new CompoundTag();
            tag.putInt("pressure", 700_000);
            tag.put("tank", lawn.tank().writeToNBT(helper.getLevel().registryAccess(), new CompoundTag()));
            lawn.tank().fill(new FluidStack(Fluids.WATER, 5), IFluidHandler.FluidAction.EXECUTE);
            lawn.loadCustomOnly(tag, helper.getLevel().registryAccess());
            lawn.tank().fill(new FluidStack(Fluids.WATER, 5), IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(cow.getHealth() == cow.getMaxHealth(), "hurt at 0.7 MPa");
            helper.succeed();
        });
    }

    // ---- Ground Hydrator ----

    @GameTest(template = LONG, timeoutTicks = 400)
    public static void aGroundHydratorWetsFarmlandAndPaysForIt(GameTestHelper helper) {
        BlockPos at = new BlockPos(8, 2, 3);
        helper.setBlock(at, FarmRegistry.GROUND_HYDRATOR.get());
        GroundHydratorBlockEntity hydrator = helper.getBlockEntity(at);
        hydrator.tank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        BlockPos farm = new BlockPos(9, 1, 3);
        helper.setBlock(farm, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(farm).getValue(FarmBlock.MOISTURE) == 7, "farmland still dry");
            helper.assertTrue(hydrator.tank().getFluidAmount() == 1000 - GroundHydratorBlockEntity.FLUID_PER_BLOCK, "paid " + (1000 - hydrator.tank().getFluidAmount()));
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100)
    public static void aGroundHydratorOnlyTakesWater(GameTestHelper helper) {
        BlockPos at = new BlockPos(8, 2, 3);
        helper.setBlock(at, FarmRegistry.GROUND_HYDRATOR.get());
        GroundHydratorBlockEntity hydrator = helper.getBlockEntity(at);
        helper.assertTrue(hydrator.tank().fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE) == 0, "took lava");
        helper.assertTrue(hydrator.tank().fill(new FluidStack(Fluids.WATER, 5000), IFluidHandler.FluidAction.EXECUTE) == GroundHydratorBlockEntity.CAPACITY,
                "water capacity");
        helper.succeed();
    }

    @GameTest(template = LONG, timeoutTicks = 60)
    public static void theHydratorsPatchIs13BySixteen(GameTestHelper helper) {
        helper.assertTrue(GroundHydratorBlockEntity.range() == 6, "range " + GroundHydratorBlockEntity.range());
        var random = net.minecraft.util.RandomSource.create(5);
        int near = 0;
        for (int i = 0; i < 5000; i++) {
            int[] pick = GroundHydratorBlockEntity.pick(random);
            helper.assertTrue(Math.abs(pick[0]) <= 6 && Math.abs(pick[1]) <= 6, "picked outside the patch");
            if (Math.abs(pick[0]) <= 2 && Math.abs(pick[1]) <= 2) {
                near++;
            }
        }
        helper.assertTrue(near > 5000 * 0.3, "the middle is not likelier: " + near);
        helper.succeed();
    }

    // ---- Fertilizer ----

    static FertilizerBlockEntity fertilizer(GameTestHelper helper, int torque, int omega) {
        BlockPos at = new BlockPos(8, 3, 3);
        WeaponGameTests.spinningFlywheel(helper, at.below(), torque, omega);
        helper.setBlock(at, FarmRegistry.FERTILIZER.get());
        return helper.getBlockEntity(at);
    }

    /** Range 2 x log2 of the torque; tries 4 x log2 of the speed. */
    @GameTest(template = LONG, timeoutTicks = 60)
    public static void aFertilizersRangeAndRateFollowTheShaft(GameTestHelper helper) {
        var fertilizer = fertilizer(helper, 64, 256);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(fertilizer.range() == 12, "range " + fertilizer.range());
            helper.assertTrue(fertilizer.updatesPerTick() == 32, "tries " + fertilizer.updatesPerTick());
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60)
    public static void aFertilizerOnlyTakesFertilizer(GameTestHelper helper) {
        var fertilizer = fertilizer(helper, 64, 256);
        helper.assertTrue(fertilizer.items().isItemValid(0, new ItemStack(Items.BONE_MEAL)), "no bone meal");
        helper.assertTrue(fertilizer.items().isItemValid(0, new ItemStack(RotaryItems.COMPOST.get())), "no compost");
        helper.assertTrue(!fertilizer.items().isItemValid(0, new ItemStack(Items.DIRT)), "took dirt");
        helper.succeed();
    }

    /** With water and bone meal it works on the plants round it, and pays in both. */
    @GameTest(template = LONG, timeoutTicks = 400)
    public static void aFertilizerUsesUpWaterAndFertilizerOnPlants(GameTestHelper helper) {
        var fertilizer = fertilizer(helper, 4, 4096);
        fertilizer.tank().fill(new FluidStack(Fluids.WATER, 6000), IFluidHandler.FluidAction.EXECUTE);
        fertilizer.items().setStackInSlot(0, new ItemStack(Items.BONE_MEAL, 16));
        for (int x = 6; x < 11; x++) {
            helper.setBlock(new BlockPos(x, 2, 5), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
            helper.setBlock(new BlockPos(x, 3, 5), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 0));
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(fertilizer.tank().getFluidAmount() < 6000, "no water used");
            helper.assertTrue(fertilizer.items().getStackInSlot(0).getCount() < 16, "no bone meal used");
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60)
    public static void aFertilizerDoesNothingWithoutWater(GameTestHelper helper) {
        var fertilizer = fertilizer(helper, 64, 256);
        fertilizer.items().setStackInSlot(0, new ItemStack(Items.BONE_MEAL, 16));
        helper.setBlock(new BlockPos(8, 2, 5), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        helper.setBlock(new BlockPos(8, 3, 5), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 0));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(!fertilizer.hasFertilizer() && fertilizer.items().getStackInSlot(0).getCount() == 16, "worked dry");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60)
    public static void farmMachinesDropWhatTheyHold(GameTestHelper helper) {
        var fertilizer = fertilizer(helper, 64, 256);
        fertilizer.items().setStackInSlot(3, new ItemStack(Items.BONE_MEAL, 5));
        helper.setBlock(new BlockPos(8, 3, 3), Blocks.AIR);
        helper.runAfterDelay(5, () -> {
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds());
            helper.assertTrue(drops.stream().anyMatch(i -> i.getItem().is(Items.BONE_MEAL) && i.getItem().getCount() == 5), "the bone meal was lost");
            helper.succeed();
        });
    }
}
