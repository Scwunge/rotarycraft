package net.scwunge.rotarycraft.weapon.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import net.scwunge.rotarycraft.weapon.FlakShot;

/**
 * Anti-Aircraft Gun, as the original: needs 1024 N*m and 65.5 kW, holds 27 stacks of scrap and fires flak (half the shots use a
 * scrap) every 6 ticks, aiming twice as fast as other turrets, at hostile flyers in the air more than two blocks away (ghasts,
 * phantoms, blazes, withers, the dragon). Range 128.
 */
public class AntiAirBlockEntity extends AmmoTurretBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1024, 1, 65536);

    public AntiAirBlockEntity(BlockPos pos, BlockState state) {
        super(WeaponRegistry.ANTI_AIR_BE.get(), pos, state, "antiAir", 27);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    public int range() {
        return 128;
    }

    @Override
    public int operationTime() {
        return 6;
    }

    @Override
    protected float aimingSpeed() {
        return 2;
    }

    @Override
    protected double randomOffset() {
        return -1;
    }

    @Override
    public boolean isAmmo(ItemStack stack) {
        return stack.is(RotaryItems.SCRAP.get());
    }

    @Override
    protected boolean isValidTarget(Entity e) {
        if (!(e instanceof LivingEntity living) || !living.isAlive() || living.getHealth() <= 0 || e.onGround() || e.isInWater()
                || e.isInFluidType((fluid, height) -> fluid.equals(net.neoforged.neoforge.common.NeoForgeMod.LAVA_TYPE.value()))) {
            return false;
        }
        boolean flyer = e instanceof FlyingMob && e instanceof Enemy || e instanceof Blaze || e instanceof WitherBoss || e instanceof EnderDragon;
        return flyer && e.position().distanceTo(worldPosition.getCenter()) > 2;
    }

    @Override
    protected void turretTick() {
        if (target == null || !hasScrap() || !isAimingAt(target) || tickcount < operationTime()) {
            return;
        }
        tickcount = 0;
        level.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1, 1.3f);
        level.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1, 0.5f);
        if (level.random.nextBoolean()) {
            useOne(RotaryItems.SCRAP.get());
        }
        Vec3 v = target.subtract(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()).normalize();
        level.addFreshEntity(new FlakShot(level, worldPosition.getX() + 0.5 + v.x, firingY(v.y), worldPosition.getZ() + 0.5 + v.z, v.scale(6),
                worldPosition, owner()));
    }

    private boolean hasScrap() {
        for (int i = 0; i < items.getSlots(); i++) {
            if (items.getStackInSlot(i).is(RotaryItems.SCRAP.get())) {
                return true;
            }
        }
        return false;
    }
}
