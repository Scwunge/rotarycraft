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
import net.scwunge.rotarycraft.farm.AutoBreederBlockEntity;
import net.scwunge.rotarycraft.farm.BaitBoxBlockEntity;
import net.scwunge.rotarycraft.farm.BlowerBlockEntity;
import net.scwunge.rotarycraft.farm.DefoliatorBlockEntity;
import net.scwunge.rotarycraft.farm.MobHarvesterBlockEntity;
import net.scwunge.rotarycraft.farm.SpawnerControllerBlockEntity;
import net.scwunge.rotarycraft.farm.VacuumBlockEntity;
import net.scwunge.rotarycraft.farm.WoodcutterBlockEntity;
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

    public static final Machine<DefoliatorBlockEntity> DEFOLIATOR_M = machine("defoliator", DefoliatorBlockEntity::new);
    public static final Machine<BlowerBlockEntity> BLOWER_M = machine("blower", BlowerBlockEntity::new);
    public static final Machine<VacuumBlockEntity> VACUUM_M = machine("vacuum", VacuumBlockEntity::new);
    public static final Machine<AutoBreederBlockEntity> AUTO_BREEDER_M = machine("auto_breeder", AutoBreederBlockEntity::new);
    public static final Machine<BaitBoxBlockEntity> BAIT_BOX_M = machine("bait_box", BaitBoxBlockEntity::new);
    public static final Machine<MobHarvesterBlockEntity> MOB_HARVESTER_M = machine("mob_harvester", MobHarvesterBlockEntity::new);
    public static final Machine<SpawnerControllerBlockEntity> SPAWNER_CONTROLLER_M = machine("spawner_controller", SpawnerControllerBlockEntity::new);

    public static final Machine<WoodcutterBlockEntity> WOODCUTTER_M = machine("woodcutter", WoodcutterBlockEntity::new);
    public static final DeferredBlock<FarmBlock> WOODCUTTER = WOODCUTTER_M.block();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WoodcutterBlockEntity>> WOODCUTTER_BE = WOODCUTTER_M.be();

    public static final DeferredBlock<FarmBlock> DEFOLIATOR = DEFOLIATOR_M.block();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DefoliatorBlockEntity>> DEFOLIATOR_BE = DEFOLIATOR_M.be();
    public static final DeferredBlock<FarmBlock> BLOWER = BLOWER_M.block();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlowerBlockEntity>> BLOWER_BE = BLOWER_M.be();
    public static final DeferredBlock<FarmBlock> VACUUM = VACUUM_M.block();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VacuumBlockEntity>> VACUUM_BE = VACUUM_M.be();
    public static final DeferredBlock<FarmBlock> AUTO_BREEDER = AUTO_BREEDER_M.block();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoBreederBlockEntity>> AUTO_BREEDER_BE = AUTO_BREEDER_M.be();
    public static final DeferredBlock<FarmBlock> BAIT_BOX = BAIT_BOX_M.block();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BaitBoxBlockEntity>> BAIT_BOX_BE = BAIT_BOX_M.be();
    public static final DeferredBlock<FarmBlock> MOB_HARVESTER = MOB_HARVESTER_M.block();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MobHarvesterBlockEntity>> MOB_HARVESTER_BE = MOB_HARVESTER_M.be();
    public static final DeferredBlock<FarmBlock> SPAWNER_CONTROLLER = SPAWNER_CONTROLLER_M.block();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpawnerControllerBlockEntity>> SPAWNER_CONTROLLER_BE = SPAWNER_CONTROLLER_M.be();

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
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DEFOLIATOR_BE.get(), (be, side) -> Handlers.slots(be.items(), slot -> slot == 0, slot -> slot == 1));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, WOODCUTTER_BE.get(), (be, side) -> Handlers.extractOnly(be.items()));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, VACUUM_BE.get(), (be, side) -> be.items());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, AUTO_BREEDER_BE.get(), (be, side) -> Handlers.insertOnly(be.items()));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BAIT_BOX_BE.get(), (be, side) -> be.items());
    }
}
