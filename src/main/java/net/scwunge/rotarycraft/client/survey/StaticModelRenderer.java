package net.scwunge.rotarycraft.client.survey;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.client.weapon.ReikaModel;

/** A machine drawn from one of the original's models as it stands, turned to its horizontal facing if it has one. */
public class StaticModelRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private final ReikaModel model;
    private final ResourceLocation texture;
    private final float yawOffset;

    public StaticModelRenderer(String model, String texture, float yawOffset) {
        this.model = new ReikaModel(model);
        this.texture = RotaryCraft.id("textures/machine/" + texture + ".png");
        this.yawOffset = yawOffset;
    }

    void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, float yaw) {
        pose.pushPose();
        ReikaModel.enterModelSpace(pose);
        pose.mulPose(Axis.YP.rotationDegrees(yaw + yawOffset));
        model.renderAll(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay);
        pose.popPose();
    }

    @Override
    public void render(T be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var state = be.getBlockState();
        Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : state.hasProperty(BlockStateProperties.FACING) ? state.getValue(BlockStateProperties.FACING) : Direction.NORTH;
        draw(pose, buffers, light, overlay, facing.getAxis().isHorizontal() ? -facing.toYRot() : 0);
    }

    /** The item form. */
    public static class Item extends BlockEntityWithoutLevelRenderer {
        private final StaticModelRenderer<?> renderer;

        public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models, StaticModelRenderer<?> renderer) {
            super(dispatcher, models);
            this.renderer = renderer;
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            renderer.draw(pose, buffers, light, overlay, 0);
        }
    }
}
