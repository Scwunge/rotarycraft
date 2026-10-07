package net.scwunge.rotarycraft.client.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.client.weapon.ReikaModel;
import org.joml.Matrix4f;

/**
 * Draws one of the original's machine models for a block entity (and, as an item, in the hand and the inventory): the model is entered the way the
 * original's renderers did, then {@link #draw} draws its parts, with whatever the machine animates. Water, lava and other fluids can be drawn inside
 * with {@link #fluidSurface}.
 */
public class ModelMachineRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    protected final ReikaModel model;
    protected final ResourceLocation texture;

    public ModelMachineRenderer(String modelName, String textureName) {
        this.model = new ReikaModel(modelName);
        this.texture = RotaryCraft.id("textures/machine/" + textureName + ".png");
    }

    /** The texture to draw {@code machine} with (null for the item): machines with more than one model override this. */
    protected ResourceLocation textureFor(T machine) {
        return texture;
    }

    /** Draws the model's parts, in model space. {@code machine} is null for the item. */
    protected void draw(T machine, float partialTick, PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        model.renderAll(pose, buffer, light, overlay);
    }

    /** Anything drawn in block space after the model (fluid surfaces, glows). */
    protected void drawExtras(T machine, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
    }

    /** Turns the model, in model space (as the original's glRotatef after its translate and scale), for machines that face somewhere. */
    protected void orient(T machine, PoseStack pose) {
    }

    @Override
    public void render(T machine, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        ReikaModel.enterModelSpace(pose);
        orient(machine, pose);
        draw(machine, partialTick, pose, buffers.getBuffer(RenderType.entityCutoutNoCull(textureFor(machine))), light, overlay);
        pose.popPose();
        drawExtras(machine, partialTick, pose, buffers, light, overlay);
    }

    /**
     * The common orientation of the original's four-sided machines: the degrees (about the model's vertical axis, in model space) for a machine
     * whose output side is {@code facing}: east 90, south 180, west 270, north 0.
     */
    public static float sideYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }

    /** A flat sheet of fluid at height {@code h} across the block, in block space, drawn at full brightness for glowing fluids. */
    protected static void fluidSurface(PoseStack pose, MultiBufferSource buffers, Fluid fluid, double h, double inset, int light) {
        if (fluid == Fluids.EMPTY) {
            return;
        }
        FluidStack stack = new FluidStack(fluid, 1000);
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
        surface(pose, buffers, ext.getStillTexture(stack), ext.getTintColor(stack), h, inset, fluid.getFluidType().getLightLevel() > 8 ? 0xF000F0 : light);
    }

    /** A flat sheet textured with a block-atlas sprite and a tint (ARGB), at height {@code h}. */
    protected static void surface(PoseStack pose, MultiBufferSource buffers, ResourceLocation spriteLocation, int tint, double h, double inset, int light) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(spriteLocation);
        int r = tint >> 16 & 0xFF;
        int g = tint >> 8 & 0xFF;
        int b = tint & 0xFF;
        VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
        Matrix4f m = pose.last().pose();
        float x0 = (float) inset;
        float x1 = (float) (1 - inset);
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float y = (float) h;
        vc.addVertex(m, x0, y, x1).setColor(r, g, b, 255).setUv(u0, v1).setLight(light).setNormal(pose.last(), 0, 1, 0).setOverlay(OverlayTexture.NO_OVERLAY);
        vc.addVertex(m, x1, y, x1).setColor(r, g, b, 255).setUv(u1, v1).setLight(light).setNormal(pose.last(), 0, 1, 0).setOverlay(OverlayTexture.NO_OVERLAY);
        vc.addVertex(m, x1, y, x0).setColor(r, g, b, 255).setUv(u1, v0).setLight(light).setNormal(pose.last(), 0, 1, 0).setOverlay(OverlayTexture.NO_OVERLAY);
        vc.addVertex(m, x0, y, x0).setColor(r, g, b, 255).setUv(u0, v0).setLight(light).setNormal(pose.last(), 0, 1, 0).setOverlay(OverlayTexture.NO_OVERLAY);
    }

    /** The item form: the model drawn at rest. */
    public static class ItemForm<T extends BlockEntity> extends BlockEntityWithoutLevelRenderer {
        private final ModelMachineRenderer<T> renderer;

        public ItemForm(BlockEntityRenderDispatcher dispatcher, EntityModelSet models, ModelMachineRenderer<T> renderer) {
            super(dispatcher, models);
            this.renderer = renderer;
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            pose.pushPose();
            pose.translate(0, -0.25, 0);
            pose.scale(1.125f, 1.125f, 1.125f);
            ReikaModel.enterModelSpace(pose);
            renderer.draw(null, 0, pose, buffers.getBuffer(RenderType.entityCutoutNoCull(renderer.textureFor(null))), light, overlay);
            pose.popPose();
        }
    }
}
