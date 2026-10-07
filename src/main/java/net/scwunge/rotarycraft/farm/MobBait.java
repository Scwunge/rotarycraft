package net.scwunge.rotarycraft.farm;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/** What each kind of creature is drawn to and driven off by in a Bait Box, from the original's table. */
public enum MobBait {
    CREEPER(EntityType.CREEPER, Items.TNT, Items.OBSIDIAN),
    ENDERMAN(EntityType.ENDERMAN, Items.GRASS_BLOCK, Items.SOUL_SAND),
    ZOMBIE(EntityType.ZOMBIE, s -> s.is(Items.GOLDEN_APPLE) || isHealingPotion(s), s -> s.is(Items.GOLDEN_APPLE)),
    SKELETON(EntityType.SKELETON, Items.SADDLE, Items.GLOWSTONE),
    SPIDER(EntityType.SPIDER, Items.COCOA_BEANS, Items.OAK_FENCE),
    SLIME(EntityType.SLIME, Items.DIAMOND, Items.ICE),
    BLAZE(EntityType.BLAZE, Items.FIRE_CHARGE, Items.WATER_BUCKET),
    ZOMBIFIED_PIGLIN(EntityType.ZOMBIFIED_PIGLIN, Items.APPLE, Items.BLAZE_ROD),
    MAGMA_CUBE(EntityType.MAGMA_CUBE, Items.BLAZE_POWDER, Items.SNOWBALL),
    GHAST(EntityType.GHAST, Items.GLOWSTONE_DUST, Items.BOW),
    SILVERFISH(EntityType.SILVERFISH, Items.STONE_BRICKS, Items.WOODEN_PICKAXE),
    VILLAGER(EntityType.VILLAGER, Items.EMERALD, Items.CACTUS),
    IRON_GOLEM(EntityType.IRON_GOLEM, Items.RED_WOOL, Items.LAVA_BUCKET),
    WITCH(EntityType.WITCH, Items.NETHER_WART, Items.ENDER_PEARL),
    SNOW_GOLEM(EntityType.SNOW_GOLEM, Items.CARROT, Items.TORCH),
    BAT(EntityType.BAT, Items.MELON_SLICE, Items.NOTE_BLOCK),
    COW(EntityType.COW, Items.WHEAT, Items.STICK),
    PIG(EntityType.PIG, Items.CARROT, Items.STICK),
    SHEEP(EntityType.SHEEP, Items.WHEAT, Items.STICK),
    CHICKEN(EntityType.CHICKEN, Items.WHEAT_SEEDS, Items.STICK),
    OCELOT(EntityType.OCELOT, Items.COD, Items.STICK),
    WOLF(EntityType.WOLF, Items.PORKCHOP, Items.GRAVEL),
    SQUID(EntityType.SQUID, Items.GOLD_INGOT, Items.LILY_PAD);

    private final EntityType<?> type;
    private final Predicate<ItemStack> attractor;
    private final Predicate<ItemStack> repellent;

    MobBait(EntityType<?> type, Item attractor, Item repellent) {
        this(type, s -> s.is(attractor), s -> s.is(repellent));
    }

    MobBait(EntityType<?> type, Predicate<ItemStack> attractor, Predicate<ItemStack> repellent) {
        this.type = type;
        this.attractor = attractor;
        this.repellent = repellent;
    }

    private static boolean isHealingPotion(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && (contents.is(Potions.HEALING) || contents.is(Potions.STRONG_HEALING));
    }

    @Nullable
    public static MobBait of(LivingEntity entity) {
        for (MobBait bait : values()) {
            if (bait.type == entity.getType()) {
                return bait;
            }
        }
        return null;
    }

    private static boolean has(IItemHandler inventory, Predicate<ItemStack> test) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && test.test(stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean attracts(LivingEntity entity, IItemHandler inventory) {
        MobBait bait = of(entity);
        return bait != null && has(inventory, bait.attractor);
    }

    public static boolean repels(LivingEntity entity, IItemHandler inventory) {
        MobBait bait = of(entity);
        return bait != null && has(inventory, bait.repellent);
    }

    /** Whether the item is any creature's bait. */
    public static boolean isBait(ItemStack stack) {
        for (MobBait bait : values()) {
            if (bait.attractor.test(stack) || bait.repellent.test(stack)) {
                return true;
            }
        }
        return false;
    }
}
