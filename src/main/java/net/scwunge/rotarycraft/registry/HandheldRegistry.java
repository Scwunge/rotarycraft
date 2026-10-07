package net.scwunge.rotarycraft.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.handheld.FireballLauncherItem;
import net.scwunge.rotarycraft.handheld.FlamethrowerItem;
import net.scwunge.rotarycraft.handheld.GravelGunItem;
import net.scwunge.rotarycraft.handheld.HandPumpItem;
import net.scwunge.rotarycraft.handheld.MatchFilterItem;
import net.scwunge.rotarycraft.handheld.MatchFilterMenu;
import net.scwunge.rotarycraft.handheld.SpringPistonItem;
import net.scwunge.rotarycraft.handheld.TileSelectorItem;
import net.scwunge.rotarycraft.handheld.VacuumGunItem;

/** The original's handheld tools beside the charged gadgets: the gravel gun, vacuum gun, flamethrower, fireball launcher, hand pump, spring piston, tile selector and match filter. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class HandheldRegistry {
    // ---- what the items keep ----
    /** What a hand pump holds. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> PUMP_CONTENTS = RotaryComponents.COMPONENTS.register("pump_contents",
            () -> DataComponentType.<SimpleFluidContent>builder().persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC).build());
    /** Whether a hand pump is in place mode (otherwise drain mode). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> PUMP_PLACING = RotaryComponents.COMPONENTS.register("pump_placing",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
    /** The machine a tile selector is linked to. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> TILE_LINK = RotaryComponents.COMPONENTS.register("tile_link",
            () -> DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());
    /** The item a match filter holds. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> MATCH_TEMPLATE = RotaryComponents.COMPONENTS.register("match_template",
            () -> DataComponentType.<ItemContainerContents>builder().persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).build());

    public static final ResourceKey<DamageType> GRAVEL_GUN_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, RotaryCraft.id("gravel_gun"));

    // ---- the items ----
    public static final DeferredItem<GravelGunItem> GRAVEL_GUN = RotaryItems.add(RotaryItems.ITEMS.register("gravel_gun", () -> new GravelGunItem(new Item.Properties())));
    public static final DeferredItem<VacuumGunItem> VACUUM_GUN = RotaryItems.add(RotaryItems.ITEMS.register("vacuum_gun", () -> new VacuumGunItem(new Item.Properties())));
    public static final DeferredItem<FlamethrowerItem> FLAMETHROWER = RotaryItems.add(RotaryItems.ITEMS.register("flamethrower", () -> new FlamethrowerItem(new Item.Properties())));
    public static final DeferredItem<FireballLauncherItem> FIREBALL_LAUNCHER = RotaryItems.add(RotaryItems.ITEMS.register("fireball_launcher",
            () -> new FireballLauncherItem(new Item.Properties())));
    public static final DeferredItem<HandPumpItem> HAND_PUMP = RotaryItems.add(RotaryItems.ITEMS.register("hand_pump", () -> new HandPumpItem(new Item.Properties())));
    public static final DeferredItem<SpringPistonItem> SPRING_PISTON = RotaryItems.add(RotaryItems.ITEMS.register("spring_piston", () -> new SpringPistonItem(new Item.Properties())));
    public static final DeferredItem<TileSelectorItem> TILE_SELECTOR = RotaryItems.add(RotaryItems.ITEMS.register("tile_selector", () -> new TileSelectorItem(new Item.Properties())));
    public static final DeferredItem<MatchFilterItem> MATCH_FILTER = RotaryItems.add(RotaryItems.ITEMS.register("match_filter", () -> new MatchFilterItem(new Item.Properties())));

    public static final DeferredHolder<MenuType<?>, MenuType<MatchFilterMenu>> MATCH_FILTER_MENU = RotaryMenus.MENUS.register("match_filter",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new MatchFilterMenu(id, inventory, buf.readEnum(InteractionHand.class))));

    private HandheldRegistry() {}

    /** Called from the mod's constructor so that the items above are registered. */
    public static void init(IEventBus modBus) {
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new FluidHandlerItemStack(PUMP_CONTENTS, stack, HandPumpItem.CAPACITY), HAND_PUMP.get());
    }
}
