package net.scwunge.rotarycraft.decor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.scwunge.rotarycraft.registry.DecorRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Decorative Tank (BlockDecoTank): a glass tank block. A tank item that is full of a fluid (25 mB) places a tank full of it, shown inside the glass; the four
 * settings (clear glass, ignore the fluid's colour, glowing, resistant) are block state, set by the item. Resistant tanks take twice as long to break and
 * shrug off explosions; glowing tanks give full light. Breaking one gives back the tank with its fluid and settings.
 */
public class DecoTankBlock extends BaseEntityBlock {
    public DecoTankBlock() {
        super(BlockBehaviour.Properties.of().strength(0.35F, 2F).sound(SoundType.GLASS).noOcclusion().isValidSpawn((s, l, p, e) -> false)
                .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)
                .lightLevel(s -> s.getValue(DecoTank.Flag.LIGHTED.property) ? 15 : 0));
        BlockState state = stateDefinition.any();
        for (DecoTank.Flag flag : DecoTank.Flag.LIST) {
            state = state.setValue(flag.property, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new DecoTankBlock());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (DecoTank.Flag flag : DecoTank.Flag.LIST) {
            builder.add(flag.property);
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (DecoTank.Flag flag : DecoTank.Flag.LIST) {
            state = state.setValue(flag.property, DecoTank.has(context.getItemInHand(), flag));
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof DecoTankBlockEntity tank) {
            SimpleFluidContent content = stack.get(DecoTank.FLUID.get());
            if (content != null && content.copy().getAmount() >= DecoTank.FILL) {
                tank.setFluid(content.copy().copyWithAmount(DecoTank.FILL));
            }
        }
    }

    /** The tank as an item: its fluid, if it has one, and its settings. */
    public static ItemStack itemOf(BlockState state, @Nullable BlockEntity be) {
        ItemStack stack = new ItemStack(DecorRegistry.DECO_TANK.get());
        int flags = 0;
        for (DecoTank.Flag flag : DecoTank.Flag.LIST) {
            if (state.getValue(flag.property)) {
                flags |= flag.bit();
            }
        }
        if (flags != 0) {
            stack.set(DecoTank.FLAGS.get(), flags);
        }
        if (be instanceof DecoTankBlockEntity tank && !tank.fluid().isEmpty()) {
            stack.set(DecoTank.FLUID.get(), SimpleFluidContent.copyOf(tank.fluid()));
        }
        return stack;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(itemOf(state, params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY)));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target, LevelReader level, BlockPos pos, Player player) {
        return itemOf(state, level.getBlockEntity(pos));
    }

    /** Resistant tanks take twice as long to break. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float progress = super.getDestroyProgress(state, player, level, pos);
        return state.getValue(DecoTank.Flag.RESISTANT.property) ? progress / 2 : progress;
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return state.getValue(DecoTank.Flag.RESISTANT.property) ? 600_000F : super.getExplosionResistance(state, level, pos, explosion);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1F;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DecoTankBlockEntity(pos, state);
    }
}
