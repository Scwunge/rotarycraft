package net.scwunge.rotarycraft.handheld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.scwunge.rotarycraft.charged.Charge;
import net.scwunge.rotarycraft.charged.ChargedItem;

import java.util.ArrayList;
import java.util.List;

/**
 * The original's spring piston: shoves the block you click one step away from you, if there is room behind it, for a unit of charge. Sneaking, it shoves the whole run of
 * blocks behind it, as many as the charge pays for (two a block, eight for the toughest, such as obsidian), up to twelve, as far as the first soft space. It will not move
 * anything with a block entity, bedrock, or anything you may not break.
 */
public class SpringPistonItem extends ChargedItem {
    public static final int MAX_LINE = 12;
    public static final int TOUGH_COST = 8;
    public static final int COST = 2;

    public SpringPistonItem(Properties properties) {
        super(properties);
    }

    private static boolean soft(BlockState state) {
        return state.isAir() || state.canBeReplaced();
    }

    private static boolean movable(Level level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        if (state.getDestroySpeed(level, pos) < 0 || level.getBlockEntity(pos) != null || !level.mayInteract(player, pos)) {
            return false;
        }
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return Charge.get(stack) > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (Charge.get(stack) <= 0) {
            Charge.use(stack, player, 1, "tool");
            return InteractionResult.FAIL;
        }
        Direction dir = context.getClickedFace().getOpposite();
        BlockPos start = context.getClickedPos();
        if (!movable(level, start, player)) {
            return InteractionResult.FAIL;
        }
        List<BlockPos> line = new ArrayList<>();
        int cost = 0;
        if (player.isShiftKeyDown()) {
            int limit = Charge.get(stack);
            for (int i = 0; i <= MAX_LINE + 1; i++) {
                BlockPos at = start.relative(dir, i);
                if (!level.isInWorldBounds(at)) {
                    return InteractionResult.FAIL;
                }
                BlockState state = level.getBlockState(at);
                if (soft(state) && i > 0) {
                    break;
                }
                if (i == MAX_LINE + 1 || !movable(level, at, player)) {
                    return InteractionResult.FAIL;
                }
                int each = state.getDestroySpeed(level, at) >= 20 ? TOUGH_COST : COST;
                if (!player.isCreative() && cost + each > limit) {
                    break;
                }
                cost += each;
                line.add(at);
            }
            if (line.isEmpty()) {
                return InteractionResult.FAIL;
            }
            BlockPos beyond = start.relative(dir, line.size());
            if (!soft(level.getBlockState(beyond)) || !level.mayInteract(player, beyond)) {
                return InteractionResult.FAIL;
            }
        } else {
            BlockPos beyond = start.relative(dir);
            if (!soft(level.getBlockState(beyond)) || !level.mayInteract(player, beyond)) {
                return InteractionResult.FAIL;
            }
            line.add(start);
            cost = 1;
        }
        if (!Charge.use(stack, player, cost, "tool")) {
            return InteractionResult.FAIL;
        }
        for (int i = line.size() - 1; i >= 0; i--) {
            BlockPos from = line.get(i);
            level.setBlock(from.relative(dir), level.getBlockState(from), 3);
        }
        level.setBlock(start, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        level.playSound(null, start, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, 1F);
        return InteractionResult.SUCCESS;
    }
}
