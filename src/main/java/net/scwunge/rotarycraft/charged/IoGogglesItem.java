package net.scwunge.rotarycraft.charged;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

/** The IO goggles: while you wear them every shaft machine near you shows which side it takes power in (green) and which it gives it out (red). */
public class IoGogglesItem extends ArmorItem {
    public IoGogglesItem(Holder<ArmorMaterial> material) {
        super(material, Type.HELMET, new Item.Properties().stacksTo(1));
    }
}
