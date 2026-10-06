package net.scwunge.rotarycraft.weapon.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.RotaryParts;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import net.scwunge.rotarycraft.weapon.GatlingShot;

/**
 * Gatling Gun, as the original: needs 1024 rad/s and 65.5 kW, loads ball bearings in its first slot, which move down a belt of 36
 * slots into the clip (the last slot); when the clip runs out it reloads for four seconds. With a target in range it spins its
 * barrels up and then fires a round every tick (a tenth of them use a ball bearing), a little off aim, at 120 blocks of range.
 */
public class GatlingBlockEntity extends AmmoTurretBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1024, 65536);
    public static final int LOAD_SLOT = 0;
    public static final int CLIP_SLOT = 36;
    private static final double AMMO_PER_SHOT = 0.1;
    private static final int RELOAD_TIME = 80;
    private static final int FEED_TIME = 4;
    private static final double SPIN_RATE = 25;
    private static final double SPIN_DELTA = 5;
    private static final double JITTER = 0.625;
    private static final double SHOT_SPEED = 1.5;

    private double spinSpeed;
    private int reloadTimer;
    private int feedTick;

    public GatlingBlockEntity(BlockPos pos, BlockState state) {
        super(WeaponRegistry.GATLING_BE.get(), pos, state, "gatling", CLIP_SLOT + 1);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    public int range() {
        return 120;
    }

    @Override
    public int operationTime() {
        return 1;
    }

    @Override
    protected double randomOffset() {
        return (level.random.nextDouble() * 2 - 1) * JITTER;
    }

    @Override
    public boolean isAmmo(ItemStack stack) {
        return stack.is(RotaryParts.part("ball_bearing").get());
    }

    /** Only the first slot takes ammunition; it travels down the belt on its own. */
    @Override
    protected boolean acceptsInSlot(int slot, ItemStack stack) {
        return slot == LOAD_SLOT && isAmmo(stack);
    }

    public boolean isReloading() {
        return reloadTimer > 0;
    }

    @Override
    public float spinAngle(float partialTick) {
        if (level == null) {
            return 0;
        }
        // the barrels turn spinSpeed degrees a tick, drawn a third as fast (the original divides its angle by three)
        return (float) ((level.getGameTime() + partialTick) * spinSpeed / 3 % 360);
    }

    private boolean hasAmmo() {
        return isAmmo(items.getStackInSlot(CLIP_SLOT));
    }

    @Override
    protected void unpoweredTick() {
        setSpin(Math.max(spinSpeed - SPIN_DELTA, 0));
    }

    private void setSpin(double speed) {
        if (speed != spinSpeed) {
            spinSpeed = speed;
            syncNow();
        }
    }

    @Override
    protected void turretTick() {
        if (feedTick > 0) {
            feedTick--;
        } else {
            feedBelt();
        }
        if (reloadTimer == 0 && items.getStackInSlot(CLIP_SLOT).isEmpty()) {
            startReload();
        }
        if (reloadTimer > 0) {
            if (--reloadTimer == 0) {
                advanceBelt();
            }
            return;
        }
        if (!hasAmmo() || target == null) {
            setSpin(Math.max(spinSpeed - SPIN_DELTA, 0));
            return;
        }
        if (spinSpeed < SPIN_RATE) {
            setSpin(Math.min(spinSpeed + SPIN_DELTA, SPIN_RATE));
        } else {
            fire();
        }
    }

    private void startReload() {
        reloadTimer = RELOAD_TIME;
        if (!items.getStackInSlot(CLIP_SLOT - 1).isEmpty()) {
            level.playSound(null, worldPosition, WeaponRegistry.GATLING_RELOAD_SOUND.get(), SoundSource.BLOCKS, 0.75f, 0.95f);
        }
    }

    /** Moves ammunition one place down the belt where there is room, one move every few ticks. */
    private void feedBelt() {
        for (int i = CLIP_SLOT - 1; i > LOAD_SLOT; i--) {
            ItemStack from = items.getStackInSlot(i - 1);
            if (from.isEmpty()) {
                continue;
            }
            ItemStack to = items.getStackInSlot(i);
            boolean moved = false;
            if (to.isEmpty()) {
                items.setStackInSlot(i, from);
                items.setStackInSlot(i - 1, ItemStack.EMPTY);
                moved = true;
            } else if (to.getCount() < 64 && ItemStack.isSameItemSameComponents(to, from)) {
                int amount = Math.min(64 - to.getCount(), from.getCount());
                ItemStack merged = to.copyWithCount(to.getCount() + amount);
                items.setStackInSlot(i, merged);
                items.setStackInSlot(i - 1, amount == from.getCount() ? ItemStack.EMPTY : from.copyWithCount(from.getCount() - amount));
                moved = true;
            }
            if (moved) {
                feedTick = FEED_TIME;
                return;
            }
        }
    }

    /** The reload: everything on the belt moves one place along, bringing the next stack into the (empty) clip. */
    private void advanceBelt() {
        for (int i = CLIP_SLOT; i > LOAD_SLOT; i--) {
            items.setStackInSlot(i, items.getStackInSlot(i - 1));
        }
        items.setStackInSlot(LOAD_SLOT, ItemStack.EMPTY);
        level.playSound(null, worldPosition, WeaponRegistry.GATLING_RELOAD_SOUND.get(), SoundSource.BLOCKS, 2, 0.75f);
    }

    /** Fires along the barrel's own direction (not at the target), as the original. */
    private void fire() {
        if (level.random.nextDouble() < AMMO_PER_SHOT) {
            ItemStack clip = items.getStackInSlot(CLIP_SLOT);
            items.setStackInSlot(CLIP_SLOT, clip.getCount() <= 1 ? ItemStack.EMPTY : clip.copyWithCount(clip.getCount() - 1));
            if (items.getStackInSlot(CLIP_SLOT).isEmpty()) {
                startReload();
            }
        }
        double elevation = Math.toRadians(theta);
        double azimuth = Math.toRadians(90 - phi);
        Vec3 v = new Vec3(SHOT_SPEED * Math.cos(elevation) * Math.cos(azimuth), SHOT_SPEED * Math.sin(elevation),
                SHOT_SPEED * Math.cos(elevation) * Math.sin(azimuth));
        level.addFreshEntity(new GatlingShot(level, worldPosition.getX() + 0.5 + v.x, firingY(v.y), worldPosition.getZ() + 0.5 + v.z, v, worldPosition, owner()));
        level.playSound(null, worldPosition, WeaponRegistry.GATLING_SOUND.get(), SoundSource.BLOCKS, 0.25f, 1.125f);
    }

    @Override
    protected void writeSync(CompoundTag tag) {
        tag.putDouble("spinSpeed", spinSpeed);
    }

    @Override
    protected void readSync(CompoundTag tag) {
        spinSpeed = tag.getDouble("spinSpeed");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("reload", reloadTimer);
        tag.putInt("feed", feedTick);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        reloadTimer = tag.getInt("reload");
        feedTick = tag.getInt("feed");
    }
}
