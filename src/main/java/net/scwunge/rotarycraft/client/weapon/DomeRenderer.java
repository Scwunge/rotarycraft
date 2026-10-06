package net.scwunge.rotarycraft.client.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.weapon.turret.DomeBlockEntity;

/**
 * The dome emitter, drawn from the original's model, and the field: a translucent sphere in the machine's colour, its
 * pattern (forcefield.png) drifting round and up, with a cone of the same from it to the top.
 */
public class DomeRenderer<T extends DomeBlockEntity> implements BlockEntityRenderer<T> {
    static final ReikaModel MODEL = new ReikaModel("dome_emitter");
    private static final ResourceLocation FIELD = RotaryCraft.id("textures/effect/forcefield.png");
    private final ResourceLocation texture;

    public DomeRenderer(String texture) {
        this.texture = RotaryCraft.id("textures/machine/" + texture + ".png");
    }

    static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, ResourceLocation texture) {
        pose.pushPose();
        ReikaModel.enterModelSpace(pose);
        MODEL.renderAll(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay);
        pose.popPose();
    }

    @Override
    public void render(T dome, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        draw(pose, buffers, light, overlay, texture);
        int r = dome.range();
        if (r <= 0 || dome.getLevel() == null) {
            return;
        }
        int rgb = dome.domeColor(dome.getLevel().getGameTime());
        float red = (rgb >> 16 & 255) / 255f, green = (rgb >> 8 & 255) / 255f, blue = (rgb & 255) / 255f;
        double millis = System.currentTimeMillis();
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        field(pose, buffers.getBuffer(DomeRenderTypes.FIELD), r, red, green, blue, millis);
        pose.popPose();
    }

    private static void field(PoseStack pose, VertexConsumer vc, int r, float red, float green, float blue, double millis) {
        PoseStack.Pose p = pose.last();
        double dk = 0.5 * r / 16;
        double di = 10;
        double spin = millis / 50D % 360;
        double rise = millis / 220D % 360;
        for (double k = -r; k < r; k += dk) {
            double top = Math.min(r, k + dk);
            double r2 = Math.sqrt(Math.max(0, r * r - k * k));
            double r3 = Math.sqrt(Math.max(0, r * r - top * top));
            for (int i = 0; i < 360; i += (int) di) {
                double a = Math.toRadians(i), a2 = Math.toRadians(i + di);
                double u = (i + spin) / 360D, du = (i + di + spin) / 360D;
                double v = (k + rise) / r, dv = (top + rise) / r;
                double c1 = Math.cos(a), s1 = Math.sin(a), c2 = Math.cos(a2), s2 = Math.sin(a2);
                vertex(vc, p, r2 * c1, k, r2 * s1, u, v, red, green, blue, r);
                vertex(vc, p, r2 * c2, k, r2 * s2, du, v, red, green, blue, r);
                vertex(vc, p, r3 * c2, top, r3 * s2, du, dv, red, green, blue, r);
                vertex(vc, p, r3 * c1, top, r3 * s1, u, dv, red, green, blue, r);
            }
        }
        // the cone from the emitter up to the top of the dome, turning slowly, as the original's fan
        double ux = (millis / 3100D) % 10, uy = (millis / 4700D) % 10;
        double turn = Math.toRadians(millis / 20D % 360);
        double y = r - 0.25;
        double dr = 2;
        for (int i = 0; i < 360; i += 10) {
            double a = Math.toRadians(i), a2 = Math.toRadians(i + 10);
            vertex(vc, p, 0, 0.5, 0, 0.5, 0.5, red, green, blue, r);
            vertex(vc, p, dr * Math.cos(a), y, dr * Math.sin(a), (Math.cos(a + turn) + ux) * 0.25, (Math.sin(a + turn) + uy) * 0.25, red, green, blue, r);
            vertex(vc, p, dr * Math.cos(a2), y, dr * Math.sin(a2), (Math.cos(a2 + turn) + ux) * 0.25, (Math.sin(a2 + turn) + uy) * 0.25, red, green, blue, r);
            vertex(vc, p, 0, 0.5, 0, 0.5, 0.5, red, green, blue, r);
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, double x, double y, double z, double u, double v, float red, float green, float blue, int r) {
        vc.addVertex(p, (float) x, (float) y, (float) z).setColor((int) (red * 255), (int) (green * 255), (int) (blue * 255), 150).setUv((float) u, (float) v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(p, (float) (x / r), (float) (y / r), (float) (z / r));
    }

    @Override
    public AABB getRenderBoundingBox(T dome) {
        return new AABB(dome.getBlockPos()).inflate(dome.range() + 1);
    }

    @Override
    public boolean shouldRenderOffScreen(T dome) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    /** The item form. */
    public static class Item extends BlockEntityWithoutLevelRenderer {
        private final ResourceLocation texture;

        public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models, String texture) {
            super(dispatcher, models);
            this.texture = RotaryCraft.id("textures/machine/" + texture + ".png");
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            draw(pose, buffers, light, overlay, texture);
        }
    }
}
