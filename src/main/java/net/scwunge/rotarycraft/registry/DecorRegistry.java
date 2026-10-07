package net.scwunge.rotarycraft.registry;

import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.BlockFillerBlockEntity;
import net.scwunge.rotarycraft.blockentity.SpillerBlockEntity;
import net.scwunge.rotarycraft.blockentity.LineBuilderBlockEntity;
import net.scwunge.rotarycraft.blockentity.ObsidianMakerBlockEntity;
import net.scwunge.rotarycraft.machine.LayoutMachineBlock;
import net.scwunge.rotarycraft.machine.LayoutMenu;
import net.scwunge.rotarycraft.machine.LayoutMenus;

/**
 * The world and decoration machines (obsidian maker, pile driver, line builder, block filler, lights, beam mirror, aerosolizer, firework machine, music
 * box, self destruct, particle emitter, decorative tank) and the extra cannons: blocks, block entities and items go into the mod's shared registers.
 * Each has a switch in the machines config.
 */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class DecorRegistry {
    // ---- Obsidian Maker ----
    public static final Machines.Entry<ObsidianMakerBlockEntity, LayoutMachineBlock> OBSIDIAN_MAKER = Machines.machine("obsidian_maker", ObsidianMakerBlockEntity::new);
    public static final DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> OBSIDIAN_MAKER_MENU = LayoutMenus.register(ObsidianMakerBlockEntity.LAYOUT);

    // ---- Line Builder ----
    public static final Machines.Entry<LineBuilderBlockEntity, LayoutMachineBlock> LINE_BUILDER = Machines.machine("line_builder", LineBuilderBlockEntity::new);
    public static final DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> LINE_BUILDER_MENU = LayoutMenus.register(LineBuilderBlockEntity.LAYOUT);

    // ---- Block Filler and Spiller ----
    public static final Machines.Entry<BlockFillerBlockEntity, LayoutMachineBlock> BLOCK_FILLER = Machines.machine("block_filler", BlockFillerBlockEntity::new);
    public static final DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> BLOCK_FILLER_MENU = LayoutMenus.register(BlockFillerBlockEntity.LAYOUT);
    public static final Machines.Entry<SpillerBlockEntity, LayoutMachineBlock> SPILLER = Machines.machine("spiller", SpillerBlockEntity::new);

    private DecorRegistry() {}

    /** Loads the class, so its entries join the shared registers. */
    public static void init(IEventBus modBus) {
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, OBSIDIAN_MAKER.type().get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, OBSIDIAN_MAKER.type().get(), (be, side) -> be.fluidHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LINE_BUILDER.type().get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BLOCK_FILLER.type().get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SPILLER.type().get(), (be, side) -> be.fluidHandler(side));
    }
}
