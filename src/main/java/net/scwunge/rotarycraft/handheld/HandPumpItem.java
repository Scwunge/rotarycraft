package net.scwunge.rotarycraft.handheld;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scwunge.rotarycraft.charged.Charge;
import net.scwunge.rotarycraft.charged.ChargedItem;
import net.scwunge.rotarycraft.registry.HandheldRegistry;

import java.util.List;

/**
 * The original's hand pump: a tank of 64 buckets in your hand. In drain mode a use on a source block of liquid within five blocks sucks it in (a bucket for a unit of
 * charge; with Aqua Affinity the same liquid in the cube of five round it too), and a use on a tank or machine pours the pump into it. Sneaking turns it to place mode,
 * where a use on a tank or machine draws its fluid into the pump, and a use on a block lays the fluid down as a source block beside it.
 */
public class HandPumpItem extends ChargedItem {
    public static final int CAPACITY = 64_000;

    public HandPumpItem(Properties properties) {
        super(properties);
    }

    public static boolean placing(ItemStack stack) {
        return stack.getOrDefault(HandheldRegistry.PUMP_PLACING.get(), false);
    }

    public static FluidStack contents(ItemStack stack) {
        return stack.getOrDefault(HandheldRegistry.PUMP_CONTENTS.get(), SimpleFluidContent.EMPTY).copy();
    }

    private static void setContents(ItemStack stack, FluidStack fluid) {
        if (fluid.isEmpty()) {
            stack.remove(HandheldRegistry.PUMP_CONTENTS.get());
        } else {
            stack.set(HandheldRegistry.PUMP_CONTENTS.get(), SimpleFluidContent.copyOf(fluid));
        }
    }

    /** Whether the player may break (or put something in place of) a block there. */
    private static boolean mayChange(Player player, BlockPos pos) {
        if (!player.level().mayInteract(player, pos)) {
            return false;
        }
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(player.level(), pos, player.level().getBlockState(pos), player);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            stack.set(HandheldRegistry.PUMP_PLACING.get(), !placing(stack));
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable(placing(stack) ? "message.rotarycraft.pump.place" : "message.rotarycraft.pump.drain"), true);
            }
            return InteractionResultHolder.success(stack);
        }
        if (placing(stack)) {
            return InteractionResultHolder.pass(stack);
        }
        BlockHitResult hit = level.clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(5)), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.consume(stack);
        }
        if (Charge.get(stack) <= 0) {
            Charge.use(stack, player, 1, "tool");
            return InteractionResultHolder.fail(stack);
        }
        BlockPos pos = hit.getBlockPos();
        FluidState fluid = level.getFluidState(pos);
        if (!fluid.isSource() || !mayChange(player, pos) || !drain(level, player, stack, pos, fluid.getType())) {
            return InteractionResultHolder.pass(stack);
        }
        if (stack.getEnchantmentLevel(((ServerLevel) level).registryAccess().holderOrThrow(Enchantments.AQUA_AFFINITY)) > 0) {
            for (BlockPos near : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
                BlockPos at = near.immutable();
                if (!at.equals(pos) && level.getFluidState(at).isSource() && level.getFluidState(at).getType() == fluid.getType() && mayChange(player, at)) {
                    if (!drain(level, player, stack, at, fluid.getType())) {
                        break;
                    }
                }
            }
        }
        return InteractionResultHolder.success(stack);
    }

    /** Takes the source block at {@code pos} into the pump, a unit of charge for each. */
    private static boolean drain(Level level, Player player, ItemStack stack, BlockPos pos, Fluid fluid) {
        FluidStack held = contents(stack);
        if (!held.isEmpty() && held.getFluid() != fluid || held.getAmount() + 1000 > CAPACITY) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof LiquidBlock) && !(block instanceof BucketPickup)) {
            return false;
        }
        if (!Charge.use(stack, player, 1, "tool")) {
            return false;
        }
        if (block instanceof LiquidBlock) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else {
            ((BucketPickup) block).pickupBlock(player, level, pos, state);
        }
        setContents(stack, new FluidStack(fluid, held.getAmount() + 1000));
        level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1F, 1F);
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, context.getClickedFace());
        if (tank != null) {
            if (!level.isClientSide()) {
                FluidStack held = contents(stack);
                if (placing(stack)) {
                    FluidStack taken = tank.drain(CAPACITY - held.getAmount(), IFluidHandler.FluidAction.SIMULATE);
                    if (!taken.isEmpty() && (held.isEmpty() || FluidStack.isSameFluidSameComponents(held, taken))) {
                        taken = tank.drain(taken, IFluidHandler.FluidAction.EXECUTE);
                        setContents(stack, new FluidStack(taken.getFluid(), held.getAmount() + taken.getAmount()));
                    }
                } else if (!held.isEmpty()) {
                    int filled = tank.fill(held, IFluidHandler.FluidAction.EXECUTE);
                    setContents(stack, held.copyWithAmount(held.getAmount() - filled));
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!placing(stack)) {
            return InteractionResult.PASS;
        }
        FluidStack held = contents(stack);
        if (level.isClientSide()) {
            return held.getAmount() >= 1000 ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (Charge.get(stack) <= 0) {
            Charge.use(stack, player, 1, "tool");
            return InteractionResult.FAIL;
        }
        if (held.getAmount() < 1000) {
            return InteractionResult.PASS;
        }
        BlockPos at = pos.relative(context.getClickedFace());
        BlockState block = held.getFluid().defaultFluidState().createLegacyBlock();
        BlockState there = level.getBlockState(at);
        if (block.isAir() || !(there.isAir() || there.canBeReplaced() || there.getBlock() == block.getBlock() && !there.getFluidState().isSource()) || !mayChange(player, at)) {
            return InteractionResult.PASS;
        }
        if (!Charge.use(stack, player, 1, "tool")) {
            return InteractionResult.FAIL;
        }
        level.setBlock(at, block, 11);
        level.playSound(null, at, SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1F, 1F);
        setContents(stack, held.copyWithAmount(held.getAmount() - 1000));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        FluidStack held = contents(stack);
        if (!held.isEmpty()) {
            tooltip.add(Component.translatable("item.rotarycraft.hand_pump.contents", held.getAmount(), held.getHoverName()));
        }
        tooltip.add(Component.translatable(placing(stack) ? "item.rotarycraft.hand_pump.mode_place" : "item.rotarycraft.hand_pump.mode_drain"));
    }
}
