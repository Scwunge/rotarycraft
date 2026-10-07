package net.scwunge.rotarycraft.handheld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scwunge.rotarycraft.registry.HandheldRegistry;

/**
 * The original's tile selector: use it on a machine that takes a selection (the Terraformer) to link to it, then on blocks to add them to that machine's selection.
 * Sneak to add the machine's own place instead of linking, or sneak in the air to forget the link.
 */
public class TileSelectorItem extends Item {
    public TileSelectorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockEntity(pos) instanceof SelectableTiles tiles && !player.isShiftKeyDown()) {
            stack.set(HandheldRegistry.TILE_LINK.get(), GlobalPos.of(level.dimension(), pos));
            player.displayClientMessage(Component.translatable("message.rotarycraft.selector.linked", tiles.selectionName()), true);
            return InteractionResult.SUCCESS;
        }
        SelectableTiles controller = controller(level, stack);
        if (controller != null) {
            boolean taken = controller.addTile(pos);
            player.displayClientMessage(Component.translatable(taken ? "message.rotarycraft.selector.added" : "message.rotarycraft.selector.refused", pos.getX(), pos.getY(), pos.getZ(),
                    controller.selectionName()), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.has(HandheldRegistry.TILE_LINK.get())) {
            stack.remove(HandheldRegistry.TILE_LINK.get());
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.rotarycraft.selector.unlinked"), true);
            }
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    /** The machine the selector is linked to, if it is still there. */
    public static SelectableTiles controller(Level level, ItemStack stack) {
        GlobalPos link = stack.get(HandheldRegistry.TILE_LINK.get());
        if (link == null || !(level instanceof ServerLevel server) || !server.dimension().equals(link.dimension()) || !server.isLoaded(link.pos())) {
            return null;
        }
        BlockEntity be = server.getBlockEntity(link.pos());
        return be instanceof SelectableTiles tiles ? tiles : null;
    }
}
