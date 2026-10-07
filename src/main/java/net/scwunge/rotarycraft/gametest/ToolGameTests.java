package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.ToolRegistry;
import net.scwunge.rotarycraft.tool.SickleItem;

import java.util.function.Consumer;

/** Tests of the steel tools and armour and the sickles. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ToolGameTests {
    static final String LONG = "empty20x8x7";

    static ServerPlayer survivor(GameTestHelper helper, ItemStack tool) {
        ServerPlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(("sickle" + tool.getItem()).getBytes()), "Sickler"));
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1)));
        return player;
    }

    static void ripeWheat(GameTestHelper helper, BlockPos pos, int age) {
        helper.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        helper.setBlock(pos, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, age));
    }

    // ---- steel tools and armour ----

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 40, batch = "tools")
    public static void steelPickaxeDigsAFifthFasterThanIronAndBreaksGlassFast(GameTestHelper helper) {
        ItemStack steel = new ItemStack(ToolRegistry.STEEL_PICKAXE.get());
        ItemStack iron = new ItemStack(Items.IRON_PICKAXE);
        BlockState stone = Blocks.STONE.defaultBlockState();
        helper.assertTrue(Math.abs(steel.getDestroySpeed(stone) - iron.getDestroySpeed(stone) * 1.2F) < 0.01F, "speed " + steel.getDestroySpeed(stone) + " vs " + iron.getDestroySpeed(stone));
        helper.assertTrue(steel.getDestroySpeed(Blocks.GLASS.defaultBlockState()) == 8F && steel.getDestroySpeed(Blocks.GLASS_PANE.defaultBlockState()) == 8F, "glass is slow");
        helper.assertTrue(steel.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState()) && !steel.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "harvest level is not iron's");
        helper.assertTrue(steel.getMaxDamage() == 600, "lasts " + steel.getMaxDamage());
        helper.succeed();
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 40, batch = "tools")
    public static void steelToolsAreRepairedWithSteelIngots(GameTestHelper helper) {
        ItemStack ingot = new ItemStack(RotaryItems.HSLA_STEEL_INGOT.get());
        for (var tool : new net.minecraft.world.item.Item[] {ToolRegistry.STEEL_PICKAXE.get(), ToolRegistry.STEEL_AXE.get(), ToolRegistry.STEEL_SHOVEL.get(),
                ToolRegistry.STEEL_HOE.get(), ToolRegistry.STEEL_SWORD.get(), ToolRegistry.STEEL_HELMET.get(), ToolRegistry.STEEL_BOOTS.get()}) {
            helper.assertTrue(tool.isValidRepairItem(new ItemStack(tool), ingot), tool + " cannot be repaired with steel");
            helper.assertTrue(!tool.isValidRepairItem(new ItemStack(tool), new ItemStack(Items.IRON_INGOT)), tool + " is repaired with iron");
        }
        helper.succeed();
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 40, batch = "tools")
    public static void steelArmourProtectsAsTheOriginalsDoes(GameTestHelper helper) {
        ArmorItem helmet = ToolRegistry.STEEL_HELMET.get(), chest = ToolRegistry.STEEL_CHESTPLATE.get(), legs = ToolRegistry.STEEL_LEGGINGS.get(), boots = ToolRegistry.STEEL_BOOTS.get();
        helper.assertTrue(helmet.getDefense() == 3 && chest.getDefense() == 7 && legs.getDefense() == 5 && boots.getDefense() == 3,
                "defence " + helmet.getDefense() + ", " + chest.getDefense() + ", " + legs.getDefense() + ", " + boots.getDefense());
        helper.assertTrue(new ItemStack(helmet).getMaxDamage() == 11 * 24 && new ItemStack(chest).getMaxDamage() == 16 * 24
                && new ItemStack(legs).getMaxDamage() == 15 * 24 && new ItemStack(boots).getMaxDamage() == 13 * 24, "durability");
        helper.succeed();
    }

    // ---- the sickles ----

    /** A ripe crop takes every ripe crop of its kind around it, replants them and wears the steel sickle down by half the number cut. */
    @GameTest(template = LONG, timeoutTicks = 80, batch = "tools")
    public static void aSickleHarvestsAndReplantsEveryRipeCropAroundIt(GameTestHelper helper) {
        for (int x = 8; x <= 10; x++) {
            for (int z = 2; z <= 4; z++) {
                ripeWheat(helper, new BlockPos(x, 3, z), 7);
            }
        }
        ripeWheat(helper, new BlockPos(18, 3, 3), 7); // too far
        ItemStack tool = new ItemStack(ToolRegistry.STEEL_SICKLE.get());
        ServerPlayer player = survivor(helper, tool);
        player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(9, 3, 3)));
        helper.runAfterDelay(2, () -> {
            for (int x = 8; x <= 10; x++) {
                for (int z = 2; z <= 4; z++) {
                    helper.assertBlockState(new BlockPos(x, 3, z), s -> s.is(Blocks.WHEAT) && s.getValue(CropBlock.AGE) == 0, () -> "not replanted");
                }
            }
            helper.assertBlockState(new BlockPos(18, 3, 3), s -> s.getValue(CropBlock.AGE) == 7, () -> "it reached too far");
            int wheat = 0;
            for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds())) {
                if (e.getItem().is(Items.WHEAT)) {
                    wheat += e.getItem().getCount();
                }
            }
            helper.assertTrue(wheat >= 9, "only " + wheat + " wheat");
            helper.assertTrue(tool.getDamageValue() == 4, "wore " + tool.getDamageValue() + " (nine crops should cost four)");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 80, batch = "tools")
    public static void aSickleLeavesUnripeCropsAloneUnlessItStartsOnOne(GameTestHelper helper) {
        ripeWheat(helper, new BlockPos(8, 3, 3), 7);
        ripeWheat(helper, new BlockPos(9, 3, 3), 3);
        ripeWheat(helper, new BlockPos(10, 3, 3), 3);
        ServerPlayer player = survivor(helper, new ItemStack(ToolRegistry.STEEL_SICKLE.get()));
        player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(8, 3, 3)));
        helper.runAfterDelay(2, () -> {
            helper.assertBlockState(new BlockPos(9, 3, 3), s -> s.getValue(CropBlock.AGE) == 3, () -> "an unripe crop was cut");
            player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(9, 3, 3)));
        });
        helper.runAfterDelay(4, () -> {
            helper.assertBlock(new BlockPos(10, 3, 3), b -> b == Blocks.AIR, () -> "the other unripe crop stayed");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 80, batch = "tools")
    public static void aSickleCutsCaneAboveItsBaseAndLeavesTheBase(GameTestHelper helper) {
        for (int x = 8; x <= 9; x++) {
            helper.setBlock(new BlockPos(x, 2, 3), Blocks.SAND);
            helper.setBlock(new BlockPos(x, 3, 3), Blocks.SUGAR_CANE);
            helper.setBlock(new BlockPos(x, 4, 3), Blocks.SUGAR_CANE);
            helper.setBlock(new BlockPos(x, 5, 3), Blocks.SUGAR_CANE);
        }
        helper.setBlock(new BlockPos(7, 2, 3), Blocks.WATER);
        helper.setBlock(new BlockPos(10, 2, 3), Blocks.WATER);
        ServerPlayer player = survivor(helper, new ItemStack(ToolRegistry.STEEL_SICKLE.get()));
        player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(8, 4, 3)));
        helper.runAfterDelay(2, () -> {
            for (int x = 8; x <= 9; x++) {
                helper.assertBlock(new BlockPos(x, 3, 3), b -> b instanceof SugarCaneBlock, () -> "a base of cane was cut");
                helper.assertBlock(new BlockPos(x, 4, 3), b -> b == Blocks.AIR, () -> "cane left");
                helper.assertBlock(new BlockPos(x, 5, 3), b -> b == Blocks.AIR, () -> "cane left");
            }
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 80, batch = "tools")
    public static void aSickleClearsPlantsOfTheSameKindAndLeavesOthers(GameTestHelper helper) {
        for (int x = 7; x <= 11; x++) {
            helper.setBlock(new BlockPos(x, 2, 3), Blocks.GRASS_BLOCK);
            helper.setBlock(new BlockPos(x, 3, 3), x == 11 ? Blocks.POPPY : Blocks.SHORT_GRASS);
        }
        ServerPlayer player = survivor(helper, new ItemStack(ToolRegistry.STEEL_SICKLE.get()));
        player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(9, 3, 3)));
        helper.runAfterDelay(2, () -> {
            for (int x = 7; x <= 10; x++) {
                helper.assertBlock(new BlockPos(x, 3, 3), b -> b == Blocks.AIR, () -> "grass left");
            }
            helper.assertBlock(new BlockPos(11, 3, 3), b -> b == Blocks.POPPY, () -> "the poppy was cut");
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 80, batch = "tools")
    public static void aSickleStripsALeafCloudButNotWood(GameTestHelper helper) {
        for (int x = 7; x <= 11; x++) {
            for (int y = 3; y <= 5; y++) {
                for (int z = 1; z <= 5; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
                }
            }
        }
        helper.setBlock(new BlockPos(9, 3, 3), Blocks.OAK_LOG);
        helper.setBlock(new BlockPos(9, 4, 3), Blocks.BIRCH_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        ItemStack tool = new ItemStack(ToolRegistry.STEEL_SICKLE.get());
        ServerPlayer player = survivor(helper, tool);
        player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(8, 4, 2)));
        helper.runAfterDelay(2, () -> {
            helper.assertBlock(new BlockPos(11, 5, 5), b -> b == Blocks.AIR, () -> "leaves left in the far corner");
            helper.assertBlock(new BlockPos(9, 3, 3), b -> b == Blocks.OAK_LOG, () -> "the log was cut");
            helper.assertBlock(new BlockPos(9, 4, 3), b -> b == Blocks.BIRCH_LEAVES, () -> "birch leaves went with the oak");
            helper.assertTrue(tool.getDamageValue() >= 1 && tool.getDamageValue() <= 8, "wore " + tool.getDamageValue());
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 160, batch = "tools")
    public static void aSickleHurtsTheCreaturesBesideTheOneItHits(GameTestHelper helper) {
        // the batch before may still be discharging a Van de Graaff here for a tick or two; give it time
        helper.runAfterDelay(40, () -> {
            Cow first = helper.spawn(EntityType.COW, new BlockPos(8, 3, 3));
            Cow second = helper.spawn(EntityType.COW, new BlockPos(9, 3, 3));
            Cow far = helper.spawn(EntityType.COW, new BlockPos(16, 3, 3));
            for (Cow c : new Cow[] {first, second, far}) {
                c.setNoAi(true);
            }
            ItemStack tool = new ItemStack(ToolRegistry.STEEL_SICKLE.get());
            ServerPlayer player = survivor(helper, tool);
            ((SickleItem) tool.getItem()).onLeftClickEntity(tool, player, first);
            helper.runAfterDelay(2, () -> {
                // judged by who hurt them: something else in the test world has been seen striking cows with lightning
                helper.assertTrue(second.getLastDamageSource() != null && second.getLastDamageSource().getEntity() == player, "the cow beside it was not hurt by the sickle");
                helper.assertTrue(far.getLastDamageSource() == null || far.getLastDamageSource().getEntity() != player, "a far cow was hurt by the sickle");
                helper.assertTrue(tool.getDamageValue() == 10, "wore " + tool.getDamageValue());
                helper.succeed();
            });
        });
    }

    /** A claim mod cancelling the break of a block stops the sickle taking that block, and only that one. */
    @GameTest(template = LONG, timeoutTicks = 80, batch = "tools_claim")
    public static void aSickleRespectsAClaimOnTheBlocksItCuts(GameTestHelper helper) {
        for (int x = 8; x <= 10; x++) {
            ripeWheat(helper, new BlockPos(x, 3, 3), 7);
        }
        BlockPos claimed = helper.absolutePos(new BlockPos(10, 3, 3));
        Consumer<BlockEvent.BreakEvent> claim = e -> {
            if (e.getPos().equals(claimed)) {
                e.setCanceled(true);
            }
        };
        Object listener = new Object() {
            @SubscribeEvent(priority = EventPriority.HIGH)
            public void onBreak(BlockEvent.BreakEvent e) {
                claim.accept(e);
            }
        };
        NeoForge.EVENT_BUS.register(listener);
        ServerPlayer player = survivor(helper, new ItemStack(ToolRegistry.STEEL_SICKLE.get()));
        player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(9, 3, 3)));
        helper.runAfterDelay(2, () -> {
            NeoForge.EVENT_BUS.unregister(listener);
            helper.assertBlockState(new BlockPos(8, 3, 3), s -> s.getValue(CropBlock.AGE) == 0, () -> "the free crop was not cut");
            helper.assertBlockState(new BlockPos(10, 3, 3), s -> s.getValue(CropBlock.AGE) == 7, () -> "the claimed crop was cut");
            helper.succeed();
        });
    }
}
