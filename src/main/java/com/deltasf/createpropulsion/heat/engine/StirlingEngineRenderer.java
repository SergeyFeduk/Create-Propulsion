package com.deltasf.createpropulsion.heat.engine;

import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class StirlingEngineRenderer extends KineticBlockEntityRenderer<StirlingEngineBlockEntity> {
    public StirlingEngineRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    private final static int[] offsetArray = {0, 7, 2, 9};
    
    @Override
    public void renderSafe(StirlingEngineBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        Direction direction = blockEntity.getBlockState().getValue(StirlingEngineBlock.FACING);
        SuperByteBuffer shaft = CachedBufferer.partialFacing(AllPartialModels.SHAFT_HALF, blockEntity.getBlockState(), direction);
        
        standardKineticRotationTransform(shaft, blockEntity, light).renderInto(ms, bufferSource.getBuffer(RenderType.solid()));
        renderPistons(blockEntity, partialTicks, ms, bufferSource, light, overlay);
    }

    private void renderPistons(StirlingEngineBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        BlockState state = blockEntity.getBlockState();
        Direction direction = state.getValue(StirlingEngineBlock.FACING);
        VertexConsumer cutoutVB = bufferSource.getBuffer(RenderType.cutoutMipped());

        SuperByteBuffer pistonModel = CachedBufferer.partial(PropulsionPartialModels.STIRLING_ENGINE_PISTON, state);

        for (int i = 0; i < 4; i++) {
            ms.pushPose();
            ms.translate(0.5, 0.5, 0.5);
            ms.mulPose(direction.getRotation());
            
            if (i >= 2) {
                ms.mulPose(Axis.ZP.rotationDegrees(180));
            }

            ms.mulPose(Axis.XP.rotationDegrees(270));
            ms.translate(-0.5, -0.5, -0.5);

            ms.translate(0,0, offsetArray[i] / 16.0f);
            pistonModel.light(light).overlay(overlay).renderInto(ms, cutoutVB);
            ms.popPose();
        }
    }
}
