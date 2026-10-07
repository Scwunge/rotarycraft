package net.scwunge.rotarycraft.tool;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.SimpleTier;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.config.RotaryConfig;

import java.util.List;

/** What the bedrock tools have in common: a tier that drops anything, never wears, and the enchantments they cannot lose. */
public final class BedrockTools {
    private BedrockTools() {}

    /** Blocks the tier's tools do not drop for: none. */
    public static final TagKey<Block> INCORRECT = TagKey.create(Registries.BLOCK, RotaryCraft.id("incorrect_for_bedrock_tool"));

    public static Tier tier(float speed) {
        return new SimpleTier(INCORRECT, 0, speed, 0F, 14, () -> Ingredient.EMPTY);
    }

    /** Attack damage in total (the hand's one included) and attack speed modifier. */
    public static Item.Properties properties(double damage, double speed) {
        return new Item.Properties().attributes(attributes(damage, speed)).component(net.minecraft.core.component.DataComponents.UNBREAKABLE,
                new net.minecraft.world.item.component.Unbreakable(true));
    }

    public static ItemAttributeModifiers attributes(double damage, double speed) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage - 1, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    /** Whether the bedrock gear can still be enchanted further: not while its enchantments are locked (the original's default). */
    public static int enchantability() {
        return RotaryConfig.get(RotaryConfig.LOCK_BEDROCK_ENCHANTS) ? 0 : 14;
    }

    public static Forced.Need need(ResourceKey<Enchantment> enchantment, int level) {
        return new Forced.Need(enchantment, level);
    }

    public static final List<Forced.Need> PICKAXE = List.of(need(Enchantments.SILK_TOUCH, 1), need(Enchantments.FORTUNE, 5));
    public static final List<Forced.Need> SWORD = List.of(need(Enchantments.SHARPNESS, 5), need(Enchantments.LOOTING, 5));
    public static final List<Forced.Need> SICKLE = List.of(need(Enchantments.FORTUNE, 5));
    public static final List<Forced.Need> HELMET = List.of(need(Enchantments.PROJECTILE_PROTECTION, 4), need(Enchantments.RESPIRATION, 3));
    public static final List<Forced.Need> CHESTPLATE = List.of(need(Enchantments.BLAST_PROTECTION, 4));
    public static final List<Forced.Need> LEGGINGS = List.of(need(Enchantments.FIRE_PROTECTION, 4));
    public static final List<Forced.Need> BOOTS = List.of(need(Enchantments.FEATHER_FALLING, 4));

    public static ResourceLocation id(String path) {
        return RotaryCraft.id(path);
    }
}
