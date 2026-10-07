package net.scwunge.rotarycraft.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.scwunge.rotarycraft.RotaryCraft;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.scwunge.rotarycraft.menu.DistributionClutchMenu;
import net.scwunge.rotarycraft.menu.MultiClutchMenu;
import net.scwunge.rotarycraft.menu.PowerBusMenu;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import net.scwunge.rotarycraft.transmission.BusControllerBlock;
import net.scwunge.rotarycraft.transmission.BusControllerBlockEntity;
import net.scwunge.rotarycraft.transmission.PowerBusBlock;
import net.scwunge.rotarycraft.transmission.PowerBusBlockEntity;
import net.scwunge.rotarycraft.transmission.DistributionClutchBlock;
import net.scwunge.rotarycraft.transmission.EngineControllerBlock;
import net.scwunge.rotarycraft.transmission.EngineControllerBlockEntity;
import net.scwunge.rotarycraft.transmission.DistributionClutchBlockEntity;
import net.scwunge.rotarycraft.transmission.MultiClutchBlock;
import net.scwunge.rotarycraft.transmission.MultiClutchBlockEntity;

/** The transmission pieces beyond shafts, gears and clutches (multi-clutch, distribution clutch, buses, belts and the like): blocks, block entities, menus and items go into the mod's shared registers. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class TransmissionRegistry {
    public static final DeferredBlock<MultiClutchBlock> MULTI_CLUTCH = RotaryBlocks.BLOCKS.register("multi_clutch",
            () -> new MultiClutchBlock(RotaryBlocks.machineProps().noOcclusion(), TransmissionRegistry.MULTI_CLUTCH_BE, MultiClutchBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MultiClutchBlockEntity>> MULTI_CLUTCH_BE =
            RotaryBlockEntities.TYPES.register("multi_clutch", () -> BlockEntityType.Builder.of(MultiClutchBlockEntity::new, MULTI_CLUTCH.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<MultiClutchMenu>> MULTI_CLUTCH_MENU = RotaryMenus.MENUS.register("multi_clutch",
            () -> IMenuTypeExtension.create(MultiClutchMenu::fromNetwork));

    public static final DeferredBlock<DistributionClutchBlock> DISTRIBUTION_CLUTCH = RotaryBlocks.BLOCKS.register("distribution_clutch",
            () -> new DistributionClutchBlock(RotaryBlocks.machineProps().noOcclusion(), TransmissionRegistry.DISTRIBUTION_CLUTCH_BE, DistributionClutchBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DistributionClutchBlockEntity>> DISTRIBUTION_CLUTCH_BE =
            RotaryBlockEntities.TYPES.register("distribution_clutch", () -> BlockEntityType.Builder.of(DistributionClutchBlockEntity::new, DISTRIBUTION_CLUTCH.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<DistributionClutchMenu>> DISTRIBUTION_CLUTCH_MENU = RotaryMenus.MENUS.register("distribution_clutch",
            () -> IMenuTypeExtension.create(DistributionClutchMenu::fromNetwork));

    public static final DeferredBlock<PowerBusBlock> POWER_BUS = RotaryBlocks.BLOCKS.register("power_bus", () -> new PowerBusBlock(RotaryBlocks.machineProps()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerBusBlockEntity>> POWER_BUS_BE =
            RotaryBlockEntities.TYPES.register("power_bus", () -> BlockEntityType.Builder.of(PowerBusBlockEntity::new, POWER_BUS.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<PowerBusMenu>> POWER_BUS_MENU = RotaryMenus.MENUS.register("power_bus",
            () -> IMenuTypeExtension.create(PowerBusMenu::fromNetwork));
    public static final DeferredBlock<BusControllerBlock> BUS_CONTROLLER = RotaryBlocks.BLOCKS.register("bus_controller",
            () -> new BusControllerBlock(RotaryBlocks.machineProps(), TransmissionRegistry.BUS_CONTROLLER_BE, BusControllerBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BusControllerBlockEntity>> BUS_CONTROLLER_BE =
            RotaryBlockEntities.TYPES.register("bus_controller", () -> BlockEntityType.Builder.of(BusControllerBlockEntity::new, BUS_CONTROLLER.get()).build(null));

    public static final DeferredBlock<EngineControllerBlock> ENGINE_CONTROLLER = RotaryBlocks.BLOCKS.register("engine_controller", () -> new EngineControllerBlock(RotaryBlocks.machineProps()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EngineControllerBlockEntity>> ENGINE_CONTROLLER_BE =
            RotaryBlockEntities.TYPES.register("engine_controller", () -> BlockEntityType.Builder.of(EngineControllerBlockEntity::new, ENGINE_CONTROLLER.get()).build(null));

    static {
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(ENGINE_CONTROLLER));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(MULTI_CLUTCH));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(DISTRIBUTION_CLUTCH));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(POWER_BUS));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(BUS_CONTROLLER));
    }

    private TransmissionRegistry() {}

    /** Loads the class, so its entries join the shared registers. */
    public static void init(IEventBus modBus) {
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, POWER_BUS_BE.get(), (be, side) -> be.automation());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ENGINE_CONTROLLER_BE.get(), (be, side) -> FluidAccess.fillOnly(be.tank()));
        // lubricant goes into a Bus Controller through its top or bottom only
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BUS_CONTROLLER_BE.get(), (be, side) -> side == null || side.getAxis().isVertical() ? FluidAccess.fillOnly(be.tank()) : null);
    }
}
