package net.scwunge.rotarycraft.registry;

import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.logistics.HeaterBlockEntity;
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

    private LogisticsRegistry() {}

    /** Loads the class, so its entries join the shared registers. */
    public static void init(IEventBus modBus) {
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SMOKE_DETECTOR.type().get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, HEATER.type().get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, IGNITER.type().get(), (be, side) -> be.automationItems());
    }
}
