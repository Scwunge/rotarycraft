package net.scwunge.rotarycraft.tool;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The bedrock tools and armour carry enchantments that cannot be taken off: strip one (a grindstone, an anvil) and the item breaks, hurting whoever holds it,
 * and one on the ground vanishes. Items that have them implement this; their stacks come enchanted from the creative tab and from their recipes.
 */
public interface Forced {
    /** An enchantment that must stay on at no less than this level. */
    record Need(ResourceKey<Enchantment> enchantment, int level) {
    }

    List<Need> needs();

    static boolean intact(ItemStack stack, HolderLookup.Provider registries, List<Need> needs) {
        var lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        for (Need need : needs) {
            if (stack.getEnchantmentLevel(lookup.getOrThrow(need.enchantment())) < need.level()) {
                return false;
            }
        }
        return true;
    }

    /** The item as it is made: with its enchantments on. */
    static ItemStack stackOf(Item item, HolderLookup.Provider registries, List<Need> needs) {
        ItemStack stack = new ItemStack(item);
        var lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        for (Need need : needs) {
            stack.enchant(lookup.getOrThrow(need.enchantment()), need.level());
        }
        return stack;
    }

    /** What an inventory does to the item each tick: it breaks if its enchantments have been taken off. */
    static void tick(ItemStack stack, Level level, Entity holder, List<Need> needs) {
        if (level.isClientSide() || intact(stack, level.registryAccess(), needs)) {
            return;
        }
        level.playSound(null, holder.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1F, 1F);
        if (holder instanceof Player player) {
            player.hurt(level.damageSources().source(DamageTypes.GENERIC), 10);
            player.sendSystemMessage(Component.translatable("message.rotarycraft.bedrock_broke"));
        }
        stack.setCount(0);
    }
}
