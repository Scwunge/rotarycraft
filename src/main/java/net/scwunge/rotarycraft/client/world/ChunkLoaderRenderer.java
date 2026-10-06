package net.scwunge.rotarycraft.client.world;

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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.ChunkLoaderBlockEntity;
import net.scwunge.rotarycraft.client.weapon.ReikaModel;

/**
 * The Chunk Loader, drawn from the original's model: four rings of blades spin above its base, each turning twice as far as
 * the one below (the original's cumulative rotations), at a speed that grows with the shaft's.
 */
public class ChunkLoaderRenderer implements BlockEntityRenderer<ChunkLoaderBlockEntity> {
    static final ReikaModel MODEL = new ReikaModel("chunk_loader");
    static final ResourceLocation TEXTURE = RotaryCraft.id("textures/machine/chunk_loader.png");
    private static final String[] BASE = {"Shape1", "Shape2", "Shape21", "Shape4", "Shape4a", "Shape4b", "Shape4d", "Shape2d", "Shape2c", "Shape2b",
            "Shape21d", "Shape21c", "Shape21b"};
    private static final String[][] RINGS = {
            {"Shape3d", "Shape31d", "Shape32d"},
            {"Shape3c", "Shape31c", "Shape32c"},
            {"Shape3b", "Shape31b", "Shape32b"},
            {"Shape3", "Shape31", "Shape32"}};

    static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, float phi) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        pose.pushPose();
        ReikaModel.enterModelSpace(pose);
        MODEL.render(pose, vc, light, overlay, BASE);
        for (String[] ring : RINGS) {
            pose.mulPose(Axis.YP.rotationDegrees(phi));
            MODEL.render(pose, vc, light, overlay, ring);
        }
        pose.popPose();
    }

    @Override
    public void render(ChunkLoaderBlockEntity loader, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        long time = loader.getLevel() == null ? 0 : loader.getLevel().getGameTime();
        float phi = loader.getOmega() <= 0 ? 0 : (float) (-(time + partialTick) * 0.25 * Math.log(loader.getOmega() + 2) / Math.log(2));
        draw(pose, buffers, light, overlay, phi);
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
            draw(pose, buffers, light, overlay, 0);
            pose.popPose();
        }
    }
}
