package net.scwunge.rotarycraft.client.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.scwunge.rotarycraft.weapon.TurretShot;

import java.util.function.Supplier;

/** A turret shot drawn as an item facing the camera (the anti-air flak is a piece of scrap, as the original draws it). */
public class ItemShotRenderer extends EntityRenderer<TurretShot> {
    private final Supplier<ItemStack> stack;

    public ItemShotRenderer(EntityRendererProvider.Context context, Supplier<ItemStack> stack) {
        super(context);
        this.stack = stack;
    }

    @Override
    public void render(TurretShot shot, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.scale(0.8f, 0.8f, 0.8f);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack.get(), ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose, buffers,
                shot.level(), shot.getId());
        pose.popPose();
        super.render(shot, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(TurretShot shot) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
