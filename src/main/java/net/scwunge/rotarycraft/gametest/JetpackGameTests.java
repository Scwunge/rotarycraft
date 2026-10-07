package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.charged.Jetpack;
import net.scwunge.rotarycraft.charged.JetpackEvents;
import net.scwunge.rotarycraft.charged.JetpackItem;
import net.scwunge.rotarycraft.charged.JetpackRecipe;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.registry.GadgetRegistry;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryParts;
import net.scwunge.rotarycraft.registry.ToolRegistry;
import net.scwunge.rotarycraft.tool.BedrockTools;
import net.scwunge.rotarycraft.tool.Forced;

import java.util.List;

/** Tests of the jetpacks: their tanks, their flying, their upgrades and how they are made. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class JetpackGameTests {
    static FakePlayer wearer(GameTestHelper helper, ItemStack pack, String name) {
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(name.getBytes()), name));
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().flying = false;
        player.setItemSlot(EquipmentSlot.CHEST, pack);
        player.setPos(helper.absoluteVec(new Vec3(4.5, 3, 3.5)));
        player.setDeltaMovement(Vec3.ZERO);
        player.jumping = false;
        player.setShiftKeyDown(false);
        player.xxa = 0;
        player.zza = 0;
        player.fallDistance = 0;
        player.getPersistentData().putInt("rotarycraft_pack_hold", 0);
        return player;
    }

    static void tick(Player player, int times) {
        for (int i = 0; i < times; i++) {
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        }
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 40, batch = "jetpack")
    public static void aPackTakesEthanolAndJetFuelAndNothingElse(GameTestHelper helper) {
        ItemStack pack = new ItemStack(GadgetRegistry.JETPACK.get());
        IFluidHandler tank = pack.getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(tank != null, "no tank");
        helper.assertTrue(tank.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "took water");
        helper.assertTrue(tank.fill(new FluidStack(RotaryFluids.ETHANOL.get(), 20000), IFluidHandler.FluidAction.EXECUTE) == 20000, "no ethanol");
        helper.assertTrue(tank.fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "mixed fuels");
        helper.assertTrue(tank.fill(new FluidStack(RotaryFluids.ETHANOL.get(), 20000), IFluidHandler.FluidAction.EXECUTE) == Jetpack.CAPACITY - 20000, "no cap");
        helper.assertTrue(Jetpack.fuel(pack) == Jetpack.CAPACITY && !Jetpack.jetFueled(pack), "fuel " + Jetpack.fuel(pack));
        helper.assertTrue(tank.drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "drained");
        helper.succeed();
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 40, batch = "jetpackJetOnly")
    public static void whenJetFuelIsRequiredEthanolIsRefused(GameTestHelper helper) {
        RotaryConfig.override(RotaryConfig.JETPACK_NEEDS_JET_FUEL, true);
        try {
            IFluidHandler tank = new ItemStack(GadgetRegistry.STEEL_JETPACK.get()).getCapability(Capabilities.FluidHandler.ITEM);
            helper.assertTrue(tank.fill(new FluidStack(RotaryFluids.ETHANOL.get(), 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "took ethanol");
            helper.assertTrue(tank.fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "no jet fuel");
        } finally {
            RotaryConfig.clearOverride(RotaryConfig.JETPACK_NEEDS_JET_FUEL);
        }
        helper.succeed();
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 60, batch = "jetpack")
    public static void holdingJumpLiftsAndBurnsFuelAfterAMoment(GameTestHelper helper) {
        ItemStack pack = Jetpack.filled(new ItemStack(GadgetRegistry.STEEL_JETPACK.get()), false, 1000);
        FakePlayer player = wearer(helper, pack, "lifter");
        player.jumping = true;
        tick(player, JetpackEvents.HOLD - 1);
        helper.assertTrue(Jetpack.fuel(pack) == 1000 && player.getDeltaMovement().y == 0, "fired on a hop");
        helper.onEachTick(() -> tick(player, 1));
        helper.succeedWhen(() -> {
            helper.assertTrue(player.getDeltaMovement().y > 0.2, "no lift " + player.getDeltaMovement().y);
            helper.assertTrue(Jetpack.fuel(pack) < 1000, "no fuel used");
            helper.assertTrue(player.fallDistance <= 0, "will fall hard");
        });
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 60, batch = "jetpack")
    public static void aPackIdleBurnsNothing(GameTestHelper helper) {
        ItemStack pack = Jetpack.filled(new ItemStack(GadgetRegistry.STEEL_JETPACK.get()), false, 1000);
        FakePlayer player = wearer(helper, pack, "idler");
        helper.onEachTick(() -> tick(player, 1));
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(Jetpack.fuel(pack) == 1000, "burned while idle");
            helper.succeed();
        });
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 80, batch = "jetpack")
    public static void anEmptyPackDoesNothingAndBedrockBurnsTwice(GameTestHelper helper) {
        FakePlayer empty = wearer(helper, new ItemStack(GadgetRegistry.JETPACK.get()), "dry");
        empty.jumping = true;
        ItemStack steel = Jetpack.filled(new ItemStack(GadgetRegistry.STEEL_JETPACK.get()), false, 5000);
        FakePlayer a = wearer(helper, steel, "steelburn");
        a.jumping = true;
        ItemStack bedrock = Jetpack.filled(Forced.stackOf(GadgetRegistry.BEDROCK_JETPACK.get(), helper.getLevel().registryAccess(), BedrockTools.CHESTPLATE), false, 5000);
        FakePlayer b = wearer(helper, bedrock, "bedburn");
        b.jumping = true;
        helper.onEachTick(() -> {
            tick(empty, 1);
            tick(a, 1);
            tick(b, 1);
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(empty.getDeltaMovement().y == 0, "flew on nothing");
            int steelUsed = 5000 - Jetpack.fuel(steel);
            int bedrockUsed = 5000 - Jetpack.fuel(bedrock);
            helper.assertTrue(steelUsed > 0 && bedrockUsed == steelUsed * 2, "steel " + steelUsed + " bedrock " + bedrockUsed);
            helper.succeed();
        });
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 60, batch = "jetpack")
    public static void sneakingHoversInsteadOfClimbing(GameTestHelper helper) {
        ItemStack pack = Jetpack.filled(new ItemStack(GadgetRegistry.STEEL_JETPACK.get()), false, 5000);
        FakePlayer player = wearer(helper, pack, "hoverer");
        player.setPos(player.position().add(0, 6, 0));
        player.setOnGround(false);
        player.setDeltaMovement(0, -0.6, 0);
        player.jumping = true;
        player.setShiftKeyDown(true);
        tick(player, JetpackEvents.HOLD + 1);
        helper.assertTrue(player.getDeltaMovement().y <= 0 && player.getDeltaMovement().y > -0.6, "motion " + player.getDeltaMovement().y);
        helper.succeed();
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 60, batch = "jetpack")
    public static void aFlameOutDestroysThePackAndLeavesTheChestplate(GameTestHelper helper) {
        ItemStack steel = Jetpack.filled(new ItemStack(GadgetRegistry.STEEL_JETPACK.get()), false, 1000);
        FakePlayer a = wearer(helper, steel, "boomsteel");
        JetpackEvents.explode(a, (Jetpack) steel.getItem());
        helper.assertTrue(a.getItemBySlot(EquipmentSlot.CHEST).is(ToolRegistry.STEEL_CHESTPLATE.get()), "no chestplate");
        helper.assertTrue(a.hurtMarked && a.getDeltaMovement().y > 1, "not thrown");

        ItemStack bedrock = Forced.stackOf(GadgetRegistry.BEDROCK_JETPACK.get(), helper.getLevel().registryAccess(), BedrockTools.CHESTPLATE);
        FakePlayer b = wearer(helper, bedrock, "boombed");
        JetpackEvents.explode(b, (Jetpack) bedrock.getItem());
        ItemStack left = b.getItemBySlot(EquipmentSlot.CHEST);
        helper.assertTrue(left.is(ToolRegistry.BEDROCK_CHESTPLATE.get()) && Forced.intact(left, helper.getLevel().registryAccess(), BedrockTools.CHESTPLATE), "no enchanted bedrock chestplate");

        FakePlayer c = wearer(helper, new ItemStack(GadgetRegistry.JETPACK.get()), "boomplain");
        JetpackEvents.explode(c, (Jetpack) GadgetRegistry.JETPACK.get());
        helper.assertTrue(c.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "plain pack left");
        helper.succeed();
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 60, batch = "jetpack")
    public static void wingsSlowAFallAndSneakUsingFoldsThem(GameTestHelper helper) {
        ItemStack pack = new ItemStack(GadgetRegistry.STEEL_JETPACK.get());
        Jetpack.Upgrade.WING.set(pack);
        helper.assertTrue(Jetpack.winged(pack), "not winged");
        FakePlayer player = wearer(helper, pack, "glider");
        player.setPos(player.position().add(0, 6, 0));
        player.setOnGround(false);
        player.setDeltaMovement(0, -1.5, 0);
        player.fallDistance = 10;
        tick(player, 1);
        helper.assertTrue(player.getDeltaMovement().y > -1.5, "not slowed " + player.getDeltaMovement().y);
        helper.assertTrue(player.fallDistance < 10, "fall not eased");
        helper.assertTrue(JetpackItem.toggleWings(pack) && !Jetpack.winged(pack) && Jetpack.Upgrade.WING.on(pack), "no fold");
        helper.assertTrue(JetpackItem.toggleWings(pack) && Jetpack.winged(pack), "no unfold");
        helper.assertTrue(!JetpackItem.toggleWings(new ItemStack(GadgetRegistry.JETPACK.get())), "folded wingless pack");
        helper.succeed();
    }

    private static CraftingInput grid(ItemStack... stacks) {
        return CraftingInput.of(3, 3, java.util.Arrays.asList(java.util.Arrays.copyOf(stacks, 9, ItemStack[].class)).stream()
                .map(s -> s == null ? ItemStack.EMPTY : s).toList());
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 40, batch = "jetpack")
    public static void theWorktableRecipesBuildUpgradeAndTakeApartPacks(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        JetpackRecipe recipe = new JetpackRecipe(net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT);
        ItemStack jetpack = Jetpack.filled(new ItemStack(GadgetRegistry.JETPACK.get()), true, 12000);
        Jetpack.Upgrade.COOLING.set(jetpack);

        // a chestplate and a jetpack make a steel pack that keeps the fuel and upgrades
        CraftingInput build = grid(new ItemStack(ToolRegistry.STEEL_CHESTPLATE.get()), jetpack);
        helper.assertTrue(recipe.matches(build, helper.getLevel()), "build did not match");
        ItemStack steel = recipe.assemble(build, access);
        helper.assertTrue(steel.is(GadgetRegistry.STEEL_JETPACK.get()) && Jetpack.fuel(steel) == 12000 && Jetpack.jetFueled(steel)
                && Jetpack.Upgrade.COOLING.on(steel), "steel pack " + steel);

        // and a bedrock one
        CraftingInput bed = grid(Forced.stackOf(ToolRegistry.BEDROCK_CHESTPLATE.get(), access, BedrockTools.CHESTPLATE), jetpack);
        ItemStack bedrock = recipe.assemble(bed, access);
        helper.assertTrue(bedrock.is(GadgetRegistry.BEDROCK_JETPACK.get()) && Forced.intact(bedrock, access, BedrockTools.CHESTPLATE), "bedrock pack");

        // wings from three ingots of the pack's metal
        ItemStack ingot = new ItemStack(net.minecraft.world.item.Items.IRON_INGOT);
        helper.assertTrue(!recipe.matches(grid(steel, ingot, ingot, ingot), helper.getLevel()), "wings from iron");
        ItemStack bedIngot = new ItemStack(RotaryParts.part("bedrock_ingot").get());
        CraftingInput wings = grid(bedrock, bedIngot, bedIngot, bedIngot);
        helper.assertTrue(recipe.matches(wings, helper.getLevel()), "no wings from bedrock");
        helper.assertTrue(Jetpack.Upgrade.WING.on(recipe.assemble(wings, access)), "wings not added");
        helper.assertTrue(!recipe.matches(grid(bedrock, bedIngot, bedIngot), helper.getLevel()), "wings from two ingots");

        // fins and thrust
        ItemStack fin = new ItemStack(RotaryBlocks.COOLING_FIN.asItem());
        helper.assertTrue(recipe.matches(grid(new ItemStack(GadgetRegistry.JETPACK.get()), fin, fin), helper.getLevel()), "no fins");
        helper.assertTrue(!recipe.matches(grid(steel, fin, fin), helper.getLevel()), "second fin set"); // steel already has cooling
        CraftingInput thrust = grid(steel, new ItemStack(RotaryBlocks.JET_ENGINE.asItem()));
        helper.assertTrue(recipe.matches(thrust, helper.getLevel()) && Jetpack.Upgrade.JET.on(recipe.assemble(thrust, access)), "no thrust");

        // taking one apart gives the chestplate and the jetpack back, fuel and all
        CraftingInput apart = grid(steel);
        helper.assertTrue(recipe.matches(apart, helper.getLevel()) && recipe.assemble(apart, access).is(ToolRegistry.STEEL_CHESTPLATE.get()), "no chestplate");
        ItemStack back = recipe.getRemainingItems(apart).get(0);
        helper.assertTrue(back.is(GadgetRegistry.JETPACK.get()) && Jetpack.fuel(back) == 12000 && Jetpack.Upgrade.COOLING.on(back), "jetpack " + back);
        helper.assertTrue(!recipe.matches(grid(new ItemStack(GadgetRegistry.JETPACK.get())), helper.getLevel()), "plain pack came apart");
        helper.succeed();
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 60, batch = "jetpack")
    public static void theFuelTankFillsAndEmptiesAMachineAndRefuelsAPack(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 2, 2);
        helper.setBlock(at, RotaryBlocks.RESERVOIR.get());
        net.scwunge.rotarycraft.blockentity.ReservoirBlockEntity reservoir = helper.getBlockEntity(at);
        reservoir.tank().setFluid(new FluidStack(RotaryFluids.ETHANOL.get(), 5000));
        ItemStack tank = new ItemStack(GadgetRegistry.FUEL_TANK.get());
        FakePlayer player = wearer(helper, new ItemStack(GadgetRegistry.STEEL_JETPACK.get()), "refueler");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tank);
        BlockPos abs = helper.absolutePos(at);
        var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false);
        var context = new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        tank.getItem().useOn(context);
        helper.assertTrue(net.scwunge.rotarycraft.charged.FuelTankItem.contents(tank).getAmount() == 5000 && reservoir.tank().isEmpty(), "tank did not take the fuel");

        // used in the air, it tops up the pack you are wearing
        tank.getItem().use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        ItemStack pack = player.getItemBySlot(EquipmentSlot.CHEST);
        helper.assertTrue(Jetpack.fuel(pack) == 5000 && net.scwunge.rotarycraft.charged.FuelTankItem.contents(tank).isEmpty(), "pack got " + Jetpack.fuel(pack));

        // water is not fuel
        IFluidHandler handler = tank.getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(handler.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "took water");
        helper.assertTrue(handler.fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 20000), IFluidHandler.FluidAction.EXECUTE) == net.scwunge.rotarycraft.charged.FuelTankItem.CAPACITY, "no cap");
        helper.succeed();
    }

    @GameTest(template = WeaponGameTests.WIDE, batch = "jetpack_shell", timeoutTicks = 300)
    public static void anExplosiveShellBlowsUpWhereItLands(GameTestHelper helper) {
        net.scwunge.rotarycraft.weapon.turret.RailGunBlockEntity gun = WeaponGameTests.railgun(helper);
        gun.items().setStackInSlot(0, new ItemStack(GadgetRegistry.EXPLOSIVE_SHELL.get(), 2));
        gun.items().setStackInSlot(1, new ItemStack(net.scwunge.rotarycraft.registry.WeaponRegistry.RAILGUN_AMMO.get(0).get()));
        helper.assertTrue(GadgetRegistry.EXPLOSIVE_SHELL.get().requiredTorque() == 0 && GadgetRegistry.EXPLOSIVE_SHELL.get().explosive(), "not a shell");
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(gun.bestAmmo() == net.scwunge.rotarycraft.registry.WeaponRegistry.RAILGUN_AMMO.get(0).get(), "shell preferred over a slug");
            gun.items().setStackInSlot(1, ItemStack.EMPTY);
        });
        helper.setBlock(new BlockPos(12, 1, 2), net.minecraft.world.level.block.Blocks.STONE);
        net.minecraft.world.entity.monster.Husk husk = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.HUSK, new Vec3(12.5, 2, 2.5));
        helper.succeedWhen(() -> {
            helper.assertTrue(gun.items().getStackInSlot(0).getCount() < 2, "no shell used");
            helper.assertTrue(!husk.isAlive() || husk.getHealth() < husk.getMaxHealth(), "husk not hit");
        });
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 60, batch = "jetpack")
    public static void anEthanolMinecartRunsOnCrystalsAndDropsItself(GameTestHelper helper) {
        BlockPos rail = new BlockPos(2, 2, 2);
        helper.setBlock(rail, net.minecraft.world.level.block.Blocks.RAIL);
        FakePlayer player = wearer(helper, ItemStack.EMPTY, "conductor");
        ItemStack cartItem = new ItemStack(GadgetRegistry.ETHANOL_MINECART.get());
        BlockPos abs = helper.absolutePos(rail);
        var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, cartItem);
        cartItem.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(cartItem.isEmpty(), "cart item kept");
        net.scwunge.rotarycraft.vehicle.GasMinecart cart = helper.getLevel().getEntitiesOfClass(net.scwunge.rotarycraft.vehicle.GasMinecart.class,
                new net.minecraft.world.phys.AABB(abs).inflate(1)).get(0);
        player.setPos(cart.position().add(-1, 0, 0));
        ItemStack crystals = new ItemStack(net.scwunge.rotarycraft.registry.RotaryItems.ETHANOL_CRYSTALS.get(), 2);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, crystals);
        cart.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(cart.fuel() == net.scwunge.rotarycraft.vehicle.GasMinecart.CRYSTAL_TICKS && crystals.getCount() == 1, "fuel " + cart.fuel());
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(cart.position().distanceTo(Vec3.atCenterOf(abs)) > 1, "cart did not move");
            helper.assertTrue(cart.fuel() < net.scwunge.rotarycraft.vehicle.GasMinecart.CRYSTAL_TICKS, "fuel not burned");
            helper.succeed();
        });
    }
}
