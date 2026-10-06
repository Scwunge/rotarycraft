package net.scwunge.rotarycraft.client.world;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.WeatherControllerBlockEntity;
import net.scwunge.rotarycraft.client.weapon.ReikaModel;

/** The Weather Controller, drawn from the original's model (it does not move). */
public class WeatherControllerRenderer implements BlockEntityRenderer<WeatherControllerBlockEntity> {
    static final ReikaModel MODEL = new ReikaModel("iodide");
    static final ResourceLocation TEXTURE = RotaryCraft.id("textures/machine/weather_controller.png");

    static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        ReikaModel.enterModelSpace(pose);
        MODEL.renderAll(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        pose.popPose();
    }

    @Override
    public void render(WeatherControllerBlockEntity controller, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        draw(pose, buffers, light, overlay);
    }

    /** The item form. */
    public static class Item extends BlockEntityWithoutLevelRenderer {
        public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
            super(dispatcher, models);
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            pose.pushPose();
            pose.translate(0, -0.25, 0);
            pose.scale(1.125f, 1.125f, 1.125f);
            draw(pose, buffers, light, overlay);
            pose.popPose();
        }
    }
}
