package net.scwunge.rotarycraft.charged;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.scwunge.rotarycraft.tool.ToolEvents;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The original's stun gun: a burst of force that throws back whatever (except players) it meets in the five blocks in front of you, for a unit of charge; and,
 * sneaking and with the charge above 8192 kJ, it clears every ore, web and soft block of a kind that touches the one you point at (the glass, ice, leaves,
 * sand and snow around it too, within four blocks), for two.
 */
public class StunGunItem extends ChargedItem {
    public static final int BLOCK_MODE_CHARGE = 8192;
    public static final int LIMIT = 512;

    public StunGunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide() && !Charge.use(stack, player, 1, "tool")) {
            return InteractionResultHolder.fail(stack);
        }
        if (level instanceof ServerLevel server) {
            burst(server, player, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void burst(ServerLevel level, Player player, ItemStack stack) {
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        Vec3 front = eye.add(look);
        for (int i = 0; i < 12; i++) {
            level.sendParticles(ParticleTypes.ENCHANT, front.x + (level.random.nextDouble() - 0.5) * 0.6, front.y + (level.random.nextDouble() - 0.5) * 0.6,
                    front.z + (level.random.nextDouble() - 0.5) * 0.6, 0, level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5, 0.5);
        }
        level.playSound(null, player.blockPosition(), net.scwunge.rotarycraft.registry.MachineSoundRegistry.get("knockback").get(), SoundSource.PLAYERS, 2F, 2F);
        Set<LivingEntity> hit = new LinkedHashSet<>();
        for (double d = 1; d <= 5; d += 0.5) {
            Vec3 at = eye.add(look.scale(d));
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(0.5))) {
                if (!(e instanceof Player) && player.hasLineOfSight(e)) {
                    hit.add(e);
                }
            }
        }
        int level_ = stack.getEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.KNOCKBACK));
        double knock = 2 * (1 + level_ * 0.25);
        for (LivingEntity e : hit) {
            e.setDeltaMovement(look.x * knock * 0.5, 0.4, look.z * knock * 0.5);
            e.hurtMarked = true;
            level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 16, 0.3, 0.3, 0.3, 0.3);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || !player.isShiftKeyDown() || (!player.isCreative() && Charge.get(stack) < BLOCK_MODE_CHARGE)) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        }
        BlockPos start = context.getClickedPos();
        BlockState state = level.getBlockState(start);
        Set<BlockPos> group = null;
        if (isClearable(state)) {
            group = connected(level, start, state, -1);
        } else if (state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(BlockTags.ICE) || state.is(BlockTags.LEAVES) || state.is(BlockTags.SAND) || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.SNOW)) {
            group = connected(level, start, state, 4);
        }
        if (group == null || !Charge.use(stack, player, 2, "tool")) {
            return InteractionResult.PASS;
        }
        for (BlockPos p : group) {
            level.sendParticles(ParticleTypes.CRIT, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.4, 0.4, 0.4, 0.3);
        }
        ToolEvents.takeAll(level, sp, stack, group);
        return InteractionResult.SUCCESS;
    }

    /** Ores, webs, mushrooms, gravel, infested blocks, lily pads, flower pots and the soft plants. */
    public static boolean isClearable(BlockState state) {
        return state.is(Tags.Blocks.ORES) || state.is(Blocks.COBWEB) || state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.BROWN_MUSHROOM) || state.is(Blocks.GRAVEL)
                || state.getBlock() instanceof InfestedBlock || state.is(Blocks.LILY_PAD) || state.is(Blocks.FLOWER_POT)
                || (state.canBeReplaced() && !state.isAir() && state.getFluidState().isEmpty() && !state.is(Blocks.SNOW));
    }

    /** The blocks of the same kind touching one another from here, up to {@link #LIMIT} (and within {@code radius} of it, if that is not negative). */
    public static Set<BlockPos> connected(ServerLevel level, BlockPos start, BlockState state, int radius) {
        Set<BlockPos> found = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        found.add(start);
        queue.add(start);
        while (!queue.isEmpty() && found.size() < LIMIT) {
            BlockPos at = queue.poll();
            for (BlockPos n : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
                if (found.contains(n) || (radius >= 0 && n.distSqr(start) > radius * radius) || level.getBlockState(n).getBlock() != state.getBlock()) {
                    continue;
                }
                BlockPos p = n.immutable();
                found.add(p);
                queue.add(p);
            }
        }
        return found;
    }
}
