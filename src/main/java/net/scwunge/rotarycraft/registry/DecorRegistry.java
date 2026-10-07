package net.scwunge.rotarycraft.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.scwunge.rotarycraft.decor.DecoTank;
import net.scwunge.rotarycraft.decor.DecoTankBlock;
import net.scwunge.rotarycraft.decor.DecoTankBlockEntity;
import net.scwunge.rotarycraft.decor.DecoTankItem;
import net.scwunge.rotarycraft.decor.DecoTankSettingsRecipe;
import net.scwunge.rotarycraft.decor.MusicBoxBlockEntity;
import net.scwunge.rotarycraft.decor.MusicDiscItem;
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
import net.scwunge.rotarycraft.blockentity.AirGunBlockEntity;
import net.scwunge.rotarycraft.blockentity.ArrowGunBlockEntity;
import net.scwunge.rotarycraft.blockentity.BeamMirrorBlockEntity;
import net.scwunge.rotarycraft.blockentity.BlockCannonBlockEntity;
import net.scwunge.rotarycraft.blockentity.BlockFillerBlockEntity;
import net.scwunge.rotarycraft.blockentity.FireworkMachineBlockEntity;
import net.scwunge.rotarycraft.blockentity.FloodlightBlockEntity;
import net.scwunge.rotarycraft.blockentity.ItemCannonBlockEntity;
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

    // ---- Item Cannon ----
    public static final Machines.Entry<ItemCannonBlockEntity, LayoutMachineBlock> ITEM_CANNON = Machines.machine("item_cannon", ItemCannonBlockEntity::new);
    public static final DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> ITEM_CANNON_MENU = LayoutMenus.register(ItemCannonBlockEntity.LAYOUT);

    // ---- Arrow Gun and Air Gun ----
    public static final Machines.Entry<ArrowGunBlockEntity, LayoutMachineBlock> ARROW_GUN = Machines.register("arrow_gun", ArrowGunBlockEntity::new,
            type -> new LayoutMachineBlock(RotaryBlocks.machineProps(), type::get, ArrowGunBlockEntity::new, true));
    public static final DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> ARROW_GUN_MENU = LayoutMenus.register(ArrowGunBlockEntity.LAYOUT);
    public static final Machines.Entry<AirGunBlockEntity, LayoutMachineBlock> AIR_GUN = Machines.register("air_gun", AirGunBlockEntity::new,
            type -> new LayoutMachineBlock(RotaryBlocks.machineProps().noOcclusion(), type::get, AirGunBlockEntity::new, true));

    // ---- Decorative Tank ----
    public static final DeferredBlock<DecoTankBlock> DECO_TANK_BLOCK = RotaryBlocks.BLOCKS.register("deco_tank", DecoTankBlock::new);
    public static final DeferredItem<DecoTankItem> DECO_TANK = RotaryItems.add(RotaryItems.ITEMS.register("deco_tank", () -> new DecoTankItem(DECO_TANK_BLOCK.get(), new Item.Properties())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DecoTankBlockEntity>> DECO_TANK_BE = RotaryBlockEntities.TYPES.register("deco_tank",
            () -> BlockEntityType.Builder.of(DecoTankBlockEntity::new, DECO_TANK_BLOCK.get()).build(null));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DecoTankSettingsRecipe>> DECO_TANK_SETTINGS = RotaryRecipes.SERIALIZERS.register("deco_tank_settings",
            DecoTankSettingsRecipe::serializer);

    // ---- Block Cannon ----
    public static final Machines.Entry<BlockCannonBlockEntity, LayoutMachineBlock> BLOCK_CANNON = Machines.machine("block_cannon", BlockCannonBlockEntity::new);
    public static final DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> BLOCK_CANNON_MENU = LayoutMenus.register(BlockCannonBlockEntity.LAYOUT);

    // ---- Music Box and its disc ----
    public static final Machines.Entry<MusicBoxBlockEntity, LayoutMachineBlock> MUSIC_BOX = Machines.machine("music_box", MusicBoxBlockEntity::new);
    public static final DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> MUSIC_BOX_MENU = LayoutMenus.register(MusicBoxBlockEntity.LAYOUT);
    public static final DeferredItem<MusicDiscItem> MUSIC_DISC = RotaryItems.add(RotaryItems.ITEMS.register("music_disc", () -> new MusicDiscItem(new Item.Properties())));

    private DecorRegistry() {}

    /** Loads the class, so its entries join the shared registers. */
    public static void init(IEventBus modBus) {
        DecoTank.COMPONENTS.register(modBus);
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
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ITEM_CANNON.type().get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ARROW_GUN.type().get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BLOCK_CANNON.type().get(), (be, side) -> be.automationItems());
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new FluidHandlerItemStack(DecoTank.FLUID, stack, DecoTank.FILL), DECO_TANK.get());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SPILLER.type().get(), (be, side) -> be.fluidHandler(side));
    }
}
