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
    @GameTest(template = WIDE, timeoutTicks = 300)
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
}
