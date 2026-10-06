package net.scwunge.rotarycraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.VanDeGraffBlockEntity;
import net.scwunge.rotarycraft.client.weapon.ReikaModel;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryBlocks;

/** Draws the Van de Graaff Generator from the original's model (converted by tools/modelbase2json.py). */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VanDeGraffClient {
    private static final ReikaModel MODEL = new ReikaModel("van_de_graff");
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/machine/van_de_graff.png");
    private static final String[] PARTS = {"Shape1", "Shape2", "Shape3", "Shape3a", "Shape3b", "Shape4", "Shape5", "Shape6", "Shape6a"};

    private VanDeGraffClient() {}

    private static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        pose.pushPose();
        ReikaModel.enterModelSpace(pose);
        MODEL.render(pose, vc, light, overlay, PARTS);
        pose.popPose();
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RotaryBlockEntities.VAN_DE_GRAAFF.get(), c -> (BlockEntityRenderer<VanDeGraffBlockEntity>)
                (be, partial, pose, buffers, light, overlay) -> draw(pose, buffers, light, overlay));
    }

    @SubscribeEvent
    public static void items(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels()) {
                        @Override
                        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                            pose.pushPose();
                            pose.translate(0, -0.25, 0);
                            draw(pose, buffers, light, overlay);
                            pose.popPose();
                        }
                    };
                }
                return renderer;
            }
        }, RotaryBlocks.VAN_DE_GRAAFF.get().asItem());
    }
}
