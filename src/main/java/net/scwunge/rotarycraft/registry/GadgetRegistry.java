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
import net.scwunge.rotarycraft.charged.BedrockJumpBootsItem;
import net.scwunge.rotarycraft.charged.FuelTankItem;
import net.scwunge.rotarycraft.charged.HandheldCraftingItem;
import net.scwunge.rotarycraft.charged.BedrockJetpackItem;
import net.scwunge.rotarycraft.charged.IoGogglesItem;
import net.scwunge.rotarycraft.charged.Jetpack;
import net.scwunge.rotarycraft.charged.JetpackItem;
import net.scwunge.rotarycraft.charged.JumpBootsItem;
import net.scwunge.rotarycraft.charged.MotionTrackerItem;
import net.scwunge.rotarycraft.charged.NightVisionGogglesItem;
import net.scwunge.rotarycraft.charged.RangeFinderItem;
import net.scwunge.rotarycraft.charged.StunGunItem;
import net.scwunge.rotarycraft.charged.TargetItem;
import net.scwunge.rotarycraft.charged.UltrasoundItem;
import net.scwunge.rotarycraft.vehicle.EthanolMinecartItem;
import net.scwunge.rotarycraft.vehicle.GasMinecart;
import net.scwunge.rotarycraft.weapon.ExplosiveShellItem;

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

    /** The IO goggles' worn texture. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> IO = ARMOR_MATERIALS.register("io", () -> unprotective("io_goggles"));

    /** The jump boots wear steel's texture and protect from nothing. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> JUMP = ARMOR_MATERIALS.register("jump", () -> unprotective("steel"));

    /** The jetpack chestplates: the plain pack protects from nothing, the steel and bedrock ones like their chestplates. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> PACK = ARMOR_MATERIALS.register("pack", () -> unprotective("jet"));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> STEEL_PACK = ARMOR_MATERIALS.register("steel_pack", () -> chestplate(7, 9, 0F, 0F, "jet"));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BEDROCK_PACK = ARMOR_MATERIALS.register("bedrock_pack", () -> chestplate(12, 18, 0F, 0.25F, "bedrock_jet"));

    public static final DeferredItem<JetpackItem> JETPACK = RotaryItems.add(RotaryItems.ITEMS.register("jetpack",
            () -> new JetpackItem((Holder<ArmorMaterial>) PACK, Jetpack.Kind.PLAIN, new Item.Properties())));
    public static final DeferredItem<JetpackItem> STEEL_JETPACK = RotaryItems.add(RotaryItems.ITEMS.register("steel_jetpack",
            () -> new JetpackItem((Holder<ArmorMaterial>) STEEL_PACK, Jetpack.Kind.STEEL,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(ToolRegistry.STEEL_ARMOR_DURABILITY)))));
    public static final DeferredItem<BedrockJetpackItem> BEDROCK_JETPACK = RotaryItems.add(RotaryItems.ITEMS.register("bedrock_jetpack",
            () -> new BedrockJetpackItem((Holder<ArmorMaterial>) BEDROCK_PACK)));

    public static final DeferredItem<FuelTankItem> FUEL_TANK = RotaryItems.add(RotaryItems.ITEMS.register("fuel_tank", () -> new FuelTankItem(new Item.Properties())));
    public static final DeferredItem<ExplosiveShellItem> EXPLOSIVE_SHELL = RotaryItems.add(RotaryItems.ITEMS.register("explosive_shell",
            () -> new ExplosiveShellItem(new Item.Properties())));

    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, RotaryCraft.MOD_ID);
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<GasMinecart>> ETHANOL_MINECART_ENTITY = ENTITIES.register("ethanol_minecart",
            () -> net.minecraft.world.entity.EntityType.Builder.<GasMinecart>of(GasMinecart::new, net.minecraft.world.entity.MobCategory.MISC).sized(0.98F, 0.7F)
                    .clientTrackingRange(8).build("ethanol_minecart"));
    public static final DeferredItem<EthanolMinecartItem> ETHANOL_MINECART = RotaryItems.add(RotaryItems.ITEMS.register("ethanol_minecart",
            () -> new EthanolMinecartItem(new Item.Properties())));

    public static final DeferredItem<IoGogglesItem> IO_GOGGLES = RotaryItems.add(RotaryItems.ITEMS.register("io_goggles", () -> new IoGogglesItem((Holder<ArmorMaterial>) IO)));
    public static final DeferredItem<JumpBootsItem> JUMP_BOOTS = RotaryItems.add(RotaryItems.ITEMS.register("jump_boots", () -> new JumpBootsItem((Holder<ArmorMaterial>) JUMP)));
    public static final DeferredItem<BedrockJumpBootsItem> BEDROCK_JUMP_BOOTS = RotaryItems.add(RotaryItems.ITEMS.register("bedrock_jump_boots",
            () -> new BedrockJumpBootsItem((Holder<ArmorMaterial>) ToolRegistry.BEDROCK_ARMOR)));

    public static final DeferredItem<StunGunItem> STUN_GUN = RotaryItems.add(RotaryItems.ITEMS.register("stun_gun", () -> new StunGunItem(new Item.Properties())));
    public static final DeferredItem<RangeFinderItem> RANGE_FINDER = RotaryItems.add(RotaryItems.ITEMS.register("range_finder", () -> new RangeFinderItem(new Item.Properties())));
    public static final DeferredItem<UltrasoundItem> ULTRASOUND = RotaryItems.add(RotaryItems.ITEMS.register("ultrasound", () -> new UltrasoundItem(new Item.Properties())));
    public static final DeferredItem<MotionTrackerItem> MOTION_TRACKER = RotaryItems.add(RotaryItems.ITEMS.register("motion_tracker", () -> new MotionTrackerItem(new Item.Properties())));
    public static final DeferredItem<NightVisionGogglesItem> NIGHT_VISION_GOGGLES = RotaryItems.add(RotaryItems.ITEMS.register("night_vision_goggles",
            () -> new NightVisionGogglesItem((Holder<ArmorMaterial>) GOGGLES)));
    public static final DeferredItem<HandheldCraftingItem> HANDHELD_CRAFTING = RotaryItems.add(RotaryItems.ITEMS.register("handheld_crafting", () -> new HandheldCraftingItem(new Item.Properties())));
    public static final DeferredItem<TargetItem> TARGET = RotaryItems.add(RotaryItems.ITEMS.register("target", () -> new TargetItem(new Item.Properties())));

    private static ArmorMaterial unprotective(String layer) {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            defense.put(type, 0);
        }
        return new ArmorMaterial(defense, 0, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.EMPTY, List.of(new ArmorMaterial.Layer(RotaryCraft.id(layer))), 0F, 0F);
    }

    private static ArmorMaterial chestplate(int defense, int enchantability, float toughness, float knockback, String layer) {
        EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            map.put(type, type == ArmorItem.Type.CHESTPLATE || type == ArmorItem.Type.BODY ? defense : 0);
        }
        return new ArmorMaterial(map, enchantability, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.EMPTY, List.of(new ArmorMaterial.Layer(RotaryCraft.id(layer))), toughness, knockback);
    }

    private GadgetRegistry() {}

    public static void init(IEventBus modBus) {
        ARMOR_MATERIALS.register(modBus);
        ENTITIES.register(modBus);
    }
}
