package net.scwunge.rotarycraft.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.scwunge.rotarycraft.menu.DistributionClutchMenu;
import net.scwunge.rotarycraft.menu.MultiClutchMenu;
import net.scwunge.rotarycraft.transmission.DistributionClutchBlock;
import net.scwunge.rotarycraft.transmission.DistributionClutchBlockEntity;
import net.scwunge.rotarycraft.transmission.MultiClutchBlock;
import net.scwunge.rotarycraft.transmission.MultiClutchBlockEntity;

/** The transmission pieces beyond shafts, gears and clutches (multi-clutch, distribution clutch, buses, belts and the like): blocks, block entities, menus and items go into the mod's shared registers. */
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

    static {
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(MULTI_CLUTCH));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(DISTRIBUTION_CLUTCH));
    }

    private TransmissionRegistry() {}

    /** Loads the class, so its entries join the shared registers. */
    public static void init(IEventBus modBus) {
    }
}
