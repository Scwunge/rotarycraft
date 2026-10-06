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
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, RotaryCraft.MOD_ID);

    public static final ResourceKey<DamageType> RAILGUN_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, RotaryCraft.id("railgun"));

    /** Whose Cannon Key this is (a player name, as in the original). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> KEY_OWNER = COMPONENTS.register("key_owner",
            () -> DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

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
        for (int i = 0; i < 16; i++) {
            int tier = i;
            RAILGUN_AMMO.add(RotaryItems.add(RotaryItems.ITEMS.register("railgun_ammo_" + i, () -> new RailgunAmmoItem(new Item.Properties(), tier))));
        }
    }

    private WeaponRegistry() {}

    /** Loads the class (so its entries join the shared registers) and registers its own registers. */
    public static void init(IEventBus modBus) {
        ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RAILGUN_BE.get(), (be, side) -> be.automationItems());
    }
}
