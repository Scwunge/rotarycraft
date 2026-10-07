package net.scwunge.rotarycraft.client.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.scwunge.rotarycraft.blockentity.ObsidianMakerBlockEntity;

/** The Obsidian Maker's model, with the water, lava or (with both) the setting obsidian inside it. */
public class ObsidianMakerRenderer extends ModelMachineRenderer<ObsidianMakerBlockEntity> {
    private static final double MAX_HEIGHT = 0.6875;
    private static final ResourceLocation OBSIDIAN = ResourceLocation.withDefaultNamespace("block/obsidian");

    public ObsidianMakerRenderer() {
        super("obsidian_maker", "obsidian_maker");
    }

    @Override
    protected void drawExtras(ObsidianMakerBlockEntity machine, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        int water = machine.waterTank().getFluidAmount();
        int lava = machine.lavaTank().getFluidAmount();
        if (water > 0 && lava > 0) {
            surface(pose, buffers, OBSIDIAN, 0xFFFFFFFF, MAX_HEIGHT, 0.0625, light);
        } else if (water > 0) {
            fluidSurface(pose, buffers, Fluids.WATER, MAX_HEIGHT * water / ObsidianMakerBlockEntity.CAPACITY, 0.0625, light);
        } else if (lava > 0) {
            fluidSurface(pose, buffers, Fluids.LAVA, MAX_HEIGHT * lava / ObsidianMakerBlockEntity.CAPACITY, 0.0625, light);
        }
    }
}
