package net.scwunge.rotarycraft.weapon.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import net.scwunge.rotarycraft.weapon.FreezeShot;

/**
 * Freeze Gun, as the original: needs 256 N*m and 262 kW, holds 27 stacks of snowballs, snow blocks (4 snowballs) or ice (16),
 * and every second while on target fires a snowball that freezes everything round where it lands. Ignores creatures already
 * frozen. Range 64.
 */
public class FreezeGunBlockEntity extends AmmoTurretBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(256, 1, 262144);

    public FreezeGunBlockEntity(BlockPos pos, BlockState state) {
        super(WeaponRegistry.FREEZE_GUN_BE.get(), pos, state, "freezeGun", 27);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    public int range() {
        return 64;
    }

    @Override
    public boolean isAmmo(ItemStack stack) {
        return stack.is(Items.ICE) || stack.is(Items.SNOW_BLOCK) || stack.is(Items.SNOWBALL);
    }

    @Override
    protected boolean isValidTarget(Entity e) {
        return super.isValidTarget(e) && !((LivingEntity) e).hasEffect(WeaponRegistry.FREEZE);
    }

    @Override
    protected void turretTick() {
        if (target == null || !isAimingAt(target)) {
            return;
        }
        convertSnow();
        if (!hasSnowball() || tickcount < operationTime()) {
            return;
        }
        tickcount = 0;
        useOne(Items.SNOWBALL);
        Vec3 v = target.subtract(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()).normalize();
        level.addFreshEntity(new FreezeShot(level, worldPosition.getX() + 0.5 + v.x, firingY(v.y), worldPosition.getZ() + 0.5 + v.z, v.scale(3),
                worldPosition, owner()));
    }

    private boolean hasSnowball() {
        for (int i = 0; i < items.getSlots(); i++) {
            if (items.getStackInSlot(i).is(Items.SNOWBALL)) {
                return true;
            }
        }
        return false;
    }

    /** A snow block becomes 4 snowballs and ice 16, if they fit, as the original. */
    private void convertSnow() {
        convert(Items.SNOW_BLOCK, 4);
        convert(Items.ICE, 16);
    }

    private void convert(net.minecraft.world.item.Item from, int balls) {
        ItemStack out = new ItemStack(Items.SNOWBALL, balls);
        for (int i = 0; i < items.getSlots(); i++) {
            if (items.getStackInSlot(i).is(from)) {
                if (ItemHandlerHelper.insertItemStacked(items, out, true).isEmpty()) {
                    items.extractItem(i, 1, false);
                    ItemHandlerHelper.insertItemStacked(items, out, false);
                }
                return;
            }
        }
    }
}
