package net.scwunge.rotarycraft.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.ChunkLoaderBlock;
import net.scwunge.rotarycraft.blockentity.ChunkLoaderBlockEntity;

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

    static {
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(CHUNK_LOADER));
    }

    private WorldMachineRegistry() {}

    /** Loads the class, so its entries join the shared registers. */
    public static void init(IEventBus modBus) {
    }

    @SubscribeEvent
    public static void tickets(RegisterTicketControllersEvent event) {
        event.register(CHUNK_TICKETS);
    }
}
