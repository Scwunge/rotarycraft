package net.scwunge.rotarycraft.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.charged.HandheldCraftingItem;
import net.scwunge.rotarycraft.charged.MotionTrackerItem;
import net.scwunge.rotarycraft.charged.NightVisionGogglesItem;
import net.scwunge.rotarycraft.charged.RangeFinderItem;
import net.scwunge.rotarycraft.charged.StunGunItem;
import net.scwunge.rotarycraft.charged.TargetItem;
import net.scwunge.rotarycraft.charged.UltrasoundItem;

import java.util.EnumMap;
import java.util.List;

/** The original's charged tools and gadgets: the stun gun, range finder, ultrasound scanner, motion tracker, night vision goggles, handheld crafting grid and target designator. */
public final class GadgetRegistry {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, RotaryCraft.MOD_ID);

    /** Goggles protect from nothing: the material only gives the worn texture. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> GOGGLES = ARMOR_MATERIALS.register("goggles", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            defense.put(type, 0);
        }
        return new ArmorMaterial(defense, 0, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.EMPTY, List.of(new ArmorMaterial.Layer(RotaryCraft.id("night_vision"))), 0F, 0F);
    });

    public static final DeferredItem<StunGunItem> STUN_GUN = RotaryItems.add(RotaryItems.ITEMS.register("stun_gun", () -> new StunGunItem(new Item.Properties())));
    public static final DeferredItem<RangeFinderItem> RANGE_FINDER = RotaryItems.add(RotaryItems.ITEMS.register("range_finder", () -> new RangeFinderItem(new Item.Properties())));
    public static final DeferredItem<UltrasoundItem> ULTRASOUND = RotaryItems.add(RotaryItems.ITEMS.register("ultrasound", () -> new UltrasoundItem(new Item.Properties())));
    public static final DeferredItem<MotionTrackerItem> MOTION_TRACKER = RotaryItems.add(RotaryItems.ITEMS.register("motion_tracker", () -> new MotionTrackerItem(new Item.Properties())));
    public static final DeferredItem<NightVisionGogglesItem> NIGHT_VISION_GOGGLES = RotaryItems.add(RotaryItems.ITEMS.register("night_vision_goggles",
            () -> new NightVisionGogglesItem((Holder<ArmorMaterial>) GOGGLES)));
    public static final DeferredItem<HandheldCraftingItem> HANDHELD_CRAFTING = RotaryItems.add(RotaryItems.ITEMS.register("handheld_crafting", () -> new HandheldCraftingItem(new Item.Properties())));
    public static final DeferredItem<TargetItem> TARGET = RotaryItems.add(RotaryItems.ITEMS.register("target", () -> new TargetItem(new Item.Properties())));

    private GadgetRegistry() {}

    public static void init(IEventBus modBus) {
        ARMOR_MATERIALS.register(modBus);
    }
}
