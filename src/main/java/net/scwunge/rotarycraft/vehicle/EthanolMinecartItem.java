package net.scwunge.rotarycraft.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

/** Puts an Ethanol Minecart on a rail. */
public class EthanolMinecartItem extends Item {
    public EthanolMinecartItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.RAILS)) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            RailShape shape = state.getBlock() instanceof BaseRailBlock rail ? rail.getRailDirection(state, level, pos, null) : RailShape.NORTH_SOUTH;
            GasMinecart cart = new GasMinecart(level, pos.getX() + 0.5, pos.getY() + 0.0625 + (shape.isAscending() ? 0.5 : 0), pos.getZ() + 0.5);
            if (context.getItemInHand().has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
                cart.setCustomName(context.getItemInHand().getHoverName());
            }
            server.addFreshEntity(cart);
        }
        context.getItemInHand().shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
