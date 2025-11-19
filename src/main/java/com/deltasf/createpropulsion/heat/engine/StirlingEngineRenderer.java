package com.deltasf.createpropulsion.heat.engine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

public class StirlingEngineRenderer extends KineticBlockEntityRenderer<StirlingEngineBlockEntity> {
    
    public StirlingEngineRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public void renderSafe(StirlingEngineBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        Direction direction = blockEntity.getBlockState().getValue(StirlingEngineBlock.FACING);
        SuperByteBuffer shaft = CachedBufferer.partialFacing(AllPartialModels.SHAFT_HALF, blockEntity.getBlockState(), direction);
        
        standardKineticRotationTransform(shaft, blockEntity, light).renderInto(ms, bufferSource.getBuffer(RenderType.solid()));
    }
}
