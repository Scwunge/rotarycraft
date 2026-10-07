package net.scwunge.rotarycraft.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.farm.FanBlockEntity;
import net.scwunge.rotarycraft.farm.FarmBlock;
import net.scwunge.rotarycraft.farm.FarmBlockEntity;
import net.scwunge.rotarycraft.farm.FertilizerBlockEntity;
import net.scwunge.rotarycraft.farm.GroundHydratorBlockEntity;
import net.scwunge.rotarycraft.farm.Handlers;
import net.scwunge.rotarycraft.farm.LawnSprinklerBlockEntity;
import net.scwunge.rotarycraft.farm.SprinklerBlockEntity;
import net.scwunge.rotarycraft.menu.FarmMenu;
import net.scwunge.rotarycraft.pipe.FluidAccess;

/**
 * The farming and automation machines: their blocks, block entities, items and the one menu type they share go into the mod's shared
 * registers. Each has an off switch in rotarycraft-farm.toml.
 */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class FarmRegistry {
    /** A machine's block and block entity type. */
    public record Machine<T extends FarmBlockEntity>(DeferredBlock<FarmBlock> block, DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> be) {
    }

    @SuppressWarnings("unchecked")
    private static <T extends FarmBlockEntity> Machine<T> machine(String name, BlockEntityType.BlockEntitySupplier<T> factory) {
        DeferredHolder<BlockEntityType<?>, BlockEntityType<T>>[] type = new DeferredHolder[1];
        DeferredBlock<FarmBlock> block = RotaryBlocks.BLOCKS.register(name, () -> new FarmBlock(RotaryBlocks.machineProps().noOcclusion(), () -> type[0].get(), factory::create));
        type[0] = RotaryBlockEntities.TYPES.register(name, () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(block));
        return new Machine<>(block, type[0]);
    }

    public static final DeferredHolder<MenuType<?>, MenuType<FarmMenu>> FARM_MENU = RotaryMenus.MENUS.register("farm_machine",
            () -> IMenuTypeExtension.create(FarmMenu::fromNetwork));

    public static final Machine<FanBlockEntity> FAN_M = machine("fan", FanBlockEntity::new);
    public static final Machine<SprinklerBlockEntity> SPRINKLER_M = machine("sprinkler", SprinklerBlockEntity::new);
    public static final Machine<LawnSprinklerBlockEntity> LAWN_SPRINKLER_M = machine("lawn_sprinkler", LawnSprinklerBlockEntity::new);
    public static final Machine<GroundHydratorBlockEntity> GROUND_HYDRATOR_M = machine("ground_hydrator", GroundHydratorBlockEntity::new);
    public static final Machine<FertilizerBlockEntity> FERTILIZER_M = machine("fertilizer", FertilizerBlockEntity::new);

    public static final DeferredBlock<FarmBlock> FAN = FAN_M.block();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FanBlockEntity>> FAN_BE = FAN_M.be();
    public static final DeferredBlock<FarmBlock> SPRINKLER = SPRINKLER_M.block();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SprinklerBlockEntity>> SPRINKLER_BE = SPRINKLER_M.be();
    public static final DeferredBlock<FarmBlock> LAWN_SPRINKLER = LAWN_SPRINKLER_M.block();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LawnSprinklerBlockEntity>> LAWN_SPRINKLER_BE = LAWN_SPRINKLER_M.be();
    public static final DeferredBlock<FarmBlock> GROUND_HYDRATOR = GROUND_HYDRATOR_M.block();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GroundHydratorBlockEntity>> GROUND_HYDRATOR_BE = GROUND_HYDRATOR_M.be();
    public static final DeferredBlock<FarmBlock> FERTILIZER = FERTILIZER_M.block();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FertilizerBlockEntity>> FERTILIZER_BE = FERTILIZER_M.be();

    private FarmRegistry() {}

    /** Loads the class, so its entries join the shared registers. */
    public static void init(IEventBus modBus) {
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SPRINKLER_BE.get(), (be, side) -> FluidAccess.fillOnly(be.tank()));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, LAWN_SPRINKLER_BE.get(), (be, side) -> FluidAccess.fillOnly(be.tank()));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GROUND_HYDRATOR_BE.get(), (be, side) -> FluidAccess.fillOnly(be.tank()));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FERTILIZER_BE.get(), (be, side) -> FluidAccess.fillOnly(be.tank()));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FERTILIZER_BE.get(), (be, side) -> Handlers.insertOnly(be.items()));
    }
}
