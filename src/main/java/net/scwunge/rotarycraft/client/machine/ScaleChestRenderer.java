package net.scwunge.rotarycraft.client.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.logistics.ScaleChestBlockEntity;

/** The Scale-able Chest: the ordinary chest's model in the original's texture, its lid up while someone has it open. */
public class ScaleChestRenderer implements BlockEntityRenderer<ScaleChestBlockEntity> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/machine/scale_chest.png");
    private final ModelPart lid;
    private final ModelPart bottom;
    private final ModelPart lock;
    /** How open each chest's lid is, by position, eased towards where it should be. */
    private final java.util.Map<Long, float[]> lids = new java.util.HashMap<>();

    public ScaleChestRenderer(BlockEntityRendererProvider.Context context) {
        this(context.getModelSet());
    }

    ScaleChestRenderer(EntityModelSet models) {
        ModelPart root = models.bakeLayer(ModelLayers.CHEST);
        bottom = root.getChild("bottom");
        lid = root.getChild("lid");
        lock = root.getChild("lock");
    }

    private void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, float yaw, float open) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.translate(-0.5, -0.5, -0.5);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
        float eased = 1 - open;
        eased = 1 - eased * eased * eased;
        lid.xRot = -(eased * (float) (Math.PI / 2));
        lock.xRot = lid.xRot;
        lid.render(pose, buffer, light, overlay);
        lock.render(pose, buffer, light, overlay);
        bottom.render(pose, buffer, light, overlay);
        pose.popPose();
    }

    @Override
    public void render(ScaleChestBlockEntity chest, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = chest.facing().getAxis().isHorizontal() ? chest.facing() : Direction.NORTH;
        float[] state = lids.computeIfAbsent(chest.getBlockPos().asLong(), k -> new float[] {0, 0});
        long now = chest.getLevel() == null ? 0 : chest.getLevel().getGameTime();
        float time = now + partialTick;
        if (time != state[1]) {
            float target = chest.users() > 0 ? 1 : 0;
            float step = Math.min(1, Math.max(0, time - state[1])) * 0.1F;
            state[0] += Math.max(-step, Math.min(step, target - state[0]));
            state[1] = time;
        }
        draw(pose, buffers, light, overlay, facing.toYRot(), state[0]);
    }

    public static class Item extends BlockEntityWithoutLevelRenderer {
        private ScaleChestRenderer renderer;

        public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
            super(dispatcher, models);
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            if (renderer == null) {
                renderer = new ScaleChestRenderer(net.minecraft.client.Minecraft.getInstance().getEntityModels());
            }
            renderer.draw(pose, buffers, light, OverlayTexture.NO_OVERLAY, 0, 0);
        }
    }
}
