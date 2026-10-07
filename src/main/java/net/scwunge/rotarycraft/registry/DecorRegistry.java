package net.scwunge.rotarycraft.registry;

import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.BeamBlock;
import net.scwunge.rotarycraft.block.BridgeBlock;
import net.scwunge.rotarycraft.block.PilePipeBlock;
import net.scwunge.rotarycraft.block.SelfDestructBlock;
import net.scwunge.rotarycraft.blockentity.AerosolizerBlockEntity;
import net.scwunge.rotarycraft.blockentity.BeamMirrorBlockEntity;
import net.scwunge.rotarycraft.blockentity.BlockFillerBlockEntity;
import net.scwunge.rotarycraft.blockentity.FireworkMachineBlockEntity;
import net.scwunge.rotarycraft.blockentity.FloodlightBlockEntity;
import net.scwunge.rotarycraft.blockentity.LampBlockEntity;
import net.scwunge.rotarycraft.blockentity.LightBridgeBlockEntity;
import net.scwunge.rotarycraft.blockentity.ParticleEmitterBlockEntity;
import net.scwunge.rotarycraft.blockentity.PileDriverBlockEntity;
import net.scwunge.rotarycraft.blockentity.SelfDestructBlockEntity;
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

    // ---- Particle Emitter and Lamp (run by a coil, not the shaft) ----
    public static final Machines.Entry<ParticleEmitterBlockEntity, LayoutMachineBlock> PARTICLE_EMITTER = Machines.machine("particle_emitter", ParticleEmitterBlockEntity::new);
    public static final DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> PARTICLE_EMITTER_MENU = LayoutMenus.register(ParticleEmitterBlockEntity.LAYOUT);
    public static final Machines.Entry<LampBlockEntity, LayoutMachineBlock> LAMP = Machines.machine("lamp", LampBlockEntity::new);
    public static final DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> LAMP_MENU = LayoutMenus.register(LampBlockEntity.LAYOUT);

    // ---- Flood Light and Light Bridge, and the light they make ----
    public static final DeferredBlock<BeamBlock> BEAM = RotaryBlocks.BLOCKS.register("beam", BeamBlock::new);
    public static final DeferredBlock<BridgeBlock> BRIDGE = RotaryBlocks.BLOCKS.register("bridge", BridgeBlock::new);
    public static final Machines.Entry<FloodlightBlockEntity, LayoutMachineBlock> FLOODLIGHT = Machines.machine("floodlight", FloodlightBlockEntity::new);
    public static final Machines.Entry<LightBridgeBlockEntity, LayoutMachineBlock> LIGHT_BRIDGE = Machines.register("light_bridge", LightBridgeBlockEntity::new,
            type -> new LayoutMachineBlock(RotaryBlocks.machineProps().noOcclusion(), type::get, LightBridgeBlockEntity::new, true));

    // ---- Aerosolizer ----
    public static final Machines.Entry<AerosolizerBlockEntity, LayoutMachineBlock> AEROSOLIZER = Machines.machine("aerosolizer", AerosolizerBlockEntity::new);
    public static final DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> AEROSOLIZER_MENU = LayoutMenus.register(AerosolizerBlockEntity.LAYOUT);

    // ---- Self Destruct ----
    public static final Machines.Entry<SelfDestructBlockEntity, LayoutMachineBlock> SELF_DESTRUCT = Machines.register("self_destruct", SelfDestructBlockEntity::new,
            type -> new SelfDestructBlock(RotaryBlocks.machineProps().noOcclusion(), type::get, SelfDestructBlockEntity::new));

    // ---- Pile Driver, and the pile it lays ----
    public static final DeferredBlock<PilePipeBlock> PILE_PIPE = RotaryBlocks.BLOCKS.register("pile_pipe", PilePipeBlock::new);
    public static final Machines.Entry<PileDriverBlockEntity, LayoutMachineBlock> PILE_DRIVER = Machines.register("pile_driver", PileDriverBlockEntity::new,
            type -> new LayoutMachineBlock(RotaryBlocks.machineProps().noOcclusion(), type::get, PileDriverBlockEntity::new, true));

    // ---- Beam Mirror and Firework Machine ----
    public static final Machines.Entry<BeamMirrorBlockEntity, LayoutMachineBlock> BEAM_MIRROR = Machines.register("beam_mirror", BeamMirrorBlockEntity::new,
            type -> new LayoutMachineBlock(RotaryBlocks.machineProps().noOcclusion(), type::get, BeamMirrorBlockEntity::new, true));
    public static final Machines.Entry<FireworkMachineBlockEntity, LayoutMachineBlock> FIREWORK_MACHINE = Machines.machine("firework_machine", FireworkMachineBlockEntity::new);
    public static final DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> FIREWORK_MACHINE_MENU = LayoutMenus.register(FireworkMachineBlockEntity.LAYOUT);

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
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PARTICLE_EMITTER.type().get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LAMP.type().get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, AEROSOLIZER.type().get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FIREWORK_MACHINE.type().get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SPILLER.type().get(), (be, side) -> be.fluidHandler(side));
    }
}
