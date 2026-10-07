package net.scwunge.rotarycraft.weapon.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import net.scwunge.rotarycraft.weapon.RailgunAmmoItem;
import net.scwunge.rotarycraft.weapon.RailgunShot;

/**
 * Rail Gun, as the original: needs 4 MW, holds 54 stacks of ammunition and, every second while on target, fires the heaviest
 * slug its torque can throw (tier n needs sqrt(512 x 2^n) N*m) at 4 blocks a tick, out to 164 blocks.
 */
public class RailGunBlockEntity extends AmmoTurretBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 4194304);
    public static final int RANGE = 164;
    public static final double SPEED = 4;

    public RailGunBlockEntity(BlockPos pos, BlockState state) {
        super(WeaponRegistry.RAILGUN_BE.get(), pos, state, "railgun", 54);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    public int range() {
        return RANGE;
    }

    @Override
    public boolean isAmmo(ItemStack stack) {
        return stack.getItem() instanceof RailgunAmmoItem;
    }

    /** The heaviest ammunition in store that the shaft's torque can fire, or null. */
    public RailgunAmmoItem bestAmmo() {
        RailgunAmmoItem best = null;
        for (int i = 0; i < items.getSlots(); i++) {
            if (items.getStackInSlot(i).getItem() instanceof RailgunAmmoItem ammo && torque >= ammo.requiredTorque()
                    && (best == null || ammo.tier() > best.tier())) {
                best = ammo;
            }
        }
        return best;
    }

    @Override
    protected void turretTick() {
        if (target == null) {
            return;
        }
        RailgunAmmoItem ammo = bestAmmo();
        if (ammo == null || !isAimingAt(target) || tickcount < operationTime()) {
            return;
        }
        tickcount = 0;
        fire(ammo, target);
    }

    private void fire(RailgunAmmoItem ammo, Vec3 at) {
        useOne(ammo);
        Vec3 v = at.subtract(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()).normalize().scale(SPEED);
        Vec3 d = v.normalize();
        RailgunShot shot = new RailgunShot(level, worldPosition.getX() + 0.5 + d.x, firingY(d.y), worldPosition.getZ() + 0.5 + d.z, v, Math.max(ammo.tier(), 0),
                worldPosition, owner());
        level.addFreshEntity(ammo.explosive() ? shot.explosive() : shot);
    }
}
