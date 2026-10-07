package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.config.FarmConfig;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.blockentity.ACEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.MagnetizerBlockEntity;
import net.scwunge.rotarycraft.item.EngineUpgradeItem;
import net.scwunge.rotarycraft.item.GearUpgradeItem;
import net.scwunge.rotarycraft.item.ShaftCoreItem;
import net.scwunge.rotarycraft.process.DynamoBlockEntity;
import net.scwunge.rotarycraft.process.MagneticMotorBlockEntity;
import net.scwunge.rotarycraft.registry.ProcessRegistry;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.UpgradeRegistry;
import net.scwunge.rotarycraft.upgrade.UpgradeEvents;

/** Tests of the engine upgrades and the integrated gearbox. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class UpgradeGameTests {
    static final BlockPos AT = new BlockPos(2, 2, 2);

    static ItemStack upgrade(EngineUpgradeItem.Kind kind) {
        return new ItemStack(UpgradeRegistry.upgrade(kind));
    }

    static ServerPlayer player(GameTestHelper helper, ItemStack held) {
        ServerPlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("upgrader".getBytes()), "Upgrader"));
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    /** Uses the held item on the block the way a click does; whether the click was taken. */
    static boolean click(GameTestHelper helper, ServerPlayer player, BlockPos rel) {
        BlockPos pos = helper.absolutePos(rel);
        var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        UpgradeEvents.use(event);
        return event.isCanceled();
    }

    // ---- the converter engines ----

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 60, batch = "upgrades_magnetostaticUpgradesRai")
    public static void magnetostaticUpgradesRaiseAConverterATierEachInOrder(GameTestHelper helper) {
        RotaryConfig.override(FarmConfig.CONVERTER_TIER, 0);
        helper.setBlock(AT, ProcessRegistry.MAGNETIC_MOTOR.get());
        MagneticMotorBlockEntity motor = helper.getBlockEntity(AT);
        helper.assertTrue(motor.tier() == 0 && motor.ratedTorque() == 8 && motor.maxSpeed() == 256, "starts at tier " + motor.tier());
        helper.assertTrue(!motor.canUpgradeWith(upgrade(EngineUpgradeItem.Kind.MAGNETOSTATIC3)), "took tier 3 first");
        for (var kind : new EngineUpgradeItem.Kind[] {EngineUpgradeItem.Kind.MAGNETOSTATIC1, EngineUpgradeItem.Kind.MAGNETOSTATIC2, EngineUpgradeItem.Kind.MAGNETOSTATIC3,
                EngineUpgradeItem.Kind.MAGNETOSTATIC4, EngineUpgradeItem.Kind.MAGNETOSTATIC5}) {
            ItemStack stack = upgrade(kind);
            if (kind == EngineUpgradeItem.Kind.MAGNETOSTATIC2) {
                helper.assertTrue(!motor.canUpgradeWith(stack), "took an unmagnetized tier 2");
                ShaftCoreItem.setMagnetization(stack, EngineUpgradeItem.REQUIRED_MAGNETIZATION);
            }
            helper.assertTrue(motor.canUpgradeWith(stack), "refused " + kind);
            motor.upgradeWith(stack);
        }
        helper.assertTrue(motor.tier() == 5 && motor.ratedTorque() == 8 * 1024 && motor.maxSpeed() == 8192, "tier " + motor.tier() + " gives " + motor.ratedTorque() + " at " + motor.maxSpeed());
        helper.assertTrue(!motor.canUpgradeWith(upgrade(EngineUpgradeItem.Kind.MAGNETOSTATIC5)), "went past tier 5");
        RotaryConfig.clearOverride(FarmConfig.CONVERTER_TIER);
            helper.succeed();
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 60, batch = "upgrades_theEfficiencyUpgradeImpr")
    public static void theEfficiencyUpgradeImprovesAConvertersEfficiencyOnce(GameTestHelper helper) {
        RotaryConfig.override(FarmConfig.CONVERTER_TIER, 0);
        helper.setBlock(AT, ProcessRegistry.MAGNETIC_MOTOR.get());
        MagneticMotorBlockEntity motor = helper.getBlockEntity(AT);
        motor.upgradeWith(upgrade(EngineUpgradeItem.Kind.MAGNETOSTATIC1));
        double plain = motor.efficiency();
        helper.assertTrue(motor.canUpgradeWith(upgrade(EngineUpgradeItem.Kind.EFFICIENCY)), "refused");
        motor.upgradeWith(upgrade(EngineUpgradeItem.Kind.EFFICIENCY));
        helper.assertTrue(motor.efficiency() > plain && Math.abs(motor.efficiency() - (1 - 0.04)) < 1e-9, "efficiency " + motor.efficiency() + " after " + plain);
        helper.assertTrue(!motor.canUpgradeWith(upgrade(EngineUpgradeItem.Kind.EFFICIENCY)), "took it twice");
        RotaryConfig.clearOverride(FarmConfig.CONVERTER_TIER);
            helper.succeed();
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 60, batch = "upgrades_usingAnUpgradeOnTheMachi")
    public static void usingAnUpgradeOnTheMachineFitsItAndUsesItUp(GameTestHelper helper) {
        RotaryConfig.override(FarmConfig.CONVERTER_TIER, 0);
        helper.setBlock(AT, ProcessRegistry.MAGNETIC_MOTOR.get());
        MagneticMotorBlockEntity motor = helper.getBlockEntity(AT);
        ItemStack stack = upgrade(EngineUpgradeItem.Kind.MAGNETOSTATIC1);
        ServerPlayer player = player(helper, stack);
        helper.assertTrue(click(helper, player, AT), "the click was not taken");
        helper.assertTrue(motor.tier() == 1 && stack.isEmpty(), "tier " + motor.tier() + ", " + stack.getCount() + " left");
        ItemStack wrong = upgrade(EngineUpgradeItem.Kind.MAGNETOSTATIC4);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrong);
        helper.assertTrue(!click(helper, player, AT) && wrong.getCount() == 1, "tier 4 was taken at tier 1");
        RotaryConfig.clearOverride(FarmConfig.CONVERTER_TIER);
            helper.succeed();
    }

    // ---- the gas engine, the dynamo, the AC engine and the magnetizer ----

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 60, batch = "upgrades")
    public static void thePerformanceUpgradeTurnsAGasEngineIntoAPerformanceEngine(GameTestHelper helper) {
        helper.setBlock(AT, RotaryBlocks.GAS_ENGINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        ItemStack stack = upgrade(EngineUpgradeItem.Kind.PERFORMANCE);
        ServerPlayer player = player(helper, stack);
        helper.assertTrue(click(helper, player, AT), "the click was not taken");
        helper.assertBlockState(AT, s -> s.is(RotaryBlocks.PERFORMANCE_ENGINE.get()) && s.getValue(MachineBlock.FACING) == Direction.EAST, () -> "not a performance engine facing east");
        helper.assertTrue(stack.isEmpty(), "the upgrade was not used up");
        helper.succeed();
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 60, batch = "upgrades")
    public static void theFluxUpgradeLetsADynamoTakeTwiceTheTorque(GameTestHelper helper) {
        WeaponGameTests.spinningFlywheel(helper, AT.below(), 4096, 512, Direction.UP);
        helper.setBlock(AT, ProcessRegistry.DYNAMO.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.UP));
        DynamoBlockEntity dynamo = helper.getBlockEntity(AT);
        helper.runAfterDelay(5, () -> {
            int plain = dynamo.generated();
            helper.assertTrue(plain > 0, "makes nothing");
            helper.assertTrue(dynamo.canUpgradeWith(upgrade(EngineUpgradeItem.Kind.FLUX)), "refused");
            dynamo.upgradeWith(upgrade(EngineUpgradeItem.Kind.FLUX));
            helper.assertTrue(dynamo.generated() == plain * 2, "made " + dynamo.generated() + " after " + plain);
            helper.succeed();
        });
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 100, batch = "upgrades")
    public static void theRedstoneUpgradeRunsAnACEngineWithoutASignal(GameTestHelper helper) {
        helper.setBlock(AT, RotaryBlocks.AC_ENGINE.get());
        ACEngineBlockEntity engine = helper.getBlockEntity(AT);
        ItemStack core = new ItemStack(RotaryItems.SHAFT_CORE.get());
        ShaftCoreItem.setMagnetization(core, 20);
        engine.items().setStackInSlot(0, core);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(engine.getOmega() == 0, "ran without a signal");
            helper.assertTrue(engine.canUpgradeWith(upgrade(EngineUpgradeItem.Kind.REDSTONE)), "refused");
            engine.upgradeWith(upgrade(EngineUpgradeItem.Kind.REDSTONE));
        });
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(engine.getOmega() > 0, "does not run on its own clock");
            helper.assertTrue(!engine.canUpgradeWith(upgrade(EngineUpgradeItem.Kind.REDSTONE)), "took it twice");
            helper.succeed();
        });
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 60, batch = "upgrades")
    public static void theLodestoneUpgradeSpeedsTheMagnetizerAndTheSecondTierUpgradeCanBeMagnetized(GameTestHelper helper) {
        WeaponGameTests.spinningFlywheel(helper, AT.below(), 64, 2048, Direction.UP);
        helper.setBlock(AT, RotaryBlocks.MAGNETIZER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.UP));
        MagnetizerBlockEntity magnetizer = helper.getBlockEntity(AT);
        helper.runAfterDelay(5, () -> {
            int plain = magnetizer.operationTime();
            helper.assertTrue(magnetizer.canUpgradeWith(upgrade(EngineUpgradeItem.Kind.LODESTONE)), "refused");
            magnetizer.upgradeWith(upgrade(EngineUpgradeItem.Kind.LODESTONE));
            helper.assertTrue(magnetizer.operationTime() < plain, "no quicker: " + magnetizer.operationTime() + " against " + plain);
            helper.assertTrue(magnetizer.items().isItemValid(0, upgrade(EngineUpgradeItem.Kind.MAGNETOSTATIC2)), "will not take the tier 2 upgrade");
            helper.assertTrue(!magnetizer.items().isItemValid(0, upgrade(EngineUpgradeItem.Kind.MAGNETOSTATIC3)), "took the tier 3 upgrade");
            helper.succeed();
        });
    }

    // ---- the integrated gearbox ----

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 100, batch = "upgrades_anIntegratedGearboxTrade")
    public static void anIntegratedGearboxTradesTorqueForSpeedAndKeepsThePower(GameTestHelper helper) {
        RotaryConfig.override(FarmConfig.CONVERTER_TIER, 0);
        helper.setBlock(AT, ProcessRegistry.MAGNETIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        MagneticMotorBlockEntity motor = helper.getBlockEntity(AT);
        motor.energy().receiveEnergy(1_000_000, false);
        ItemStack empty = new ItemStack(UpgradeRegistry.GEARS[2].get());
        ServerPlayer player = player(helper, empty);
        helper.assertTrue(!click(helper, player, AT) || empty.getCount() == 1 && motor.integratedGear() == 0, "an empty gearbox was fitted");
        ItemStack torqueMode = GearUpgradeItem.stackFor(4, true, UpgradeRegistry.gearItems());
        player.setItemInHand(InteractionHand.MAIN_HAND, torqueMode);
        helper.assertTrue(click(helper, player, AT) && motor.integratedGear() == 4 && torqueMode.isEmpty(), "gear " + motor.integratedGear());
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(motor.getTorque() == 32, "torque " + motor.getTorque() + " (8 x 4)");
            helper.assertTrue(motor.getOmega() == 64, "speed " + motor.getOmega() + " (256 / 4)");
            helper.assertTrue(motor.getTorque() * motor.getOmega() == 8 * 256, "power not kept");
            RotaryConfig.clearOverride(FarmConfig.CONVERTER_TIER);
            helper.succeed();
        });
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 100, batch = "upgrades_aSpeedModeGearboxGivesSp")
    public static void aSpeedModeGearboxGivesSpeedForTorque(GameTestHelper helper) {
        RotaryConfig.override(FarmConfig.CONVERTER_TIER, 0);
        helper.setBlock(AT, ProcessRegistry.MAGNETIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        MagneticMotorBlockEntity motor = helper.getBlockEntity(AT);
        motor.energy().receiveEnergy(1_000_000, false);
        ItemStack speedMode = GearUpgradeItem.stackFor(2, false, UpgradeRegistry.gearItems());
        ServerPlayer player = player(helper, speedMode);
        helper.assertTrue(click(helper, player, AT) && motor.integratedGear() == -2, "gear " + motor.integratedGear());
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(motor.getTorque() == 4 && motor.getOmega() == 512, "torque " + motor.getTorque() + ", speed " + motor.getOmega());
            RotaryConfig.clearOverride(FarmConfig.CONVERTER_TIER);
            helper.succeed();
        });
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 100, batch = "upgrades_aGearboxCannotBeFittedTo")
    public static void aGearboxCannotBeFittedToARunningEngineAndComesBackWhenItIsBroken(GameTestHelper helper) {
        RotaryConfig.override(FarmConfig.CONVERTER_TIER, 0);
        helper.setBlock(AT, ProcessRegistry.MAGNETIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        MagneticMotorBlockEntity motor = helper.getBlockEntity(AT);
        motor.energy().receiveEnergy(1_000_000, false);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(motor.getOmega() > 0, "not running");
            helper.assertTrue(!motor.applyIntegratedGear(2), "fitted while running");
            motor.setRemoved();
        });
        helper.runAfterDelay(21, () -> {
            helper.setBlock(AT, net.minecraft.world.level.block.Blocks.AIR);
            helper.setBlock(AT, ProcessRegistry.MAGNETIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
            MagneticMotorBlockEntity fresh = helper.getBlockEntity(AT);
            helper.assertTrue(fresh.applyIntegratedGear(8), "not fitted to a still engine");
            helper.getLevel().destroyBlock(helper.absolutePos(AT), false);
            boolean back = false;
            for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds())) {
                back |= e.getItem().getItem() instanceof GearUpgradeItem g && g.ratio() == 8 && GearUpgradeItem.ratioOf(e.getItem()) == 8;
            }
            helper.assertTrue(back, "the gearbox was not given back");
            RotaryConfig.clearOverride(FarmConfig.CONVERTER_TIER);
            helper.succeed();
        });
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 60, batch = "upgrades")
    public static void aGearboxFillsFromLubricantAndLiquidNitrogenOnly(GameTestHelper helper) {
        ItemStack frame = new ItemStack(UpgradeRegistry.GEARS[1].get());
        var handler = frame.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
        helper.assertTrue(handler != null, "no fluid handler");
        helper.assertTrue(handler.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 500), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 0, "took water");
        int taken = handler.fill(new net.neoforged.neoforge.fluids.FluidStack(net.scwunge.rotarycraft.registry.RotaryFluids.LUBRICANT.get(), 700), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(taken == 500, "took " + taken);
        helper.assertTrue(GearUpgradeItem.ratioOf(frame) == 2, "ratio " + GearUpgradeItem.ratioOf(frame));
        helper.succeed();
    }
}
