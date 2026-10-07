package net.scwunge.rotarycraft.tool;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.scwunge.rotarycraft.RotaryCraft;

import java.util.List;

/**
 * The bedrock pickaxe: never wears, mines anything (obsidian at 48, other hard blocks at 12 or more), and is silk touch and fortune V for good. It also picks
 * up a mob spawner whole (see {@link ToolEvents}).
 */
public class BedrockPickaxeItem extends PickaxeItem implements Forced {
    public BedrockPickaxeItem() {
        super(BedrockTools.tier(12F), BedrockTools.properties(5, -2.8F));
    }

    @Override
    public List<Need> needs() {
        return BedrockTools.PICKAXE;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        Forced.tick(stack, level, entity, needs());
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide() && !Forced.intact(stack, entity.level().registryAccess(), needs())) {
            entity.discard();
        }
        return false;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return BedrockTools.enchantability();
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.OBSIDIAN || b == Blocks.CRYING_OBSIDIAN) {
            return 48F;
        }
        var key = BuiltInRegistries.BLOCK.getKey(b);
        if (key.getNamespace().equals(RotaryCraft.MOD_ID)) {
            String path = key.getPath();
            if (path.equals("blast_glass")) {
                return 48F;
            }
            if (path.equals("blast_pane") || path.startsWith("shaft") || path.startsWith("gearbox")) {
                return 32F;
            }
            return 12F;
        }
        if (b == Blocks.SPAWNER) {
            return 18F;
        }
        if (state.is(Blocks.LEVER) || state.is(BlockTags.BUTTONS) || state.is(BlockTags.PRESSURE_PLATES) || b == Blocks.IRON_DOOR) {
            return 18F;
        }
        if (b == Blocks.REDSTONE_LAMP) {
            return 10F;
        }
        if (b == Blocks.GLOWSTONE || b == Blocks.PISTON || b == Blocks.STICKY_PISTON || b == Blocks.BOOKSHELF) {
            return 8F;
        }
        if (b instanceof InfestedBlock) {
            return 6F;
        }
        if (state.is(Tags.Blocks.ORES)) {
            return 24F;
        }
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(BlockTags.ICE)) {
            return 12F;
        }
        return 1F;
    }
}
