package net.scwunge.rotarycraft.handheld;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.scwunge.rotarycraft.registry.HandheldRegistry;

import java.util.List;

/**
 * The original's match filter: an item you put another item in (use it to open its screen), which it keeps; sneak and use it to forget the item. In the original that was
 * the whole of it; here an Item Filter given one in its template slot filters by the item inside it, so a filter can be carried from machine to machine.
 */
public class MatchFilterItem extends Item {
    public MatchFilterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** The item the filter holds, or empty. */
    public static ItemStack template(ItemStack filter) {
        return filter.getOrDefault(HandheldRegistry.MATCH_TEMPLATE.get(), ItemContainerContents.EMPTY).copyOne();
    }

    public static void setTemplate(ItemStack filter, ItemStack template) {
        if (template.isEmpty()) {
            filter.remove(HandheldRegistry.MATCH_TEMPLATE.get());
        } else {
            filter.set(HandheldRegistry.MATCH_TEMPLATE.get(), ItemContainerContents.fromItems(List.of(template.copyWithCount(1))));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            setTemplate(stack, ItemStack.EMPTY);
            return InteractionResultHolder.success(stack);
        }
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new MatchFilterMenu(id, inventory, hand), Component.translatable("item.rotarycraft.match_filter")),
                    buf -> buf.writeEnum(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ItemStack held = template(stack);
        if (!held.isEmpty()) {
            tooltip.add(Component.translatable("item.rotarycraft.match_filter.holds", held.getHoverName()));
        }
    }
}
