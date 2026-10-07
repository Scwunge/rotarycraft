package net.scwunge.rotarycraft.tool;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;

import java.util.List;

/** Bedrock armour: never wears, comes enchanted with what it cannot lose, and holds back knocking (the material says how much). */
public class BedrockArmorItem extends ArmorItem implements Forced {
    private final List<Need> needs;

    public BedrockArmorItem(Holder<ArmorMaterial> material, Type type, List<Need> needs) {
        super(material, type, new Properties().component(DataComponents.UNBREAKABLE, new Unbreakable(true)));
        this.needs = needs;
    }

    @Override
    public List<Need> needs() {
        return needs;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        Forced.tick(stack, level, entity, needs);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide() && !Forced.intact(stack, entity.level().registryAccess(), needs)) {
            entity.discard();
        }
        return false;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return BedrockTools.enchantability();
    }
}
