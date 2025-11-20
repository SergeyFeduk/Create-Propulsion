package com.deltasf.createpropulsion.heat.engine;

import org.joml.Vector4f;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.deltasf.createpropulsion.utility.math.MathUtility;
import com.jozufozu.flywheel.backend.Backend;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class StirlingEngineRenderer extends KineticBlockEntityRenderer<StirlingEngineBlockEntity> {
    private final static int[] offsetArray = {0, 7, 2, 9};

    public StirlingEngineRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public void renderSafe(StirlingEngineBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        if (Backend.canUseInstancing(blockEntity.getLevel())) return;

        Direction direction = blockEntity.getBlockState().getValue(StirlingEngineBlock.FACING);
        SuperByteBuffer shaft = CachedBufferer.partialFacing(AllPartialModels.SHAFT_HALF, blockEntity.getBlockState(), direction);
        standardKineticRotationTransform(shaft, blockEntity, light).renderInto(ms, bufferSource.getBuffer(RenderType.solid()));

        float speed = blockEntity.getSpeed() / StirlingEngineBlockEntity.MAX_GENERATED_RPM;
        renderPistons(blockEntity, partialTicks, ms, bufferSource, light, overlay, direction, speed);
    }

    private void renderPistons(StirlingEngineBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay, Direction direction, float speed) {
        BlockState state = blockEntity.getBlockState();
        VertexConsumer cutoutVB = bufferSource.getBuffer(RenderType.cutoutMipped());
        float time = AnimationTickHolder.getRenderTime(blockEntity.getLevel());

        SuperByteBuffer pistonModel = CachedBufferer.partial(PropulsionPartialModels.STIRLING_ENGINE_PISTON, state);

        float timeSeconds = time / 20.0f;
        float effectiveRevolutionPeriod = Float.MAX_VALUE;
        if (speed > MathUtility.epsilon) {
            double revolutionPeriod = PropulsionConfig.STIRLING_REVOLUTION_PERIOD.get();
            effectiveRevolutionPeriod = (float)revolutionPeriod / speed;
        }
        double crankRadius = PropulsionConfig.STIRLING_CRANK_RADIUS.get();
        double conrodLength = PropulsionConfig.STIRLING_CONROD_LENGTH.get();
        Vector4f normalizedExtensions = calculateExtensions(timeSeconds, (float)crankRadius, (float)conrodLength, effectiveRevolutionPeriod);

        for (int i = 0; i < 4; i++) {
            float normalized;
            if (i == 0) normalized = normalizedExtensions.x;
            else if (i == 1) normalized = normalizedExtensions.y;
            else if (i == 2) normalized = normalizedExtensions.z;
            else normalized = normalizedExtensions.w;

            final float offsetDistance = 2 / 16.0f;
            float offset = Math.min(offsetDistance - 0.001f, normalized * offsetDistance); //Avoid z-fighting

            ms.pushPose();
            ms.translate(0.5, 0.5, 0.5);
            ms.mulPose(direction.getRotation());
            
            if (i >= 2) {
                ms.mulPose(Axis.ZP.rotationDegrees(180));
            }

            ms.mulPose(Axis.XP.rotationDegrees(270));
            ms.translate(-0.5, -0.5, -0.5);

            ms.translate(offset, 0, offsetArray[i] / 16.0f);
            pistonModel.light(light).overlay(overlay).renderInto(ms, cutoutVB);
            ms.popPose();
        }
    }

    public static Vector4f calculateExtensions(float time, float crankRadius, float conrodLength, float revolutionPeriod, float[] phases) {
        float[] phaseOffsets = phases;
        float angularSpeed = 2.0f * (float)Math.PI / revolutionPeriod;
        float[] crankAngles = new float[4];
        for (int i = 0; i < 4; ++i) {
            crankAngles[i] = angularSpeed * time + phaseOffsets[i];
        }
        float[] extensions = new float[4];
        for (int i = 0; i < 4; ++i) {
            float sinTheta = (float)Math.sin(crankAngles[i]);
            float cosTheta = (float)Math.cos(crankAngles[i]);
            float underSqrt = conrodLength * conrodLength - (crankRadius * sinTheta) * (crankRadius * sinTheta);
            float pistonDisplacement = crankRadius * (1.0f - cosTheta) + conrodLength - (float)Math.sqrt(underSqrt);
            float normalizedExtension = pistonDisplacement / (2.0f * crankRadius);
            extensions[i] = normalizedExtension;
        }
        return new Vector4f(extensions[0], extensions[1], extensions[2], extensions[3]);
    }

    public static Vector4f calculateExtensions(float time, float crankRadius, float conrodLength, float revolutionPeriod) {
        return calculateExtensions(time, crankRadius, conrodLength, revolutionPeriod, new float[] { 0.0f, (float)Math.PI, 2.0f * (float)Math.PI, 3.0f * (float)Math.PI });
    }
}
