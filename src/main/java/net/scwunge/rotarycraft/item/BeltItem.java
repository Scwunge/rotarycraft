package net.scwunge.rotarycraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.scwunge.rotarycraft.registry.RotaryComponents;
import net.scwunge.rotarycraft.transmission.BeltHubBlock;
import net.scwunge.rotarycraft.transmission.BeltHubBlockEntity;

/**
 * A belt or a chain, as the original: use it on one pulley and then on another to join them, spending one for every block between them (the
 * first click is remembered in the stack until the second; any other use forgets it). A belt fits belts and split belts, a chain fits chains.
 */
public class BeltItem extends Item {
    private final boolean chain;

    public BeltItem(Properties props, boolean chain) {
        super(props);
        this.chain = chain;
    }

    private boolean fits(BeltHubBlock.Kind kind) {
        return chain == (kind == BeltHubBlock.Kind.CHAIN);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (!(level.getBlockEntity(pos) instanceof BeltHubBlockEntity hub) || !fits(hub.kind())) {
            stack.remove(RotaryComponents.BELT_END.get());
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        GlobalPos first = stack.get(RotaryComponents.BELT_END.get());
        if (first == null) {
            stack.set(RotaryComponents.BELT_END.get(), GlobalPos.of(level.dimension(), pos));
            message(player, "message.rotarycraft.belt.first");
            return InteractionResult.SUCCESS;
        }
        stack.remove(RotaryComponents.BELT_END.get());
        if (first.dimension() != level.dimension() || !level.isLoaded(first.pos()) || !(level.getBlockEntity(first.pos()) instanceof BeltHubBlockEntity start)) {
            message(player, "message.rotarycraft.belt.lost");
            return InteractionResult.FAIL;
        }
        int needed = Math.max(first.pos().distManhattan(pos) - 1, 0);
        boolean creative = player != null && player.getAbilities().instabuild;
        if (stack.getCount() < needed && !creative) {
            message(player, "message.rotarycraft.belt.short", needed);
            return InteractionResult.FAIL;
        }
        start.resetOther();
        hub.resetOther();
        start.reset();
        hub.reset();
        if (start.tryConnect(pos) && hub.tryConnect(first.pos())) {
            if (!creative) {
                stack.shrink(needed);
            }
            return InteractionResult.SUCCESS;
        }
        start.reset();
        hub.reset();
        message(player, "message.rotarycraft.belt.invalid");
        return InteractionResult.FAIL;
    }

    private static void message(Player player, String key, Object... args) {
        if (player != null) {
            player.displayClientMessage(Component.translatable(key, args), true);
        }
    }
}
