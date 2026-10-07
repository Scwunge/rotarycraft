package net.scwunge.rotarycraft.crafting;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.scwunge.rotarycraft.menu.CraftPatternMenu;
import net.scwunge.rotarycraft.registry.RotaryComponents;

import java.util.List;

/**
 * A Craft Pattern, the original's programmable recipe card. Right-click to open it and lay a recipe out (the kind of recipe, and how many
 * of each ingredient a machine may keep, are set there too); sneak and right-click to wipe it. Put in a Worktable it makes the table take
 * only that recipe's items, and in an Auto-Crafter it is the recipe that slot makes.
 */
public class CraftPatternItem extends Item {
    public CraftPatternItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                stack.remove(RotaryComponents.CRAFT_PATTERN.get());
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (player instanceof ServerPlayer sp) {
            sp.openMenu(new SimpleMenuProvider((id, inventory, p) -> new CraftPatternMenu(id, inventory, hand), stack.getHoverName()), buf -> buf.writeEnum(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CraftPattern pattern = CraftPattern.of(stack);
        if (pattern.hasRecipe()) {
            tooltip.add(Component.translatable("item.rotarycraft.craft_pattern.crafts", pattern.output().getCount(), pattern.output().getHoverName()));
        } else if (pattern.inputs().stream().anyMatch(s -> !s.isEmpty())) {
            tooltip.add(Component.translatable("item.rotarycraft.craft_pattern.no_output"));
        } else {
            tooltip.add(Component.translatable("item.rotarycraft.craft_pattern.empty"));
        }
        tooltip.add(Component.translatable("item.rotarycraft.craft_pattern.mode", pattern.mode().label()));
    }
}
