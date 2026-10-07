package net.scwunge.rotarycraft.weapon;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Rail gun ammunition, sixteen tiers as in the original: tier n needs sqrt(512 x 2^n) N*m to fire and hits harder for each tier. */
public class RailgunAmmoItem extends Item {
    private final int tier;

    public RailgunAmmoItem(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    public int requiredTorque() {
        return tier < 0 ? 0 : (int) Math.sqrt(512 * Math.pow(2, tier));
    }

    /** Explosive shells blow up where they land (the original's explosive shell) instead of smashing blocks by tier. */
    public boolean explosive() {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.rotarycraft.railgun_ammo", requiredTorque()));
    }
}
