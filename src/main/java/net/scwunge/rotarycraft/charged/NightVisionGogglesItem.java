package net.scwunge.rotarycraft.charged;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** The night vision goggles: worn on the head, they let you see in the dark while they have charge, and use a kJ now and then (about one every eight seconds). */
public class NightVisionGogglesItem extends ArmorItem implements Rechargeable {
    public NightVisionGogglesItem(Holder<ArmorMaterial> material) {
        super(material, Type.HELMET, new Properties().stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (level.isClientSide() || !(entity instanceof Player player) || player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD) != stack || Charge.get(stack) <= 0) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false, false));
        if (level.random.nextInt(160) == 0) {
            Charge.use(stack, player, 1, "armor");
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13F * Charge.get(stack) / Charge.FULL);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x3FA8FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.rotarycraft.charge", Charge.get(stack), Charge.FULL).withStyle(ChatFormatting.GRAY));
    }
}
