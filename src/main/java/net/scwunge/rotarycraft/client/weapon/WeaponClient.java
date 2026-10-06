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

    private WeaponClient() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(WeaponRegistry.RAILGUN_BE.get(), c -> new TurretRenderer<>(RAILGUN));
        event.registerEntityRenderer(WeaponRegistry.RAILGUN_SHOT.get(), RailgunShotRenderer::new);
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(WeaponRegistry.AMMO_MENU.get(), AmmoScreen::new);
    }

    @SubscribeEvent
    public static void items(RegisterClientExtensionsEvent event) {
        event.registerItem(turretItem(RAILGUN), WeaponRegistry.RAILGUN.get().asItem());
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
