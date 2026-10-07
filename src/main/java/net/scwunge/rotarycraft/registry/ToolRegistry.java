package net.scwunge.rotarycraft.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.tool.SickleItem;
import net.scwunge.rotarycraft.tool.SteelPickaxeItem;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The HSLA steel tools and armour, as the original's: iron in everything but how long they last (600 uses, armour 24 times the base) and a fifth more
 * speed for the pickaxe, axe and shovel (and a point more damage). Repaired with steel ingots (the c:ingots/steel tag, so other mods' steel works).
 */
public final class ToolRegistry {
    public static final TagKey<Item> STEEL_INGOTS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/steel"));

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, RotaryCraft.MOD_ID);

    /** Iron with a fifth more speed and a point more damage, wearing out after 600 uses. */
    public static final Tier STEEL = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 600, 7.2F, 3.0F, 14, () -> Ingredient.of(STEEL_INGOTS));
    /** Plain iron that wears out after 600 uses (the hoe, sword and shears). */
    public static final Tier STEEL_PLAIN = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 600, 6.0F, 2.0F, 14, () -> Ingredient.of(STEEL_INGOTS));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> STEEL_ARMOR = ARMOR_MATERIALS.register("steel", () -> {
        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.CHESTPLATE, 7);
        defense.put(ArmorItem.Type.LEGGINGS, 5);
        defense.put(ArmorItem.Type.BOOTS, 3);
        defense.put(ArmorItem.Type.BODY, 7);
        return new ArmorMaterial(defense, 9, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(STEEL_INGOTS),
                List.of(new ArmorMaterial.Layer(RotaryCraft.id("steel"))), 0F, 0F);
    });

    /** Armour lasts the vanilla base for its slot times this. */
    public static final int STEEL_ARMOR_DURABILITY = 24;

    public static final DeferredItem<PickaxeItem> STEEL_PICKAXE = RotaryItems.add(RotaryItems.ITEMS.register("steel_pickaxe",
            () -> new SteelPickaxeItem(STEEL, new Item.Properties().attributes(PickaxeItem.createAttributes(STEEL, 1.0F, -2.8F)))));
    public static final DeferredItem<AxeItem> STEEL_AXE = RotaryItems.add(RotaryItems.ITEMS.register("steel_axe",
            () -> new AxeItem(STEEL, new Item.Properties().attributes(AxeItem.createAttributes(STEEL, 6.0F, -3.1F)))));
    public static final DeferredItem<ShovelItem> STEEL_SHOVEL = RotaryItems.add(RotaryItems.ITEMS.register("steel_shovel",
            () -> new ShovelItem(STEEL, new Item.Properties().attributes(ShovelItem.createAttributes(STEEL, 1.5F, -3.0F)))));
    public static final DeferredItem<HoeItem> STEEL_HOE = RotaryItems.add(RotaryItems.ITEMS.register("steel_hoe",
            () -> new HoeItem(STEEL_PLAIN, new Item.Properties().attributes(HoeItem.createAttributes(STEEL_PLAIN, -2.0F, -1.0F)))));
    public static final DeferredItem<SwordItem> STEEL_SWORD = RotaryItems.add(RotaryItems.ITEMS.register("steel_sword",
            () -> new SwordItem(STEEL_PLAIN, new Item.Properties().attributes(SwordItem.createAttributes(STEEL_PLAIN, 3, -2.4F)))));
    public static final DeferredItem<ShearsItem> STEEL_SHEARS = RotaryItems.add(RotaryItems.ITEMS.register("steel_shears",
            () -> new ShearsItem(new Item.Properties().durability(600).component(DataComponents.TOOL, ShearsItem.createToolProperties()))));
    public static final DeferredItem<SickleItem> STEEL_SICKLE = RotaryItems.add(RotaryItems.ITEMS.register("steel_sickle",
            () -> new SickleItem(new Item.Properties().durability(600), 4, new SickleItem.Reach(4, 4, 4), true, false)));

    public static final DeferredItem<ArmorItem> STEEL_HELMET = armor("steel_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> STEEL_CHESTPLATE = armor("steel_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> STEEL_LEGGINGS = armor("steel_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> STEEL_BOOTS = armor("steel_boots", ArmorItem.Type.BOOTS);

    private static DeferredItem<ArmorItem> armor(String name, ArmorItem.Type type) {
        return RotaryItems.add(RotaryItems.ITEMS.register(name, () -> new ArmorItem((Holder<ArmorMaterial>) STEEL_ARMOR, type,
                new Item.Properties().durability(type.getDurability(STEEL_ARMOR_DURABILITY)))));
    }

    private ToolRegistry() {}

    public static void init(IEventBus modBus) {
        ARMOR_MATERIALS.register(modBus);
    }
}
