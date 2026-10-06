package net.scwunge.rotarycraft.client.weapon;

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
import net.minecraft.world.phys.AABB;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.weapon.turret.TurretBlockEntity;

/**
 * A turret drawn from the original's model: the base, then the parts that turn with the aim (phi, about the vertical), then the
 * barrel parts that also tilt with it (theta), each about the pivot one block up, as the original's renderAll. Hanging turrets
 * are drawn upside down.
 */
public class TurretRenderer<T extends TurretBlockEntity> implements BlockEntityRenderer<T> {
    private final Look look;

    public TurretRenderer(Look look) {
        this.look = look;
    }

    /** The model, its texture and which parts turn and tilt. */
    public record Look(ReikaModel model, ResourceLocation texture, String[] base, String[] turning, String[] tilting) {
        public Look(String model, String texture, String[] base, String[] turning, String[] tilting) {
            this(new ReikaModel(model), RotaryCraft.id("textures/machine/" + texture + ".png"), base, turning, tilting);
        }

        void draw(PoseStack pose, MultiBufferSource buffers, float phi, float theta, boolean hanging, int light, int overlay) {
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
            pose.pushPose();
            ReikaModel.enterModelSpace(pose);
            if (hanging) {
                pose.translate(0, 2, 0);
                pose.scale(1, -1, 1);
            }
            model.render(pose, vc, light, overlay, base);
            pose.translate(0, 1, 0);
            pose.mulPose(Axis.YP.rotationDegrees(-phi));
            pose.translate(0, -1, 0);
            model.render(pose, vc, light, overlay, turning);
            pose.translate(0, 1, 0);
            pose.mulPose(Axis.XP.rotationDegrees(hanging ? theta : -theta));
            pose.translate(0, -1, 0);
            model.render(pose, vc, light, overlay, tilting);
            pose.popPose();
        }
    }

    @Override
    public void render(T turret, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        look.draw(pose, buffers, turret.phi, turret.theta, turret.dir() == -1, light, overlay);
    }

    @Override
    public AABB getRenderBoundingBox(T turret) {
        return new AABB(turret.getBlockPos()).inflate(1, 2, 1);
    }

    /** The turret as an item: standing, pointing straight ahead. */
    public static class Item extends BlockEntityWithoutLevelRenderer {
        private final Look look;

        public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models, Look look) {
            super(dispatcher, models);
            this.look = look;
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            pose.pushPose();
            pose.translate(0, -0.25, 0);
            look.draw(pose, buffers, 0, 0, false, light, overlay);
            pose.popPose();
        }
    }
}
