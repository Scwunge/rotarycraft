package net.scwunge.rotarycraft.client.survey;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.client.weapon.ReikaModel;
import net.scwunge.rotarycraft.survey.ProjectorBlockEntity;
import org.joml.Matrix4f;

/**
 * The Projector, drawn from the original's model, and its picture: the slide on the wall in front, seven blocks wide and five tall,
 * with faint beams from the lens to its corners. With nothing in its first slot the picture is a blank error screen.
 */
public class ProjectorRenderer implements BlockEntityRenderer<ProjectorBlockEntity> {
    static final ReikaModel MODEL = new ReikaModel("projector");
    static final ResourceLocation TEXTURE = RotaryCraft.id("textures/machine/projector.png");

    static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, float yaw) {
        pose.pushPose();
        ReikaModel.enterModelSpace(pose);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        MODEL.renderAll(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        pose.popPose();
    }

    @Override
    public void render(ProjectorBlockEntity projector, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction dir = projector.direction();
        draw(pose, buffers, light, overlay, -dir.toYRot());
        if (!projector.isShowing()) {
            return;
        }
        pose.pushPose();
        // into a frame with the origin at the block's middle, looking the way it projects: z forward, x to the viewer's right, y up
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-dir.toYRot()));
        float d = projector.distance() - 0.5f - 0.002f;
        float left = -ProjectorBlockEntity.HALF_WIDTH - 0.5f, right = ProjectorBlockEntity.HALF_WIDTH + 0.5f;
        float bottom = -ProjectorBlockEntity.BELOW - 0.5f, top = ProjectorBlockEntity.ABOVE + 0.5f;
        Matrix4f m = pose.last().pose();
        int slide = projector.slide();
        if (slide >= 0 && slide < 24) {
            VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(RotaryCraft.id("textures/projector/image" + slide + ".png")));
            // viewed from the projector, x grows to the left in this frame (the frame looks down +z), so u runs the other way
            vertex(vc, pose, m, right, top, d, 0, 0);
            vertex(vc, pose, m, right, bottom, d, 0, 1);
            vertex(vc, pose, m, left, bottom, d, 1, 1);
            vertex(vc, pose, m, left, top, d, 1, 0);
        } else {
            VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());
            int color = 0xF0101010;
            vc.addVertex(m, right, top, d).setColor(color);
            vc.addVertex(m, right, bottom, d).setColor(color);
            vc.addVertex(m, left, bottom, d).setColor(color);
            vc.addVertex(m, left, top, d).setColor(color);
        }
        if (!net.scwunge.rotarycraft.config.RotaryConfig.get(net.scwunge.rotarycraft.config.RotaryConfig.PROJECTOR_LINES)) {
            pose.popPose();
            return;
        }
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        var last = pose.last();
        float lensX = 0, lensY = 0.0f, lensZ = 0.5f;
        float[][] corners = {{left, top}, {right, top}, {right, bottom}, {left, bottom}};
        for (int i = 0; i < 4; i++) {
            float[] a = corners[i], b = corners[(i + 1) % 4];
            lines.addVertex(last, lensX, lensY, lensZ).setColor(0x60FFFFFF).setNormal(last, 0, 1, 0);
            lines.addVertex(last, a[0], a[1], d).setColor(0x60FFFFFF).setNormal(last, 0, 1, 0);
            lines.addVertex(last, a[0], a[1], d).setColor(0x60FFFFFF).setNormal(last, 0, 1, 0);
            lines.addVertex(last, b[0], b[1], d).setColor(0x60FFFFFF).setNormal(last, 0, 1, 0);
        }
        pose.popPose();
    }

    private static void vertex(VertexConsumer vc, PoseStack pose, Matrix4f m, float x, float y, float z, float u, float v) {
        vc.addVertex(m, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(), 0, 0, -1);
    }

    @Override
    public AABB getRenderBoundingBox(ProjectorBlockEntity projector) {
        return new AABB(projector.getBlockPos()).inflate(ProjectorBlockEntity.MAX_RANGE + 4);
    }

    @Override
    public boolean shouldRenderOffScreen(ProjectorBlockEntity projector) {
        return true;
    }

    /** The item form. */
    public static class Item extends BlockEntityWithoutLevelRenderer {
        public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
            super(dispatcher, models);
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            draw(pose, buffers, light, overlay, 0);
        }
    }
}
