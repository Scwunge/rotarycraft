package net.scwunge.rotarycraft.registry;

import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.scwunge.rotarycraft.RotaryCraft;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.scwunge.rotarycraft.logistics.AggregatorBlockEntity;
import net.scwunge.rotarycraft.logistics.BucketFillerBlockEntity;
import net.scwunge.rotarycraft.logistics.FillingStationBlockEntity;
import net.scwunge.rotarycraft.logistics.GrindstoneBlockEntity;
import net.scwunge.rotarycraft.logistics.HeaterBlockEntity;
import net.scwunge.rotarycraft.logistics.ItemRefresherBlockEntity;
import net.scwunge.rotarycraft.logistics.PurifierBlockEntity;
import net.scwunge.rotarycraft.logistics.SpillwayBlockEntity;
import net.scwunge.rotarycraft.logistics.WetterBlockEntity;
import net.scwunge.rotarycraft.recipe.WettingRecipe;
import net.scwunge.rotarycraft.logistics.IgniterBlockEntity;
import net.scwunge.rotarycraft.logistics.PlayerDetectorBlockEntity;
import net.scwunge.rotarycraft.logistics.SmokeDetectorBlockEntity;
import net.scwunge.rotarycraft.machine.LayoutMachineBlock;
import net.scwunge.rotarycraft.machine.LayoutMenu;
import net.scwunge.rotarycraft.machine.LayoutMenus;
import net.scwunge.rotarycraft.machine.SignalMachineBlock;

/**
 * The automation and processing machines: detectors, heaters, filters and the fluid and item handling machines. Blocks, block entities and items go into the
 * mod's shared registers; each machine has a switch in the machines config.
 */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class LogisticsRegistry {
    // ---- detectors (they give a redstone signal) ----
    public static final Machines.Entry<PlayerDetectorBlockEntity, LayoutMachineBlock> PLAYER_DETECTOR = Machines.register("player_detector", PlayerDetectorBlockEntity::new,
            type -> new SignalMachineBlock(RotaryBlocks.machineProps().noOcclusion(), type::get, PlayerDetectorBlockEntity::new));
    public static final Machines.Entry<SmokeDetectorBlockEntity, LayoutMachineBlock> SMOKE_DETECTOR = Machines.register("smoke_detector", SmokeDetectorBlockEntity::new,
            type -> new SignalMachineBlock(RotaryBlocks.machineProps().noOcclusion(), type::get, SmokeDetectorBlockEntity::new));
    public static final DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> SMOKE_DETECTOR_MENU = LayoutMenus.register(SmokeDetectorBlockEntity.LAYOUT);

    // ---- heat ----
    public static final Machines.Entry<HeaterBlockEntity, LayoutMachineBlock> HEATER = Machines.machine("heater", HeaterBlockEntity::new);
    public static final DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> HEATER_MENU = LayoutMenus.register(HeaterBlockEntity.LAYOUT);
    public static final Machines.Entry<IgniterBlockEntity, LayoutMachineBlock> IGNITER = Machines.machine("firestarter", IgniterBlockEntity::new);
    public static final DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> IGNITER_MENU = LayoutMenus.register(IgniterBlockEntity.LAYOUT);

    // ---- item and fluid handling ----
    public static final Machines.Entry<ItemRefresherBlockEntity, LayoutMachineBlock> ITEM_REFRESHER = Machines.machine("refresher", ItemRefresherBlockEntity::new);
    public static final Machines.Entry<WetterBlockEntity, LayoutMachineBlock> WETTER = Machines.machine("wetter", WetterBlockEntity::new);
    public static final DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> WETTER_MENU = LayoutMenus.register(WetterBlockEntity.LAYOUT);
    public static final Machines.Entry<AggregatorBlockEntity, LayoutMachineBlock> AGGREGATOR = Machines.machine("aggregator", AggregatorBlockEntity::new);
    public static final Machines.Entry<GrindstoneBlockEntity, LayoutMachineBlock> GRINDSTONE = Machines.register("grindstone", GrindstoneBlockEntity::new,
            type -> new LayoutMachineBlock(RotaryBlocks.machineProps().noOcclusion(), type::get, GrindstoneBlockEntity::new, true));
    public static final DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> GRINDSTONE_MENU = LayoutMenus.register(GrindstoneBlockEntity.LAYOUT);
    public static final Machines.Entry<PurifierBlockEntity, LayoutMachineBlock> PURIFIER = Machines.machine("purifier", PurifierBlockEntity::new);
    public static final DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> PURIFIER_MENU = LayoutMenus.register(PurifierBlockEntity.LAYOUT);
    public static final Machines.Entry<BucketFillerBlockEntity, LayoutMachineBlock> BUCKET_FILLER = Machines.machine("bucket_filler", BucketFillerBlockEntity::new);
    public static final DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> BUCKET_FILLER_MENU = LayoutMenus.register(BucketFillerBlockEntity.LAYOUT);
    public static final Machines.Entry<FillingStationBlockEntity, LayoutMachineBlock> FILLING_STATION = Machines.machine("filling_station", FillingStationBlockEntity::new);
    public static final DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> FILLING_STATION_MENU = LayoutMenus.register(FillingStationBlockEntity.LAYOUT);

    public static final Machines.Entry<SpillwayBlockEntity, LayoutMachineBlock> SPILLWAY = Machines.machine("spillway", SpillwayBlockEntity::new);

    // ---- recipes ----
    public static final DeferredHolder<RecipeType<?>, RecipeType<WettingRecipe>> WETTING = RotaryRecipes.TYPES.register("wetting", () -> RecipeType.simple(RotaryCraft.id("wetting")));
    public static final DeferredHolder<RecipeSerializer<?>, WettingRecipe.Serializer> WETTING_SERIALIZER = RotaryRecipes.SERIALIZERS.register("wetting", WettingRecipe.Serializer::new);

    private LogisticsRegistry() {}

    /** Loads the class, so its entries join the shared registers. */
    public static void init(IEventBus modBus) {
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SMOKE_DETECTOR.type().get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, HEATER.type().get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, IGNITER.type().get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SPILLWAY.type().get(), (be, side) -> be.fluidHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, WETTER.type().get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, WETTER.type().get(), (be, side) -> be.fluidHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, AGGREGATOR.type().get(), (be, side) -> be.fluidHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GRINDSTONE.type().get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GRINDSTONE.type().get(), (be, side) -> be.fluidHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PURIFIER.type().get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BUCKET_FILLER.type().get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BUCKET_FILLER.type().get(), (be, side) -> be.fluidHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FILLING_STATION.type().get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FILLING_STATION.type().get(), (be, side) -> be.fluidHandler(side));
    }
}
