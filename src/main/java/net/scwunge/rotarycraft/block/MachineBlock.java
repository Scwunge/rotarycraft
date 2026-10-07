package net.scwunge.rotarycraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.scwunge.rotarycraft.blockentity.BlastFurnaceBlockEntity;
import net.scwunge.rotarycraft.blockentity.ExtractorBlockEntity;
import net.scwunge.rotarycraft.blockentity.FermenterBlockEntity;
import net.scwunge.rotarycraft.blockentity.GrinderBlockEntity;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A directional shaft-power block. FACING is the output side; power comes in from the opposite side. When placed, the
 * block faces where the player is looking, so a line of shafts points away from you.
 */
public class MachineBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private final Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type;
    private final BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory;

    public MachineBlock(Properties props, Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type,
                        BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory) {
        super(props);
        this.type = type;
        this.factory = factory;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        // Block codecs are only used to serialize block types, which modded blocks don't need; keep a valid one anyway.
        return simpleCodec(p -> new MachineBlock(p, type, factory));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Buckets and other fluid containers fill or empty machines that have tanks. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, hit.getDirection());
        if (tank != null && FluidUtil.getFluidHandler(stack).isPresent()
                && FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /** Machines with a screen open it on right-click. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
            if (player instanceof ServerPlayer sp) {
                sp.openMenu(provider, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    /** Drop machine inventories when the block is broken. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof net.scwunge.rotarycraft.blockentity.RefrigeratorBlockEntity fridge && !level.isClientSide()) {
                fridge.onBroken();
            }
            net.neoforged.neoforge.items.ItemStackHandler items = level.getBlockEntity(pos) instanceof GrinderBlockEntity g ? g.items()
                    : level.getBlockEntity(pos) instanceof ExtractorBlockEntity e ? e.items()
                    : level.getBlockEntity(pos) instanceof BlastFurnaceBlockEntity b ? b.items()
                    : level.getBlockEntity(pos) instanceof FermenterBlockEntity f ? f.items()
                    : level.getBlockEntity(pos) instanceof net.scwunge.rotarycraft.blockentity.CentrifugeBlockEntity c ? c.items()
                    : level.getBlockEntity(pos) instanceof net.scwunge.rotarycraft.blockentity.RockMelterBlockEntity m ? m.items()
                    : level.getBlockEntity(pos) instanceof net.scwunge.rotarycraft.blockentity.FuelEngineBlockEntity f ? f.items()
                    : level.getBlockEntity(pos) instanceof net.scwunge.rotarycraft.menu.OneSlotMenu.Host h ? h.items()
                    : level.getBlockEntity(pos) instanceof net.scwunge.rotarycraft.blockentity.FractionatorBlockEntity fr ? fr.items()
                    : level.getBlockEntity(pos) instanceof net.scwunge.rotarycraft.blockentity.CrystallizerBlockEntity cr ? cr.items()
                    : level.getBlockEntity(pos) instanceof net.scwunge.rotarycraft.blockentity.PulseFurnaceBlockEntity pf ? pf.items()
                    : level.getBlockEntity(pos) instanceof net.scwunge.rotarycraft.blockentity.RefrigeratorBlockEntity rf ? rf.items()
                    : level.getBlockEntity(pos) instanceof net.scwunge.rotarycraft.blockentity.CompactorBlockEntity cp ? cp.items() : null;
            if (items != null) {
                for (int i = 0; i < items.getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.getStackInSlot(i));
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide() || blockEntityType != type.get()) {
            return null;
        }
        return (l, p, s, be) -> {
            if (!((PowerBlockEntity) be).isShutdown()) {
                ((PowerBlockEntity) be).serverTick();
                if (!(be instanceof net.scwunge.rotarycraft.farm.FarmBlockEntity farm) || farm.isSwitchedOn()) {
                    net.scwunge.rotarycraft.sound.MachineSounds.tick((PowerBlockEntity) be);
                }
            }
        };
    }
}
