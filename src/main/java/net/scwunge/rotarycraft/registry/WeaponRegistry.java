package net.scwunge.rotarycraft.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.weapon.AmmoMenu;
import net.scwunge.rotarycraft.weapon.CannonKeyItem;
import net.scwunge.rotarycraft.weapon.FlakShot;
import net.scwunge.rotarycraft.weapon.FlameShot;
import net.scwunge.rotarycraft.weapon.GatlingShot;
import net.scwunge.rotarycraft.weapon.turret.FlameTurretBlockEntity;
import net.scwunge.rotarycraft.weapon.turret.LaserGunBlockEntity;
import net.scwunge.rotarycraft.weapon.turret.GatlingBlockEntity;
import net.scwunge.rotarycraft.weapon.turret.TntCannonBlockEntity;
import net.scwunge.rotarycraft.weapon.FreezeEffect;
import net.scwunge.rotarycraft.weapon.FreezeShot;
import net.scwunge.rotarycraft.weapon.turret.AntiAirBlockEntity;
import net.scwunge.rotarycraft.weapon.turret.FreezeGunBlockEntity;
import net.scwunge.rotarycraft.weapon.RailgunAmmoItem;
import net.scwunge.rotarycraft.weapon.RailgunShot;
import net.scwunge.rotarycraft.weapon.turret.RailGunBlockEntity;
import net.scwunge.rotarycraft.weapon.turret.TurretBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * The weapons and defence machines (and the utility machines that come with them): blocks, block entities and items go into the
 * mod's shared registers; their entities and item components have registers of their own.
 */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class WeaponRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, RotaryCraft.MOD_ID);
    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, RotaryCraft.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.effect.MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, RotaryCraft.MOD_ID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, RotaryCraft.MOD_ID);

    public static final ResourceKey<DamageType> TURRET_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, RotaryCraft.id("turret"));

    /** Whose Cannon Key this is (a player name, as in the original). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> KEY_OWNER = COMPONENTS.register("key_owner",
            () -> DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

    public static final DeferredHolder<net.minecraft.world.effect.MobEffect, FreezeEffect> FREEZE = EFFECTS.register("frozen_solid", FreezeEffect::new);

    public static final DeferredHolder<MenuType<?>, MenuType<AmmoMenu>> AMMO_MENU = RotaryMenus.MENUS.register("ammo",
            () -> IMenuTypeExtension.create(AmmoMenu::fromNetwork));

    // ---- Rail Gun ----
    public static final DeferredBlock<TurretBlock> RAILGUN = RotaryBlocks.BLOCKS.register("railgun",
            () -> new TurretBlock(RotaryBlocks.machineProps().noOcclusion(), WeaponRegistry.RAILGUN_BE, RailGunBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RailGunBlockEntity>> RAILGUN_BE = RotaryBlockEntities.TYPES.register("railgun",
            () -> BlockEntityType.Builder.of(RailGunBlockEntity::new, RAILGUN.get()).build(null));
    public static final DeferredHolder<EntityType<?>, EntityType<RailgunShot>> RAILGUN_SHOT = ENTITIES.register("railgun_shot",
            () -> EntityType.Builder.<RailgunShot>of(RailgunShot::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(16).updateInterval(1)
                    .noSave().fireImmune().build("railgun_shot"));
    public static final List<DeferredItem<RailgunAmmoItem>> RAILGUN_AMMO = new ArrayList<>();

    // ---- Freeze Gun and Anti-Air ----
    public static final DeferredBlock<TurretBlock> FREEZE_GUN = RotaryBlocks.BLOCKS.register("freeze_gun",
            () -> new TurretBlock(RotaryBlocks.machineProps().noOcclusion(), WeaponRegistry.FREEZE_GUN_BE, FreezeGunBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FreezeGunBlockEntity>> FREEZE_GUN_BE = RotaryBlockEntities.TYPES.register("freeze_gun",
            () -> BlockEntityType.Builder.of(FreezeGunBlockEntity::new, FREEZE_GUN.get()).build(null));
    public static final DeferredBlock<TurretBlock> ANTI_AIR = RotaryBlocks.BLOCKS.register("anti_air",
            () -> new TurretBlock(RotaryBlocks.machineProps().noOcclusion(), WeaponRegistry.ANTI_AIR_BE, AntiAirBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AntiAirBlockEntity>> ANTI_AIR_BE = RotaryBlockEntities.TYPES.register("anti_air",
            () -> BlockEntityType.Builder.of(AntiAirBlockEntity::new, ANTI_AIR.get()).build(null));
    public static final DeferredHolder<EntityType<?>, EntityType<FreezeShot>> FREEZE_SHOT = ENTITIES.register("freeze_shot",
            () -> EntityType.Builder.<FreezeShot>of(FreezeShot::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(16).updateInterval(1)
                    .noSave().fireImmune().build("freeze_shot"));
    public static final DeferredHolder<EntityType<?>, EntityType<FlakShot>> FLAK_SHOT = ENTITIES.register("flak_shot",
            () -> EntityType.Builder.<FlakShot>of(FlakShot::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(16).updateInterval(1)
                    .noSave().fireImmune().build("flak_shot"));

    // ---- Gatling Gun ----
    public static final DeferredBlock<TurretBlock> GATLING = RotaryBlocks.BLOCKS.register("gatling",
            () -> new TurretBlock(RotaryBlocks.machineProps().noOcclusion(), WeaponRegistry.GATLING_BE, GatlingBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GatlingBlockEntity>> GATLING_BE = RotaryBlockEntities.TYPES.register("gatling",
            () -> BlockEntityType.Builder.of(GatlingBlockEntity::new, GATLING.get()).build(null));
    public static final DeferredHolder<EntityType<?>, EntityType<GatlingShot>> GATLING_SHOT = ENTITIES.register("gatling_shot",
            () -> EntityType.Builder.<GatlingShot>of(GatlingShot::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(16).updateInterval(1)
                    .noSave().fireImmune().build("gatling_shot"));
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> GATLING_SOUND = sound("gatling");
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> GATLING_RELOAD_SOUND = sound("gatlingreload");

    // ---- Sonic Weapon ----
    public static final DeferredBlock<net.scwunge.rotarycraft.weapon.SonicBlock> SONIC = RotaryBlocks.BLOCKS.register("sonic_weapon",
            () -> new net.scwunge.rotarycraft.weapon.SonicBlock(RotaryBlocks.machineProps().noOcclusion(), WeaponRegistry.SONIC_BE));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.weapon.turret.SonicWeaponBlockEntity>> SONIC_BE =
            RotaryBlockEntities.TYPES.register("sonic_weapon",
                    () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.weapon.turret.SonicWeaponBlockEntity::new, SONIC.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<net.scwunge.rotarycraft.weapon.SonicMenu>> SONIC_MENU = RotaryMenus.MENUS.register("sonic_weapon",
            () -> IMenuTypeExtension.create(net.scwunge.rotarycraft.weapon.SonicMenu::fromNetwork));
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> SONIC_SOUND = sound("sonic");

    // ---- Heat Ray ----
    public static final DeferredBlock<net.scwunge.rotarycraft.weapon.OwnedMachineBlock> HEAT_RAY = RotaryBlocks.BLOCKS.register("heat_ray",
            () -> new net.scwunge.rotarycraft.weapon.OwnedMachineBlock(RotaryBlocks.machineProps().noOcclusion(), WeaponRegistry.HEAT_RAY_BE,
                    net.scwunge.rotarycraft.weapon.turret.HeatRayBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.weapon.turret.HeatRayBlockEntity>> HEAT_RAY_BE =
            RotaryBlockEntities.TYPES.register("heat_ray",
                    () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.weapon.turret.HeatRayBlockEntity::new, HEAT_RAY.get()).build(null));

    // ---- Coils, the Winder and the Landmine ----
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> COIL_CHARGE = COMPONENTS.register("coil_charge",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    public static final DeferredItem<net.scwunge.rotarycraft.item.CoilItem> SPRING = RotaryItems.add(RotaryItems.ITEMS.register("spring",
            () -> new net.scwunge.rotarycraft.item.CoilItem(new Item.Properties(), 1, 1, true)));
    public static final DeferredItem<net.scwunge.rotarycraft.item.CoilItem> STRONG_COIL = RotaryItems.add(RotaryItems.ITEMS.register("strong_coil",
            () -> new net.scwunge.rotarycraft.item.CoilItem(new Item.Properties(), 16, 4, false)));
    public static final DeferredBlock<net.scwunge.rotarycraft.block.MachineBlock> WINDER = RotaryBlocks.BLOCKS.register("winder",
            () -> new net.scwunge.rotarycraft.block.MachineBlock(RotaryBlocks.machineProps().noOcclusion(), WeaponRegistry.WINDER_BE,
                    net.scwunge.rotarycraft.blockentity.WinderBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.blockentity.WinderBlockEntity>> WINDER_BE =
            RotaryBlockEntities.TYPES.register("winder",
                    () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.blockentity.WinderBlockEntity::new, WINDER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<net.scwunge.rotarycraft.menu.OneSlotMenu>> WINDER_MENU = RotaryMenus.oneSlot("winder");
    public static final DeferredBlock<net.scwunge.rotarycraft.weapon.LandmineBlock> LANDMINE = RotaryBlocks.BLOCKS.register("landmine",
            () -> new net.scwunge.rotarycraft.weapon.LandmineBlock(RotaryBlocks.machineProps().noOcclusion()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.weapon.turret.LandmineBlockEntity>> LANDMINE_BE =
            RotaryBlockEntities.TYPES.register("landmine",
                    () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.weapon.turret.LandmineBlockEntity::new, LANDMINE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<net.scwunge.rotarycraft.weapon.LandmineMenu>> LANDMINE_MENU = RotaryMenus.MENUS.register("landmine",
            () -> IMenuTypeExtension.create(net.scwunge.rotarycraft.weapon.LandmineMenu::fromNetwork));

    // ---- EMP ----
    public static final DeferredBlock<net.scwunge.rotarycraft.weapon.OwnedMachineBlock> EMP = RotaryBlocks.BLOCKS.register("emp",
            () -> new net.scwunge.rotarycraft.weapon.OwnedMachineBlock(RotaryBlocks.machineProps().noOcclusion(), WeaponRegistry.EMP_BE,
                    net.scwunge.rotarycraft.weapon.turret.EmpBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.weapon.turret.EmpBlockEntity>> EMP_BE =
            RotaryBlockEntities.TYPES.register("emp",
                    () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.weapon.turret.EmpBlockEntity::new, EMP.get()).build(null));

    // ---- Laser Gun and Flame Turret ----
    public static final DeferredBlock<TurretBlock> LASER_GUN = RotaryBlocks.BLOCKS.register("laser_gun",
            () -> new TurretBlock(RotaryBlocks.machineProps().noOcclusion(), WeaponRegistry.LASER_GUN_BE, LaserGunBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LaserGunBlockEntity>> LASER_GUN_BE = RotaryBlockEntities.TYPES.register("laser_gun",
            () -> BlockEntityType.Builder.of(LaserGunBlockEntity::new, LASER_GUN.get()).build(null));
    public static final DeferredBlock<TurretBlock> FLAME_TURRET = RotaryBlocks.BLOCKS.register("flame_turret",
            () -> new TurretBlock(RotaryBlocks.machineProps().noOcclusion(), WeaponRegistry.FLAME_TURRET_BE, FlameTurretBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FlameTurretBlockEntity>> FLAME_TURRET_BE = RotaryBlockEntities.TYPES.register("flame_turret",
            () -> BlockEntityType.Builder.of(FlameTurretBlockEntity::new, FLAME_TURRET.get()).build(null));
    public static final DeferredHolder<EntityType<?>, EntityType<FlameShot>> FLAME_SHOT = ENTITIES.register("flame_shot",
            () -> EntityType.Builder.<FlameShot>of(FlameShot::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(16).updateInterval(1)
                    .noSave().fireImmune().build("flame_shot"));
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> FLAME_TURRET_SOUND = sound("flameturret");

    // ---- TNT Cannon ----
    public static final DeferredBlock<net.scwunge.rotarycraft.weapon.CannonBlock> TNT_CANNON = RotaryBlocks.BLOCKS.register("tnt_cannon",
            () -> new net.scwunge.rotarycraft.weapon.CannonBlock(RotaryBlocks.machineProps().noOcclusion(), WeaponRegistry.TNT_CANNON_BE, TntCannonBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TntCannonBlockEntity>> TNT_CANNON_BE = RotaryBlockEntities.TYPES.register("tnt_cannon",
            () -> BlockEntityType.Builder.of(TntCannonBlockEntity::new, TNT_CANNON.get()).build(null));
    public static final DeferredHolder<EntityType<?>, EntityType<net.scwunge.rotarycraft.weapon.CannonTnt>> CANNON_TNT = ENTITIES.register("cannon_tnt",
            () -> EntityType.Builder.<net.scwunge.rotarycraft.weapon.CannonTnt>of(net.scwunge.rotarycraft.weapon.CannonTnt::new, MobCategory.MISC).fireImmune()
                    .sized(0.98f, 0.98f).eyeHeight(0.15f).clientTrackingRange(10).updateInterval(10).build("cannon_tnt"));
    public static final DeferredHolder<MenuType<?>, MenuType<net.scwunge.rotarycraft.weapon.CannonMenu>> CANNON_MENU = RotaryMenus.MENUS.register("tnt_cannon",
            () -> IMenuTypeExtension.create(net.scwunge.rotarycraft.weapon.CannonMenu::fromNetwork));

    // ---- Force Field and Containment ----
    public static final DeferredHolder<MenuType<?>, MenuType<net.scwunge.rotarycraft.weapon.RangeMenu>> RANGE_MENU = RotaryMenus.MENUS.register("range",
            () -> IMenuTypeExtension.create(net.scwunge.rotarycraft.weapon.RangeMenu::fromNetwork));
    public static final DeferredBlock<net.scwunge.rotarycraft.weapon.OwnedMachineBlock> FORCE_FIELD = RotaryBlocks.BLOCKS.register("force_field",
            () -> new net.scwunge.rotarycraft.weapon.OwnedMachineBlock(RotaryBlocks.machineProps().noOcclusion(), WeaponRegistry.FORCE_FIELD_BE,
                    net.scwunge.rotarycraft.weapon.turret.ForceFieldBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.weapon.turret.ForceFieldBlockEntity>> FORCE_FIELD_BE =
            RotaryBlockEntities.TYPES.register("force_field",
                    () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.weapon.turret.ForceFieldBlockEntity::new, FORCE_FIELD.get()).build(null));
    public static final DeferredBlock<net.scwunge.rotarycraft.weapon.OwnedMachineBlock> CONTAINMENT = RotaryBlocks.BLOCKS.register("containment",
            () -> new net.scwunge.rotarycraft.weapon.OwnedMachineBlock(RotaryBlocks.machineProps().noOcclusion(), WeaponRegistry.CONTAINMENT_BE,
                    net.scwunge.rotarycraft.weapon.turret.ContainmentBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.weapon.turret.ContainmentBlockEntity>> CONTAINMENT_BE =
            RotaryBlockEntities.TYPES.register("containment",
                    () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.weapon.turret.ContainmentBlockEntity::new, CONTAINMENT.get()).build(null));

    /** Turret parts (the original's barrel, lens, bulb, rail head, turret base and aiming unit). */
    public static final java.util.Map<String, DeferredItem<Item>> PARTS = new java.util.LinkedHashMap<>();

    static {
        for (String id : new String[] {"barrel", "lens", "bulb", "rail_head", "rail_base", "rail_aiming_unit"}) {
            PARTS.put(id, RotaryItems.add(RotaryItems.ITEMS.registerSimpleItem(id)));
        }
    }

    public static final DeferredItem<CannonKeyItem> CANNON_KEY = RotaryItems.add(RotaryItems.ITEMS.register("cannon_key",
            () -> new CannonKeyItem(new Item.Properties().stacksTo(1))));

    static {
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(RAILGUN));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(FREEZE_GUN));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(ANTI_AIR));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(GATLING));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(TNT_CANNON));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(SONIC));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(HEAT_RAY));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(EMP));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(WINDER));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(LANDMINE));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(LASER_GUN));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(FLAME_TURRET));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(FORCE_FIELD));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(CONTAINMENT));
        for (int i = 0; i < 16; i++) {
            int tier = i;
            RAILGUN_AMMO.add(RotaryItems.add(RotaryItems.ITEMS.register("railgun_ammo_" + i, () -> new RailgunAmmoItem(new Item.Properties(), tier))));
        }
    }

    private static DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(RotaryCraft.id(name)));
    }

    private WeaponRegistry() {}

    /** Loads the class (so its entries join the shared registers) and registers its own registers. */
    public static void init(IEventBus modBus) {
        ENTITIES.register(modBus);
        EFFECTS.register(modBus);
        SOUNDS.register(modBus);
        COMPONENTS.register(modBus);
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RAILGUN_BE.get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FREEZE_GUN_BE.get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ANTI_AIR_BE.get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GATLING_BE.get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TNT_CANNON_BE.get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LANDMINE_BE.get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, WINDER_BE.get(), (be, side) -> be.items());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FLAME_TURRET_BE.get(), (be, side) -> be.intake());
    }
}
