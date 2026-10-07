package net.scwunge.rotarycraft.client.machine;

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
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.client.weapon.ReikaModel;

import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * A machine drawn from one of the original's models, with the moving parts it had. The model's own draw program (see tools/modelanim.py) turns
 * its parts by an angle {@code phi} that grows by {@link Look#speed} degrees each tick (the original's animation, which the renderers
 * kept as the block entity's phi), and the whole model is turned to the way the block faces by the original's table for that kind of machine.
 */
public class MachineRenderer<T extends PowerBlockEntity> implements BlockEntityRenderer<T> {
    /** Yaw in degrees for a block facing WEST, EAST, NORTH, SOUTH (the way each original's renderer turned its model). */
    public static final float[] BEAM = {90, 270, 180, 0};
    public static final float[] ENGINE = {0, 180, 90, 270};

    /**
     * How a machine looks.
     *
     * @param model   the model, under reika_models
     * @param texture the texture name under textures/machine
     * @param yaws    the table by facing (WEST, EAST, NORTH, SOUTH), or null if the model is never turned
     * @param offset  degrees added to the table, for a model that faces another way
     * @param speed   how far the model's moving parts turn each tick, in degrees (zero when it is still)
     * @param sign    1 or -1: the way they turn
     * @param flags   the booleans the original's renderer handed its model
     */
    public record Look<T extends PowerBlockEntity>(String model, Function<T, String> texture, float[] yaws, float offset, ToDoubleFunction<T> speed, int sign,
                                                   Function<T, boolean[]> flags) {
        public static <T extends PowerBlockEntity> Look<T> still(String model, String texture, float[] yaws) {
            return new Look<>(model, be -> texture, yaws, 0, be -> 0, 1, be -> new boolean[0]);
        }

        public static <T extends PowerBlockEntity> Look<T> spinning(String model, String texture, float[] yaws, ToDoubleFunction<T> speed, int sign) {
            return new Look<>(model, be -> texture, yaws, 0, speed, sign, be -> new boolean[0]);
        }

        public Look<T> turned(float degrees) {
            return new Look<>(model, texture, yaws, degrees, speed, sign, flags);
        }

        public Look<T> textured(Function<T, String> texture) {
            return new Look<>(model, texture, yaws, offset, speed, sign, flags);
        }

        public Look<T> withFlags(Function<T, boolean[]> flags) {
            return new Look<>(model, texture, yaws, offset, speed, sign, flags);
        }
    }

    private final Look<T> look;
    private final ReikaModel model;

    public MachineRenderer(Look<T> look) {
        this.look = look;
        this.model = new ReikaModel(look.model());
    }

    private static ResourceLocation texture(String name) {
        return RotaryCraft.id("textures/machine/" + name + ".png");
    }

    /** The original's turning of the model for the way the block faces. */
    static void turn(PoseStack pose, float[] yaws, float offset, Direction facing) {
        switch (facing) {
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(yaws[0] + offset));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(yaws[1] + offset));
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(yaws[2] + offset));
            case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(yaws[3] + offset));
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

    private void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, String texture, Direction facing, double phi, boolean[] flags) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(texture(texture)));
        pose.pushPose();
        ReikaModel.enterModelSpace(pose);
        if (look.yaws() != null) {
            turn(pose, look.yaws(), look.offset(), facing);
        }
        model.renderAnimated(pose, vc, light, overlay, look.sign() * phi, 0, flags);
        pose.popPose();
    }

    @Override
    public void render(T be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        double speed = look.speed().applyAsDouble(be);
        if (be.getLevel() != null) {
            long now = be.getLevel().getGameTime();
            if (be.phiTime != now) {
                if (be.phiTime != 0 && now - be.phiTime < 40) {
                    be.phi = (float) ((be.phi + (now - be.phiTime) * speed) % 360);
                }
                be.phiTime = now;
            }
        }
        draw(pose, buffers, light, overlay, look.texture().apply(be), be.facing(), be.phi + speed * partialTick, look.flags().apply(be));
    }

    @Override
    public AABB getRenderBoundingBox(T be) {
        return new AABB(be.getBlockPos()).inflate(1);
    }

    /** The item form: the model standing still, as the original drew its items. */
    public static class Item<T extends PowerBlockEntity> extends BlockEntityWithoutLevelRenderer {
        private final MachineRenderer<T> renderer;
        private final String texture;
        private final boolean[] flags;

        public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models, MachineRenderer<T> renderer, String texture, boolean... flags) {
            super(dispatcher, models);
            this.renderer = renderer;
            this.texture = texture;
            this.flags = flags;
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            renderer.draw(pose, buffers, light, overlay, texture, Direction.SOUTH, 0, flags);
        }
    }
}
