package net.scwunge.rotarycraft.tool;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The bedrock sword: never wears, is sharpness V and looting V for good, wrecks the armour of what it hits, and, with {@link ToolEvents}, makes kills give
 * far more experience and sometimes the head.
 */
public class BedrockSwordItem extends SwordItem implements Forced {
    public static final int ARMOR_DAMAGE = 100;

    public BedrockSwordItem() {
        super(BedrockTools.tier(12F), BedrockTools.properties(5, -2.4F));
    }

    @Override
    public List<Need> needs() {
        return BedrockTools.SWORD;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        Forced.tick(stack, level, entity, needs());
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide() && !Forced.intact(stack, entity.level().registryAccess(), needs())) {
            entity.discard();
        }
        return false;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return BedrockTools.enchantability();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack worn = target.getItemBySlot(slot);
            if (!worn.isEmpty() && worn.isDamageableItem()) {
                worn.hurtAndBreak(ARMOR_DAMAGE, target, slot);
            }
        }
        return true;
    }
}
