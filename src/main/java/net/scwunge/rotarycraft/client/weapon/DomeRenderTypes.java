package net.scwunge.rotarycraft.client.weapon;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.scwunge.rotarycraft.RotaryCraft;

/** The dome field's render type: the pattern is added to what is behind it (the original's additive blending), unlit and seen from both sides. */
public final class DomeRenderTypes extends RenderStateShard {
    public static final RenderType FIELD = RenderType.create("rotarycraft_dome_field", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1 << 16, false, true,
            RenderType.CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/effect/forcefield.png"), false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).setOverlayState(OVERLAY)
                    .createCompositeState(false));

    private DomeRenderTypes() {
        super("rotarycraft_dome", () -> {}, () -> {});
    }
}
