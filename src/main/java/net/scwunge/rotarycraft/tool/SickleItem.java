package net.scwunge.rotarycraft.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The original's sickle: breaking a plant, a ripe crop, a stand of sugar cane or a leaf takes out every block of the same kind around it, within a reach
 * that differs for leaves, crops and other plants. Ripe crops are replanted, a block of cane keeps its base, and sneaking takes plants of every state at
 * once. Hitting a creature hurts the others of its kind beside it. The steel sickle wears out; the bedrock one does not, and shears what it cuts.
 */
public class SickleItem extends Item implements Forced {
    public record Reach(int leaf, int crop, int plant) {
    }

    private static final ThreadLocal<Boolean> BUSY = ThreadLocal.withInitial(() -> false);

    private final Reach reach;
    private final boolean breakable;
    private final boolean shears;
    private final List<Need> needs;

    public SickleItem(Properties properties, double damage, Reach reach, boolean breakable, boolean shears, List<Need> needs) {
        super(withAttributes(properties, damage, breakable));
        this.reach = reach;
        this.breakable = breakable;
        this.shears = shears;
        this.needs = needs;
    }

    @Override
    public List<Need> needs() {
        return needs;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!needs.isEmpty()) {
            Forced.tick(stack, level, entity, needs);
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, net.minecraft.world.entity.item.ItemEntity item) {
        if (!needs.isEmpty() && !item.level().isClientSide() && !Forced.intact(stack, item.level().registryAccess(), needs)) {
            item.discard();
        }
        return false;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return breakable ? 14 : BedrockTools.enchantability();
    }

    private static Properties withAttributes(Properties properties, double damage, boolean breakable) {
        ItemAttributeModifiers attributes = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage - 1, AttributeModifier.Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.2, AttributeModifier.Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build();
        Properties p = properties.stacksTo(1).attributes(attributes)
                .component(DataComponents.TOOL, new Tool(List.of(Tool.Rule.overrideSpeed(BlockTags.LEAVES, 6.0F), Tool.Rule.overrideSpeed(BlockTags.SWORD_EFFICIENT, 4.0F)), 1.0F, 1));
        return breakable ? p : p.component(DataComponents.UNBREAKABLE, new net.minecraft.world.item.component.Unbreakable(true));
    }

    public Reach reach() {
        return reach;
    }

    public boolean isBreakable() {
        return breakable;
    }

    public boolean actsAsShears() {
        return shears;
    }

    private enum Kind {
        PLANT, CANE, LEAF, CROP, ANY
    }

    private static boolean isRipeCrop(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        return state.getBlock() instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= 3;
    }

    private static boolean isCane(Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof SugarCaneBlock && level.getBlockState(pos.below()).is(Blocks.SUGAR_CANE);
    }

    private static boolean isPlant(BlockState state) {
        Block b = state.getBlock();
        return (b instanceof BushBlock && !(b instanceof CropBlock) && !(b instanceof NetherWartBlock) || b instanceof CactusBlock || b instanceof VineBlock) && !(b instanceof SugarCaneBlock);
    }

    private static Kind kindOf(Level level, BlockPos pos, BlockState state) {
        if (isPlant(state)) {
            return Kind.PLANT;
        }
        if (isCane(level, pos, state)) {
            return Kind.CANE;
        }
        if (state.getBlock() instanceof LeavesBlock) {
            return Kind.LEAF;
        }
        if (isRipeCrop(state)) {
            return Kind.CROP;
        }
        if (state.getBlock() instanceof CropBlock || state.getBlock() instanceof NetherWartBlock || state.getBlock() instanceof SugarCaneBlock) {
            return Kind.ANY;
        }
        return null;
    }

    private boolean matches(Kind kind, Level level, BlockPos pos, BlockState start, BlockPos at, BlockState state, boolean ignoreState) {
        if (state.getBlock() != start.getBlock()) {
            return false;
        }
        return switch (kind) {
            case PLANT, LEAF -> true;
            case CANE -> level.getBlockState(at.below()).is(Blocks.SUGAR_CANE);
            case CROP -> isRipeCrop(state);
            case ANY -> ignoreState || state == start;
        };
    }

    /** Whether it took over breaking the block (it cuts around it, so the single break is not wanted). */
    public boolean breakAround(ItemStack stack, BlockPos pos, Player player) {
        Level level = player.level();
        if (BUSY.get() || player.isCreative()) {
            return false;
        }
        BlockState start = level.getBlockState(pos);
        Kind kind = kindOf(level, pos, start);
        if (kind == null) {
            return false;
        }
        if (level.isClientSide() || !(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) {
            return true;
        }
        int range = switch (kind) {
            case PLANT, ANY -> reach.plant;
            case CANE, CROP -> reach.crop;
            case LEAF -> reach.leaf;
        };
        int ry = kind == Kind.CROP ? Math.max(1, range / 3) : range;
        boolean ignoreState = player.isShiftKeyDown();
        int mined = 0;
        BUSY.set(true);
        try {
            for (BlockPos at : BlockPos.betweenClosed(pos.offset(-range, -ry, -range), pos.offset(range, ry, range))) {
                BlockState state = server.getBlockState(at);
                if (!matches(kind, server, pos, start, at, state, ignoreState)) {
                    continue;
                }
                BlockPos p = at.immutable();
                BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(server, p, state, sp);
                if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
                    continue;
                }
                harvest(server, sp, stack, kind, p, state);
                mined++;
            }
        } finally {
            BUSY.set(false);
        }
        if (breakable && mined > 0) {
            int damage = switch (kind) {
                case CROP, CANE -> Math.max(1, mined / 2);
                case LEAF -> Math.max(1, mined / 12);
                default -> 1;
            };
            stack.hurtAndBreak(damage, sp, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    private void harvest(ServerLevel level, ServerPlayer player, ItemStack tool, Kind kind, BlockPos pos, BlockState state) {
        List<ItemStack> drops = new ArrayList<>();
        Block block = state.getBlock();
        if (shears && (kind == Kind.PLANT || kind == Kind.ANY) && block instanceof IShearable shearable && shearable.isShearable(player, tool, level, pos)) {
            drops.addAll(shearable.onSheared(player, tool, level, pos));
        } else {
            drops.addAll(Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, tool));
        }
        level.playSound(null, pos, state.getSoundType().getBreakSound(), net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1F);
        if (kind == Kind.CROP) {
            removeOneSeed(level, pos, state, drops);
            replant(level, pos, state);
        } else {
            level.destroyBlock(pos, false, player);
        }
        for (ItemStack drop : drops) {
            Block.popResource(level, pos, drop);
        }
    }

    private static void removeOneSeed(ServerLevel level, BlockPos pos, BlockState state, List<ItemStack> drops) {
        ItemStack seed = state.getBlock().getCloneItemStack(level, pos, state);
        for (ItemStack drop : drops) {
            if (!drop.isEmpty() && ItemStack.isSameItem(drop, seed)) {
                drop.shrink(1);
                break;
            }
        }
        drops.removeIf(ItemStack::isEmpty);
    }

    private static void replant(ServerLevel level, BlockPos pos, BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty age && age.getName().equals("age")) {
                level.setBlock(pos, state.setValue(age, 0), Block.UPDATE_ALL);
                return;
            }
        }
        level.destroyBlock(pos, false);
    }

    /** Hitting something hurts the others of its kind beside it as well. */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (target instanceof LivingEntity hit && player.level() instanceof ServerLevel server) {
            AABB box = target.getBoundingBox().inflate(2, 0, 2).expandTowards(0, 1, 0);
            double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            int others = 0;
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, box)) {
                if (e != target && e != player && e.getType().getCategory() == hit.getType().getCategory()) {
                    e.hurt(player.damageSources().playerAttack(player), (float) damage);
                    others++;
                }
            }
            if (breakable && others > 0 && player instanceof ServerPlayer sp) {
                stack.hurtAndBreak(Math.min(10 * others, 40), sp, EquipmentSlot.MAINHAND);
            }
        }
        return false;
    }
}
