package net.scwunge.rotarycraft.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.survey.MobRadarBlockEntity;
import net.scwunge.rotarycraft.survey.RadarMenu;

/**
 * The surveying machines (Mob Radar, Ground-Penetrating Radar, Cave Scanner, CCTV, Spy Cam) and the display machines. Their blocks,
 * block entities and menus go into the mod's shared registers; {@link WeaponRegistry#init} loads this class too.
 */
public final class SurveyRegistry {
    public static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> SERIALIZERS =
            net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.RECIPE_SERIALIZER, net.scwunge.rotarycraft.RotaryCraft.MOD_ID);

    // ---- Mob Radar ----
    public static final DeferredBlock<MachineBlock> MOB_RADAR = RotaryBlocks.BLOCKS.register("mob_radar",
            () -> new MachineBlock(RotaryBlocks.machineProps().noOcclusion(), SurveyRegistry.MOB_RADAR_BE, MobRadarBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MobRadarBlockEntity>> MOB_RADAR_BE = RotaryBlockEntities.TYPES.register("mob_radar",
            () -> BlockEntityType.Builder.of(MobRadarBlockEntity::new, MOB_RADAR.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<RadarMenu>> RADAR_MENU = RotaryMenus.MENUS.register("mob_radar",
            () -> IMenuTypeExtension.create(RadarMenu::fromNetwork));

    // ---- Ground-Penetrating Radar ----
    public static final DeferredBlock<net.scwunge.rotarycraft.survey.GprBlock> GPR = RotaryBlocks.BLOCKS.register("gpr",
            () -> new net.scwunge.rotarycraft.survey.GprBlock(RotaryBlocks.machineProps(), SurveyRegistry.GPR_BE, net.scwunge.rotarycraft.survey.GprBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.survey.GprBlockEntity>> GPR_BE = RotaryBlockEntities.TYPES.register("gpr",
            () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.survey.GprBlockEntity::new, GPR.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<net.scwunge.rotarycraft.survey.GprMenu>> GPR_MENU = RotaryMenus.MENUS.register("gpr",
            () -> IMenuTypeExtension.create(net.scwunge.rotarycraft.survey.GprMenu::fromNetwork));

    // ---- Cave Scanner ----
    public static final DeferredBlock<net.scwunge.rotarycraft.survey.CaveScannerBlock> CAVE_SCANNER = RotaryBlocks.BLOCKS.register("cave_scanner",
            () -> new net.scwunge.rotarycraft.survey.CaveScannerBlock(RotaryBlocks.machineProps().noOcclusion(), SurveyRegistry.CAVE_SCANNER_BE,
                    net.scwunge.rotarycraft.survey.CaveScannerBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.survey.CaveScannerBlockEntity>> CAVE_SCANNER_BE =
            RotaryBlockEntities.TYPES.register("cave_scanner",
                    () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.survey.CaveScannerBlockEntity::new, CAVE_SCANNER.get()).build(null));

    // ---- CCTV, Spy Cam and the Screen that calls them up ----
    public static final DeferredBlock<net.scwunge.rotarycraft.survey.SurveyBlock> CCTV = RotaryBlocks.BLOCKS.register("cctv",
            () -> new net.scwunge.rotarycraft.survey.SurveyBlock(RotaryBlocks.machineProps().noOcclusion(), SurveyRegistry.CCTV_BE,
                    net.scwunge.rotarycraft.survey.CctvBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.survey.CctvBlockEntity>> CCTV_BE =
            RotaryBlockEntities.TYPES.register("cctv",
                    () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.survey.CctvBlockEntity::new, CCTV.get()).build(null));
    public static final DeferredBlock<net.scwunge.rotarycraft.survey.SurveyBlock> SPY_CAM = RotaryBlocks.BLOCKS.register("spy_cam",
            () -> new net.scwunge.rotarycraft.survey.SurveyBlock(RotaryBlocks.machineProps().noOcclusion(), SurveyRegistry.SPY_CAM_BE,
                    net.scwunge.rotarycraft.survey.SpyCamBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.survey.SpyCamBlockEntity>> SPY_CAM_BE =
            RotaryBlockEntities.TYPES.register("spy_cam",
                    () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.survey.SpyCamBlockEntity::new, SPY_CAM.get()).build(null));
    public static final DeferredBlock<net.scwunge.rotarycraft.survey.ScreenBlock> SCREEN = RotaryBlocks.BLOCKS.register("cctv_screen",
            () -> new net.scwunge.rotarycraft.survey.ScreenBlock(RotaryBlocks.machineProps().noOcclusion(), SurveyRegistry.SCREEN_BE,
                    net.scwunge.rotarycraft.survey.ScreenBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.survey.ScreenBlockEntity>> SCREEN_BE =
            RotaryBlockEntities.TYPES.register("cctv_screen",
                    () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.survey.ScreenBlockEntity::new, SCREEN.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<net.scwunge.rotarycraft.survey.RemoteMenu>> REMOTE_MENU = RotaryMenus.MENUS.register("camera",
            () -> IMenuTypeExtension.create(net.scwunge.rotarycraft.survey.RemoteMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<net.scwunge.rotarycraft.survey.ScreenMenu>> SCREEN_MENU = RotaryMenus.MENUS.register("cctv_screen",
            () -> IMenuTypeExtension.create(net.scwunge.rotarycraft.survey.ScreenMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<net.scwunge.rotarycraft.survey.SpyCamMenu>> SPY_CAM_MENU = RotaryMenus.MENUS.register("spy_cam",
            () -> IMenuTypeExtension.create(net.scwunge.rotarycraft.survey.SpyCamMenu::fromNetwork));

    // ---- Display ----
    public static final DeferredBlock<net.scwunge.rotarycraft.survey.DisplayBlock> DISPLAY = RotaryBlocks.BLOCKS.register("display",
            () -> new net.scwunge.rotarycraft.survey.DisplayBlock(RotaryBlocks.machineProps().noOcclusion(), SurveyRegistry.DISPLAY_BE,
                    net.scwunge.rotarycraft.survey.DisplayBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.survey.DisplayBlockEntity>> DISPLAY_BE =
            RotaryBlockEntities.TYPES.register("display",
                    () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.survey.DisplayBlockEntity::new, DISPLAY.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<net.scwunge.rotarycraft.survey.CoilMenu>> DISPLAY_MENU = RotaryMenus.MENUS.register("display",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> net.scwunge.rotarycraft.survey.CoilMenu.fromNetwork(SurveyRegistry.DISPLAY_MENU.get(), id, inventory, buf)));

    // ---- Projector and its slides ----
    public static final DeferredBlock<net.scwunge.rotarycraft.survey.ProjectorBlock> PROJECTOR = RotaryBlocks.BLOCKS.register("projector",
            () -> new net.scwunge.rotarycraft.survey.ProjectorBlock(RotaryBlocks.machineProps().noOcclusion(), SurveyRegistry.PROJECTOR_BE,
                    net.scwunge.rotarycraft.survey.ProjectorBlockEntity::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.survey.ProjectorBlockEntity>> PROJECTOR_BE =
            RotaryBlockEntities.TYPES.register("projector",
                    () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.survey.ProjectorBlockEntity::new, PROJECTOR.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<net.scwunge.rotarycraft.survey.ProjectorMenu>> PROJECTOR_MENU = RotaryMenus.MENUS.register("projector",
            () -> IMenuTypeExtension.create(net.scwunge.rotarycraft.survey.ProjectorMenu::fromNetwork));
    public static final java.util.List<net.neoforged.neoforge.registries.DeferredItem<net.scwunge.rotarycraft.survey.SlideItem>> SLIDES = new java.util.ArrayList<>();
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, net.minecraft.world.item.crafting.RecipeSerializer<net.scwunge.rotarycraft.survey.SlideDyeRecipe>> SLIDE_DYE_SERIALIZER =
            SERIALIZERS.register("slide_dye", net.scwunge.rotarycraft.survey.SlideDyeRecipe::serializer);

    static {
        for (int i = 0; i < net.scwunge.rotarycraft.survey.SlideItem.COUNT; i++) {
            int index = i;
            SLIDES.add(RotaryItems.add(RotaryItems.ITEMS.register("slide_" + i,
                    () -> new net.scwunge.rotarycraft.survey.SlideItem(new net.minecraft.world.item.Item.Properties(), index))));
        }
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(PROJECTOR));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(DISPLAY));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(CCTV));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(SPY_CAM));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(SCREEN));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(MOB_RADAR));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(CAVE_SCANNER));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(GPR));
    }

    private SurveyRegistry() {}

    /** Loads the class so its entries join the shared registers, and registers its own. */
    public static void init(net.neoforged.bus.api.IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }
}
