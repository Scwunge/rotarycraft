package net.scwunge.rotarycraft.client.survey;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.client.weapon.ReikaModel;
import net.scwunge.rotarycraft.survey.CctvBlockEntity;

/**
 * The CCTV, drawn from the original's model: the head pans (and tilts) as aimed, and a coloured stripe along the top for each of the
 * three dyes that name it.
 */
public class CctvRenderer implements BlockEntityRenderer<CctvBlockEntity> {
    static final ReikaModel MODEL = new ReikaModel("cctv");
    static final ResourceLocation TEXTURE = RotaryCraft.id("textures/machine/cctv.png");
    private static final String[] HEAD = {"Shape3", "Shape3a", "Shape4a", "Shape4", "Shape5a", "Shape5", "Shape6", "Shape7"};
    private static final double PAN_PIVOT = 0.0625;
    private static final double TILT_PIVOT = 0.175;

    static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, float phi, float theta, int[] colors) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        pose.pushPose();
        ReikaModel.enterModelSpace(pose);
        MODEL.render(pose, vc, light, overlay, "Shape1", "Shape2");
        pose.translate(0, 0, PAN_PIVOT);
        pose.mulPose(Axis.YP.rotationDegrees(-phi));
        pose.translate(0, 0, -PAN_PIVOT);
        pose.translate(0, 1, TILT_PIVOT);
        pose.mulPose(Axis.XP.rotationDegrees(theta));
        pose.translate(0, -1, -TILT_PIVOT);
        MODEL.render(pose, vc, light, overlay, HEAD);
        if (colors != null) {
            VertexConsumer lines = buffers.getBuffer(RenderType.lines());
            var last = pose.last();
            for (int i = 0; i < 3; i++) {
                if (colors[i] < 0) {
                    continue;
                }
                int rgb = DyeColor.byId(colors[i]).getTextureDiffuseColor();
                float x = (i - 1) * 0.07f;
                lines.addVertex(last, x, 0.97f, 0.1f - 0.37f).setColor(0xFF000000 | rgb).setNormal(last, 0, 0, 1);
                lines.addVertex(last, x, 0.97f, 0.1f + 0.37f).setColor(0xFF000000 | rgb).setNormal(last, 0, 0, 1);
            }
        }
        pose.popPose();
    }

    @Override
    public void render(CctvBlockEntity cctv, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        draw(pose, buffers, light, overlay, cctv.phi(), cctv.theta(), cctv.colors());
    }

    /** The item form. */
    public static class Item extends BlockEntityWithoutLevelRenderer {
        public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
            super(dispatcher, models);
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            draw(pose, buffers, light, overlay, 0, 0, null);
        }
    }
}
