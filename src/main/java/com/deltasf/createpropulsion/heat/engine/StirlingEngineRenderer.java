package com.deltasf.createpropulsion.heat.engine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class StirlingEngineRenderer extends KineticBlockEntityRenderer<StirlingEngineBlockEntity> {
    
    public StirlingEngineRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public void renderSafe(StirlingEngineBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        
    }
}
