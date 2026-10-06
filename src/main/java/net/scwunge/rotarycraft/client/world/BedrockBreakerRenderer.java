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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.BedrockBreakerBlockEntity;
import net.scwunge.rotarycraft.client.weapon.ReikaModel;

import java.util.ArrayList;
import java.util.List;

/**
 * The Bedrock Breaker, drawn from the original's two models (one for the horizontal machine, one for the vertical): the grinding drill turns
 * with the shaft, and a rod of repeated segments reaches out to the block it is working on, its last segment stretched by how far through
 * that block it has got.
 */
public class BedrockBreakerRenderer implements BlockEntityRenderer<BedrockBreakerBlockEntity> {
    static final ReikaModel HORIZONTAL = new ReikaModel("bedrock_breaker");
    static final ReikaModel VERTICAL = new ReikaModel("bedrock_breaker_v");
    static final ResourceLocation HORIZONTAL_TEXTURE = RotaryCraft.id("textures/machine/bedrock_breaker.png");
    static final ResourceLocation VERTICAL_TEXTURE = RotaryCraft.id("textures/machine/bedrock_breaker_v.png");
    private static final int MAX_SEGMENTS = 360;
    private static final String[] BASE = {"Shape1", "Shape2", "Shape2a"};

    private record Groups(String[] head, String[] shaft, String[] post) {
        static Groups of(ReikaModel model) {
            List<String> head = new ArrayList<>();
            List<String> shaft = new ArrayList<>();
            List<String> post = new ArrayList<>();
            for (String name : model.names()) {
                if (List.of(BASE).contains(name)) {
                    continue;
                }
                (name.startsWith("Shape3") ? head : name.startsWith("Shape7") ? shaft : post).add(name);
            }
            return new Groups(head.toArray(new String[0]), shaft.toArray(new String[0]), post.toArray(new String[0]));
        }
    }

    private static Groups horizontalGroups;
    private static Groups verticalGroups;

    static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, Direction facing, float phi, int step, float grind) {
        boolean vertical = facing.getAxis().isVertical();
        ReikaModel model = vertical ? VERTICAL : HORIZONTAL;
        if (horizontalGroups == null) {
            horizontalGroups = Groups.of(HORIZONTAL);
            verticalGroups = Groups.of(VERTICAL);
        }
        Groups groups = vertical ? verticalGroups : horizontalGroups;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(vertical ? VERTICAL_TEXTURE : HORIZONTAL_TEXTURE));
        pose.pushPose();
        ReikaModel.enterModelSpace(pose);
        switch (facing) {
            case UP -> {
                pose.mulPose(Axis.YP.rotationDegrees(180));
                pose.mulPose(Axis.ZP.rotationDegrees(270));
                pose.translate(-1, -1, 0);
            }
            case DOWN -> {
                pose.mulPose(Axis.ZP.rotationDegrees(90));
                pose.translate(-1, -1, 0);
                pose.translate(2, 0, 0);
            }
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(-270));
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(-90));
            default -> {
            }
        }
        model.render(pose, vc, light, overlay, BASE);
        pose.pushPose();
        pose.translate(0, 1, 0);
        pose.mulPose(Axis.XP.rotationDegrees(phi));
        pose.translate(0, -1, 0);
        model.render(pose, vc, light, overlay, groups.head());
        int segments = vertical ? step : Math.min(step, MAX_SEGMENTS);
        for (int i = 1; i < segments; i++) {
            pose.pushPose();
            pose.translate(i - 1, 0, 0);
            if (i == step - 1) {
                pose.translate(-grind / 2, 0, 0);
                pose.scale(1 + grind, 1, 1);
            }
            model.render(pose, vc, light, overlay, groups.shaft());
            pose.popPose();
        }
        pose.popPose();
        model.render(pose, vc, light, overlay, groups.post());
        pose.popPose();
    }

    @Override
    public void render(BedrockBreakerBlockEntity breaker, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        long time = breaker.getLevel() == null ? 0 : breaker.getLevel().getGameTime();
        boolean turning = breaker.hasEnoughPower();
        float phi = turning ? (float) ((time + partialTick) * Math.pow(Math.log(breaker.getOmega() + 1) / Math.log(2), 1.05)) : 0;
        draw(pose, buffers, light, overlay, breaker.facing(), phi, breaker.step(), breaker.grindFraction());
    }

    @Override
    public AABB getRenderBoundingBox(BedrockBreakerBlockEntity breaker) {
        Direction dir = breaker.facing();
        int reach = Math.min(breaker.step(), MAX_SEGMENTS) + 1;
        return new AABB(breaker.getBlockPos()).expandTowards(dir.getStepX() * reach, dir.getStepY() * reach, dir.getStepZ() * reach).inflate(1);
    }

    @Override
    public boolean shouldRenderOffScreen(BedrockBreakerBlockEntity breaker) {
        return true;
    }

    /** The item form. */
    public static class Item extends BlockEntityWithoutLevelRenderer {
        public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
            super(dispatcher, models);
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            draw(pose, buffers, light, overlay, Direction.EAST, 0, 1, 0);
        }
    }
}
