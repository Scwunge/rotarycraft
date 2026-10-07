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
import net.scwunge.rotarycraft.registry.RotaryComponents;

import java.util.List;

/** The plain jetpack, and the steel chestplate with one built in. */
public class JetpackItem extends ArmorItem implements Jetpack {
    private final Kind kind;

    public JetpackItem(Holder<ArmorMaterial> material, Kind kind, Properties properties) {
        super(material, Type.CHESTPLATE, properties.stacksTo(1));
        this.kind = kind;
    }

    @Override
    public Kind kind() {
        return kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && toggleWings(stack)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    /** Folds winged pack's wings away or unfolds them; false if it has none. */
    public static boolean toggleWings(ItemStack stack) {
        if (!Jetpack.Upgrade.WING.on(stack)) {
            return false;
        }
        stack.set(RotaryComponents.PACK_UPGRADES.get(), Jetpack.bits(stack) ^ Jetpack.WINGS_OFF);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Jetpack.describe(stack, tooltip);
    }
}
