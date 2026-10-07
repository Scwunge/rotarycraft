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
import net.scwunge.rotarycraft.farm.AutoBreederBlockEntity;
import net.scwunge.rotarycraft.farm.BaitBoxBlockEntity;
import net.scwunge.rotarycraft.farm.BlowerBlockEntity;
import net.scwunge.rotarycraft.farm.DefoliatorBlockEntity;
import net.scwunge.rotarycraft.farm.MobHarvesterBlockEntity;
import net.scwunge.rotarycraft.farm.SpawnerControllerBlockEntity;
import net.scwunge.rotarycraft.farm.VacuumBlockEntity;
import net.scwunge.rotarycraft.farm.WoodcutterBlockEntity;
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
    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm")
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

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm")
    public static void fanBelowItsMinimumPowerBlowsNothing(GameTestHelper helper) {
        var fan = fan(helper, 1, 512);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(fan.range() == 0 && fan.clippedRange() == 0, "blew on 512 W: " + fan.range());
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm")
    public static void aSolidBlockStopsTheBeam(GameTestHelper helper) {
        var fan = fan(helper, 64, 128);
        helper.setBlock(new BlockPos(8, 2, 3), Blocks.STONE);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(fan.clippedRange() == 5, "beam reaches " + fan.clippedRange() + ", not 5");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm")
    public static void cropsDoNotStopTheBeam(GameTestHelper helper) {
        var fan = fan(helper, 64, 128);
        helper.setBlock(new BlockPos(5, 2, 3), Blocks.WHEAT);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(fan.clippedRange() == fan.range(), "crops stopped the beam");
            helper.succeed();
        });
    }

    /** What is in the beam is flung along it. */
    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm")
    public static void fanBlowsWhatIsInTheBeam(GameTestHelper helper) {
        fan(helper, 64, 128);
        var cow = helper.spawn(EntityType.COW, new BlockPos(6, 2, 3));
        double x = cow.getX();
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(cow.getX() > x + 0.5, "the cow did not move: " + (cow.getX() - x));
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm")
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
    @GameTest(template = LONG, timeoutTicks = 80, batch = "farm")
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
    @GameTest(template = LONG, timeoutTicks = 80, batch = "farm")
    public static void fanHarvestsRipeWheat(GameTestHelper helper) {
        var fan = fan(helper, 64, 1024);
        BlockPos crop = new BlockPos(5, 2, 3);
        helper.setBlock(crop.below(), Blocks.FARMLAND);
        helper.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        helper.setBlock(new BlockPos(6, 1, 3), Blocks.FARMLAND);
        helper.setBlock(new BlockPos(6, 2, 3), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 2));
        helper.runAfterDelay(15, () -> {
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
    @GameTest(template = LONG, timeoutTicks = 80, batch = "farm_griefing")
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

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm_fandoesnothingwhenswitchedoff")
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
    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm")
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

    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm")
    public static void aSprinklerWithWaterAndPressureReaches8Blocks(GameTestHelper helper) {
        BlockPos at = new BlockPos(8, 3, 3);
        helper.setBlock(at, FarmRegistry.SPRINKLER.get());
        feed(helper, at.above(), 500);
        SprinklerBlockEntity sprinkler = helper.getBlockEntity(at);
        helper.succeedWhen(() -> {
            helper.assertTrue(sprinkler.water() > 0 && sprinkler.range() == 8, "water " + sprinkler.water() + ", range " + sprinkler.range());
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm")
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
    @GameTest(template = LONG, timeoutTicks = 400, batch = "farm")
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

    @GameTest(template = LONG, timeoutTicks = 400, batch = "farm")
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
    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm")
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

    @GameTest(template = LONG, timeoutTicks = 400, batch = "farm")
    public static void aGroundHydratorWetsFarmlandAndPaysForIt(GameTestHelper helper) {
        BlockPos at = new BlockPos(8, 2, 3);
        helper.setBlock(at, FarmRegistry.GROUND_HYDRATOR.get());
        GroundHydratorBlockEntity hydrator = helper.getBlockEntity(at);
        hydrator.tank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        for (int x = 6; x <= 10; x++) {
            for (int z = 1; z <= 5; z++) {
                if (x != 8 || z != 3) {
                    helper.setBlock(new BlockPos(x, 1, z), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
                    helper.setBlock(new BlockPos(x, 1, z), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
                }
            }
        }
        helper.succeedWhen(() -> {
            int wet = 0;
            for (int x = 6; x <= 10; x++) {
                for (int z = 1; z <= 5; z++) {
                    BlockState state = helper.getBlockState(new BlockPos(x, 1, z));
                    if (state.is(Blocks.FARMLAND) && state.getValue(FarmBlock.MOISTURE) == 7) {
                        wet++;
                    }
                }
            }
            helper.assertTrue(wet >= 1, "farmland still dry");
            int paid = 1000 - hydrator.tank().getFluidAmount();
            helper.assertTrue(paid >= GroundHydratorBlockEntity.FLUID_PER_BLOCK && paid % GroundHydratorBlockEntity.FLUID_PER_BLOCK == 0, "paid " + paid);
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm")
    public static void aGroundHydratorOnlyTakesWater(GameTestHelper helper) {
        BlockPos at = new BlockPos(8, 2, 3);
        helper.setBlock(at, FarmRegistry.GROUND_HYDRATOR.get());
        GroundHydratorBlockEntity hydrator = helper.getBlockEntity(at);
        helper.assertTrue(hydrator.tank().fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE) == 0, "took lava");
        helper.assertTrue(hydrator.tank().fill(new FluidStack(Fluids.WATER, 5000), IFluidHandler.FluidAction.EXECUTE) == GroundHydratorBlockEntity.CAPACITY,
                "water capacity");
        helper.succeed();
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm")
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
    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm")
    public static void aFertilizersRangeAndRateFollowTheShaft(GameTestHelper helper) {
        var fertilizer = fertilizer(helper, 64, 256);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(fertilizer.range() == 12, "range " + fertilizer.range());
            helper.assertTrue(fertilizer.updatesPerTick() == 32, "tries " + fertilizer.updatesPerTick());
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm")
    public static void aFertilizerOnlyTakesFertilizer(GameTestHelper helper) {
        var fertilizer = fertilizer(helper, 64, 256);
        helper.assertTrue(fertilizer.items().isItemValid(0, new ItemStack(Items.BONE_MEAL)), "no bone meal");
        helper.assertTrue(fertilizer.items().isItemValid(0, new ItemStack(RotaryItems.COMPOST.get())), "no compost");
        helper.assertTrue(!fertilizer.items().isItemValid(0, new ItemStack(Items.DIRT)), "took dirt");
        helper.succeed();
    }

    /** With water and bone meal it works on the plants round it, and pays in both. */
    @GameTest(template = LONG, timeoutTicks = 900, batch = "farm")
    public static void aFertilizerUsesUpWaterAndFertilizerOnPlants(GameTestHelper helper) {
        var fertilizer = fertilizer(helper, 4, 4096);
        fertilizer.tank().fill(new FluidStack(Fluids.WATER, 6000), IFluidHandler.FluidAction.EXECUTE);
        fertilizer.items().setStackInSlot(0, new ItemStack(Items.BONE_MEAL, 16));
        for (int x = 6; x < 11; x++) {
            for (int z = 4; z <= 6; z++) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
                helper.setBlock(new BlockPos(x, 3, z), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 0));
            }
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(fertilizer.tank().getFluidAmount() < 6000, "no water used");
            helper.assertTrue(fertilizer.items().getStackInSlot(0).getCount() < 16, "no bone meal used");
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm")
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

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm")
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

    /** Succeeds when the checks pass, and only then puts the machine's switch back (they would be switched off after the first look otherwise). */
    static void succeedWhenThenReset(GameTestHelper helper, String machine, Runnable checks) {
        helper.succeedWhen(() -> {
            checks.run();
            RotaryConfig.clearOverride(FarmConfig.MACHINES.get(machine));
        });
    }

    // ---- Defoliator ----

    static DefoliatorBlockEntity defoliator(GameTestHelper helper, int torque, int omega) {
        RotaryConfig.override(FarmConfig.MACHINES.get("defoliator"), true);
        BlockPos at = new BlockPos(8, 3, 3);
        WeaponGameTests.spinningFlywheel(helper, at.below(), torque, omega);
        helper.setBlock(at, FarmRegistry.DEFOLIATOR.get());
        return helper.getBlockEntity(at);
    }

    static ItemStack poisonPotion() {
        return net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.POISON);
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm_adefoliatordrinkspoisonpotionsandleavesbottles")
    public static void aDefoliatorDrinksPoisonPotionsAndLeavesBottles(GameTestHelper helper) {
        var defoliator = defoliator(helper, 64, 256);
        helper.assertTrue(defoliator.items().isItemValid(0, poisonPotion()), "no poison potion");
        helper.assertTrue(!defoliator.items().isItemValid(0, new ItemStack(Items.POTION)), "took water");
        defoliator.items().setStackInSlot(0, poisonPotion());
        helper.runAfterDelay(10, () -> {
            RotaryConfig.clearOverride(FarmConfig.MACHINES.get("defoliator"));
            helper.assertTrue(defoliator.poison() == 1000 || defoliator.poison() > 0, "poison " + defoliator.poison());
            helper.assertTrue(defoliator.items().getStackInSlot(1).is(Items.GLASS_BOTTLE), "no bottle left");
            helper.assertTrue(defoliator.items().getStackInSlot(0).isEmpty(), "potion not used");
            helper.succeed();
        });
    }

    /** Foliage in its range is stripped, a millibucket of poison each, and nothing else is touched. */
    @GameTest(template = LONG, timeoutTicks = 200, batch = "farm_adefoliatorstripsfoliageandnothingelse")
    public static void aDefoliatorStripsFoliageAndNothingElse(GameTestHelper helper) {
        var defoliator = defoliator(helper, 2, 65536);
        defoliator.addPoison(500);
        for (int x = 6; x < 11; x++) {
            helper.setBlock(new BlockPos(x, 2, 5), Blocks.OAK_LEAVES);
        }
        helper.setBlock(new BlockPos(8, 2, 6), Blocks.STONE);
        helper.setBlock(new BlockPos(9, 2, 6), Blocks.OAK_PLANKS);
        succeedWhenThenReset(helper, "defoliator", () -> {
            helper.assertBlock(new BlockPos(8, 2, 5), b -> b == Blocks.AIR, () -> "leaves left");
            helper.assertBlock(new BlockPos(8, 2, 6), b -> b == Blocks.STONE, () -> "stone gone");
            helper.assertBlock(new BlockPos(9, 2, 6), b -> b == Blocks.OAK_PLANKS, () -> "planks gone");
            helper.assertTrue(defoliator.poison() < 500, "poison not used");
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm_adefoliatorwithoutpoisondoesnothing")
    public static void aDefoliatorWithoutPoisonDoesNothing(GameTestHelper helper) {
        var defoliator = defoliator(helper, 64, 256);
        helper.setBlock(new BlockPos(8, 2, 5), Blocks.OAK_LEAVES);
        helper.runAfterDelay(40, () -> {
            RotaryConfig.clearOverride(FarmConfig.MACHINES.get("defoliator"));
            helper.assertBlock(new BlockPos(8, 2, 5), b -> b == Blocks.OAK_LEAVES, () -> "stripped without poison");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm_adefoliatorisoffunlessswitchedon")
    public static void aDefoliatorIsOffUnlessSwitchedOn(GameTestHelper helper) {
        BlockPos at = new BlockPos(8, 3, 3);
        WeaponGameTests.spinningFlywheel(helper, at.below(), 64, 256);
        helper.setBlock(at, FarmRegistry.DEFOLIATOR.get());
        DefoliatorBlockEntity defoliator = helper.getBlockEntity(at);
        defoliator.addPoison(500);
        helper.setBlock(new BlockPos(8, 2, 5), Blocks.OAK_LEAVES);
        helper.runAfterDelay(40, () -> {
            helper.assertBlock(new BlockPos(8, 2, 5), b -> b == Blocks.OAK_LEAVES, () -> "it is on by default");
            helper.succeed();
        });
    }

    // ---- Item Pump ----

    static BlowerBlockEntity pump(GameTestHelper helper, int torque, int omega) {
        BlockPos at = new BlockPos(8, 2, 3);
        WeaponGameTests.spinningFlywheel(helper, at.below(), torque, omega);
        helper.setBlock(at, FarmRegistry.BLOWER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(at.west(), Blocks.CHEST);
        helper.setBlock(at.east(), Blocks.CHEST);
        return helper.getBlockEntity(at);
    }

    static net.minecraft.world.level.block.entity.ChestBlockEntity chest(GameTestHelper helper, int x) {
        return helper.getBlockEntity(new BlockPos(x, 2, 3));
    }

    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm")
    public static void anItemPumpMovesItemsFromBehindToInFront(GameTestHelper helper) {
        pump(helper, 4, 512);
        chest(helper, 7).setItem(0, new ItemStack(Items.COBBLESTONE, 40));
        helper.succeedWhen(() -> {
            helper.assertTrue(chest(helper, 9).countItem(Items.COBBLESTONE) == 40 && chest(helper, 7).isEmpty(), "moved " + chest(helper, 9).countItem(Items.COBBLESTONE));
        });
    }

    /** It moves one item a tick for each 1024 W: 2 kW, 2 items a tick. */
    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm")
    public static void anItemPumpsRateFollowsPower(GameTestHelper helper) {
        var pump = pump(helper, 4, 512);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(pump.itemsPerTick() == 2, "rate " + pump.itemsPerTick());
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm")
    public static void anItemPumpNeedsSpeedAndPower(GameTestHelper helper) {
        pump(helper, 64, 128);
        chest(helper, 7).setItem(0, new ItemStack(Items.COBBLESTONE, 40));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(chest(helper, 9).isEmpty(), "pumped at 128 rad/s");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm")
    public static void anItemPumpsPatternsPickWhatMoves(GameTestHelper helper) {
        var pump = pump(helper, 4, 512);
        pump.items().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
        pump.menuButton(helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL), BlowerBlockEntity.WHITELIST);
        chest(helper, 7).setItem(0, new ItemStack(Items.COBBLESTONE, 10));
        chest(helper, 7).setItem(1, new ItemStack(Items.DIRT, 10));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(chest(helper, 9).countItem(Items.COBBLESTONE) == 10 && chest(helper, 9).countItem(Items.DIRT) == 0, "whitelist: "
                    + chest(helper, 9).countItem(Items.COBBLESTONE) + " cobble, " + chest(helper, 9).countItem(Items.DIRT) + " dirt");
            helper.assertTrue(chest(helper, 7).countItem(Items.DIRT) == 10, "dirt was taken");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm")
    public static void anItemPumpsBlacklistHoldsBackWhatItNames(GameTestHelper helper) {
        var pump = pump(helper, 4, 512);
        pump.items().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
        chest(helper, 7).setItem(0, new ItemStack(Items.COBBLESTONE, 10));
        chest(helper, 7).setItem(1, new ItemStack(Items.DIRT, 10));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(chest(helper, 9).countItem(Items.DIRT) == 10 && chest(helper, 9).countItem(Items.COBBLESTONE) == 0, "blacklist");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm")
    public static void anItemPumpWithNothingInFrontSpraysItemsOut(GameTestHelper helper) {
        var pump = pump(helper, 4, 512);
        helper.setBlock(new BlockPos(9, 2, 3), Blocks.AIR);
        chest(helper, 7).setItem(0, new ItemStack(Items.COBBLESTONE, 4));
        helper.runAfterDelay(4, () -> {
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds());
            helper.assertTrue(drops.stream().mapToInt(i -> i.getItem().getCount()).sum() == 4, "sprayed " + drops.size() + ", chest has " + chest(helper, 7).countItem(Items.COBBLESTONE) + ", omega " + pump.getOmega() + ", front " + pump.front());
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm")
    public static void itemPumpsPassItemsAlongALine(GameTestHelper helper) {
        var pump = pump(helper, 4, 512);
        BlockPos second = new BlockPos(9, 2, 3);
        WeaponGameTests.spinningFlywheel(helper, second.below(), 4, 512);
        helper.setBlock(second, FarmRegistry.BLOWER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(10, 2, 3), Blocks.CHEST);
        chest(helper, 7).setItem(0, new ItemStack(Items.COBBLESTONE, 10));
        helper.succeedWhen(() -> {
            helper.assertTrue(chest(helper, 10).countItem(Items.COBBLESTONE) == 10, "items did not pass along the line");
        });
    }

    // ---- Item Vacuum ----

    static VacuumBlockEntity vacuum(GameTestHelper helper, int torque, int omega) {
        BlockPos at = new BlockPos(8, 3, 3);
        WeaponGameTests.spinningFlywheel(helper, at.below(), torque, omega);
        helper.setBlock(at, FarmRegistry.VACUUM.get());
        return helper.getBlockEntity(at);
    }

    @GameTest(template = LONG, timeoutTicks = 200, batch = "farm")
    public static void aVacuumPullsInDroppedItemsAndXp(GameTestHelper helper) {
        var vacuum = vacuum(helper, 64, 512);
        var item = new ItemEntity(helper.getLevel(), helper.absolutePos(new BlockPos(13, 3, 3)).getX() + 0.5, helper.absolutePos(new BlockPos(13, 3, 3)).getY(),
                helper.absolutePos(new BlockPos(13, 3, 3)).getZ() + 0.5, new ItemStack(Items.DIAMOND, 3));
        item.setDeltaMovement(0, 0, 0);
        helper.getLevel().addFreshEntity(item);
        var orb = new net.minecraft.world.entity.ExperienceOrb(helper.getLevel(), helper.absolutePos(new BlockPos(4, 3, 3)).getX() + 0.5,
                helper.absolutePos(new BlockPos(4, 3, 3)).getY(), helper.absolutePos(new BlockPos(4, 3, 3)).getZ() + 0.5, 7);
        helper.getLevel().addFreshEntity(orb);
        helper.succeedWhen(() -> {
            helper.assertTrue(net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(vacuum.items(), ItemStack.EMPTY, true).isEmpty()
                    && countItems(vacuum, Items.DIAMOND) == 3, "diamonds: " + countItems(vacuum, Items.DIAMOND));
            helper.assertTrue(vacuum.experience() == 7, "xp " + vacuum.experience());
        });
    }

    static int countItems(VacuumBlockEntity vacuum, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < vacuum.items().getSlots(); i++) {
            if (vacuum.items().getStackInSlot(i).is(item)) {
                n += vacuum.items().getStackInSlot(i).getCount();
            }
        }
        return n;
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm")
    public static void aVacuumsRangeFollowsPower(GameTestHelper helper) {
        var vacuum = vacuum(helper, 64, 512);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(vacuum.range() == 8 + 32768 / 4096, "range " + vacuum.range());
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm")
    public static void aVacuumEmptiesTheChestBesideIt(GameTestHelper helper) {
        var vacuum = vacuum(helper, 64, 512);
        helper.setBlock(new BlockPos(9, 3, 3), Blocks.CHEST);
        chest3(helper, 9).setItem(0, new ItemStack(Items.IRON_INGOT, 20));
        helper.succeedWhen(() -> {
            helper.assertTrue(countItems(vacuum, Items.IRON_INGOT) == 20, "took " + countItems(vacuum, Items.IRON_INGOT));
        });
    }

    static net.minecraft.world.level.block.entity.ChestBlockEntity chest3(GameTestHelper helper, int x) {
        return helper.getBlockEntity(new BlockPos(x, 3, 3));
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm")
    public static void aVacuumDoesNothingBelowItsMinimumPower(GameTestHelper helper) {
        var vacuum = vacuum(helper, 4, 512);
        var item = new ItemEntity(helper.getLevel(), helper.absolutePos(new BlockPos(8, 3, 3)).getX() + 0.5, helper.absolutePos(new BlockPos(8, 3, 3)).getY() + 0.1,
                helper.absolutePos(new BlockPos(8, 3, 3)).getZ() + 0.5, new ItemStack(Items.DIAMOND));
        helper.getLevel().addFreshEntity(item);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(countItems(vacuum, Items.DIAMOND) == 0, "took an item without power");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm")
    public static void aBrokenVacuumSpillsItsExperience(GameTestHelper helper) {
        var vacuum = vacuum(helper, 64, 512);
        var tag = new CompoundTag();
        tag.putInt("xp", 30);
        vacuum.loadCustomOnly(tag, helper.getLevel().registryAccess());
        helper.setBlock(new BlockPos(8, 3, 3), Blocks.AIR);
        helper.runAfterDelay(5, () -> {
            var orbs = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, helper.getBounds());
            helper.assertTrue(orbs.stream().mapToInt(net.minecraft.world.entity.ExperienceOrb::getValue).sum() == 30, "orbs " + orbs.size());
            helper.succeed();
        });
    }

    // ---- Auto-Breeder ----

    static AutoBreederBlockEntity breeder(GameTestHelper helper) {
        BlockPos at = new BlockPos(8, 3, 3);
        WeaponGameTests.spinningFlywheel(helper, at.below(), 16, 2048);
        helper.setBlock(at, FarmRegistry.AUTO_BREEDER.get());
        return helper.getBlockEntity(at);
    }

    @GameTest(template = LONG, timeoutTicks = 200, batch = "farm")
    public static void anAutoBreederPutsAnimalsInLoveAndUsesFood(GameTestHelper helper) {
        var breeder = breeder(helper);
        breeder.items().setStackInSlot(0, new ItemStack(Items.WHEAT, 4));
        var cow = helper.spawn(EntityType.COW, new BlockPos(8, 3, 4));
        helper.succeedWhen(() -> {
            helper.assertTrue(cow.isInLove(), "not in love");
            helper.assertTrue(breeder.items().getStackInSlot(0).getCount() == 3, "wheat left " + breeder.items().getStackInSlot(0).getCount());
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm")
    public static void anAutoBreederOnlyFeedsWhatTheAnimalEats(GameTestHelper helper) {
        var breeder = breeder(helper);
        breeder.items().setStackInSlot(0, new ItemStack(Items.WHEAT_SEEDS, 4));
        var cow = helper.spawn(EntityType.COW, new BlockPos(8, 3, 4));
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(!cow.isInLove() && breeder.items().getStackInSlot(0).getCount() == 4, "fed a cow seeds");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm")
    public static void anAutoBreederTakesOnlyFood(GameTestHelper helper) {
        var breeder = breeder(helper);
        helper.assertTrue(breeder.items().isItemValid(0, new ItemStack(Items.WHEAT)), "no wheat");
        helper.assertTrue(breeder.items().isItemValid(0, new ItemStack(net.scwunge.rotarycraft.registry.RotaryItems.CANOLA_HUSKS.get())), "no canola husks");
        helper.assertTrue(!breeder.items().isItemValid(0, new ItemStack(Items.DIRT)), "took dirt");
        helper.succeed();
    }

    // ---- Bait Box ----

    static BaitBoxBlockEntity baitBox(GameTestHelper helper) {
        BlockPos at = new BlockPos(8, 3, 3);
        WeaponGameTests.spinningFlywheel(helper, at.below(), 16, 4096);
        helper.setBlock(at, FarmRegistry.BAIT_BOX.get());
        return helper.getBlockEntity(at);
    }

    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm")
    public static void aBaitBoxDrawsInTheCreaturesItHoldsBaitFor(GameTestHelper helper) {
        var box = baitBox(helper);
        box.items().setStackInSlot(0, new ItemStack(Items.WHEAT));
        var cow = helper.spawn(EntityType.COW, new BlockPos(16, 3, 3));
        var pig = helper.spawn(EntityType.PIG, new BlockPos(16, 3, 5));
        double cowX = cow.getX(), pigX = pig.getX();
        helper.runAfterDelay(3, () -> helper.assertTrue(box.canAttract(cow) && !box.canRepel(cow) && !box.canAttract(pig), "wrong bait read"));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(cow.getX() < cowX - 1, "the cow did not come: " + (cowX - cow.getX()));
            helper.assertTrue(Math.abs(pig.getX() - pigX) < 1.5, "the pig was drawn");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm")
    public static void aBaitBoxDrivesOffTheCreaturesItHoldsRepellentFor(GameTestHelper helper) {
        var box = baitBox(helper);
        box.items().setStackInSlot(0, new ItemStack(Items.STICK));
        var cow = helper.spawn(EntityType.COW, new BlockPos(11, 3, 3));
        double cowX = cow.getX();
        helper.runAfterDelay(3, () -> helper.assertTrue(box.canRepel(cow) && !box.canAttract(cow), "wrong bait read"));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(cow.getX() > cowX + 1, "the cow did not go: " + (cow.getX() - cowX));
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm")
    public static void aBaitBoxHatchesEggsBesideFire(GameTestHelper helper) {
        var box = baitBox(helper);
        box.items().setStackInSlot(0, new ItemStack(Items.EGG, 2));
        helper.setBlock(new BlockPos(9, 2, 3), Blocks.NETHERRACK);
        helper.setBlock(new BlockPos(9, 3, 3), Blocks.FIRE);
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Chicken.class, helper.getBounds()).size() >= 1, "no chicken");
            helper.assertTrue(box.items().getStackInSlot(0).getCount() < 2, "egg not used");
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm")
    public static void aBaitBoxsRangeFollowsPower(GameTestHelper helper) {
        var box = baitBox(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(box.range() == 8 + (65536 - 32768) / 4096, "range " + box.range());
            helper.succeed();
        });
    }

    // ---- Mob Harvester ----

    static MobHarvesterBlockEntity harvester(GameTestHelper helper, boolean owned) {
        RotaryConfig.override(FarmConfig.MACHINES.get("mobHarvester"), true);
        BlockPos at = new BlockPos(8, 2, 3);
        WeaponGameTests.spinningFlywheel(helper, at.below(), 16, 1024);
        helper.setBlock(at, FarmRegistry.MOB_HARVESTER.get());
        MobHarvesterBlockEntity harvester = helper.getBlockEntity(at);
        if (owned) {
            harvester.setOwner(helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL));
        }
        return harvester;
    }

    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm_amobharvesterkillswhatstandsinitsbeam")
    public static void aMobHarvesterKillsWhatStandsInItsBeam(GameTestHelper helper) {
        var harvester = harvester(helper, true);
        var pig = helper.spawn(EntityType.PIG, new BlockPos(8, 3, 3));
        var cow = helper.spawn(EntityType.COW, new BlockPos(11, 3, 3));
        var villager = helper.spawn(EntityType.VILLAGER, new BlockPos(8, 4, 3));
        helper.runAfterDelay(40, () -> {
            RotaryConfig.clearOverride(FarmConfig.MACHINES.get("mobHarvester"));
            helper.assertTrue(!pig.isAlive(), "the pig lives");
            helper.assertTrue(cow.isAlive() && cow.getHealth() == cow.getMaxHealth(), "the cow outside the beam was hurt");
            helper.assertTrue(villager.isAlive() && villager.getHealth() == villager.getMaxHealth(), "the villager was hurt");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm_amobharvesterwithnoownerdoesnothing")
    public static void aMobHarvesterWithNoOwnerDoesNothing(GameTestHelper helper) {
        harvester(helper, false);
        var pig = helper.spawn(EntityType.PIG, new BlockPos(8, 3, 3));
        helper.runAfterDelay(40, () -> {
            RotaryConfig.clearOverride(FarmConfig.MACHINES.get("mobHarvester"));
            helper.assertTrue(pig.isAlive() && pig.getHealth() == pig.getMaxHealth(), "an unowned harvester hurt the pig");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm_amobharvesterisoffunlessswitchedon")
    public static void aMobHarvesterIsOffUnlessSwitchedOn(GameTestHelper helper) {
        BlockPos at = new BlockPos(8, 2, 3);
        WeaponGameTests.spinningFlywheel(helper, at.below(), 16, 1024);
        helper.setBlock(at, FarmRegistry.MOB_HARVESTER.get());
        MobHarvesterBlockEntity harvester = helper.getBlockEntity(at);
        harvester.setOwner(helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL));
        var pig = helper.spawn(EntityType.PIG, new BlockPos(8, 3, 3));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(pig.isAlive() && pig.getHealth() == pig.getMaxHealth(), "it is on by default");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm_amobharvestersdamagegrowswithpower")
    public static void aMobHarvestersDamageGrowsWithPower(GameTestHelper helper) {
        var harvester = harvester(helper, true);
        helper.runAfterDelay(5, () -> {
            RotaryConfig.clearOverride(FarmConfig.MACHINES.get("mobHarvester"));
            int low = harvester.damage();
            WeaponGameTests.spinningFlywheel(helper, new BlockPos(8, 1, 3), 1024, 4096);
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(harvester.damage() > low && low >= 6, "damage " + low + " then " + harvester.damage());
                helper.succeed();
            });
        });
    }

    // ---- Spawner Controller ----

    static SpawnerControllerBlockEntity spawnerController(GameTestHelper helper, boolean spawnerBelow) {
        RotaryConfig.override(FarmConfig.MACHINES.get("spawnerController"), true);
        BlockPos at = new BlockPos(8, 3, 3);
        if (spawnerBelow) {
            helper.setBlock(at.below(), Blocks.SPAWNER);
            net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner = helper.getBlockEntity(at.below());
            spawner.setEntityId(EntityType.PIG, helper.getLevel().getRandom());
        }
        WeaponGameTests.spinningFlywheel(helper, at.east(), 1, 1048576, Direction.WEST);
        helper.setBlock(at, FarmRegistry.SPAWNER_CONTROLLER.get());
        SpawnerControllerBlockEntity controller = helper.getBlockEntity(at);
        controller.menuButton(helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL), SpawnerControllerBlockEntity.SET_DELAY + 5);
        return controller;
    }

    @GameTest(template = LONG, timeoutTicks = 200, batch = "farm_aspawnercontrollerrunsthespawneronitsowndelay")
    public static void aSpawnerControllerRunsTheSpawnerOnItsOwnDelay(GameTestHelper helper) {
        var controller = spawnerController(helper, true);
        succeedWhenThenReset(helper, "spawnerController", () -> {
            var pigs = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class, helper.getBounds());
            helper.assertTrue(pigs.size() >= 4, "pigs " + pigs.size());
            helper.assertTrue(pigs.get(0).getPersistentData().getBoolean(SpawnerControllerBlockEntity.CONTROLLED_TAG), "not flagged");
            helper.assertTrue(controller.isValidLocation(), "no spawner");
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm")
    public static void aDisabledSpawnerControllerSpawnsNothing(GameTestHelper helper) {
        var controller = spawnerController(helper, true);
        controller.menuButton(helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL), SpawnerControllerBlockEntity.TOGGLE);
        helper.runAfterDelay(60, () -> {
            RotaryConfig.clearOverride(FarmConfig.MACHINES.get("spawnerController"));
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class, helper.getBounds()).isEmpty(), "spawned while disabled");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm_aspawnercontrollerneedsaspawnerbelow")
    public static void aSpawnerControllerNeedsASpawnerBelow(GameTestHelper helper) {
        var controller = spawnerController(helper, false);
        helper.runAfterDelay(20, () -> {
            RotaryConfig.clearOverride(FarmConfig.MACHINES.get("spawnerController"));
            helper.assertTrue(!controller.isValidLocation(), "found a spawner");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "farm_aspawnercontrollersdelayfollowstheshaft")
    public static void aSpawnerControllersDelayFollowsTheShaft(GameTestHelper helper) {
        var controller = spawnerController(helper, true);
        controller.menuButton(helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL), SpawnerControllerBlockEntity.SET_DELAY + 0);
        helper.runAfterDelay(5, () -> {
            RotaryConfig.clearOverride(FarmConfig.MACHINES.get("spawnerController"));
            helper.assertTrue(controller.operationTime() == 0, "operation time " + controller.operationTime());
            helper.succeed();
        });
    }

    // ---- Woodcutter ----

    static WoodcutterBlockEntity woodcutter(GameTestHelper helper, int omega) {
        RotaryConfig.override(FarmConfig.MACHINES.get("woodcutter"), true);
        BlockPos at = new BlockPos(8, 3, 3);
        WeaponGameTests.spinningFlywheel(helper, at.west(), 64, omega, Direction.EAST);
        helper.setBlock(at, FarmRegistry.WOODCUTTER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(at.below(), Blocks.CHEST);
        helper.setBlock(new BlockPos(9, 2, 3), Blocks.DIRT);
        WoodcutterBlockEntity cutter = helper.getBlockEntity(at);
        cutter.setOwner(helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL));
        return cutter;
    }

    /** A small oak: a trunk of three logs from the machine's height, with natural leaves round its top. */
    static void oak(GameTestHelper helper) {
        for (int y = 3; y <= 5; y++) {
            helper.setBlock(new BlockPos(9, y, 3), Blocks.OAK_LOG);
        }
        for (int x = 8; x <= 10; x++) {
            for (int z = 2; z <= 4; z++) {
                for (int y = 5; y <= 6; y++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).isAir()) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, false));
                    }
                }
            }
        }
    }

    @GameTest(template = LONG, timeoutTicks = 400, batch = "farm_woodcutter_cuts")
    public static void aWoodcutterTakesDownATreeAndDeliversTheWood(GameTestHelper helper) {
        woodcutter(helper, 512);
        oak(helper);
        succeedWhenThenReset(helper, "woodcutter", () -> {
            helper.assertBlock(new BlockPos(9, 3, 3), b -> b != Blocks.OAK_LOG, () -> "root still standing");
            helper.assertBlock(new BlockPos(9, 5, 3), b -> b == Blocks.AIR, () -> "top log still standing");
            helper.assertBlock(new BlockPos(10, 6, 4), b -> b == Blocks.AIR, () -> "leaves still hanging");
            helper.assertTrue(((net.minecraft.world.level.block.entity.ChestBlockEntity) helper.getBlockEntity(new BlockPos(8, 2, 3))).countItem(Items.OAK_LOG) == 3,
                    "the chest has " + ((net.minecraft.world.level.block.entity.ChestBlockEntity) helper.getBlockEntity(new BlockPos(8, 2, 3))).countItem(Items.OAK_LOG) + " logs");
        });
    }

    /** Leaves go first, and the trunk comes down from the top. */
    @GameTest(template = LONG, timeoutTicks = 400, batch = "farm_woodcutter_order")
    public static void aWoodcutterTakesTheTopFirst(GameTestHelper helper) {
        var cutter = woodcutter(helper, 512);
        oak(helper);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(cutter.remaining() > 0, "it cut the lot already");
            helper.assertBlock(new BlockPos(9, 3, 3), b -> b == Blocks.OAK_LOG, () -> "the root went first");
            RotaryConfig.clearOverride(FarmConfig.MACHINES.get("woodcutter"));
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 500, batch = "farm_woodcutter_replant")
    public static void aWoodcutterReplantsFromItsSapling(GameTestHelper helper) {
        var cutter = woodcutter(helper, 512);
        cutter.items().setStackInSlot(0, new ItemStack(Items.OAK_SAPLING, 2));
        oak(helper);
        succeedWhenThenReset(helper, "woodcutter", () -> {
            helper.assertBlock(new BlockPos(9, 3, 3), b -> b == Blocks.OAK_SAPLING, () -> "nothing planted");
            helper.assertTrue(cutter.items().getStackInSlot(0).getCount() >= 1, "the sapling slot is empty");
        });
    }

    @GameTest(template = LONG, timeoutTicks = 500, batch = "farm_woodcutter_infinity")
    public static void aWoodcutterWithInfinityKeepsItsSapling(GameTestHelper helper) {
        var cutter = woodcutter(helper, 512);
        cutter.items().setStackInSlot(0, new ItemStack(Items.OAK_SAPLING, 1));
        cutter.enchantments().set(net.minecraft.world.item.enchantment.Enchantments.INFINITY, 1);
        oak(helper);
        succeedWhenThenReset(helper, "woodcutter", () -> {
            helper.assertBlock(new BlockPos(9, 3, 3), b -> b == Blocks.OAK_SAPLING, () -> "nothing planted");
            helper.assertTrue(cutter.items().getStackInSlot(0).getCount() == 1, "the sapling was used up");
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm_woodcutter_none")
    public static void aWoodcutterWithNoTreeDoesNothing(GameTestHelper helper) {
        var cutter = woodcutter(helper, 512);
        helper.setBlock(new BlockPos(9, 4, 3), Blocks.OAK_LOG);
        helper.runAfterDelay(40, () -> {
            RotaryConfig.clearOverride(FarmConfig.MACHINES.get("woodcutter"));
            helper.assertBlock(new BlockPos(9, 4, 3), b -> b == Blocks.OAK_LOG, () -> "cut a log above its reach");
            helper.assertTrue(!cutter.hasWood(), "found wood");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm_woodcutter_off")
    public static void aWoodcutterIsOffUnlessSwitchedOn(GameTestHelper helper) {
        BlockPos at = new BlockPos(8, 3, 3);
        WeaponGameTests.spinningFlywheel(helper, at.west(), 64, 512, Direction.EAST);
        helper.setBlock(at, FarmRegistry.WOODCUTTER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        ((WoodcutterBlockEntity) helper.getBlockEntity(at)).setOwner(helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL));
        helper.setBlock(new BlockPos(9, 3, 3), Blocks.OAK_LOG);
        helper.runAfterDelay(40, () -> {
            helper.assertBlock(new BlockPos(9, 3, 3), b -> b == Blocks.OAK_LOG, () -> "it is on by default");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 100, batch = "farm_woodcutter_player_leaves")
    public static void aWoodcutterLeavesPlayerBuiltLeavesAlone(GameTestHelper helper) {
        woodcutter(helper, 512);
        helper.setBlock(new BlockPos(9, 3, 3), Blocks.OAK_LOG);
        helper.setBlock(new BlockPos(9, 4, 3), Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        helper.runAfterDelay(60, () -> {
            RotaryConfig.clearOverride(FarmConfig.MACHINES.get("woodcutter"));
            helper.assertBlock(new BlockPos(9, 3, 3), b -> b == Blocks.AIR, () -> "log still there");
            helper.assertBlock(new BlockPos(9, 4, 3), b -> b == Blocks.OAK_LEAVES, () -> "took a placed leaf block");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60)
    public static void aTreesSaplingIsFoundFromItsLog(GameTestHelper helper) {
        helper.assertTrue(WoodcutterBlockEntity.saplingFor(Blocks.OAK_LOG).equals(net.minecraft.resources.ResourceLocation.withDefaultNamespace("oak_sapling")), "oak");
        helper.assertTrue(WoodcutterBlockEntity.saplingFor(Blocks.STRIPPED_BIRCH_WOOD).equals(net.minecraft.resources.ResourceLocation.withDefaultNamespace("birch_sapling")), "birch");
        helper.assertTrue(WoodcutterBlockEntity.saplingFor(Blocks.MANGROVE_LOG).equals(net.minecraft.resources.ResourceLocation.withDefaultNamespace("mangrove_propagule")), "mangrove");
        helper.assertTrue(WoodcutterBlockEntity.saplingFor(Blocks.CRIMSON_STEM) == null, "crimson stem has no sapling");
        helper.succeed();
    }

    @GameTest(template = LONG, timeoutTicks = 60)
    public static void aWoodcuttersSpeedFollowsTheShaft(GameTestHelper helper) {
        var cutter = woodcutter(helper, 512);
        helper.runAfterDelay(5, () -> {
            RotaryConfig.clearOverride(FarmConfig.MACHINES.get("woodcutter"));
            helper.assertTrue(cutter.operationTime() == 3 && cutter.operations() == 1, "time " + cutter.operationTime() + ", ops " + cutter.operations());
            helper.succeed();
        });
    }
}
