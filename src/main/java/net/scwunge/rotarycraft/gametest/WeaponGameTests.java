package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.power.FlywheelType;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import net.minecraft.world.item.Items;
import net.scwunge.rotarycraft.weapon.turret.AntiAirBlockEntity;
import net.scwunge.rotarycraft.weapon.turret.FreezeGunBlockEntity;
import net.scwunge.rotarycraft.weapon.turret.GatlingBlockEntity;
import net.scwunge.rotarycraft.weapon.turret.LaserGunBlockEntity;
import net.scwunge.rotarycraft.weapon.turret.RailGunBlockEntity;
import net.scwunge.rotarycraft.weapon.turret.TurretBlockEntity;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class WeaponGameTests {
    static final String TEMPLATE = RotaryGameTests.TEMPLATE;
    static final String WIDE = "empty20x8x7";
    static final BlockPos TURRET = new BlockPos(2, 2, 2);

    /** A bedrock flywheel under {@code pos}, already spinning with this torque and speed (it coasts, so it keeps them for the test). */
    static void spinningFlywheel(GameTestHelper helper, BlockPos pos, int torque, int omega) {
        helper.setBlock(pos, RotaryBlocks.FLYWHEELS.get(FlywheelType.BEDROCK).get().defaultBlockState().setValue(MachineBlock.FACING, Direction.UP));
        CompoundTag tag = new CompoundTag();
        tag.putInt("torque", torque);
        tag.putInt("omega", omega);
        helper.getBlockEntity(pos).loadCustomOnly(tag, helper.getLevel().registryAccess());
    }

    static RailGunBlockEntity railgun(GameTestHelper helper) {
        spinningFlywheel(helper, TURRET.below(), 4096, 1100);
        helper.setBlock(TURRET, WeaponRegistry.RAILGUN.get().defaultBlockState());
        return helper.getBlockEntity(TURRET);
    }

    /**
     * Ten blocks out: as in the original, a turret checks its aim from its block's corner, so it holds fire at point-blank
     * range where that differs too much from the centre it aims from.
     */
    @GameTest(template = WIDE, batch = "weapon_railgunshootsahostilemob", timeoutTicks = 300)
    public static void railgunShootsAHostileMob(GameTestHelper helper) {
        RailGunBlockEntity gun = railgun(helper);
        gun.items().setStackInSlot(0, new ItemStack(WeaponRegistry.RAILGUN_AMMO.get(3).get(), 4));
        // level with the turret: it cannot aim more than 10 degrees down
        helper.setBlock(new BlockPos(12, 1, 2), net.minecraft.world.level.block.Blocks.STONE);
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(12.5, 2, 2.5));
        helper.succeedWhen(() -> {
            helper.assertTrue(gun.hasEnoughPower(), "railgun has no power");
            helper.assertTrue(husk.getHealth() < husk.getMaxHealth(), "husk not hit (aim " + gun.phi + ", " + gun.theta + ")");
            helper.assertTrue(gun.items().getStackInSlot(0).getCount() < 4, "no ammunition used");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void railgunNeedsTorqueForHeavyAmmo(GameTestHelper helper) {
        RailGunBlockEntity gun = railgun(helper);
        gun.items().setStackInSlot(0, new ItemStack(WeaponRegistry.RAILGUN_AMMO.get(15).get()));
        gun.items().setStackInSlot(1, new ItemStack(WeaponRegistry.RAILGUN_AMMO.get(12).get()));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(WeaponRegistry.RAILGUN_AMMO.get(15).get().requiredTorque() == 4096, "tier 15 needs 4096 N*m");
            helper.assertTrue(gun.bestAmmo() == WeaponRegistry.RAILGUN_AMMO.get(15).get(), "should pick tier 15 with 4096 N*m");
            spinningFlywheel(helper, TURRET.below(), 2048, 2048);
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(gun.bestAmmo() == WeaponRegistry.RAILGUN_AMMO.get(12).get(), "2048 N*m should only throw up to tier 13");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void ownerAndWhitelistAreSafe(GameTestHelper helper) {
        TurretBlockEntity gun = railgun(helper);
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        gun.setOwner(owner);
        helper.assertTrue(gun.isSafe(owner), "owner is a target");
        helper.assertFalse(gun.isSafe(stranger), "a stranger is safe without being on the whitelist");
        helper.assertTrue(gun.addSafePlayer("Friend") && gun.safePlayers().contains("Friend"), "whitelist add failed");
        helper.assertFalse(gun.addSafePlayer("Friend"), "added twice");
        gun.removeSafePlayer("Friend");
        helper.assertTrue(gun.safePlayers().isEmpty(), "whitelist remove failed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void hangingTurretTakesPowerFromAbove(GameTestHelper helper) {
        helper.setBlock(TURRET.above(), RotaryBlocks.FLYWHEELS.get(FlywheelType.BEDROCK).get().defaultBlockState().setValue(MachineBlock.FACING, Direction.DOWN));
        CompoundTag tag = new CompoundTag();
        tag.putInt("torque", 4096);
        tag.putInt("omega", 1024);
        helper.getBlockEntity(TURRET.above()).loadCustomOnly(tag, helper.getLevel().registryAccess());
        helper.setBlock(TURRET, WeaponRegistry.RAILGUN.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.DOWN));
        RailGunBlockEntity gun = helper.getBlockEntity(TURRET);
        helper.succeedWhen(() -> {
            helper.assertTrue(gun.dir() == -1, "not hanging");
            helper.assertTrue(gun.hasEnoughPower(), "hanging turret got no power from above");
        });
    }

    @GameTest(template = WIDE, batch = "weapon_freezegunfreezesahostilemob", timeoutTicks = 300)
    public static void freezeGunFreezesAHostileMob(GameTestHelper helper) {
        spinningFlywheel(helper, TURRET.below(), 4096, 1100);
        helper.setBlock(TURRET, WeaponRegistry.FREEZE_GUN.get().defaultBlockState());
        FreezeGunBlockEntity gun = helper.getBlockEntity(TURRET);
        gun.items().setStackInSlot(0, new ItemStack(Items.SNOW_BLOCK, 2));
        helper.setBlock(new BlockPos(12, 1, 2), net.minecraft.world.level.block.Blocks.STONE);
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(12.5, 2, 2.5));
        helper.succeedWhen(() -> {
            helper.assertTrue(husk.hasEffect(WeaponRegistry.FREEZE), "husk not frozen (aim " + gun.phi + ", " + gun.theta + ")");
            helper.assertTrue(husk.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) <= 0.0001, "frozen husk can still move");
        });
    }

    @GameTest(template = WIDE, batch = "weapon_freezegunturnsiceintosnowballs", timeoutTicks = 200)
    public static void freezeGunTurnsIceIntoSnowballs(GameTestHelper helper) {
        spinningFlywheel(helper, TURRET.below(), 4096, 1100);
        helper.setBlock(TURRET, WeaponRegistry.FREEZE_GUN.get().defaultBlockState());
        FreezeGunBlockEntity gun = helper.getBlockEntity(TURRET);
        helper.assertTrue(gun.isAmmo(new ItemStack(Items.ICE)) && gun.isAmmo(new ItemStack(Items.SNOWBALL)) && !gun.isAmmo(new ItemStack(Items.DIRT)), "wrong ammo rules");
        helper.setBlock(new BlockPos(12, 1, 2), net.minecraft.world.level.block.Blocks.STONE);
        // a target on aim, so the gun works (it also fires one snowball a second)
        helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(12.5, 2, 2.5));
        gun.items().setStackInSlot(0, new ItemStack(Items.ICE));
        helper.succeedWhen(() -> {
            int balls = 0;
            for (int i = 0; i < gun.items().getSlots(); i++) {
                helper.assertFalse(gun.items().getStackInSlot(i).is(Items.ICE), "ice not converted (aim " + gun.phi + ", " + gun.theta + ", power " + gun.hasEnoughPower() + ", torque " + gun.getTorque() + ", omega " + gun.getOmega() + ")");
                balls += gun.items().getStackInSlot(i).is(Items.SNOWBALL) ? gun.items().getStackInSlot(i).getCount() : 0;
            }
            helper.assertTrue(balls >= 10, "only " + balls + " snowballs");
        });
    }

    @GameTest(template = WIDE, batch = "weapon_antiairshootsaflyer", timeoutTicks = 400)
    public static void antiAirShootsAFlyer(GameTestHelper helper) {
        spinningFlywheel(helper, TURRET.below(), 4096, 1100);
        helper.setBlock(TURRET, WeaponRegistry.ANTI_AIR.get().defaultBlockState());
        AntiAirBlockEntity gun = helper.getBlockEntity(TURRET);
        gun.items().setStackInSlot(0, new ItemStack(net.scwunge.rotarycraft.registry.RotaryItems.SCRAP.get(), 16));
        net.minecraft.world.entity.monster.Phantom phantom = helper.spawnWithNoFreeWill(EntityType.PHANTOM, new Vec3(12.5, 4, 2.5));
        helper.succeedWhen(() -> helper.assertTrue(phantom.getHealth() < phantom.getMaxHealth() || !phantom.isAlive(), "phantom not hit (aim " + gun.phi + ", " + gun.theta + ")"));
    }

    @GameTest(template = WIDE, batch = "weapon_antiairignoresgroundmobs", timeoutTicks = 100)
    public static void antiAirIgnoresGroundMobs(GameTestHelper helper) {
        spinningFlywheel(helper, TURRET.below(), 4096, 1100);
        helper.setBlock(TURRET, WeaponRegistry.ANTI_AIR.get().defaultBlockState());
        AntiAirBlockEntity gun = helper.getBlockEntity(TURRET);
        gun.items().setStackInSlot(0, new ItemStack(net.scwunge.rotarycraft.registry.RotaryItems.SCRAP.get(), 16));
        helper.setBlock(new BlockPos(12, 1, 2), net.minecraft.world.level.block.Blocks.STONE);
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(12.5, 2, 2.5));
        // a mob without AI never lands by itself; stand it on the stone
        helper.onEachTick(() -> husk.setOnGround(true));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(husk.getHealth() == husk.getMaxHealth(), "anti-air shot a ground mob");
            helper.succeed();
        });
    }

    /** Ball bearings put in the first slot travel down the belt into the clip, which takes a reload to bring forward. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void gatlingFeedsAmmoDownTheBelt(GameTestHelper helper) {
        spinningFlywheel(helper, TURRET.below(), 4096, 1100);
        helper.setBlock(TURRET, WeaponRegistry.GATLING.get().defaultBlockState());
        GatlingBlockEntity gun = helper.getBlockEntity(TURRET);
        ItemStack ball = new ItemStack(net.scwunge.rotarycraft.registry.RotaryParts.part("ball_bearing").get(), 32);
        helper.assertTrue(gun.items().insertItem(0, ball, false).isEmpty(), "first slot refused ball bearings");
        helper.assertTrue(gun.items().insertItem(5, ball, true).getCount() == 32, "a middle slot took ammunition");
        helper.succeedWhen(() -> helper.assertTrue(gun.items().getStackInSlot(GatlingBlockEntity.CLIP_SLOT).getCount() == 32, "ammunition not in the clip"));
    }

    @GameTest(template = WIDE, batch = "weapon_gatlinghurtsahostilemob", timeoutTicks = 500)
    public static void gatlingHurtsAHostileMob(GameTestHelper helper) {
        spinningFlywheel(helper, TURRET.below(), 4096, 1100);
        helper.setBlock(TURRET, WeaponRegistry.GATLING.get().defaultBlockState());
        GatlingBlockEntity gun = helper.getBlockEntity(TURRET);
        gun.items().setStackInSlot(GatlingBlockEntity.CLIP_SLOT, new ItemStack(net.scwunge.rotarycraft.registry.RotaryParts.part("ball_bearing").get(), 64));
        helper.setBlock(new BlockPos(12, 1, 2), net.minecraft.world.level.block.Blocks.STONE);
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(12.5, 2, 2.5));
        helper.succeedWhen(() -> helper.assertTrue(husk.getHealth() < husk.getMaxHealth(), "husk not hit (aim " + gun.phi + ", " + gun.theta + ")"));
    }

    @GameTest(template = WIDE, batch = "weapon_gatlingneedsspeed", timeoutTicks = 200)
    public static void gatlingNeedsSpeed(GameTestHelper helper) {
        spinningFlywheel(helper, TURRET.below(), 4096, 512);
        helper.setBlock(TURRET, WeaponRegistry.GATLING.get().defaultBlockState());
        GatlingBlockEntity gun = helper.getBlockEntity(TURRET);
        gun.items().setStackInSlot(GatlingBlockEntity.CLIP_SLOT, new ItemStack(net.scwunge.rotarycraft.registry.RotaryParts.part("ball_bearing").get(), 64));
        helper.setBlock(new BlockPos(12, 1, 2), net.minecraft.world.level.block.Blocks.STONE);
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(12.5, 2, 2.5));
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(husk.getHealth() == husk.getMaxHealth(), "fired at 512 rad/s");
            helper.succeed();
        });
    }

    static LaserGunBlockEntity laser(GameTestHelper helper, int omega) {
        spinningFlywheel(helper, TURRET.below(), 4096, omega);
        helper.setBlock(TURRET, WeaponRegistry.LASER_GUN.get().defaultBlockState());
        return helper.getBlockEntity(TURRET);
    }

    /** Aimed straight ahead (+z) with no target, the beam melts the first block in its way and stops there. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void laserTurnsSandToGlassAndStops(GameTestHelper helper) {
        LaserGunBlockEntity gun = laser(helper, 4096);
        helper.setBlock(new BlockPos(2, 2, 3), net.minecraft.world.level.block.Blocks.SAND);
        helper.setBlock(new BlockPos(2, 2, 4), net.minecraft.world.level.block.Blocks.DIRT);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.GLASS, new BlockPos(2, 2, 3));
            helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.DIRT, new BlockPos(2, 2, 4));
            helper.assertTrue(gun.beamLength() > 0 && gun.beamLength() < 10, "beam length " + gun.beamLength());
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void laserBurnsCreaturesInTheBeam(GameTestHelper helper) {
        laser(helper, 4096);
        helper.setBlock(new BlockPos(2, 2, 4), net.minecraft.world.level.block.Blocks.OBSIDIAN);
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(2.5, 2, 3.5));
        helper.succeedWhen(() -> {
            helper.assertTrue(husk.getHealth() < husk.getMaxHealth(), "husk not hurt");
            helper.assertTrue(husk.getRemainingFireTicks() > 0, "husk not burning");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void laserNeedsFullPowerAndRespectsMobGriefing(GameTestHelper helper) {
        // 4096 N*m at 1024 rad/s is 4.2 MW: not enough for the laser's 8.4 MW
        LaserGunBlockEntity weak = laser(helper, 1024);
        helper.setBlock(new BlockPos(2, 1, 3), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(new BlockPos(2, 2, 3), net.minecraft.world.level.block.Blocks.SAND);
        helper.runAfterDelay(10, () -> {
            helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.SAND, new BlockPos(2, 2, 3));
            helper.assertTrue(weak.beamLength() == 0, "underpowered laser fires");
            spinningFlywheel(helper, TURRET.below(), 4096, 4096);
            var rule = helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
            rule.set(false, helper.getLevel().getServer());
            helper.runAfterDelay(10, () -> {
                helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.SAND, new BlockPos(2, 2, 3));
                rule.set(true, helper.getLevel().getServer());
                helper.succeedWhen(() -> helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.GLASS, new BlockPos(2, 2, 3)));
            });
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void flameTurretTakesOnlyFuel(GameTestHelper helper) {
        spinningFlywheel(helper, TURRET.below(), 1024, 1024);
        helper.setBlock(TURRET, WeaponRegistry.FLAME_TURRET.get().defaultBlockState());
        var in = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, helper.absolutePos(TURRET), Direction.NORTH);
        helper.assertTrue(in.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 0, "took water");
        helper.assertTrue(in.fill(new net.neoforged.neoforge.fluids.FluidStack(net.scwunge.rotarycraft.registry.RotaryFluids.ETHANOL.get(), 1500),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 1000, "ethanol not accepted up to a bucket");
        helper.assertTrue(((net.scwunge.rotarycraft.weapon.turret.FlameTurretBlockEntity) helper.getBlockEntity(TURRET)).range() == 32, "ethanol range");
        helper.succeed();
    }

    @GameTest(template = WIDE, batch = "weapon_flameturretburnsahostilemob", timeoutTicks = 500)
    public static void flameTurretBurnsAHostileMob(GameTestHelper helper) {
        spinningFlywheel(helper, TURRET.below(), 1024, 1024);
        helper.setBlock(TURRET, WeaponRegistry.FLAME_TURRET.get().defaultBlockState());
        net.scwunge.rotarycraft.weapon.turret.FlameTurretBlockEntity gun = helper.getBlockEntity(TURRET);
        gun.tank().fill(new net.neoforged.neoforge.fluids.FluidStack(net.scwunge.rotarycraft.registry.RotaryFluids.ETHANOL.get(), 1000),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        helper.setBlock(new BlockPos(14, 1, 2), net.minecraft.world.level.block.Blocks.STONE);
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(14.5, 2, 2.5));
        // Its flames are lobbed and calibrated to land at 32 blocks, so a closer target is not hit reliably; it turns to face it and burns fuel.
        helper.succeedWhen(() -> {
            helper.assertTrue(gun.tank().getFluidAmount() < 1000, "no fuel used (aim " + gun.phi + ", " + gun.theta + ")");
            helper.assertTrue(Math.abs(gun.phi - 90) < 10, "not facing the husk (phi " + gun.phi + ")");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void flameLandingLightsAFire(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), net.minecraft.world.level.block.Blocks.STONE);
        var attack = new net.scwunge.rotarycraft.weapon.turret.FlameTurretBlockEntity.Attack(1, 3, 1, 4);
        Vec3 from = Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 3, 2)));
        helper.getLevel().addFreshEntity(new net.scwunge.rotarycraft.weapon.FlameShot(helper.getLevel(), from.x, from.y, from.z, new Vec3(0, -0.2, 0),
                null, null, attack));
        helper.succeedWhen(() -> helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.FIRE, new BlockPos(2, 2, 2)));
    }
}
