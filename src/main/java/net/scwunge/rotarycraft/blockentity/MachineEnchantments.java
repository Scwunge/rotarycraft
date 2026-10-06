package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** The enchantments a machine has been given from enchanted books (as the original's machine enchantments), limited to the ones it uses. */
public final class MachineEnchantments {
    private final Set<ResourceKey<Enchantment>> allowed = new LinkedHashSet<>();
    private final Map<ResourceKey<Enchantment>, Integer> levels = new LinkedHashMap<>();

    @SafeVarargs
    public MachineEnchantments(ResourceKey<Enchantment>... allowed) {
        Collections.addAll(this.allowed, allowed);
    }

    public int level(ResourceKey<Enchantment> enchantment) {
        return levels.getOrDefault(enchantment, 0);
    }

    public boolean has(ResourceKey<Enchantment> enchantment) {
        return level(enchantment) > 0;
    }

    public boolean any() {
        return !levels.isEmpty();
    }

    public Map<ResourceKey<Enchantment>, Integer> all() {
        return Collections.unmodifiableMap(levels);
    }

    public boolean set(ResourceKey<Enchantment> enchantment, int level) {
        if (!allowed.contains(enchantment)) {
            return false;
        }
        levels.put(enchantment, level);
        return true;
    }

    /** Takes the better of each allowed enchantment on an enchanted book; whether anything was gained. */
    public boolean apply(ItemStack book) {
        boolean gained = false;
        ItemEnchantments stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var entry : stored.entrySet()) {
            ResourceKey<Enchantment> key = entry.getKey().unwrapKey().orElse(null);
            int level = entry.getIntValue();
            if (key != null && level > level(key)) {
                gained |= set(key, level);
            }
        }
        return gained;
    }

    /** A tool carrying these enchantments, for working out what a block drops (silk touch, fortune). */
    public ItemStack tool(HolderLookup.Provider registries) {
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        var lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        EnchantmentHelper.updateEnchantments(tool, mutable -> levels.forEach((key, level) -> lookup.get(key).ifPresent(holder -> mutable.set(holder, level))));
        return tool;
    }

    public ListTag save() {
        ListTag list = new ListTag();
        levels.forEach((key, level) -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", key.location().toString());
            tag.putInt("lvl", level);
            list.add(tag);
        });
        return list;
    }

    public void load(ListTag list) {
        levels.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
            if (id != null) {
                ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, id);
                if (allowed.contains(key)) {
                    levels.put(key, tag.getInt("lvl"));
                }
            }
        }
    }

    public static ListTag listTag(CompoundTag parent, String name) {
        return parent.getList(name, Tag.TAG_COMPOUND);
    }
}
