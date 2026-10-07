package net.scwunge.rotarycraft.upgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * An engine that takes an integrated gearbox: a ratio of 2 to 16, either in torque mode (a lubricated one: more torque, less speed) or speed mode (one with liquid
 * nitrogen in it: the other way). Power stays the same. A negative number is speed mode.
 */
public interface Geared {
    int integratedGear();

    /** Fits a gearbox of this ratio (sign for the mode); false if it already has one or it is running. */
    boolean applyIntegratedGear(int ratio);

    /** The gearbox item for what is fitted (to give back when the engine is broken), or empty. */
    ItemStack removeIntegratedGear();

    static int torque(int torque, int gear) {
        return gear == 0 ? torque : gear > 0 ? gear * torque : torque / -gear;
    }

    static int speed(int speed, int gear) {
        return gear == 0 ? speed : gear < 0 ? -gear * speed : speed / gear;
    }

    /** Drops the fitted gearbox when the block goes. */
    static void drop(Level level, BlockPos pos, Geared geared) {
        ItemStack gear = geared.removeIntegratedGear();
        if (!gear.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), gear);
        }
    }
}
