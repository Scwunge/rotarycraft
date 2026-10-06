package net.scwunge.rotarycraft.client.survey;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/** Render types drawn through everything: the Cave Scanner's points are seen through the rock they outline. */
public final class CaveRenderTypes extends RenderStateShard {
    public static final RenderType POINTS = RenderType.create("rotarycraft_cave_points", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 1 << 16,
            false, false, RenderType.CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));

    private CaveRenderTypes() {
        super("rotarycraft_cave", () -> {}, () -> {});
    }
}
