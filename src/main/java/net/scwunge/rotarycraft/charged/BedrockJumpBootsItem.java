package net.scwunge.rotarycraft.charged;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.scwunge.rotarycraft.tool.BedrockArmorItem;
import net.scwunge.rotarycraft.tool.BedrockTools;

/** The bedrock jump boots: bedrock boots (unbreakable, feather falling for good) with the jump boots' leap, speed and stride that never run out. */
public class BedrockJumpBootsItem extends BedrockArmorItem {
    public BedrockJumpBootsItem(Holder<ArmorMaterial> material) {
        super(material, ArmorItem.Type.BOOTS, BedrockTools.BOOTS);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (entity instanceof Player player && !stack.isEmpty() && player.getItemBySlot(EquipmentSlot.FEET) == stack) {
            JumpBootsItem.effects(player);
        }
    }
}
