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
import net.scwunge.rotarycraft.weapon.turret.EmpBlockEntity;

/**
 * The EMP, drawn from the original's model (its darker skin while it loads), and the blast: two glowing spheres that swell out to
 * its 64 block range over a second, then fade as they pass it.
 */
public class EmpRenderer implements BlockEntityRenderer<EmpBlockEntity> {
    static final ReikaModel MODEL = new ReikaModel("emp");
    static final ResourceLocation READY = RotaryCraft.id("textures/machine/emp.png");
    static final ResourceLocation LOADING = RotaryCraft.id("textures/machine/emp_loading.png");
    private static final ResourceLocation SPHERE_1 = RotaryCraft.id("textures/effect/emp1.png");
    private static final ResourceLocation SPHERE_2 = RotaryCraft.id("textures/effect/emp2.png");
    private static final int EXPAND = 20;
    private static final int FADE = 10;

    static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, boolean loading) {
        pose.pushPose();
        ReikaModel.enterModelSpace(pose);
        MODEL.renderAll(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(loading ? LOADING : READY)), light, overlay);
        pose.popPose();
    }

    @Override
    public void render(EmpBlockEntity emp, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        draw(pose, buffers, light, overlay, emp.isLoading() && emp.usable());
        int age = emp.effectAge();
        if (age < 0) {
            return;
        }
        double radius = age >= EXPAND ? EmpBlockEntity.RANGE + 2 * (age - EXPAND + partialTick)
                : Math.min(EmpBlockEntity.RANGE, 0.25 + (age + partialTick) * EmpBlockEntity.RANGE / (double) EXPAND);
        float brightness = age <= EXPAND ? 1 : Math.max(0, 1 - (age - EXPAND + partialTick) / FADE);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        sphere(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(SPHERE_1)), (float) radius, 0.28f, 0.95f, 0.89f, brightness);
        sphere(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(SPHERE_2)), (float) radius * 0.98f, 0.75f, 1f, 1f, brightness);
        pose.popPose();
    }

    private static void sphere(PoseStack pose, VertexConsumer vc, float r, float red, float green, float blue, float alpha) {
        int stacks = 16, slices = 24;
        PoseStack.Pose p = pose.last();
        int a = (int) (alpha * 140);
        int packed = LightTexture.FULL_BRIGHT;
        for (int i = 0; i < stacks; i++) {
            double lat0 = Math.PI * (-0.5 + (double) i / stacks), lat1 = Math.PI * (-0.5 + (double) (i + 1) / stacks);
            for (int j = 0; j < slices; j++) {
                double lon0 = 2 * Math.PI * j / slices, lon1 = 2 * Math.PI * (j + 1) / slices;
                vertex(vc, p, r, lat0, lon0, (float) j / slices, (float) i / stacks, red, green, blue, a, packed);
                vertex(vc, p, r, lat0, lon1, (float) (j + 1) / slices, (float) i / stacks, red, green, blue, a, packed);
                vertex(vc, p, r, lat1, lon1, (float) (j + 1) / slices, (float) (i + 1) / stacks, red, green, blue, a, packed);
                vertex(vc, p, r, lat1, lon0, (float) j / slices, (float) (i + 1) / stacks, red, green, blue, a, packed);
            }
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, float r, double lat, double lon, float u, float v, float red, float green, float blue, int a, int light) {
        float x = (float) (r * Math.cos(lat) * Math.cos(lon)), y = (float) (r * Math.sin(lat)), z = (float) (r * Math.cos(lat) * Math.sin(lon));
        vc.addVertex(p, x, y, z).setColor((int) (red * 255), (int) (green * 255), (int) (blue * 255), a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(p, x / r, y / r, z / r);
    }

    @Override
    public AABB getRenderBoundingBox(EmpBlockEntity emp) {
        return new AABB(emp.getBlockPos()).inflate(EmpBlockEntity.RANGE + 40);
    }

    @Override
    public boolean shouldRenderOffScreen(EmpBlockEntity emp) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
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
            draw(pose, buffers, light, overlay, false);
            pose.popPose();
        }
    }
}
