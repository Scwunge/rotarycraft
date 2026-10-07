package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.PileDriverBlockEntity;
import net.scwunge.rotarycraft.registry.DecorRegistry;

/** The Pile Driver, on a stack of layers of its own making: the machine sits at the top of a 5 by 5 test, and the layers under it are what it works on. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class PileDriverGameTests {
    static final String SMALL = RotaryGameTests.TEMPLATE;
    static final BlockPos MACHINE = new BlockPos(2, 3, 2);
    static final BlockPos CENTER = new BlockPos(2, 2, 2);

    /** Fills the layer at {@code y} with {@code block}. */
    static void layer(GameTestHelper helper, int y, net.minecraft.world.level.block.Block block) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, y, z), block);
            }
        }
    }

    /** The switch on, and the pile no deeper than two layers, so it cannot leave pipe below the test where nothing cleans it up. */
    static Runnable enabled() {
        Runnable restore = DecorGameTests.enable("pileDriver");
        net.scwunge.rotarycraft.config.MachineConfig.override(net.scwunge.rotarycraft.config.MachineConfig.PILE_DRIVER_DEPTH, 2);
        return () -> {
            restore.run();
            net.scwunge.rotarycraft.config.MachineConfig.clearOverride(net.scwunge.rotarycraft.config.MachineConfig.PILE_DRIVER_DEPTH);
        };
    }

    /** 80 kN*m is the least torque; at 16 rad/s it strikes every three ticks. */
    static PileDriverBlockEntity driver(GameTestHelper helper, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, MACHINE.west(), torque, omega, Direction.EAST);
        helper.setBlock(MACHINE, DecorRegistry.PILE_DRIVER.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        return helper.getBlockEntity(MACHINE);
    }

    @GameTest(template = SMALL, batch = "pile_smash", timeoutTicks = 100)
    public static void pileDriverSmashesStoneAndLaysPile(GameTestHelper helper) {
        Runnable restore = enabled();
        layer(helper, 2, Blocks.STONE);
        PileDriverBlockEntity driver = driver(helper, 80_000, 16);
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(helper.getBlockState(new BlockPos(1, 2, 2)).is(Blocks.COBBLESTONE), "stone should be cobblestone after a stroke");
            helper.assertTrue(driver.depth() == 0, "the layer is not clear yet");
        });
        helper.runAfterDelay(30, () -> {
            restore.run();
            helper.assertTrue(helper.getBlockState(CENTER).is(DecorRegistry.PILE_PIPE.get()), "no pile in the cleared layer");
            helper.assertTrue(driver.depth() >= 1, "it did not go down: " + driver.depth());
            helper.assertTrue(helper.getBlockState(new BlockPos(0, 2, 0)).is(Blocks.STONE), "the corners are not hit");
            helper.assertTrue(helper.getBlockState(new BlockPos(1, 2, 1)).isAir(), "the layer should be cleared");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "pile_obsidian", timeoutTicks = 100)
    public static void obsidianTakesFiveStrokes(GameTestHelper helper) {
        Runnable restore = enabled();
        helper.setBlock(CENTER, Blocks.OBSIDIAN);
        PileDriverBlockEntity driver = driver(helper, 80_000, 16);
        // a stroke every three ticks: the first is at tick three
        helper.runAfterDelay(11, () -> helper.assertTrue(helper.getBlockState(CENTER).is(Blocks.OBSIDIAN), "obsidian broke before five strokes"));
        helper.runAfterDelay(30, () -> {
            restore.run();
            helper.assertFalse(helper.getBlockState(CENTER).is(Blocks.OBSIDIAN), "obsidian should have broken");
            helper.assertTrue(driver.depth() >= 1, "it did not go down");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "pile_shock", timeoutTicks = 100)
    public static void theShockBreaksNetherrackAndWoolDeeper(GameTestHelper helper) {
        Runnable restore = enabled();
        layer(helper, 2, Blocks.STONE);
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.NETHERRACK);
        helper.setBlock(new BlockPos(3, 0, 2), Blocks.NETHERRACK);
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.WHITE_WOOL);
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 0, 2), Blocks.WHITE_WOOL);
        driver(helper, 80_000, 16);
        helper.runAfterDelay(5, () -> {
            restore.run();
            helper.assertTrue(helper.getBlockState(new BlockPos(3, 1, 2)).isAir() && helper.getBlockState(new BlockPos(3, 0, 2)).isAir(),
                    "netherrack two layers down should break from the shock");
            helper.assertTrue(helper.getBlockState(new BlockPos(1, 1, 2)).isAir(), "wool one layer down should break from the shock");
            helper.assertTrue(helper.getBlockState(new BlockPos(4, 1, 2)).is(Blocks.STONE), "stone is not shocked");
            helper.assertTrue(helper.getBlockState(new BlockPos(4, 0, 2)).is(Blocks.WHITE_WOOL), "wool two layers down is out of reach");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "pile_bedrock", timeoutTicks = 100)
    public static void bedrockStopsThePileDriver(GameTestHelper helper) {
        Runnable restore = enabled();
        helper.setBlock(CENTER, Blocks.BEDROCK);
        PileDriverBlockEntity driver = driver(helper, 80_000, 16);
        helper.runAfterDelay(30, () -> {
            restore.run();
            helper.assertTrue(helper.getBlockState(CENTER).is(Blocks.BEDROCK) && driver.depth() == 0, "it went through bedrock");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "pile_power", timeoutTicks = 100)
    public static void pileDriverNeedsTorqueAndPowerForEveryMetre(GameTestHelper helper) {
        Runnable restore = enabled();
        layer(helper, 2, Blocks.COBBLESTONE);
        PileDriverBlockEntity weak = driver(helper, 79_999, 400);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(helper.getBlockState(new BlockPos(1, 2, 2)).is(Blocks.COBBLESTONE), "it struck on too little torque");
            // enough torque, but with five lengths down it needs 98 kW and has 80
            WeaponGameTests.spinningFlywheel(helper, MACHINE.west(), 80_000, 1, Direction.EAST);
            var registries = helper.getLevel().registryAccess();
            CompoundTag tag = weak.saveWithoutMetadata(registries);
            tag.putInt("step", 5);
            weak.loadCustomOnly(tag, registries);
        });
        helper.runAfterDelay(60, () -> {
            restore.run();
            helper.assertTrue(helper.getBlockState(new BlockPos(1, 2, 2)).is(Blocks.COBBLESTONE), "it struck without the power for six metres of pile");
            helper.assertTrue(weak.powerNeeded() == 6L * PileDriverBlockEntity.BASE_POWER, "power needed " + weak.powerNeeded());
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "pile_claim", timeoutTicks = 100)
    public static void pileDriverKeepsOutOfClaimedBlocks(GameTestHelper helper) {
        Runnable restore = enabled();
        Runnable release = DecorGameTests.claim(new AABB(helper.absolutePos(new BlockPos(1, 2, 2))).inflate(0.1));
        layer(helper, 2, Blocks.STONE);
        driver(helper, 80_000, 16);
        helper.runAfterDelay(15, () -> {
            restore.run();
            release.run();
            helper.assertTrue(helper.getBlockState(new BlockPos(1, 2, 2)).is(Blocks.STONE), "a claimed block was smashed");
            helper.assertTrue(helper.getBlockState(new BlockPos(3, 2, 2)).isAir() || helper.getBlockState(new BlockPos(3, 2, 2)).is(Blocks.COBBLESTONE), "free blocks were left alone");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "pile_off", timeoutTicks = 100)
    public static void pileDriverIsOffUnlessEnabled(GameTestHelper helper) {
        helper.assertFalse(net.scwunge.rotarycraft.config.MachineConfig.SWITCHES.get("pileDriver").getDefault(), "the pile driver should start off");
        Runnable restore = DecorGameTests.disable("pileDriver");
        layer(helper, 2, Blocks.STONE);
        driver(helper, 80_000, 16);
        helper.runAfterDelay(15, () -> {
            restore.run();
            helper.assertTrue(helper.getBlockState(new BlockPos(1, 2, 2)).is(Blocks.STONE), "a switched-off pile driver struck");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "pile_hole", timeoutTicks = 100)
    public static void strokesKillWhatIsInTheHole(GameTestHelper helper) {
        Runnable restore = enabled();
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(2.5, 2, 2.5));
        driver(helper, 80_000, 16);
        helper.runAfterDelay(15, () -> {
            restore.run();
            helper.assertFalse(pig.isAlive(), "the pig in the hole survived");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "pile_fragile", timeoutTicks = 100)
    public static void strokesBreakGlassWebsAndIceAround(GameTestHelper helper) {
        Runnable restore = enabled();
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.GLASS);
        helper.setBlock(new BlockPos(0, 3, 4), Blocks.COBWEB);
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.ICE);
        helper.setBlock(new BlockPos(4, 1, 0), Blocks.STONE_BRICKS);
        driver(helper, 80_000, 16);
        helper.runAfterDelay(10, () -> {
            restore.run();
            helper.assertTrue(helper.getBlockState(new BlockPos(4, 3, 4)).isAir(), "the glass should break");
            helper.assertTrue(helper.getBlockState(new BlockPos(0, 3, 4)).isAir(), "the web should go");
            helper.assertTrue(helper.getBlockState(new BlockPos(0, 1, 0)).is(Blocks.WATER), "ice should melt to water");
            helper.assertTrue(helper.getBlockState(new BlockPos(4, 1, 0)).is(Blocks.STONE_BRICKS), "only the hole is struck, not bricks round it");
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(2, 2, 2))).inflate(8),
                    e -> e.getItem().is(Items.STRING)).isEmpty(), "the web should drop string");
            helper.succeed();
        });
    }
}
