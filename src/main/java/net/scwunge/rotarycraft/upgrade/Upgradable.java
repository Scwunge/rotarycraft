package net.scwunge.rotarycraft.upgrade;

import net.minecraft.world.item.ItemStack;

/** A machine that takes engine upgrades (the original's upgrade items) when you use one on it. */
public interface Upgradable {
    /** Whether this stack can be fitted now. */
    boolean canUpgradeWith(ItemStack stack);

    /** Fits it; the caller uses the item up. */
    void upgradeWith(ItemStack stack);
}
