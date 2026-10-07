package net.scwunge.rotarycraft.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The bedrock hoe: never wears, tills a square five across at once, and, sneaking, sows grass back over dirt and tilled soil (using a wheat seed for each
 * block, unless you are in creative).
 */
public class BedrockHoeItem extends HoeItem {
    public static final int RADIUS = 2;

    public BedrockHoeItem() {
        super(BedrockTools.tier(12F), BedrockTools.properties(1, -1.0F));
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 0;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player != null && !ToolEvents.mayUseAbilities(player)) {
            return super.useOn(context);
        }
        BlockPos origin = context.getClickedPos();
        boolean any = false;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                BlockPos pos = origin.offset(dx, 0, dz);
                if (player != null && player.isShiftKeyDown()) {
                    any |= sow(level, player, pos);
                } else {
                    BlockHitResult hit = new BlockHitResult(context.getClickLocation().add(dx, 0, dz), context.getClickedFace(), pos, false);
                    UseOnContext each = new UseOnContext(level, player, context.getHand(), context.getItemInHand(), hit);
                    if (super.useOn(each).consumesAction()) {
                        any = true;
                        BlockState tilled = level.getBlockState(pos);
                        if (tilled.is(Blocks.FARMLAND)) {
                            level.setBlock(pos, tilled.setValue(FarmBlock.MOISTURE, 2), Block.UPDATE_ALL);
                        }
                    }
                }
            }
        }
        return any ? InteractionResult.sidedSuccess(level.isClientSide()) : InteractionResult.PASS;
    }

    private static boolean sow(Level level, Player player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.is(Blocks.DIRT) || state.is(Blocks.FARMLAND) || state.is(Blocks.COARSE_DIRT)) || level.getBlockState(pos.above()).isSolidRender(level, pos.above())) {
            return false;
        }
        if (!player.isCreative()) {
            int slot = -1;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i).is(Items.WHEAT_SEEDS)) {
                    slot = i;
                    break;
                }
            }
            if (slot < 0) {
                return false;
            }
            if (!level.isClientSide()) {
                player.getInventory().getItem(slot).shrink(1);
            }
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.4F, 1F);
        }
        return true;
    }
}
