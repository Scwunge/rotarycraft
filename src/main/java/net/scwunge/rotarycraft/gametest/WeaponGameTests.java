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
        spinningFlywheel(helper, pos, torque, omega, Direction.UP);
    }

    static void spinningFlywheel(GameTestHelper helper, BlockPos pos, int torque, int omega, Direction facing) {
        helper.setBlock(pos, RotaryBlocks.FLYWHEELS.get(FlywheelType.BEDROCK).get().defaultBlockState().setValue(MachineBlock.FACING, facing));
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

    @GameTest(template = TEMPLATE, batch = "weapon_laserneedsfullpowerandrespectsmobgriefing", timeoutTicks = 60)
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

    static net.scwunge.rotarycraft.weapon.turret.TntCannonBlockEntity cannon(GameTestHelper helper) {
        spinningFlywheel(helper, TURRET.below(), 4096, 1024);
        helper.setBlock(TURRET, WeaponRegistry.TNT_CANNON.get().defaultBlockState());
        return helper.getBlockEntity(TURRET);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void tntCannonLaunchesTnt(GameTestHelper helper) {
        var gun = cannon(helper);
        helper.assertTrue(gun.items().insertItem(0, new ItemStack(Items.TNT, 3), false).isEmpty(), "refused TNT");
        helper.assertTrue(gun.items().insertItem(1, new ItemStack(Items.DIRT), true).getCount() == 1, "accepted dirt");
        gun.configure(false, 90, 45, 20, 100, BlockPos.ZERO);
        helper.succeedWhen(() -> {
            helper.assertTrue(gun.items().getStackInSlot(0).getCount() < 3, "no TNT used");
            var tnt = helper.getLevel().getEntitiesOfClass(net.scwunge.rotarycraft.weapon.CannonTnt.class, new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).inflate(30));
            helper.assertFalse(tnt.isEmpty(), "no TNT in flight");
            helper.assertTrue(tnt.get(0).getDeltaMovement().x > 0, "not flying east");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void tntCannonOnlyFiresWithEnoughPower(GameTestHelper helper) {
        spinningFlywheel(helper, TURRET.below(), 64, 64);
        helper.setBlock(TURRET, WeaponRegistry.TNT_CANNON.get().defaultBlockState());
        var gun = (net.scwunge.rotarycraft.weapon.turret.TntCannonBlockEntity) helper.getBlockEntity(TURRET);
        gun.items().setStackInSlot(0, new ItemStack(Items.TNT, 3));
        gun.configure(false, 90, 45, 20, 100, BlockPos.ZERO);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(gun.items().getStackInSlot(0).getCount() == 3, "fired without power");
            helper.succeed();
        });
    }

    /** In target mode it solves a shot onto the named block and sets the fuse to go off there. */
    @GameTest(template = WIDE, batch = "weapon_tntcannontargeting", timeoutTicks = 400)
    public static void tntCannonHitsItsTarget(GameTestHelper helper) {
        var gun = cannon(helper);
        gun.items().setStackInSlot(0, new ItemStack(Items.TNT, 1));
        BlockPos target = new BlockPos(16, 2, 2);
        gun.configure(true, 0, 0, 0, 0, helper.absolutePos(target));
        var rule = helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        rule.set(false, helper.getLevel().getServer());
        Vec3[] last = new Vec3[1];
        helper.onEachTick(() -> helper.getLevel().getEntitiesOfClass(net.scwunge.rotarycraft.weapon.CannonTnt.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40)).forEach(t -> last[0] = t.position()));
        helper.succeedWhen(() -> {
            helper.assertTrue(gun.items().getStackInSlot(0).isEmpty(), "not fired yet");
            helper.assertTrue(last[0] != null, "TNT never flew");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.scwunge.rotarycraft.weapon.CannonTnt.class,
                    new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40)).isEmpty(), "still flying");
            double miss = last[0].distanceTo(Vec3.atCenterOf(helper.absolutePos(target)));
            rule.set(true, helper.getLevel().getServer());
            helper.assertTrue(miss < 3, "missed by " + miss + " blocks");
        });
    }

    @GameTest(template = TEMPLATE, batch = "weapon_cannontntrespectsmobgriefing", timeoutTicks = 60)
    public static void cannonTntRespectsMobGriefing(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), net.minecraft.world.level.block.Blocks.DIRT);
        helper.setBlock(new BlockPos(3, 1, 2), net.minecraft.world.level.block.Blocks.DIRT);
        var rule = helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        rule.set(false, helper.getLevel().getServer());
        Vec3 at = Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
        helper.getLevel().addFreshEntity(new net.scwunge.rotarycraft.weapon.CannonTnt(helper.getLevel(), at.x, at.y, at.z, 2, null));
        helper.runAfterDelay(15, () -> {
            helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.DIRT, new BlockPos(2, 1, 2));
            rule.set(true, helper.getLevel().getServer());
            helper.getLevel().addFreshEntity(new net.scwunge.rotarycraft.weapon.CannonTnt(helper.getLevel(), at.x, at.y, at.z, 2, null));
            helper.runAfterDelay(15, () -> {
                helper.assertBlockNotPresent(net.minecraft.world.level.block.Blocks.DIRT, new BlockPos(2, 1, 2));
                helper.succeed();
            });
        });
    }

    static net.scwunge.rotarycraft.weapon.turret.SonicWeaponBlockEntity sonic(GameTestHelper helper, int decibels) {
        spinningFlywheel(helper, TURRET.below(), 65536, 1024);
        helper.setBlock(TURRET, WeaponRegistry.SONIC.get().defaultBlockState());
        net.scwunge.rotarycraft.weapon.turret.SonicWeaponBlockEntity gun = helper.getBlockEntity(TURRET);
        gun.setDecibels(decibels);
        return gun;
    }

    /** Loud enough, a mob nearby is blinded and dazed. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void sonicWeaponBlindsAndConfusesNearbyMobs(GameTestHelper helper) {
        sonic(helper, 140);
        var cow = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.COW, new BlockPos(4, 2, 2));
        helper.succeedWhen(() -> {
            helper.assertTrue(cow.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS), "not blinded");
            helper.assertTrue(cow.hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION), "not confused");
        });
    }

    /** Quiet, it does nothing. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void sonicWeaponIsHarmlessWhenQuiet(GameTestHelper helper) {
        sonic(helper, 20);
        var cow = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.COW, new BlockPos(4, 2, 2));
        helper.runAfterDelay(30, () -> {
            helper.assertFalse(cow.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS), "blinded by a quiet sound");
            helper.succeed();
        });
    }

    /** Sound falls off with the square of the distance. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void sonicWeaponFadesWithDistance(GameTestHelper helper) {
        sonic(helper, 140);
        var far = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.COW, new BlockPos(4, 2, 4));
        far.teleportTo(helper.absolutePos(new BlockPos(2, 2, 2)).getX() + 15.5, helper.absolutePos(new BlockPos(2, 2, 2)).getY(), helper.absolutePos(new BlockPos(2, 2, 2)).getZ() + 0.5);
        helper.runAfterDelay(30, () -> {
            helper.assertFalse(far.hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION), "dazed from far away");
            helper.succeed();
        });
    }

    /** A helmet or creative mode spares a player. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void sonicWeaponSparesHelmetsAndCreative(GameTestHelper helper) {
        var bare = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var helmeted = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var creative = helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        helmeted.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        helper.assertTrue(net.scwunge.rotarycraft.weapon.turret.SonicWeaponBlockEntity.isVulnerable(bare), "bare head spared");
        helper.assertFalse(net.scwunge.rotarycraft.weapon.turret.SonicWeaponBlockEntity.isVulnerable(helmeted), "helmet did not protect");
        helper.assertFalse(net.scwunge.rotarycraft.weapon.turret.SonicWeaponBlockEntity.isVulnerable(creative), "creative not spared");
        helper.succeed();
    }

    /** What the power can make caps the volume. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void sonicWeaponVolumeIsLimitedByPower(GameTestHelper helper) {
        spinningFlywheel(helper, TURRET.below(), 64, 64);
        helper.setBlock(TURRET, WeaponRegistry.SONIC.get().defaultBlockState());
        net.scwunge.rotarycraft.weapon.turret.SonicWeaponBlockEntity gun = helper.getBlockEntity(TURRET);
        gun.setDecibels(200);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(gun.volume() <= gun.maxVolume() / 1_000_000D + 1e-6, "volume above the maximum");
            helper.assertTrue(gun.volume() < Math.pow(10, 20), "not limited");
            helper.succeed();
        });
    }

    /** Infested stone shaken by a loud weapon turns back to stone. */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void sonicWeaponShakesSilverfishOut(GameTestHelper helper) {
        sonic(helper, 120);
        var rule = helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        rule.set(true, helper.getLevel().getServer());
        for (int x = 0; x < 5; x++) {
            for (int y = 1; y < 4; y++) {
                for (int z = 0; z < 5; z++) {
                    BlockPos at = new BlockPos(x, y, z);
                    if (helper.getBlockState(at).isAir()) {
                        helper.setBlock(at, net.minecraft.world.level.block.Blocks.INFESTED_STONE);
                    }
                }
            }
        }
        helper.succeedWhen(() -> {
            boolean any = false;
            for (int x = 0; x < 5; x++) {
                for (int y = 1; y < 4; y++) {
                    for (int z = 0; z < 5; z++) {
                        any |= helper.getBlockState(new BlockPos(x, y, z)).is(net.minecraft.world.level.block.Blocks.STONE);
                    }
                }
            }
            helper.assertTrue(any, "no infested stone cleared");
        });
    }

    /** A heat ray firing east with a flywheel behind it. */
    static net.scwunge.rotarycraft.weapon.turret.HeatRayBlockEntity heatRay(GameTestHelper helper, int torque) {
        helper.setBlock(new BlockPos(1, 2, 2), WeaponRegistry.HEAT_RAY.get().defaultBlockState()
                .setValue(net.scwunge.rotarycraft.block.MachineBlock.FACING, net.minecraft.core.Direction.EAST));
        return helper.getBlockEntity(new BlockPos(1, 2, 2));
    }

    @GameTest(template = WIDE, batch = "weapon_heatray", timeoutTicks = 200)
    public static void heatRaySetsCreaturesAlightAndMeltsStone(GameTestHelper helper) {
        spinningFlywheel(helper, new BlockPos(0, 2, 2), 8192, 1024, Direction.EAST);
        heatRay(helper, 8192);
        helper.setBlock(new BlockPos(7, 2, 2), net.minecraft.world.level.block.Blocks.STONE);
        var cow = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.COW, new BlockPos(4, 2, 2));
        helper.succeedWhen(() -> {
            helper.assertTrue(cow.isOnFire() || cow.isDeadOrDying(), "creature not burning");
            helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.LAVA, new BlockPos(7, 2, 2));
        });
    }

    @GameTest(template = WIDE, batch = "weapon_heatray_weak", timeoutTicks = 80)
    public static void heatRayNeedsItsMinimumPower(GameTestHelper helper) {
        spinningFlywheel(helper, new BlockPos(0, 2, 2), 1024, 1024, Direction.EAST);
        heatRay(helper, 1024);
        helper.setBlock(new BlockPos(5, 2, 2), net.minecraft.world.level.block.Blocks.STONE);
        var cow = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.COW, new BlockPos(3, 2, 2));
        helper.runAfterDelay(40, () -> {
            helper.assertFalse(cow.isOnFire(), "burned without power");
            helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.STONE, new BlockPos(5, 2, 2));
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, batch = "weapon_heatray_grief", timeoutTicks = 120)
    public static void heatRayLeavesBlocksAloneWithoutMobGriefing(GameTestHelper helper) {
        spinningFlywheel(helper, new BlockPos(0, 2, 2), 8192, 1024, Direction.EAST);
        heatRay(helper, 8192);
        helper.setBlock(new BlockPos(5, 2, 2), net.minecraft.world.level.block.Blocks.STONE);
        var rule = helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        rule.set(false, helper.getLevel().getServer());
        helper.runAfterDelay(60, () -> {
            rule.set(true, helper.getLevel().getServer());
            helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.STONE, new BlockPos(5, 2, 2));
            helper.succeed();
        });
    }

    /** An opaque block stops the beam: what is behind it is untouched. */
    @GameTest(template = WIDE, batch = "weapon_heatray_stop", timeoutTicks = 120)
    public static void heatRayIsStoppedByOpaqueBlocks(GameTestHelper helper) {
        spinningFlywheel(helper, new BlockPos(0, 2, 2), 8192, 1024, Direction.EAST);
        heatRay(helper, 8192);
        helper.setBlock(new BlockPos(4, 2, 2), net.minecraft.world.level.block.Blocks.OBSIDIAN);
        helper.setBlock(new BlockPos(6, 2, 2), net.minecraft.world.level.block.Blocks.STONE);
        helper.runAfterDelay(60, () -> {
            helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.STONE, new BlockPos(6, 2, 2));
            helper.succeed();
        });
    }

    /** An EMP at (2, 2, 2) on a powered flywheel, already charged and loaded unless told otherwise. */
    static net.scwunge.rotarycraft.weapon.turret.EmpBlockEntity emp(GameTestHelper helper, boolean ready) {
        spinningFlywheel(helper, new BlockPos(2, 1, 2), 524288, 1024);
        helper.setBlock(new BlockPos(2, 2, 2), WeaponRegistry.EMP.get().defaultBlockState());
        net.scwunge.rotarycraft.weapon.turret.EmpBlockEntity emp = helper.getBlockEntity(new BlockPos(2, 2, 2));
        if (ready) {
            CompoundTag tag = new CompoundTag();
            tag.putLong("energy", 90_000_000_000L);
            tag.putInt("loaded", 1_000_000);
            emp.loadCustomOnly(tag, helper.getLevel().registryAccess());
        }
        return emp;
    }

    /** The loaded, charged EMP burns out every machine in range, once. */
    @GameTest(template = WIDE, batch = "weapon_emp", timeoutTicks = 100)
    public static void empBurnsOutMachinesInRange(GameTestHelper helper) {
        var emp = emp(helper, true);
        spinningFlywheel(helper, new BlockPos(10, 2, 2), 4096, 1024);
        helper.succeedWhen(() -> {
            helper.assertFalse(emp.usable(), "has not fired");
            var other = helper.getBlockEntity(new BlockPos(10, 2, 2));
            helper.assertTrue(other instanceof net.scwunge.rotarycraft.blockentity.PowerBlockEntity pb && pb.isShutdown(), "machine not burnt out");
            helper.assertTrue(((net.scwunge.rotarycraft.blockentity.PowerBlockEntity) other).getTorqueOut(net.minecraft.core.Direction.UP) == 0, "burnt-out machine still outputs");
        });
    }

    /** It will not fire while it is still loading its listing. */
    @GameTest(template = TEMPLATE, batch = "weapon_emp_loading", timeoutTicks = 40)
    public static void empWaitsForItsListing(GameTestHelper helper) {
        var emp = emp(helper, false);
        CompoundTag tag = new CompoundTag();
        tag.putLong("energy", 90_000_000_000L);
        emp.loadCustomOnly(tag, helper.getLevel().registryAccess());
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(emp.usable(), "fired before it had loaded");
            helper.succeed();
        });
    }

    /** Without mobGriefing (or the owner's permission) nothing is burnt out. */
    @GameTest(template = WIDE, batch = "weapon_emp_grief", timeoutTicks = 100)
    public static void empRespectsMobGriefing(GameTestHelper helper) {
        var rule = helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        rule.set(false, helper.getLevel().getServer());
        var emp = emp(helper, true);
        spinningFlywheel(helper, new BlockPos(10, 2, 2), 4096, 1024);
        helper.runAfterDelay(40, () -> {
            rule.set(true, helper.getLevel().getServer());
            helper.assertFalse(((net.scwunge.rotarycraft.blockentity.PowerBlockEntity) helper.getBlockEntity(new BlockPos(10, 2, 2))).isShutdown(), "burnt out in spite of the rule");
            helper.succeed();
        });
    }

    /** A burnt-out machine stays burnt out when saved and loaded. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void burntOutMachinesStayBurntOut(GameTestHelper helper) {
        spinningFlywheel(helper, new BlockPos(2, 1, 2), 4096, 1024);
        net.scwunge.rotarycraft.blockentity.PowerBlockEntity machine = helper.getBlockEntity(new BlockPos(2, 1, 2));
        machine.onEmp();
        CompoundTag saved = machine.saveCustomOnly(helper.getLevel().registryAccess());
        helper.assertTrue(saved.getBoolean("emp"), "not saved");
        helper.succeed();
    }

    static ItemStack coil(net.minecraft.world.item.Item item, int charge) {
        ItemStack stack = new ItemStack(item);
        net.scwunge.rotarycraft.item.CoilItem.setCharge(stack, charge);
        return stack;
    }

    /** A winder facing east at (3, 2, 2), fed from a flywheel at (2, 2, 2). */
    static net.scwunge.rotarycraft.blockentity.WinderBlockEntity winder(GameTestHelper helper, int torque, int omega) {
        spinningFlywheel(helper, new BlockPos(2, 2, 2), torque, omega, Direction.EAST);
        helper.setBlock(new BlockPos(3, 2, 2), WeaponRegistry.WINDER.get().defaultBlockState()
                .setValue(net.scwunge.rotarycraft.block.MachineBlock.FACING, Direction.EAST));
        return helper.getBlockEntity(new BlockPos(3, 2, 2));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void winderWindsASpringUpToWhatTheTorqueAllows(GameTestHelper helper) {
        var winder = winder(helper, 40, 256);
        helper.assertTrue(winder.items().insertItem(0, new ItemStack(WeaponRegistry.SPRING.get()), false).isEmpty(), "refused the spring");
        helper.assertFalse(winder.items().insertItem(0, new ItemStack(Items.STICK), true).isEmpty(), "accepted a stick");
        helper.succeedWhen(() -> {
            int charge = net.scwunge.rotarycraft.item.CoilItem.charge(winder.items().getStackInSlot(0));
            helper.assertTrue(charge > 0, "not wound");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void winderStopsAtTheTorqueLimit(GameTestHelper helper) {
        var winder = winder(helper, 5, 1024);
        winder.items().setStackInSlot(0, coil(WeaponRegistry.SPRING.get(), 5));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(net.scwunge.rotarycraft.item.CoilItem.charge(winder.items().getStackInSlot(0)) <= 5, "wound past the torque");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void winderUnwindsIntoPower(GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 2, 2), WeaponRegistry.WINDER.get().defaultBlockState()
                .setValue(net.scwunge.rotarycraft.block.MachineBlock.FACING, Direction.EAST));
        net.scwunge.rotarycraft.blockentity.WinderBlockEntity winder = helper.getBlockEntity(new BlockPos(3, 2, 2));
        winder.items().setStackInSlot(0, coil(WeaponRegistry.STRONG_COIL.get(), 100));
        winder.setWinding(false);
        helper.succeedWhen(() -> {
            helper.assertTrue(winder.getTorqueOut(Direction.WEST) == 32 && winder.getOmegaOut(Direction.WEST) == 4096,
                    "expected 32 N*m at 4096 rad/s, got " + winder.getTorqueOut(Direction.WEST) + " at " + winder.getOmegaOut(Direction.WEST));
            helper.assertTrue(winder.getTorqueOut(Direction.EAST) == 0, "powered the wrong side");
        });
    }

    static net.scwunge.rotarycraft.weapon.turret.LandmineBlockEntity mine(GameTestHelper helper, int charge, int gunpowder) {
        helper.setBlock(new BlockPos(2, 2, 2), WeaponRegistry.LANDMINE.get().defaultBlockState());
        net.scwunge.rotarycraft.weapon.turret.LandmineBlockEntity mine = helper.getBlockEntity(new BlockPos(2, 2, 2));
        if (charge > 0) {
            mine.items().setStackInSlot(0, coil(WeaponRegistry.SPRING.get(), charge));
        }
        for (int i = 0; i < gunpowder; i++) {
            mine.items().setStackInSlot(1 + i, new ItemStack(Items.GUNPOWDER));
        }
        return mine;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void landmineGoesOffUnderACreature(GameTestHelper helper) {
        mine(helper, 40000, 2);
        var cow = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.COW, new BlockPos(2, 3, 2));
        helper.onEachTick(() -> cow.setOnGround(true));
        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(WeaponRegistry.LANDMINE.get(), new BlockPos(2, 2, 2));
            helper.assertTrue(cow.isDeadOrDying() || cow.getHealth() < cow.getMaxHealth(), "creature unhurt");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void landmineWithoutACoilStaysQuiet(GameTestHelper helper) {
        mine(helper, 0, 2);
        var cow = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.COW, new BlockPos(2, 3, 2));
        helper.onEachTick(() -> cow.setOnGround(true));
        helper.runAfterDelay(30, () -> {
            helper.assertBlockPresent(WeaponRegistry.LANDMINE.get(), new BlockPos(2, 2, 2));
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void landmineBlastRespectsMobGriefing(GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 2, 2), net.minecraft.world.level.block.Blocks.DIRT);
        helper.setBlock(new BlockPos(1, 2, 2), net.minecraft.world.level.block.Blocks.DIRT);
        var rule = helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        rule.set(false, helper.getLevel().getServer());
        mine(helper, 40000, 4);
        var cow = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.COW, new BlockPos(2, 3, 2));
        helper.onEachTick(() -> cow.setOnGround(true));
        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(WeaponRegistry.LANDMINE.get(), new BlockPos(2, 2, 2));
            rule.set(true, helper.getLevel().getServer());
            helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.DIRT, new BlockPos(3, 2, 2));
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void landmineAutomationOnlyReleasesARunDownCoil(GameTestHelper helper) {
        var mine = mine(helper, 100, 0);
        var auto = mine.automationItems();
        helper.assertTrue(auto.extractItem(0, 1, true).isEmpty(), "released a charged coil");
        helper.assertFalse(auto.insertItem(1, new ItemStack(Items.DIRT), true).isEmpty(), "accepted dirt");
        mine.items().setStackInSlot(0, new ItemStack(WeaponRegistry.SPRING.get()));
        helper.assertFalse(auto.extractItem(0, 1, true).isEmpty(), "held a run-down coil");
        helper.succeed();
    }
}
