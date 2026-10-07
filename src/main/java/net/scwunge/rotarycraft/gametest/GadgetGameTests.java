package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.BlockCannonBlockEntity;
import net.scwunge.rotarycraft.charged.Charge;
import net.scwunge.rotarycraft.charged.StunGunItem;
import net.scwunge.rotarycraft.crafting.WorktableBlockEntity;
import net.scwunge.rotarycraft.item.CoilItem;
import net.scwunge.rotarycraft.registry.CraftingRegistry;
import net.scwunge.rotarycraft.registry.DecorRegistry;
import net.scwunge.rotarycraft.registry.GadgetRegistry;
import net.scwunge.rotarycraft.registry.RotaryItems;

/** Tests of the charged tools and gadgets. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class GadgetGameTests {
    static final String LONG = "empty20x8x7";

    static ServerPlayer player(GameTestHelper helper, ItemStack held, String name) {
        ServerPlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(name.getBytes()), name));
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 3, 3.5));
        player.setPos(at);
        player.setYRot(-90); // looking east, along +x
        player.setXRot(0);
        return player;
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 40, batch = "gadgets")
    public static void chargeIsUsedAUnitAtATimeAndAFlatToolRefuses(GameTestHelper helper) {
        ItemStack gun = new ItemStack(GadgetRegistry.STUN_GUN.get());
        ServerPlayer player = player(helper, gun, "chargeuser");
        helper.assertTrue(!Charge.use(gun, player, 1, "tool"), "an empty tool worked");
        Charge.set(gun, 3);
        helper.assertTrue(Charge.use(gun, player, 1, "tool") && Charge.get(gun) == 2, "charge " + Charge.get(gun));
        Charge.set(gun, Charge.FULL * 2);
        helper.assertTrue(Charge.get(gun) == Charge.FULL, "not capped");
        helper.succeed();
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "gadgets")
    public static void theStunGunThrowsBackWhatStandsInFrontOfItForAUnitOfCharge(GameTestHelper helper) {
        Cow near = helper.spawn(EntityType.COW, new BlockPos(7, 3, 3));
        Cow behind = helper.spawn(EntityType.COW, new BlockPos(1, 3, 3));
        near.setNoAi(true);
        behind.setNoAi(true);
        ItemStack gun = Charge.full(new ItemStack(GadgetRegistry.STUN_GUN.get()));
        ServerPlayer player = player(helper, gun, "stunner");
        gun.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(near.getDeltaMovement().x > 0.3 || near.getX() > helper.absoluteVec(new Vec3(7.6, 3, 3.5)).x, "the cow in front was not thrown: " + near.getDeltaMovement());
            helper.assertTrue(Math.abs(behind.getDeltaMovement().x) < 0.1, "the cow behind was thrown");
            helper.assertTrue(Charge.get(gun) == Charge.FULL - 1, "charge " + Charge.get(gun));
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "gadgets")
    public static void theStunGunClearsAVeinOfOreButOnlyWithEnoughCharge(GameTestHelper helper) {
        for (int x = 7; x <= 9; x++) {
            helper.setBlock(new BlockPos(x, 3, 3), Blocks.IRON_ORE);
            helper.setBlock(new BlockPos(x, 4, 3), Blocks.IRON_ORE);
        }
        helper.setBlock(new BlockPos(10, 3, 3), Blocks.COAL_ORE);
        ItemStack gun = new ItemStack(GadgetRegistry.STUN_GUN.get());
        Charge.set(gun, StunGunItem.BLOCK_MODE_CHARGE - 1);
        ServerPlayer player = player(helper, gun, "veiner");
        player.setShiftKeyDown(true);
        BlockPos at = helper.absolutePos(new BlockPos(8, 3, 3));
        var hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.WEST, at, false);
        helper.assertTrue(!gun.getItem().useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, gun, hit)).consumesAction(), "worked with too little charge");
        Charge.set(gun, StunGunItem.BLOCK_MODE_CHARGE + 10);
        helper.assertTrue(gun.getItem().useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, gun, hit)).consumesAction(), "did not work");
        helper.runAfterDelay(2, () -> {
            for (int x = 7; x <= 9; x++) {
                helper.assertBlock(new BlockPos(x, 3, 3), b -> b == Blocks.AIR, () -> "iron ore left");
                helper.assertBlock(new BlockPos(x, 4, 3), b -> b == Blocks.AIR, () -> "iron ore left above");
            }
            helper.assertBlock(new BlockPos(10, 3, 3), b -> b == Blocks.COAL_ORE, () -> "the coal ore went too");
            helper.assertTrue(Charge.get(gun) == StunGunItem.BLOCK_MODE_CHARGE + 8, "charge " + Charge.get(gun));
            helper.succeed();
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "gadgets")
    public static void theRangeFinderTakesAUnitAndTheOthersToo(GameTestHelper helper) {
        helper.setBlock(new BlockPos(12, 3, 3), Blocks.STONE);
        for (var item : new net.minecraft.world.item.Item[] {GadgetRegistry.RANGE_FINDER.get(), GadgetRegistry.ULTRASOUND.get(), GadgetRegistry.MOTION_TRACKER.get()}) {
            ItemStack stack = Charge.full(new ItemStack(item));
            ServerPlayer player = player(helper, stack, "scanner");
            stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(Charge.get(stack) == Charge.FULL - 1, item + " took " + (Charge.FULL - Charge.get(stack)));
            ItemStack flat = new ItemStack(item);
            player.setItemInHand(InteractionHand.MAIN_HAND, flat);
            flat.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(Charge.get(flat) == 0, item + " worked flat");
        }
        helper.succeed();
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 60, batch = "gadgets")
    public static void theGogglesGiveNightVisionWhileChargedAndNotWhenFlat(GameTestHelper helper) {
        ItemStack goggles = Charge.full(new ItemStack(GadgetRegistry.NIGHT_VISION_GOGGLES.get()));
        ServerPlayer player = player(helper, ItemStack.EMPTY, "goggled");
        player.setItemSlot(EquipmentSlot.HEAD, goggles);
        goggles.getItem().inventoryTick(goggles, helper.getLevel(), player, 39, false);
        helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION), "no night vision");
        player.removeEffect(MobEffects.NIGHT_VISION);
        ItemStack flat = new ItemStack(GadgetRegistry.NIGHT_VISION_GOGGLES.get());
        player.setItemSlot(EquipmentSlot.HEAD, flat);
        flat.getItem().inventoryTick(flat, helper.getLevel(), player, 39, false);
        helper.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION), "flat goggles worked");
        helper.succeed();
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 120, batch = "gadgets")
    public static void theWorktableWindsAToolUpFromACoil(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 2, 2), CraftingRegistry.WORKTABLE.get());
        WorktableBlockEntity table = helper.getBlockEntity(new BlockPos(2, 2, 2));
        ItemStack coil = new ItemStack(net.scwunge.rotarycraft.registry.WeaponRegistry.SPRING.get());
        CoilItem.setCharge(coil, 1200);
        table.items().setStackInSlot(0, new ItemStack(GadgetRegistry.STUN_GUN.get()));
        table.items().setStackInSlot(1, coil);
        helper.succeedWhen(() -> {
            boolean charged = false;
            boolean coilBack = false;
            for (int i = WorktableBlockEntity.FIRST_OUTPUT; i < WorktableBlockEntity.PATTERN; i++) {
                ItemStack out = table.items().getStackInSlot(i);
                charged |= out.is(GadgetRegistry.STUN_GUN.get()) && Charge.get(out) == 1200;
                coilBack |= out.getItem() instanceof CoilItem && CoilItem.charge(out) == 0;
            }
            helper.assertTrue(charged && coilBack, "not wound up yet: " + Charge.get(table.items().getStackInSlot(0)));
        });
    }

    @GameTest(template = LONG, timeoutTicks = 60, batch = "gadgets")
    public static void theTargetDesignatorAimsCannonsInTargetMode(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 3, 3), DecorRegistry.BLOCK_CANNON.block().get());
        BlockCannonBlockEntity cannon = helper.getBlockEntity(new BlockPos(4, 3, 3));
        cannon.aimAt(BlockPos.ZERO);
        helper.setBlock(new BlockPos(12, 4, 3), Blocks.STONE);
        ItemStack target = new ItemStack(GadgetRegistry.TARGET.get());
        ServerPlayer player = player(helper, target, "designator");
        player.setPos(helper.absoluteVec(new Vec3(2.5, 3, 3.5)));
        target.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(cannon.target().equals(helper.absolutePos(new BlockPos(12, 4, 3))), "aimed at " + cannon.target());
        helper.succeed();
    }
}
