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
        return (int) Math.sqrt(512 * Math.pow(2, tier));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.rotarycraft.railgun_ammo", requiredTorque()));
    }
}
