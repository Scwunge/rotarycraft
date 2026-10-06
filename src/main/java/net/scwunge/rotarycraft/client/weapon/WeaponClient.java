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

    private WeaponClient() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(WeaponRegistry.RAILGUN_BE.get(), c -> new TurretRenderer<>(RAILGUN));
        event.registerBlockEntityRenderer(WeaponRegistry.FREEZE_GUN_BE.get(), c -> new TurretRenderer<>(FREEZE_GUN));
        event.registerBlockEntityRenderer(WeaponRegistry.ANTI_AIR_BE.get(), c -> new TurretRenderer<>(ANTI_AIR));
        event.registerEntityRenderer(WeaponRegistry.RAILGUN_SHOT.get(), c -> new StarShotRenderer(c, "railgun_shot"));
        event.registerEntityRenderer(WeaponRegistry.FREEZE_SHOT.get(), c -> new StarShotRenderer(c, "freeze_shot"));
        event.registerEntityRenderer(WeaponRegistry.FLAK_SHOT.get(), c -> new ItemShotRenderer(c, () -> new net.minecraft.world.item.ItemStack(RotaryItems.SCRAP.get())));
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(WeaponRegistry.AMMO_MENU.get(), AmmoScreen::new);
    }

    @SubscribeEvent
    public static void items(RegisterClientExtensionsEvent event) {
        event.registerItem(turretItem(RAILGUN), WeaponRegistry.RAILGUN.get().asItem());
        event.registerItem(turretItem(FREEZE_GUN), WeaponRegistry.FREEZE_GUN.get().asItem());
        event.registerItem(turretItem(ANTI_AIR), WeaponRegistry.ANTI_AIR.get().asItem());
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
