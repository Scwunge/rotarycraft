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
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.crafting.AutoCrafterBlock;
import net.scwunge.rotarycraft.crafting.AutoCrafterBlockEntity;
import net.scwunge.rotarycraft.crafting.CraftPatternItem;
import net.scwunge.rotarycraft.crafting.WorktableBlock;
import net.scwunge.rotarycraft.crafting.WorktableBlockEntity;
import net.scwunge.rotarycraft.menu.AutoCrafterMenu;
import net.scwunge.rotarycraft.menu.CraftPatternMenu;
import net.scwunge.rotarycraft.menu.WorktableMenu;

/** The Worktable, the Auto-Crafter and the Craft Pattern they read: blocks, block entities, menus and the item go into the mod's shared registers. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class CraftingRegistry {
    public static final DeferredBlock<WorktableBlock> WORKTABLE = RotaryBlocks.BLOCKS.register("worktable", () -> new WorktableBlock(RotaryBlocks.machineProps()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorktableBlockEntity>> WORKTABLE_BE =
            RotaryBlockEntities.TYPES.register("worktable", () -> BlockEntityType.Builder.of(WorktableBlockEntity::new, WORKTABLE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<WorktableMenu>> WORKTABLE_MENU = RotaryMenus.MENUS.register("worktable",
            () -> IMenuTypeExtension.create(WorktableMenu::fromNetwork));

    public static final DeferredBlock<AutoCrafterBlock> AUTO_CRAFTER = RotaryBlocks.BLOCKS.register("auto_crafter",
            () -> new AutoCrafterBlock(RotaryBlocks.machineProps(), CraftingRegistry.AUTO_CRAFTER_BE, AutoCrafterBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoCrafterBlockEntity>> AUTO_CRAFTER_BE =
            RotaryBlockEntities.TYPES.register("auto_crafter", () -> BlockEntityType.Builder.of(AutoCrafterBlockEntity::new, AUTO_CRAFTER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<AutoCrafterMenu>> AUTO_CRAFTER_MENU = RotaryMenus.MENUS.register("auto_crafter",
            () -> IMenuTypeExtension.create(AutoCrafterMenu::fromNetwork));

    public static final DeferredItem<CraftPatternItem> CRAFT_PATTERN = RotaryItems.add(RotaryItems.ITEMS.register("craft_pattern",
            () -> new CraftPatternItem(new Item.Properties())));
    public static final DeferredHolder<MenuType<?>, MenuType<CraftPatternMenu>> CRAFT_PATTERN_MENU = RotaryMenus.MENUS.register("craft_pattern",
            () -> IMenuTypeExtension.create(CraftPatternMenu::fromNetwork));

    static {
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(WORKTABLE));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(AUTO_CRAFTER));
    }

    private CraftingRegistry() {}

    /** Loads the class, so its entries join the shared registers. */
    public static void init(IEventBus modBus) {
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, WORKTABLE_BE.get(), (be, side) -> be.automation());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, AUTO_CRAFTER_BE.get(), (be, side) -> be.automation());
    }
}
