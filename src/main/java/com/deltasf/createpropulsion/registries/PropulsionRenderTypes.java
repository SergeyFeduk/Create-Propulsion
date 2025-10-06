package com.deltasf.createpropulsion.registries;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderType;

public class PropulsionRenderTypes extends RenderType {
    private PropulsionRenderTypes(String name, VertexFormat fmt, VertexFormat.Mode mode, int bufSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, fmt, mode, bufSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    public static final RenderType SOLID_TRANSLUCENT_BEAM = create(
        "solid_translucent_beam",
        DefaultVertexFormat.POSITION_COLOR_NORMAL, 
        VertexFormat.Mode.QUADS,
        256, 
        false,
        true,
        CompositeState.builder()
            .setShaderState(POSITION_COLOR_SHADER)
            .setTextureState(NO_TEXTURE)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(CULL)
            .setLightmapState(NO_LIGHTMAP)
            .setOverlayState(NO_OVERLAY)
            .setOutputState(TRANSLUCENT_TARGET)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false)
    );

    public static final RenderType PROPELLER_BLUR = create(
        "propeller_blur",
        DefaultVertexFormat.BLOCK,
        VertexFormat.Mode.QUADS,
        256,
        false,
        true,
        CompositeState.builder()
            .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
            .setTextureState(BLOCK_SHEET_MIPPED)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .setCullState(NO_CULL)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(true)
    );

}
