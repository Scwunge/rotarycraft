package net.scwunge.rotarycraft.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.BedrockBreakerBlock;
import net.scwunge.rotarycraft.block.BedrockSliceBlock;
import net.scwunge.rotarycraft.block.BorerBlock;
import net.scwunge.rotarycraft.block.ChunkLoaderBlock;
import net.scwunge.rotarycraft.block.MiningPipeBlock;
import net.scwunge.rotarycraft.block.WeatherControllerBlock;
import net.scwunge.rotarycraft.blockentity.BedrockBreakerBlockEntity;
import net.scwunge.rotarycraft.blockentity.BorerBlockEntity;
import net.scwunge.rotarycraft.blockentity.ChunkLoaderBlockEntity;
import net.scwunge.rotarycraft.blockentity.WeatherControllerBlockEntity;
import net.scwunge.rotarycraft.menu.BorerMenu;
import net.scwunge.rotarycraft.menu.WeatherMenu;

/**
 * The machines that load or change the world (chunk loader, weather controller, terraformer, borers): their blocks, block entities
 * and items go into the mod's shared registers. Each has an off switch in the config's world_machines section.
 */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class WorldMachineRegistry {
    /** The chunk loader's tickets. Whether a ticket is still wanted is decided by the loader that owns it, on its first tick. */
    public static final TicketController CHUNK_TICKETS = new TicketController(RotaryCraft.id("chunk_loader"));

    // ---- Chunk Loader ----
    public static final DeferredBlock<ChunkLoaderBlock> CHUNK_LOADER = RotaryBlocks.BLOCKS.register("chunk_loader",
            () -> new ChunkLoaderBlock(RotaryBlocks.machineProps().noOcclusion(), WorldMachineRegistry.CHUNK_LOADER_BE, ChunkLoaderBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChunkLoaderBlockEntity>> CHUNK_LOADER_BE =
            RotaryBlockEntities.TYPES.register("chunk_loader",
                    () -> BlockEntityType.Builder.of(ChunkLoaderBlockEntity::new, CHUNK_LOADER.get()).build(null));

    // ---- Weather Controller ----
    public static final DeferredItem<Item> SILVER_IODIDE = RotaryItems.add(RotaryItems.ITEMS.registerSimpleItem("silver_iodide"));
    public static final DeferredBlock<WeatherControllerBlock> WEATHER_CONTROLLER = RotaryBlocks.BLOCKS.register("weather_controller",
            () -> new WeatherControllerBlock(RotaryBlocks.machineProps().noOcclusion(), WorldMachineRegistry.WEATHER_CONTROLLER_BE,
                    WeatherControllerBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WeatherControllerBlockEntity>> WEATHER_CONTROLLER_BE =
            RotaryBlockEntities.TYPES.register("weather_controller",
                    () -> BlockEntityType.Builder.of(WeatherControllerBlockEntity::new, WEATHER_CONTROLLER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<WeatherMenu>> WEATHER_MENU = RotaryMenus.MENUS.register("weather_controller",
            () -> IMenuTypeExtension.create(WeatherMenu::fromNetwork));

    // ---- Borer ----
    public static final DeferredBlock<MiningPipeBlock> MINING_PIPE = RotaryBlocks.BLOCKS.register("mining_pipe", () -> new MiningPipeBlock(MiningPipeBlock.props()));
    public static final DeferredBlock<BorerBlock> BORER = RotaryBlocks.BLOCKS.register("borer",
            () -> new BorerBlock(RotaryBlocks.machineProps(), WorldMachineRegistry.BORER_BE, BorerBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BorerBlockEntity>> BORER_BE =
            RotaryBlockEntities.TYPES.register("borer", () -> BlockEntityType.Builder.of(BorerBlockEntity::new, BORER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<BorerMenu>> BORER_MENU = RotaryMenus.MENUS.register("borer",
            () -> IMenuTypeExtension.create(BorerMenu::fromNetwork));

    // ---- Bedrock Breaker ----
    public static final DeferredBlock<BedrockSliceBlock> BEDROCK_SLICE = RotaryBlocks.BLOCKS.register("bedrock_slice", () -> new BedrockSliceBlock(BedrockSliceBlock.props()));
    public static final DeferredBlock<BedrockBreakerBlock> BEDROCK_BREAKER = RotaryBlocks.BLOCKS.register("bedrock_breaker",
            () -> new BedrockBreakerBlock(RotaryBlocks.machineProps().noOcclusion(), WorldMachineRegistry.BEDROCK_BREAKER_BE, BedrockBreakerBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BedrockBreakerBlockEntity>> BEDROCK_BREAKER_BE =
            RotaryBlockEntities.TYPES.register("bedrock_breaker",
                    () -> BlockEntityType.Builder.of(BedrockBreakerBlockEntity::new, BEDROCK_BREAKER.get()).build(null));

    static {
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(CHUNK_LOADER));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(BEDROCK_BREAKER));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(BORER));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(WEATHER_CONTROLLER));
    }

    private WorldMachineRegistry() {}

    /** Loads the class, so its entries join the shared registers. */
    public static void init(IEventBus modBus) {
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, WEATHER_CONTROLLER_BE.get(), (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BEDROCK_BREAKER_BE.get(), (be, side) -> be.automationItems());
    }

    @SubscribeEvent
    public static void tickets(RegisterTicketControllersEvent event) {
        event.register(CHUNK_TICKETS);
    }
}
