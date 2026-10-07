package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.TerraformerBlockEntity;
import net.scwunge.rotarycraft.charged.Charge;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.handheld.FireballLauncherItem;
import net.scwunge.rotarycraft.handheld.GravelGunItem;
import net.scwunge.rotarycraft.handheld.HandPumpItem;
import net.scwunge.rotarycraft.handheld.MatchFilterItem;
import net.scwunge.rotarycraft.logistics.ItemFilterBlockEntity;
import net.scwunge.rotarycraft.registry.HandheldRegistry;
import net.scwunge.rotarycraft.registry.WorldMachineRegistry;

/** The handheld tools: gravel gun, vacuum gun, fireball launcher, hand pump, spring piston, tile selector and match filter. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class HandheldGameTests {
    static final String LONG = "empty20x8x7";

    /** A player at (4.5, 3, 3.5) looking along +x, holding {@code held}. */
    static ServerPlayer player(GameTestHelper helper, ItemStack held, String name) {
        ServerPlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(name.getBytes()), name));
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        player.setPos(helper.absoluteVec(new Vec3(4.5, 3, 3.5)));
        player.setYRot(-90);
        player.setYHeadRot(-90);
        player.setXRot(0);
        player.setShiftKeyDown(false);
        return player;
    }

    static UseOnContext on(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos rel, Direction face) {
        BlockPos at = helper.absolutePos(rel);
        return new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, new BlockHitResult(Vec3.atCenterOf(at), face, at, false));
    }

    // ---- gravel gun ----

    @GameTest(template = LONG, timeoutTicks = 40, batch = "handheld")
    public static void gravelGunDamageGrowsWithChargeAndEachShotCostsItsLogarithm(GameTestHelper helper) {
        helper.assertTrue(GravelGunItem.damage(0) == 0 && GravelGunItem.damage(1) == 1, "the lowest charges");
        helper.assertTrue(GravelGunItem.damage(100) < GravelGunItem.damage(1000) && GravelGunItem.damage(1000) < GravelGunItem.damage(32000), "damage does not grow with charge");
        helper.assertTrue(GravelGunItem.damage(1000) >= 10, "a thousand kJ should kill a cow: " + GravelGunItem.damage(1000));
        helper.assertTrue(GravelGunItem.cost(1) == 1 && GravelGunItem.cost(1000) == 9 && GravelGunItem.cost(32000) == 14, "costs " + GravelGunItem.cost(1000));
        helper.succeed();
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "handheld")
    public static void gravelGunShootsTheFirstCreatureInLineUsingGravel(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(9, 3, 3));
        cow.setNoAi(true);
        ItemStack gun = new ItemStack(HandheldRegistry.GRAVEL_GUN.get());
        Charge.set(gun, 1000);
        ServerPlayer player = player(helper, gun, "gravelshooter");
        player.getInventory().setItem(1, new ItemStack(Items.GRAVEL, 3));
        gun.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(!cow.isAlive() || cow.getHealth() <= 0, "the cow lived: " + cow.getHealth());
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 2, "gravel left " + player.getInventory().getItem(1).getCount());
            helper.assertTrue(Charge.get(gun) == 1000 - GravelGunItem.cost(1000), "charge " + Charge.get(gun));
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "handheld")
    public static void gravelGunNeedsGravelAndChargeAndSparesPlayersWhenSwitchedOff(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(9, 3, 3));
        cow.setNoAi(true);
        ItemStack gun = new ItemStack(HandheldRegistry.GRAVEL_GUN.get());
        Charge.set(gun, 1000);
        ServerPlayer player = player(helper, gun, "gravelrefuser");
        gun.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(cow.isAlive() && cow.getHealth() == cow.getMaxHealth() && Charge.get(gun) == 1000, "it fired with no gravel");
        player.getInventory().setItem(1, new ItemStack(Items.GRAVEL));
        Charge.set(gun, 0);
        gun.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(cow.isAlive() && player.getInventory().getItem(1).getCount() == 1, "it fired with no charge");
        helper.succeed();
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "handheld_pvp")
    public static void gravelGunHitsPlayersOnlyWhenTheServerAllowsIt(GameTestHelper helper) {
        ItemStack gun = new ItemStack(HandheldRegistry.GRAVEL_GUN.get());
        Charge.set(gun, 1000);
        ServerPlayer shooter = player(helper, gun, "gravelpvp1");
        shooter.getInventory().setItem(1, new ItemStack(Items.GRAVEL, 3));
        net.minecraft.world.entity.player.Player target = helper.makeMockPlayer(GameType.SURVIVAL);
        target.setPos(helper.absoluteVec(new Vec3(9.5, 3, 3.5)));
        helper.getLevel().addFreshEntity(target);
        target.setHealth(target.getMaxHealth());
        MachineConfig.override(MachineConfig.GRAVEL_GUN_PVP, false);
        gun.getItem().use(helper.getLevel(), shooter, InteractionHand.MAIN_HAND);
        boolean spared = target.getHealth() == target.getMaxHealth();
        MachineConfig.clearOverride(MachineConfig.GRAVEL_GUN_PVP);
        helper.assertTrue(spared, "a player was hit with pvp off");
        gun.getItem().use(helper.getLevel(), shooter, InteractionHand.MAIN_HAND);
        helper.assertTrue(target.getHealth() < target.getMaxHealth() || target.isDeadOrDying(), "a player was not hit with pvp on");
        helper.succeed();
    }

    // ---- vacuum gun ----

    @GameTest(template = LONG, timeoutTicks = 60, batch = "handheld")
    public static void vacuumGunPullsItemsTowardsYouForAUnitOfCharge(GameTestHelper helper) {
        ItemEntity item = helper.spawnItem(Items.DIRT, new Vec3(9.5, 3, 3.5));
        ItemStack gun = new ItemStack(HandheldRegistry.VACUUM_GUN.get());
        Charge.set(gun, 10);
        ServerPlayer player = player(helper, gun, "vacuumer");
        gun.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(item.getDeltaMovement().x < -0.001, "the item was not pulled: " + item.getDeltaMovement());
            helper.assertTrue(Charge.get(gun) == 9, "charge " + Charge.get(gun));
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "handheld")
    public static void vacuumGunTipsOutAnInventoryWhenSneaking(GameTestHelper helper) {
        helper.setBlock(new BlockPos(5, 3, 3), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(new BlockPos(5, 3, 3));
        chest.setItem(0, new ItemStack(Items.DIAMOND, 4));
        ItemStack gun = new ItemStack(HandheldRegistry.VACUUM_GUN.get());
        Charge.set(gun, 10);
        ServerPlayer player = player(helper, gun, "spiller");
        player.setShiftKeyDown(true);
        player.setXRot(30);
        gun.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(chest.isEmpty(), "the chest still holds things");
            helper.assertFalse(helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(2), e -> e.getItem().is(Items.DIAMOND)).isEmpty(), "nothing was spilled");
            helper.assertTrue(Charge.get(gun) == 8, "charge " + Charge.get(gun));
            helper.succeed();
        });
    }

    // ---- fireball launcher ----

    @GameTest(template = LONG, timeoutTicks = 60, batch = "handheld")
    public static void fireballLauncherBlastGrowsWithTheTimeItIsHeldAndCostsHalfIt(GameTestHelper helper) {
        helper.assertTrue(FireballLauncherItem.level(1, false, false) == 0 && FireballLauncherItem.level(10, false, false) == 3 && FireballLauncherItem.level(200, false, false) == 8,
                "the blast sizes: " + FireballLauncherItem.level(10, false, false));
        helper.assertTrue(FireballLauncherItem.level(10, true, false) > FireballLauncherItem.level(10, false, false), "creative is not quicker");
        ItemStack launcher = new ItemStack(HandheldRegistry.FIREBALL_LAUNCHER.get());
        Charge.set(launcher, 10);
        ServerPlayer player = player(helper, launcher, "fireballer");
        player.getInventory().setItem(1, new ItemStack(Items.FIRE_CHARGE, 8));
        launcher.getItem().releaseUsing(launcher, helper.getLevel(), player, 72000 - 100);
        helper.runAfterDelay(1, () -> {
            helper.assertFalse(helper.getLevel().getEntitiesOfClass(LargeFireball.class, helper.getBounds().inflate(4)).isEmpty(), "no fireball");
            helper.assertTrue(Charge.get(launcher) == 10 - 3, "charge " + Charge.get(launcher));
            helper.getLevel().getEntitiesOfClass(LargeFireball.class, helper.getBounds().inflate(4)).forEach(net.minecraft.world.entity.Entity::discard);
            helper.succeed();
        });
    }

    // ---- hand pump ----

    @GameTest(template = LONG, timeoutTicks = 60, batch = "handheld")
    public static void handPumpDrainsASourceAndPlacesItBack(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 2, 3), Blocks.WATER);
        ItemStack pump = new ItemStack(HandheldRegistry.HAND_PUMP.get());
        Charge.set(pump, 10);
        ServerPlayer player = player(helper, pump, "pumper");
        player.setXRot(90);
        pump.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertBlock(new BlockPos(4, 2, 3), b -> b == Blocks.AIR, () -> "the water is still there");
        FluidStack held = HandPumpItem.contents(pump);
        helper.assertTrue(held.getFluid() == Fluids.WATER && held.getAmount() == 1000 && Charge.get(pump) == 9, "held " + held + ", charge " + Charge.get(pump));
        IFluidHandler cap = pump.getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(cap != null && cap.getFluidInTank(0).getAmount() == 1000, "no fluid capability on the pump");
        // place mode: sneaking turns it, and a use on a block lays a source beside it
        player.setShiftKeyDown(true);
        pump.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
        helper.assertTrue(HandPumpItem.placing(pump), "it did not turn to place mode");
        helper.setBlock(new BlockPos(8, 3, 3), Blocks.STONE);
        pump.getItem().useOn(on(helper, player, pump, new BlockPos(8, 3, 3), Direction.UP));
        helper.assertBlock(new BlockPos(8, 4, 3), b -> b == Blocks.WATER, () -> "no water was placed");
        helper.assertTrue(HandPumpItem.contents(pump).isEmpty() && Charge.get(pump) == 8, "held " + HandPumpItem.contents(pump) + ", charge " + Charge.get(pump));
        helper.succeed();
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "handheld")
    public static void handPumpTradesFluidWithTanks(GameTestHelper helper) {
        helper.setBlock(new BlockPos(8, 3, 3), net.scwunge.rotarycraft.registry.LogisticsRegistry.SPILLWAY.block().get());
        net.scwunge.rotarycraft.logistics.SpillwayBlockEntity spillway = helper.getBlockEntity(new BlockPos(8, 3, 3));
        spillway.tank().fill(new FluidStack(Fluids.WATER, 5000), IFluidHandler.FluidAction.EXECUTE);
        helper.setBlock(new BlockPos(12, 3, 3), net.scwunge.rotarycraft.registry.LogisticsRegistry.FILLING_STATION.block().get());
        net.scwunge.rotarycraft.logistics.FillingStationBlockEntity station = helper.getBlockEntity(new BlockPos(12, 3, 3));
        ItemStack pump = new ItemStack(HandheldRegistry.HAND_PUMP.get());
        ServerPlayer player = player(helper, pump, "tankpumper");
        pump.set(HandheldRegistry.PUMP_PLACING.get(), true);
        // a spillway gives its water from underneath
        pump.getItem().useOn(on(helper, player, pump, new BlockPos(8, 3, 3), Direction.DOWN));
        helper.assertTrue(HandPumpItem.contents(pump).getAmount() == 5000 && spillway.tank().isEmpty(), "pumped " + HandPumpItem.contents(pump).getAmount());
        pump.set(HandheldRegistry.PUMP_PLACING.get(), false);
        pump.getItem().useOn(on(helper, player, pump, new BlockPos(12, 3, 3), Direction.UP));
        helper.assertTrue(HandPumpItem.contents(pump).isEmpty() && station.tank().getFluidAmount() == 5000, "pumped back " + station.tank().getFluidAmount());
        helper.succeed();
    }

    // ---- spring piston ----

    @GameTest(template = LONG, timeoutTicks = 60, batch = "handheld")
    public static void springPistonShovesABlockOneStep(GameTestHelper helper) {
        helper.setBlock(new BlockPos(8, 3, 3), Blocks.STONE);
        ItemStack piston = new ItemStack(HandheldRegistry.SPRING_PISTON.get());
        Charge.set(piston, 10);
        ServerPlayer player = player(helper, piston, "shover");
        piston.getItem().useOn(on(helper, player, piston, new BlockPos(8, 3, 3), Direction.WEST));
        helper.assertBlock(new BlockPos(8, 3, 3), b -> b == Blocks.AIR, () -> "the block did not leave");
        helper.assertBlock(new BlockPos(9, 3, 3), b -> b == Blocks.STONE, () -> "the block did not arrive");
        helper.assertTrue(Charge.get(piston) == 9, "charge " + Charge.get(piston));
        // blocked: nothing moves
        helper.setBlock(new BlockPos(10, 3, 3), Blocks.STONE);
        piston.getItem().useOn(on(helper, player, piston, new BlockPos(9, 3, 3), Direction.WEST));
        helper.assertBlock(new BlockPos(9, 3, 3), b -> b == Blocks.STONE, () -> "it shoved a block into another");
        helper.assertTrue(Charge.get(piston) == 9, "it paid for nothing");
        helper.succeed();
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "handheld")
    public static void springPistonShovesALineWhenSneakingAndIsStoppedByBedrockAndBlockEntities(GameTestHelper helper) {
        for (int x = 8; x <= 10; x++) {
            helper.setBlock(new BlockPos(x, 3, 3), Blocks.STONE);
        }
        ItemStack piston = new ItemStack(HandheldRegistry.SPRING_PISTON.get());
        Charge.set(piston, 20);
        ServerPlayer player = player(helper, piston, "linepusher");
        player.setShiftKeyDown(true);
        piston.getItem().useOn(on(helper, player, piston, new BlockPos(8, 3, 3), Direction.WEST));
        helper.assertBlock(new BlockPos(8, 3, 3), b -> b == Blocks.AIR, () -> "the line did not leave its start");
        for (int x = 9; x <= 11; x++) {
            helper.assertBlock(new BlockPos(x, 3, 3), b -> b == Blocks.STONE, () -> "the line is missing a block");
        }
        helper.assertTrue(Charge.get(piston) == 14, "charge " + Charge.get(piston));
        helper.setBlock(new BlockPos(14, 3, 3), Blocks.BEDROCK);
        piston.getItem().useOn(on(helper, player, piston, new BlockPos(14, 3, 3), Direction.WEST));
        helper.assertBlock(new BlockPos(14, 3, 3), b -> b == Blocks.BEDROCK, () -> "bedrock moved");
        helper.setBlock(new BlockPos(14, 4, 3), Blocks.CHEST);
        piston.getItem().useOn(on(helper, player, piston, new BlockPos(14, 4, 3), Direction.WEST));
        helper.assertBlock(new BlockPos(14, 4, 3), b -> b == Blocks.CHEST, () -> "a chest moved");
        helper.succeed();
    }

    // ---- tile selector ----

    @GameTest(template = LONG, timeoutTicks = 60, batch = "handheld")
    public static void tileSelectorLinksToTheTerraformerAndAddsTilesToIt(GameTestHelper helper) {
        helper.setBlock(new BlockPos(8, 3, 3), WorldMachineRegistry.TERRAFORMER.get());
        TerraformerBlockEntity terraformer = helper.getBlockEntity(new BlockPos(8, 3, 3));
        ItemStack selector = new ItemStack(HandheldRegistry.TILE_SELECTOR.get());
        ServerPlayer player = player(helper, selector, "selector");
        selector.getItem().useOn(on(helper, player, selector, new BlockPos(8, 3, 3), Direction.UP));
        helper.assertTrue(selector.has(HandheldRegistry.TILE_LINK.get()), "it did not link");
        helper.setBlock(new BlockPos(12, 3, 3), Blocks.STONE);
        selector.getItem().useOn(on(helper, player, selector, new BlockPos(12, 3, 3), Direction.UP));
        helper.assertTrue(terraformer.selectedCount() == 1, "tiles " + terraformer.selectedCount());
        selector.getItem().useOn(on(helper, player, selector, new BlockPos(12, 3, 3), Direction.UP));
        helper.assertTrue(terraformer.selectedCount() == 1, "the same patch twice: " + terraformer.selectedCount());
        selector.getItem().useOn(on(helper, player, selector, new BlockPos(19, 3, 3), Direction.UP));
        helper.assertTrue(terraformer.selectedCount() == 2, "tiles " + terraformer.selectedCount());
        net.minecraft.nbt.CompoundTag saved = terraformer.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.setBlock(new BlockPos(8, 3, 3), Blocks.AIR);
        helper.setBlock(new BlockPos(8, 3, 3), WorldMachineRegistry.TERRAFORMER.get());
        TerraformerBlockEntity again = helper.getBlockEntity(new BlockPos(8, 3, 3));
        again.loadCustomOnly(saved, helper.getLevel().registryAccess());
        helper.assertTrue(again.selectedCount() == 2, "the selection was not saved");
        again.setRadius(8);
        helper.assertTrue(again.selectedCount() == 0, "a new radius should clear the selection");
        selector.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(true);
        selector.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertFalse(selector.has(HandheldRegistry.TILE_LINK.get()), "sneaking in the air did not unlink");
        helper.succeed();
    }

    // ---- match filter ----

    @GameTest(template = LONG, timeoutTicks = 60, batch = "handheld_filter")
    public static void matchFilterKeepsAnItemAndAnItemFilterFiltersByIt(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("itemFilter");
        ItemStack filter = new ItemStack(HandheldRegistry.MATCH_FILTER.get());
        MatchFilterItem.setTemplate(filter, new ItemStack(Items.IRON_INGOT, 5));
        helper.assertTrue(MatchFilterItem.template(filter).is(Items.IRON_INGOT) && MatchFilterItem.template(filter).getCount() == 1, "it did not keep a single iron ingot");
        ItemFilterBlockEntity machine = ItemFilterGameTests.filter(helper, filter);
        helper.runAfterDelay(5, () -> {
            boolean iron = ItemFilterGameTests.takes(machine, new ItemStack(Items.IRON_INGOT));
            boolean gold = ItemFilterGameTests.takes(machine, new ItemStack(Items.GOLD_INGOT));
            restore.run();
            helper.assertTrue(iron, "the filter refused what the match filter holds");
            helper.assertFalse(gold, "the filter took something else");
            ServerPlayer player = player(helper, filter, "matcher");
            player.setShiftKeyDown(true);
            filter.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(MatchFilterItem.template(filter).isEmpty(), "sneaking did not empty the match filter");
            helper.succeed();
        });
    }
}
