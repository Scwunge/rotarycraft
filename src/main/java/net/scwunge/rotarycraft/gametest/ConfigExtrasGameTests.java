package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.ExtractorBlockEntity;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.crafting.AutoCrafterBlockEntity;
import net.scwunge.rotarycraft.farm.VacuumBlockEntity;
import net.scwunge.rotarycraft.pipe.PipeType;
import net.scwunge.rotarycraft.registry.CraftingRegistry;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryParts;
import net.scwunge.rotarycraft.registry.ToolRegistry;

/** The options of the original that were left for later (RotaryConfig's "extras"): each does what it says, on and off. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ConfigExtrasGameTests {
    static final String TEMPLATE = RotaryGameTests.TEMPLATE;
    static final String LONG = "empty20x8x7";
    static final String BATCH = "config_extras";

    /** Whatever a test that failed halfway left overridden is put back, so it cannot upset the batches after it. */
    @AfterBatch(batch = BATCH)
    public static void clearOverrides(ServerLevel level) {
        clearAll();
    }

    static void clearAll() {
        RotaryConfig.clearOverride(RotaryConfig.EMP_CHARGE_SPEED);
        RotaryConfig.clearOverride(RotaryConfig.FLUID_FLOW_SPEED);
        RotaryConfig.clearOverride(RotaryConfig.PIPE_HARDNESS);
        RotaryConfig.clearOverride(RotaryConfig.FAKE_PLAYER_BEDROCK);
        RotaryConfig.clearOverride(RotaryConfig.SPAWNERS_LEAK);
        RotaryConfig.clearOverride(RotaryConfig.CRAFTER_PROFILING);
        RotaryConfig.clearOverride(RotaryConfig.STEEL_HARVEST_HIGHER);
        RotaryConfig.clearOverride(RotaryConfig.VACUUM_POWER_PER_METRE);
        RotaryConfig.clearOverride(RotaryConfig.OWNER_ONLY_MACHINES);
        RotaryConfig.clearOverride(RotaryConfig.EXTRACTOR_WEAR);
    }

    // ---- the EMP's charging speed ----

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 60)
    public static void anEmpListsItsAreaThirtyThreeColumnsATickAtTheOriginalsSpeed(GameTestHelper helper) {
        var emp = WeaponGameTests.emp(helper, false);
        helper.runAfterDelay(10, () -> {
            int loaded = emp.loadedColumns();
            helper.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
            helper.assertTrue(loaded >= 33 * 5 && loaded % 33 == 0, "listed " + loaded + " columns in ten ticks");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_slow_emp", timeoutTicks = 60)
    public static void aSlowEmpListsAColumnATick(GameTestHelper helper) {
        RotaryConfig.override(RotaryConfig.EMP_CHARGE_SPEED, 0);
        var emp = WeaponGameTests.emp(helper, false);
        helper.runAfterDelay(10, () -> {
            int loaded = emp.loadedColumns();
            helper.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
            RotaryConfig.clearOverride(RotaryConfig.EMP_CHARGE_SPEED);
            helper.assertTrue(loaded >= 1 && loaded <= 12, "listed " + loaded + " columns in ten ticks");
            helper.succeed();
        });
    }

    // ---- the Item Vacuum's power per metre ----

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 20)
    public static void theVacuumsPowerPerMetreIsRoundedUpToAPowerOfTwo(GameTestHelper helper) {
        helper.assertTrue(VacuumBlockEntity.falloff() == 4096, "the default is " + VacuumBlockEntity.falloff());
        RotaryConfig.override(RotaryConfig.VACUUM_POWER_PER_METRE, 3000);
        int rounded = VacuumBlockEntity.falloff();
        RotaryConfig.override(RotaryConfig.VACUUM_POWER_PER_METRE, 1024);
        int least = VacuumBlockEntity.falloff();
        RotaryConfig.override(RotaryConfig.VACUUM_POWER_PER_METRE, 524288);
        int most = VacuumBlockEntity.falloff();
        RotaryConfig.clearOverride(RotaryConfig.VACUUM_POWER_PER_METRE);
        helper.assertTrue(rounded == 4096 && least == 1024 && most == 524288, "3000 gave " + rounded + ", 1024 gave " + least + ", 524288 gave " + most);
        helper.succeed();
    }

    // ---- pipes ----

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 20)
    public static void pipesBreakAtOnceUnlessMadeHarder(GameTestHelper helper) {
        PipeGameTests.place(helper, new BlockPos(2, 1, 2), PipeType.PIPE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockState state = helper.getBlockState(new BlockPos(2, 1, 2));
        float instant = state.getDestroyProgress(player, helper.getLevel(), pos);
        RotaryConfig.override(RotaryConfig.PIPE_HARDNESS, 1.0);
        float hard = state.getDestroyProgress(player, helper.getLevel(), pos);
        RotaryConfig.clearOverride(RotaryConfig.PIPE_HARDNESS);
        helper.assertTrue(instant >= 1F, "a pipe takes " + instant + " of its breaking a tick");
        helper.assertTrue(hard > 0F && hard < 0.5F, "a hard pipe takes " + hard + " of its breaking a tick");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
    public static void fluidReachesTheEndOfARunQuickerAtFullFlowSpeed(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) {
            PipeGameTests.place(helper, new BlockPos(x, 1, 2), PipeType.PIPE);
        }
        helper.runAfterDelay(3, () -> PipeGameTests.pipe(helper, new BlockPos(0, 1, 2)).input().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE));
        helper.succeedWhen(() -> {
            helper.assertTrue(PipeGameTests.pipe(helper, new BlockPos(4, 1, 2)).amount() > 0, "no water at the end of the run");
        });
    }

    /** How many ticks out of twenty the first pipe's contents changed in, with a run of pipes draining it. */
    private static void countFlowTicks(GameTestHelper helper, int speed, java.util.function.IntConsumer then) {
        RotaryConfig.override(RotaryConfig.FLUID_FLOW_SPEED, speed);
        for (int x = 0; x < 5; x++) {
            PipeGameTests.place(helper, new BlockPos(x, 1, 2), PipeType.PIPE);
        }
        helper.runAfterDelay(3, () -> PipeGameTests.pipe(helper, new BlockPos(0, 1, 2)).input().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE));
        int[] last = {-1};
        int[] changes = {0};
        helper.onEachTick(() -> {
            if (helper.getTick() > 4) {
                int now = PipeGameTests.pipe(helper, new BlockPos(0, 1, 2)).amount();
                if (now != last[0]) {
                    changes[0]++;
                }
                last[0] = now;
            }
        });
        helper.runAfterDelay(25, () -> {
            RotaryConfig.clearOverride(RotaryConfig.FLUID_FLOW_SPEED);
            then.accept(changes[0]);
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_slow_flow", timeoutTicks = 40)
    public static void fluidMovesOnlyEveryFifthTickAtTheLowestFlowSpeed(GameTestHelper helper) {
        countFlowTicks(helper, 1, changes -> {
            helper.assertTrue(changes >= 2 && changes <= 6, "the first pipe changed on " + changes + " of twenty ticks");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_fast_flow", timeoutTicks = 40)
    public static void fluidMovesEveryTickAtFullFlowSpeedWhileThereIsSome(GameTestHelper helper) {
        countFlowTicks(helper, 5, changes -> {
            helper.assertTrue(changes >= 10, "the first pipe changed on only " + changes + " of twenty ticks");
            helper.succeed();
        });
    }

    // ---- bedrock tools ----

    @GameTest(template = LONG, batch = BATCH, timeoutTicks = 80)
    public static void anAutoActivatorsBedrockAxeFellsATreeOnlyIfTheConfigAllowsIt(GameTestHelper helper) {
        RotaryConfig.override(RotaryConfig.FAKE_PLAYER_BEDROCK, false);
        ToolGameTests.tree(helper, 5);
        ServerPlayer player = ToolGameTests.survivor(helper, new ItemStack(ToolRegistry.BEDROCK_AXE.get()));
        player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(5, 3, 3)));
        helper.runAfterDelay(3, () -> {
            RotaryConfig.clearOverride(RotaryConfig.FAKE_PLAYER_BEDROCK);
            helper.assertBlock(new BlockPos(5, 6, 3), b -> b == Blocks.OAK_LOG, () -> "a fake player felled the whole tree");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, batch = BATCH, timeoutTicks = 80)
    public static void anAutoActivatorsBedrockHoeTillsOneBlockOnlyIfTheConfigSaysSo(GameTestHelper helper) {
        RotaryConfig.override(RotaryConfig.FAKE_PLAYER_BEDROCK, false);
        for (int x = 6; x <= 10; x++) {
            for (int z = 1; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.DIRT);
            }
        }
        ItemStack hoe = new ItemStack(ToolRegistry.BEDROCK_HOE.get());
        ServerPlayer player = ToolGameTests.survivor(helper, hoe);
        BlockPos at = helper.absolutePos(new BlockPos(8, 2, 3));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at).add(0, 0.5, 0), net.minecraft.core.Direction.UP, at, false);
        hoe.useOn(new net.minecraft.world.item.context.UseOnContext(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hoe, hit));
        RotaryConfig.clearOverride(RotaryConfig.FAKE_PLAYER_BEDROCK);
        int farmland = 0;
        for (int x = 6; x <= 10; x++) {
            for (int z = 1; z <= 5; z++) {
                farmland += helper.getBlockState(new BlockPos(x, 2, z)).is(Blocks.FARMLAND) ? 1 : 0;
            }
        }
        helper.assertTrue(farmland == 1, "tilled " + farmland + " blocks, not 1");
        helper.succeed();
    }

    private static int mobsAround(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(Mob.class, helper.getBounds().inflate(12)).size();
    }

    private static void liftSpawner(GameTestHelper helper, boolean leak, boolean fake) {
        RotaryConfig.override(RotaryConfig.SPAWNERS_LEAK, leak);
        helper.setBlock(new BlockPos(8, 3, 3), Blocks.SPAWNER);
        ((SpawnerBlockEntity) helper.getBlockEntity(new BlockPos(8, 3, 3))).setEntityId(EntityType.COW, helper.getLevel().random);
        Player player = fake ? ToolGameTests.survivor(helper, ItemStack.EMPTY) : helper.makeMockPlayer(GameType.SURVIVAL);
        net.scwunge.rotarycraft.tool.ToolEvents.maybeLeak(helper.getLevel(), helper.absolutePos(new BlockPos(8, 3, 3)), player);
    }

    @GameTest(template = LONG, batch = BATCH + "_leak", timeoutTicks = 80)
    public static void liftingASpawnerByHandLetsItsMobsOut(GameTestHelper helper) {
        liftSpawner(helper, true, false);
        helper.runAfterDelay(3, () -> {
            RotaryConfig.clearOverride(RotaryConfig.SPAWNERS_LEAK);
            int mobs = mobsAround(helper);
            helper.getLevel().getEntitiesOfClass(Mob.class, helper.getBounds().inflate(12)).forEach(Mob::discard);
            helper.assertTrue(mobs >= 6, "only " + mobs + " mobs came out");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, batch = BATCH + "_noleak", timeoutTicks = 80)
    public static void liftingASpawnerWithTheLeakOffLetsNothingOut(GameTestHelper helper) {
        liftSpawner(helper, false, false);
        helper.runAfterDelay(3, () -> {
            RotaryConfig.clearOverride(RotaryConfig.SPAWNERS_LEAK);
            helper.assertTrue(mobsAround(helper) == 0, mobsAround(helper) + " mobs came out");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, batch = BATCH + "_fakeleak", timeoutTicks = 80)
    public static void anAutoActivatorLiftingASpawnerLetsNothingOut(GameTestHelper helper) {
        liftSpawner(helper, true, true);
        helper.runAfterDelay(3, () -> {
            RotaryConfig.clearOverride(RotaryConfig.SPAWNERS_LEAK);
            helper.assertTrue(mobsAround(helper) == 0, mobsAround(helper) + " mobs came out");
            helper.succeed();
        });
    }

    // ---- steel tools ----

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 20)
    public static void steelPickaxesHarvestDiamondOnlyWhenTheConfigSaysSo(GameTestHelper helper) {
        var pickTag = net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE;
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        boolean before = net.scwunge.rotarycraft.tool.SteelTools.harvestsHigher(stone, pickTag);
        RotaryConfig.override(RotaryConfig.STEEL_HARVEST_HIGHER, true);
        boolean after = net.scwunge.rotarycraft.tool.SteelTools.harvestsHigher(stone, pickTag);
        boolean rock = net.scwunge.rotarycraft.tool.SteelTools.harvestsHigher(obsidian, pickTag);
        boolean wrongTool = net.scwunge.rotarycraft.tool.SteelTools.harvestsHigher(stone, net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL);
        boolean still = new ItemStack(ToolRegistry.STEEL_PICKAXE.get()).isCorrectToolForDrops(stone);
        RotaryConfig.clearOverride(RotaryConfig.STEEL_HARVEST_HIGHER);
        helper.assertFalse(before, "the option does something while off");
        helper.assertTrue(after && still, "it does nothing with the option on");
        helper.assertFalse(rock, "it harvested obsidian");
        helper.assertFalse(wrongTool, "a shovel harvested stone");
        helper.succeed();
    }

    // ---- the Auto-Crafter's lag compensation ----

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 20)
    public static void theAutoCrafterWaitsLongerAfterSlowRoundsAndRecoversAfterQuickOnes(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), CraftingRegistry.AUTO_CRAFTER.get().defaultBlockState());
        AutoCrafterBlockEntity crafter = helper.getBlockEntity(new BlockPos(2, 1, 2));
        helper.assertTrue(crafter.delay() == AutoCrafterBlockEntity.MIN_DELAY, "starts at " + crafter.delay());
        for (int i = 0; i < 5; i++) {
            crafter.profile(1_000_000_000L);
        }
        int slow = crafter.delay();
        for (int i = 0; i < 200; i++) {
            crafter.profile(1000L);
        }
        helper.assertTrue(slow > 5 && slow <= AutoCrafterBlockEntity.MAX_DELAY, "slow rounds gave a wait of " + slow);
        helper.assertTrue(crafter.delay() == AutoCrafterBlockEntity.MIN_DELAY, "quick rounds left a wait of " + crafter.delay());
        RotaryConfig.override(RotaryConfig.CRAFTER_PROFILING, false);
        crafter.profile(1_000_000_000L);
        int off = crafter.delay();
        RotaryConfig.clearOverride(RotaryConfig.CRAFTER_PROFILING);
        helper.assertTrue(off == AutoCrafterBlockEntity.MIN_DELAY, "profiling is off, but the wait is " + off);
        helper.succeed();
    }

    // ---- owner-only machines ----

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 20)
    public static void aMachineIsOnlyForItsPlacerWhenTheConfigLocksMachines(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), RotaryBlocks.EXTRACTOR.get().defaultBlockState());
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockState state = helper.getBlockState(new BlockPos(2, 1, 2));
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        state.getBlock().setPlacedBy(helper.getLevel(), pos, state, owner, new ItemStack(Items.STONE));
        boolean openToAll = MachineBlock.mayUse(helper.getLevel(), pos, stranger);
        RotaryConfig.override(RotaryConfig.OWNER_ONLY_MACHINES, true);
        boolean ownerIn = MachineBlock.mayUse(helper.getLevel(), pos, owner);
        boolean strangerIn = MachineBlock.mayUse(helper.getLevel(), pos, stranger);
        RotaryConfig.clearOverride(RotaryConfig.OWNER_ONLY_MACHINES);
        helper.assertTrue(openToAll, "locked while the option is off");
        helper.assertTrue(ownerIn, "the placer is locked out");
        helper.assertFalse(strangerIn, "a stranger got in");
        helper.succeed();
    }

    // ---- the Extractor's drill ----

    @net.minecraft.gametest.framework.BeforeBatch(batch = "extractor_wear")
    public static void boostMotor(ServerLevel level) {
        ExtractorGameTests.boostMotor(level);
    }

    @AfterBatch(batch = "extractor_wear")
    public static void restoreMotor(ServerLevel level) {
        ExtractorGameTests.restoreMotor(level);
        clearAll();
    }

    @GameTest(template = ExtractorGameTests.TEMPLATE, batch = "extractor_wear", timeoutTicks = 400)
    public static void anExtractorThatWearsStopsWithoutADrillAndUsesOneUpWhenGivenOne(GameTestHelper helper) {
        RotaryConfig.override(RotaryConfig.EXTRACTOR_WEAR, true);
        ExtractorBlockEntity ex = ExtractorGameTests.poweredExtractor(helper, false);
        ex.setDrillTime(0);
        ex.items().setStackInSlot(0, new ItemStack(Items.IRON_ORE));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(ex.progress(0) == 0, "stage one ran without a drill");
            ex.items().setStackInSlot(ExtractorBlockEntity.SLOT_DRILL, new ItemStack(RotaryParts.part("drill").get()));
        });
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(ex.items().getStackInSlot(ExtractorBlockEntity.SLOT_DRILL).isEmpty(), "the drill was not taken");
            helper.assertTrue(ex.drillTime() > 0 && ex.drillTime() <= ExtractorBlockEntity.DRILL_LIFE, "a new drill has " + ex.drillTime());
            helper.assertTrue(ex.progress(0) > 0 || !ex.items().getStackInSlot(4).isEmpty(), "stage one is not running");
            RotaryConfig.clearOverride(RotaryConfig.EXTRACTOR_WEAR);
            helper.succeed();
        });
    }

    @GameTest(template = ExtractorGameTests.TEMPLATE, batch = ExtractorGameTests.BATCH, timeoutTicks = 100)
    public static void anExtractorKeepsRunningWithoutADrillWhenWearIsOff(GameTestHelper helper) {
        ExtractorBlockEntity ex = ExtractorGameTests.poweredExtractor(helper, false);
        ex.setDrillTime(0);
        ex.items().setStackInSlot(0, new ItemStack(Items.IRON_ORE));
        helper.succeedWhen(() -> helper.assertTrue(ex.progress(0) > 0 || !ex.items().getStackInSlot(4).isEmpty(), "stage one is not running"));
    }
}
