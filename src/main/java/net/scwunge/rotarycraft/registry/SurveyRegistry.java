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

    static {
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(MOB_RADAR));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(CAVE_SCANNER));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(GPR));
    }

    private SurveyRegistry() {}

    /** Loads the class so its entries join the shared registers. */
    public static void init() {
    }
}
