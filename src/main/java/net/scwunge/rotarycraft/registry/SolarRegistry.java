package net.scwunge.rotarycraft.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import net.scwunge.rotarycraft.solar.SolarMirrorBlock;
import net.scwunge.rotarycraft.solar.SolarMirrorBlockEntity;
import net.scwunge.rotarycraft.solar.SolarTowerBlock;
import net.scwunge.rotarycraft.solar.SolarTowerBlockEntity;

/** The Solar Tower and the Solar Mirrors that feed it: blocks, block entities and items go into the mod's shared registers. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class SolarRegistry {
    public static final DeferredBlock<SolarTowerBlock> SOLAR_TOWER = RotaryBlocks.BLOCKS.register("solar_tower",
            () -> new SolarTowerBlock(RotaryBlocks.machineProps().noOcclusion()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarTowerBlockEntity>> SOLAR_TOWER_BE =
            RotaryBlockEntities.TYPES.register("solar_tower", () -> BlockEntityType.Builder.of(SolarTowerBlockEntity::new, SOLAR_TOWER.get()).build(null));
    public static final DeferredBlock<SolarMirrorBlock> SOLAR_MIRROR = RotaryBlocks.BLOCKS.register("solar_mirror",
            () -> new SolarMirrorBlock(RotaryBlocks.machineProps().noOcclusion()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarMirrorBlockEntity>> SOLAR_MIRROR_BE =
            RotaryBlockEntities.TYPES.register("solar_mirror", () -> BlockEntityType.Builder.of(SolarMirrorBlockEntity::new, SOLAR_MIRROR.get()).build(null));

    static {
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(SOLAR_TOWER));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(SOLAR_MIRROR));
    }

    private SolarRegistry() {}

    /** Loads the class, so its entries join the shared registers. */
    public static void init(IEventBus modBus) {
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SOLAR_TOWER_BE.get(), (be, side) -> FluidAccess.fillOnly(be.tank()));
    }
}
