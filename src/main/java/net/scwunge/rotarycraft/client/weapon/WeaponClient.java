package net.scwunge.rotarycraft.client.weapon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.WeaponRegistry;

/** Client setup for the weapons and defence machines: renderers, screens and item models. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WeaponClient {
    /** The original's RenderRailGun groups: base; turntable; barrel. */
    static final TurretRenderer.Look RAILGUN = new TurretRenderer.Look("rail_gun", "railgun",
            new String[] {"Shape5"},
            new String[] {"Shape6", "Shape7", "Shape7a", "Shape7b", "Shape7c", "Shape7d", "Shape7e", "Shape7f", "Shape7g"},
            new String[] {"Shape8", "Shape8a", "Shape9", "Shape3d", "Shape3", "Shape2a2", "Shape2a", "Shape1", "Shape1a", "Shape2", "Shape2c", "Shape2b",
                    "Shape2a1", "Shape4", "Shape4a"});

    static final TurretRenderer.Look FREEZE_GUN = new TurretRenderer.Look("freeze_gun", "freeze_gun",
            new String[] {"Shape5"},
            new String[] {"Shape8", "Shape8a", "Shape9", "Shape6", "Shape7", "Shape7a", "Shape7b", "Shape7c", "Shape7d", "Shape7e", "Shape7f", "Shape7g"},
            new String[] {"Shape1", "Shape3d", "Shape1a", "Shape1b", "Shape3da", "Shape2", "Shape2a", "Shape2b", "Shape2c", "Shape2d", "Shape2e"});
    static final TurretRenderer.Look ANTI_AIR = new TurretRenderer.Look("anti_air", "anti_air",
            new String[] {"Shape5"},
            new String[] {"Shape7", "Shape7a", "Shape7b", "Shape7c", "Shape7d", "Shape7e", "Shape7f", "Shape7g", "Shape6"},
            new String[] {"Shape1", "Shape3", "Shape8", "Shape8a", "Shape9", "Shape3d", "Shape1b", "Shape1c"});

    static final TurretRenderer.Look GATLING = new TurretRenderer.Look("gatling", "gatling",
            new String[] {"Shape5"},
            new String[] {"Shape6", "Shape7", "Shape7a", "Shape7b", "Shape7c", "Shape7d", "Shape7e", "Shape7f", "Shape7g"},
            new String[] {"Shape3", "Shape8", "Shape8a", "Shape9", "Shape3d"},
            new String[] {"Shape1a", "Shape1a0", "Shape1a1", "Shape1a2", "Shape1a3", "Shape1a4", "Shape10", "Shape10d", "Shape10e", "Shape10f", "Shape10g",
                    "Shape10h", "Shape10i", "Shape10j", "Shape10k", "Shape10a", "Shape10b", "Shape10c"}, 0.725);

    static final TurretRenderer.Look LASER_GUN = new TurretRenderer.Look("laser_gun", "laser_gun",
            new String[] {"Shape5"},
            new String[] {"Shape6", "Shape7", "Shape7a", "Shape7b", "Shape7c", "Shape7d", "Shape7e", "Shape7f", "Shape7g", "Shape8", "Shape8a", "Shape9"},
            new String[] {"Shape3da", "Shape3", "Shape3da1", "Shape3d", "Shape1", "Shape1a", "Shape1b", "Shape1c"});
    static final TurretRenderer.Look FLAME_TURRET = new TurretRenderer.Look("flame_turret", "flame_turret",
            new String[] {"Shape5"},
            new String[] {"Shape6", "Shape6b", "Shape7", "Shape7a", "Shape7b", "Shape7c", "Shape7d", "Shape7e", "Shape7f", "Shape7g", "Shape8", "Shape8a", "Shape9"},
            new String[] {"Shape12b2", "Shape1a2b2", "Shape1b2", "Shape3da", "Shape1b23", "Shape1bb2", "Shape1ab2", "Shape1b", "Shape1b22", "Shape1a2",
                    "Shape1a2b", "Shape12", "Shape12b", "Shape1", "Shape1bb", "Shape1a", "Shape1ab"});

    private WeaponClient() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(WeaponRegistry.RAILGUN_BE.get(), c -> new TurretRenderer<>(RAILGUN));
        event.registerBlockEntityRenderer(WeaponRegistry.FREEZE_GUN_BE.get(), c -> new TurretRenderer<>(FREEZE_GUN));
        event.registerBlockEntityRenderer(WeaponRegistry.ANTI_AIR_BE.get(), c -> new TurretRenderer<>(ANTI_AIR));
        event.registerBlockEntityRenderer(WeaponRegistry.GATLING_BE.get(), c -> new TurretRenderer<>(GATLING));
        event.registerEntityRenderer(WeaponRegistry.GATLING_SHOT.get(), c -> new StarShotRenderer(c, "gatling_shot"));
        event.registerBlockEntityRenderer(WeaponRegistry.LASER_GUN_BE.get(), c -> new LaserRenderer(LASER_GUN));
        event.registerBlockEntityRenderer(WeaponRegistry.FLAME_TURRET_BE.get(), c -> new TurretRenderer<>(FLAME_TURRET));
        event.registerEntityRenderer(WeaponRegistry.FLAME_SHOT.get(), c -> new StarShotRenderer(c, "flame_shot", 0.5f, true));
        event.registerBlockEntityRenderer(WeaponRegistry.TNT_CANNON_BE.get(), c -> new CannonRenderer());
        event.registerBlockEntityRenderer(WeaponRegistry.SONIC_BE.get(), c -> new SonicRenderer());
        event.registerBlockEntityRenderer(WeaponRegistry.HEAT_RAY_BE.get(), c -> new HeatRayRenderer());
        event.registerBlockEntityRenderer(WeaponRegistry.EMP_BE.get(), c -> new EmpRenderer());
        event.registerBlockEntityRenderer(WeaponRegistry.WINDER_BE.get(), c -> new WinderRenderer());
        event.registerBlockEntityRenderer(WeaponRegistry.LANDMINE_BE.get(), c -> new LandmineRenderer());
        event.registerBlockEntityRenderer(WeaponRegistry.FORCE_FIELD_BE.get(), c -> new DomeRenderer<>("force_field"));
        event.registerBlockEntityRenderer(WeaponRegistry.CONTAINMENT_BE.get(), c -> new DomeRenderer<>("containment"));
        event.registerEntityRenderer(WeaponRegistry.CANNON_TNT.get(), net.minecraft.client.renderer.entity.TntRenderer::new);
        event.registerEntityRenderer(WeaponRegistry.RAILGUN_SHOT.get(), c -> new StarShotRenderer(c, "railgun_shot"));
        event.registerEntityRenderer(WeaponRegistry.FREEZE_SHOT.get(), c -> new StarShotRenderer(c, "freeze_shot"));
        event.registerEntityRenderer(WeaponRegistry.FLAK_SHOT.get(), c -> new ItemShotRenderer(c, () -> new net.minecraft.world.item.ItemStack(RotaryItems.SCRAP.get())));
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(WeaponRegistry.AMMO_MENU.get(), AmmoScreen::new);
        event.register(WeaponRegistry.CANNON_MENU.get(), CannonScreen::new);
        event.register(WeaponRegistry.SONIC_MENU.get(), SonicScreen::new);
        event.register(WeaponRegistry.WINDER_MENU.get(), WinderScreen::new);
        event.register(WeaponRegistry.LANDMINE_MENU.get(), LandmineScreen::new);
        event.register(WeaponRegistry.RANGE_MENU.get(), RangeScreen::new);
    }

    @SubscribeEvent
    public static void items(RegisterClientExtensionsEvent event) {
        event.registerItem(turretItem(RAILGUN), WeaponRegistry.RAILGUN.get().asItem());
        event.registerItem(turretItem(FREEZE_GUN), WeaponRegistry.FREEZE_GUN.get().asItem());
        event.registerItem(turretItem(ANTI_AIR), WeaponRegistry.ANTI_AIR.get().asItem());
        event.registerItem(turretItem(GATLING), WeaponRegistry.GATLING.get().asItem());
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new CannonRenderer.Item(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return renderer;
            }
        }, WeaponRegistry.TNT_CANNON.get().asItem());
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new SonicRenderer.Item(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return renderer;
            }
        }, WeaponRegistry.SONIC.get().asItem());
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new HeatRayRenderer.Item(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return renderer;
            }
        }, WeaponRegistry.HEAT_RAY.get().asItem());
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new EmpRenderer.Item(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return renderer;
            }
        }, WeaponRegistry.EMP.get().asItem());
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new WinderRenderer.Item(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return renderer;
            }
        }, WeaponRegistry.WINDER.get().asItem());
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new LandmineRenderer.Item(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return renderer;
            }
        }, WeaponRegistry.LANDMINE.get().asItem());
        event.registerItem(turretItem(LASER_GUN), WeaponRegistry.LASER_GUN.get().asItem());
        event.registerItem(turretItem(FLAME_TURRET), WeaponRegistry.FLAME_TURRET.get().asItem());
        event.registerItem(domeItem("force_field"), WeaponRegistry.FORCE_FIELD.get().asItem());
        event.registerItem(domeItem("containment"), WeaponRegistry.CONTAINMENT.get().asItem());
    }

    private static IClientItemExtensions domeItem(String texture) {
        return new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new DomeRenderer.Item(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels(), texture);
                }
                return renderer;
            }
        };
    }

    private static IClientItemExtensions turretItem(TurretRenderer.Look look) {
        return new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new TurretRenderer.Item(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels(), look);
                }
                return renderer;
            }
        };
    }
}
