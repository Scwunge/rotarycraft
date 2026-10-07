package net.scwunge.rotarycraft.charged;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.scwunge.rotarycraft.tool.BedrockArmorItem;
import net.scwunge.rotarycraft.tool.BedrockTools;

import java.util.List;

/** The bedrock chestplate with a jetpack built in: it never wears, and burns twice the fuel. */
public class BedrockJetpackItem extends BedrockArmorItem implements Jetpack {
    public BedrockJetpackItem(Holder<ArmorMaterial> material) {
        super(material, ArmorItem.Type.CHESTPLATE, BedrockTools.CHESTPLATE);
    }

    @Override
    public Kind kind() {
        return Kind.BEDROCK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && JetpackItem.toggleWings(stack)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Jetpack.describe(stack, tooltip);
    }
}
