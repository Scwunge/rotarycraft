package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base for engines. Like the original: while an engine can run it spins up towards its top speed by 4 x log2(top + 1) rad/s
 * per tick, and when it stops it coasts down by omega/256 + 1 per tick. Torque is the engine's rated torque while it turns.
 */
public abstract class EngineBlockEntity extends PowerBlockEntity implements net.scwunge.rotarycraft.upgrade.Geared {
    private int gear;

    protected EngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Rated torque (N*m). */
    protected abstract int ratedTorque();

    /** Speed (rad/s) the engine spins up to right now. */
    protected abstract int targetSpeed();

    /** Whether the engine's running conditions are met this tick. */
    protected abstract boolean canRun();

    /** How much of its top speed the engine may use right now, 0 to 1 (an Engine Control Unit throttles it). */
    protected double throttle() {
        return 1;
    }

    /** Called every tick after the speed update; {@code running} is whether it ran. */
    protected void afterTick(boolean running) {
    }

    @Override
    public void serverTick() {
        boolean running = canRun();
        int target = running ? Math.max(0, net.scwunge.rotarycraft.upgrade.Geared.speed((int) (targetSpeed() * throttle()), gear)) : 0;
        int w = omega;
        if (running && target > 0) {
            if (w < target) {
                w += (int) (4 * (Math.log(target + 1) / Math.log(2)));
                w = Math.min(w, target);
            } else if (w > target) {
                w = target;
            }
        } else if (w > 0) {
            w -= w / 256 + 1;
        }
        w = Math.max(0, w);
        setPower(w > 0 ? net.scwunge.rotarycraft.upgrade.Geared.torque(ratedTorque(), gear) : 0, w);
        afterTick(running);
    }

    @Override
    public int integratedGear() {
        return gear;
    }

    @Override
    public boolean applyIntegratedGear(int ratio) {
        if (gear != 0 || ratio == 0 || omega > 0) {
            return false;
        }
        gear = ratio;
        setChanged();
        return true;
    }

    @Override
    public net.minecraft.world.item.ItemStack removeIntegratedGear() {
        if (gear == 0) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        net.minecraft.world.item.ItemStack stack = net.scwunge.rotarycraft.item.GearUpgradeItem.stackFor(gear, gear > 0,
                net.scwunge.rotarycraft.registry.UpgradeRegistry.gearItems());
        gear = 0;
        setChanged();
        return stack;
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (gear != 0) {
            tag.putInt("gear", gear);
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        gear = tag.getInt("gear");
    }
}
