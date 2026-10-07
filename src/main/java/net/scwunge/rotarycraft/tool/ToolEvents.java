package net.scwunge.rotarycraft.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.scwunge.rotarycraft.RotaryCraft;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.scwunge.rotarycraft.config.RotaryConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the sickles, the bedrock axe, shovel, pickaxe, shears and sword do to the world beyond the single block or creature: they act as blocks break, after claim
 * and protection mods have had their say on the first one (and on each further block, which is offered to them too).
 */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID)
public final class ToolEvents {
    private static final ThreadLocal<Boolean> BUSY = ThreadLocal.withInitial(() -> false);
    private static final int TREE_LIMIT = 2048;

    private ToolEvents() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void blockBroken(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null || !(event.getLevel() instanceof ServerLevel level) || !(player instanceof ServerPlayer sp) || BUSY.get()) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        if (tool.getItem() instanceof SickleItem sickle) {
            if (sickle.breakAround(tool, pos, player)) {
                event.setCanceled(true);
            }
        } else if (tool.getItem() instanceof BedrockAxeItem && !player.isShiftKeyDown() && !player.isCreative() && mayUseAbilities(player)) {
            if (fell(level, sp, tool, pos, state)) {
                event.setCanceled(true);
            }
        } else if (tool.getItem() instanceof BedrockPickaxeItem && state.is(Blocks.SPAWNER) && RotaryConfig.get(RotaryConfig.BEDROCK_PICK_SPAWNERS) && !player.isCreative() && mayUseAbilities(player)) {
            ItemStack drop = new ItemStack(Blocks.SPAWNER);
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                var tag = be.saveWithoutMetadata(level.registryAccess());
                BlockEntity.addEntityType(tag, be.getType());
                drop.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
            }
            maybeLeak(level, pos, player);
            level.removeBlock(pos, false);
            Block.popResource(level, pos, drop);
            event.setCanceled(true);
        } else if (tool.getItem() instanceof BedrockShearsItem && !player.isCreative()) {
            if (shearWhole(level, sp, tool, pos, state)) {
                event.setCanceled(true);
            }
        } else if (tool.getItem() instanceof BedrockShovelItem && !player.isCreative()) {
            extraFinds(level, tool, pos, state);
        }
    }

    /** The bedrock tools' special abilities are for fake players (auto activators) too, unless the config says not. */
    public static boolean mayUseAbilities(Player player) {
        return !(player instanceof FakePlayer) || RotaryConfig.get(RotaryConfig.FAKE_PLAYER_BEDROCK);
    }

    /** Lifting a spawner by hand (not with an auto activator) lets out a dozen to three dozen of what it spawns, if the config says so. */
    public static void maybeLeak(ServerLevel level, BlockPos pos, Player player) {
        if (RotaryConfig.get(RotaryConfig.SPAWNERS_LEAK) && !(player instanceof FakePlayer)) {
            leak(level, pos);
        }
    }

    private static void leak(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner)) {
            return;
        }
        net.minecraft.world.entity.Entity sample = spawner.getSpawner().getOrCreateDisplayEntity(level, pos);
        if (sample == null) {
            return;
        }
        int count = 12 + level.random.nextInt(25);
        for (int i = 0; i < count; i++) {
            net.minecraft.world.entity.Entity mob = sample.getType().create(level);
            if (mob == null) {
                continue;
            }
            mob.moveTo(pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 8, pos.getY() + level.random.nextInt(3) - 1, pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 8,
                    level.random.nextFloat() * 360F, 0F);
            if (!level.noCollision(mob)) {
                continue;
            }
            if (mob instanceof net.minecraft.world.entity.Mob m) {
                m.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), net.minecraft.world.entity.MobSpawnType.SPAWNER, null);
            }
            level.addFreshEntity(mob);
        }
    }

    // ---- the bedrock axe takes the whole tree ----

    private static boolean isLeafLike(BlockState state) {
        return state.getBlock() instanceof LeavesBlock || state.is(BlockTags.WART_BLOCKS) || state.is(Blocks.SHROOMLIGHT);
    }

    private static boolean fell(ServerLevel level, ServerPlayer player, ItemStack tool, BlockPos start, BlockState first) {
        if (first.getBlock() instanceof HugeMushroomBlock) {
            Set<BlockPos> shroom = new LinkedHashSet<>();
            for (BlockPos p : BlockPos.betweenClosed(start.offset(-3, -3, -3), start.offset(3, 3, 3))) {
                if (level.getBlockState(p).getBlock() instanceof HugeMushroomBlock) {
                    shroom.add(p.immutable());
                }
            }
            takeAll(level, player, tool, shroom);
            return true;
        }
        if (!first.is(BlockTags.LOGS)) {
            return false;
        }
        Set<BlockPos> logs = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        logs.add(start);
        while (!queue.isEmpty() && logs.size() < TREE_LIMIT) {
            BlockPos at = queue.poll();
            for (BlockPos n : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
                if (!logs.contains(n) && level.getBlockState(n).is(BlockTags.LOGS)) {
                    BlockPos p = n.immutable();
                    logs.add(p);
                    queue.add(p);
                }
            }
        }
        Set<BlockPos> leaves = new LinkedHashSet<>();
        for (BlockPos log : logs) {
            queue.add(log);
        }
        while (!queue.isEmpty() && logs.size() + leaves.size() < TREE_LIMIT) {
            BlockPos at = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = at.relative(d);
                if (logs.contains(n) || leaves.contains(n)) {
                    continue;
                }
                BlockState s = level.getBlockState(n);
                if (isLeafLike(s) && !(s.hasProperty(LeavesBlock.PERSISTENT) && s.getValue(LeavesBlock.PERSISTENT))) {
                    leaves.add(n.immutable());
                    queue.add(n.immutable());
                }
            }
        }
        if (leaves.isEmpty()) {
            return false; // a pillar of logs is not a tree
        }
        Set<BlockPos> all = new LinkedHashSet<>(logs);
        all.addAll(leaves);
        takeAll(level, player, tool, all);
        return true;
    }

    /** Breaks each block, offering it to protection mods first, and drops what it would with this tool. */
    public static void takeAll(ServerLevel level, ServerPlayer player, ItemStack tool, Set<BlockPos> positions) {
        BUSY.set(true);
        try {
            for (BlockPos pos : positions) {
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()) {
                    continue;
                }
                List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, tool);
                level.destroyBlock(pos, false, player);
                for (ItemStack drop : drops) {
                    Block.popResource(level, pos, drop);
                }
            }
        } finally {
            BUSY.set(false);
        }
    }

    // ---- the bedrock shears take the block itself ----

    private static boolean shearWhole(ServerLevel level, ServerPlayer player, ItemStack tool, BlockPos pos, BlockState state) {
        Block b = state.getBlock();
        if (state.isAir() || level.getBlockEntity(pos) != null || state.getDestroySpeed(level, pos) < 0 || b instanceof SugarCaneBlock || b instanceof StemBlock
                || b instanceof net.minecraft.world.level.block.AttachedStemBlock) {
            return false;
        }
        ItemStack silk = tool.copy();
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, silk);
        if (drops.isEmpty()) {
            return false;
        }
        level.destroyBlock(pos, false, player);
        for (ItemStack drop : drops) {
            Block.popResource(level, pos, drop);
        }
        return true;
    }

    // ---- the bedrock shovel turns up finds ----

    private record Find(ItemLike item, double percent) {
    }

    private static final Map<Block, List<Find>> FINDS = Map.of(
            Blocks.GRASS_BLOCK, List.of(new Find(Items.WHEAT_SEEDS, 10), new Find(Items.CLAY_BALL, 5), new Find(Blocks.MYCELIUM, 0.5), new Find(Items.PUMPKIN_SEEDS, 5),
                    new Find(Items.MELON_SEEDS, 5)),
            Blocks.DIRT, List.of(new Find(Items.WHEAT_SEEDS, 10), new Find(Items.GLOWSTONE_DUST, 2), new Find(Items.NETHER_WART, 0.5), new Find(Items.EMERALD, 0.05),
                    new Find(Items.DIAMOND, 0.05)),
            Blocks.SAND, List.of(new Find(Items.GUNPOWDER, 2)),
            Blocks.CLAY, List.of(new Find(Items.BONE, 5), new Find(Blocks.SOUL_SAND, 2), new Find(Items.GOLD_NUGGET, 4)),
            Blocks.SOUL_SAND, List.of(new Find(Items.BLAZE_POWDER, 4), new Find(Items.NETHER_WART, 5), new Find(Items.QUARTZ, 2)));

    private static void extraFinds(ServerLevel level, ItemStack tool, BlockPos pos, BlockState state) {
        List<Find> finds = FINDS.get(state.getBlock());
        if (finds == null) {
            return;
        }
        var fortune = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        double multiplier = Math.sqrt(1 + tool.getEnchantmentLevel(fortune));
        for (Find f : finds) {
            if (level.random.nextDouble() * 100 < f.percent() * multiplier) {
                Block.popResource(level, pos, new ItemStack(f.item()));
            }
        }
    }

    // ---- the bedrock sword takes more from what it kills ----

    @SubscribeEvent
    public static void experience(LivingExperienceDropEvent event) {
        Player killer = event.getAttackingPlayer();
        if (killer != null && killer.getMainHandItem().getItem() instanceof BedrockSwordItem) {
            event.setDroppedExperience(event.getDroppedExperience() * (1 + killer.getRandom().nextInt(10)));
        }
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer) || !(killer.getMainHandItem().getItem() instanceof BedrockSwordItem)) {
            return;
        }
        var type = event.getEntity().getType();
        ItemLike head = type == EntityType.SKELETON ? Items.SKELETON_SKULL : type == EntityType.WITHER_SKELETON ? Items.WITHER_SKELETON_SKULL
                : type == EntityType.ZOMBIE ? Items.ZOMBIE_HEAD : type == EntityType.CREEPER ? Items.CREEPER_HEAD : type == EntityType.PIGLIN ? Items.PIGLIN_HEAD
                : type == EntityType.PLAYER ? Items.PLAYER_HEAD : type == EntityType.ENDER_DRAGON ? Items.DRAGON_HEAD : null;
        if (head != null && killer.getRandom().nextInt(5) == 0) {
            var e = event.getEntity();
            event.getDrops().add(new ItemEntity(e.level(), e.getX(), e.getY(), e.getZ(), new ItemStack(head)));
        }
    }
}
