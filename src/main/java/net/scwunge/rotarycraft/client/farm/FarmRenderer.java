package net.scwunge.rotarycraft.client.farm;

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
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.client.weapon.ReikaModel;
import net.scwunge.rotarycraft.farm.FarmBlockEntity;

import java.util.Set;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * A farming or automation machine drawn from one of the original's models. Some of its parts may turn (the parts not named as fixed), about an
 * axis through a pivot in the model's space, at a speed the machine gives (degrees a tick); the model is turned to the way the block faces, as
 * the original's renderers did for each of its six facings.
 */
public class FarmRenderer<T extends FarmBlockEntity> implements BlockEntityRenderer<T> {
    /** How a machine looks: its model, its texture, what holds still, and how the rest turns. */
    public record Look<T extends FarmBlockEntity>(String model, Function<T, String> texture, Set<String> fixed, Axis axis, double pivotY, boolean facing,
                                                  ToDoubleFunction<T> speed) {
        public static <T extends FarmBlockEntity> Look<T> still(String model, String texture) {
            return new Look<>(model, be -> texture, Set.of(), null, 0, true, be -> 0);
        }
    }

    private final Look<T> look;
    private final ReikaModel model;

    public FarmRenderer(Look<T> look) {
        this.look = look;
        this.model = new ReikaModel(look.model());
    }

    private static ResourceLocation texture(String name) {
        return RotaryCraft.id("textures/machine/" + name + ".png");
    }

    private void draw(T be, PoseStack pose, MultiBufferSource buffers, int light, int overlay, Direction facing, float phi) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(texture(be == null ? look.texture().apply(null) : look.texture().apply(be))));
        pose.pushPose();
        ReikaModel.enterModelSpace(pose);
        if (look.facing()) {
            turn(pose, facing);
        }
        if (look.axis() == null) {
            model.renderAll(pose, vc, light, overlay);
        } else {
            model.render(pose, vc, light, overlay, look.fixed().toArray(String[]::new));
            pose.pushPose();
            pose.translate(0, look.pivotY(), 0);
            pose.mulPose(look.axis().rotationDegrees(phi));
            pose.translate(0, -look.pivotY(), 0);
            String[] moving = model.names().stream().filter(n -> !look.fixed().contains(n)).toArray(String[]::new);
            model.render(pose, vc, light, overlay, moving);
            pose.popPose();
        }
        pose.popPose();
    }

    /** The original's rotation of its models for each of the six sides it can face. */
    public static void turn(PoseStack pose, Direction facing) {
        switch (facing) {
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(270));
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> {
            }
            case UP -> {
                pose.mulPose(Axis.XP.rotationDegrees(270));
                pose.translate(0, -1, 1);
            }
            case DOWN -> {
                pose.mulPose(Axis.XP.rotationDegrees(90));
                pose.translate(0, -1, 1);
                pose.translate(0, 0, -2);
            }
        }
    }

    @Override
    public void render(T be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() != null && look.axis() != null) {
            long now = be.getLevel().getGameTime();
            if (be.phiTime != now) {
                if (be.phiTime != 0 && now - be.phiTime < 40) {
                    be.phi = (float) ((be.phi + (now - be.phiTime) * look.speed().applyAsDouble(be)) % 360);
                }
                be.phiTime = now;
            }
        }
        float phi = be.phi + (float) (look.speed().applyAsDouble(be) * partialTick);
        draw(be, pose, buffers, light, overlay, be.facing(), phi);
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(T be) {
        return new net.minecraft.world.phys.AABB(be.getBlockPos()).inflate(1);
    }

    /** The item form. */
    public static class Item<T extends FarmBlockEntity> extends BlockEntityWithoutLevelRenderer {
        private final FarmRenderer<T> renderer;
        private final String texture;

        public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models, FarmRenderer<T> renderer, String texture) {
            super(dispatcher, models);
            this.renderer = renderer;
            this.texture = texture;
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(texture(texture)));
            pose.pushPose();
            ReikaModel.enterModelSpace(pose);
            if (renderer.look.facing()) {
                turn(pose, Direction.SOUTH);
            }
            renderer.model.renderAll(pose, vc, light, overlay);
            pose.popPose();
        }
    }
}
