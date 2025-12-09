package com.deltasf.createpropulsion.tilt_adapter;

import org.joml.Vector3f;

import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TiltAdapterRenderer extends KineticBlockEntityRenderer<TiltAdapterBlockEntity> {
    public TiltAdapterRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

    @Override
	protected void renderSafe(TiltAdapterBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        //if (VisualizationManager.supportsVisualization(blockEntity.getLevel())) return;

        Level level = blockEntity.getLevel();
        if (level == null) return;

        Block block = blockEntity.getBlockState().getBlock();
		final Axis axis = ((IRotate) block).getRotationAxis(blockEntity.getBlockState());
		final BlockPos pos = blockEntity.getBlockPos();
        final BlockState blockState = blockEntity.getBlockState();
        final Direction direction = TiltAdapterBlock.getDirection(blockState);
        final Direction invDirection = direction.getOpposite();
        final boolean alignedX = blockState.getValue(TiltAdapterBlock.ALIGNED_X);
        final boolean positive = blockState.getValue(TiltAdapterBlock.POSITIVE);
		float time = AnimationTickHolder.getRenderTime(level);

        SuperByteBuffer inputShaft = CachedBuffers.partialFacing(PropulsionPartialModels.TILT_ADAPTER_INPUT_SHAFT, blockState, invDirection);
        SuperByteBuffer outputShaft = CachedBuffers.partialFacing(PropulsionPartialModels.TILT_ADAPTER_OUTPUT_SHAFT, blockState, invDirection);
        
        float angle = (time * blockEntity.getSpeed() * 3f / 10) % 360;
        float offset = getRotationOffsetForPosition(blockEntity, pos, axis);
        float outputModifier = blockEntity.getRotationSpeedModifier(direction);
        //TODO: ALLOC 
        Vector3f aomInput = new Vector3f(angle, offset, 1);
        Vector3f aomOutput = new Vector3f(angle, offset, outputModifier);

        renderShaft(aomInput, inputShaft, blockEntity, axis, light, ms, buffer);
        renderShaft(aomOutput, outputShaft, blockEntity, axis, light, ms, buffer);
        renderOverlays(blockEntity, blockState, invDirection, aomOutput, light, ms, buffer, direction, alignedX, positive);
    }

    private void renderShaft(Vector3f aom, SuperByteBuffer shaft, TiltAdapterBlockEntity blockEntity, Axis axis, int light, PoseStack ms, MultiBufferSource buffer) {
        float angle = getAngle(aom);
        kineticRotationTransform(shaft, blockEntity, axis, angle, light);
        shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    private void renderOverlays(TiltAdapterBlockEntity blockEntity, BlockState blockState, Direction invDirection, Vector3f aom, int light, PoseStack ms, MultiBufferSource buffer, Direction direction, boolean alignedX, boolean positive) {
        float redstoneLeft = blockEntity.getLeft() / 15.0f;
        float redstoneRight = blockEntity.getRight() / 15.0f;

        float angle = getAngle(aom);

        SuperByteBuffer gantry = CachedBuffers.partialFacing(PropulsionPartialModels.TILT_ADAPTER_GANTRY, blockState, invDirection);
        SuperByteBuffer sideOverlay = CachedBuffers.partialFacing(PropulsionPartialModels.TILT_ADAPTER_SIDE_INDICATOR, blockState, invDirection);

        renderOverlaySide(redstoneLeft, angle, gantry, sideOverlay, light, ms, buffer, direction, true, alignedX, positive);
        renderOverlaySide(redstoneRight, angle, gantry, sideOverlay, light, ms, buffer, direction, false, alignedX, positive);
    }

    private void renderOverlaySide(float redstoneSignal, float angle, SuperByteBuffer gantry, SuperByteBuffer sideOverlay, int light, PoseStack ms, MultiBufferSource buffer, Direction direction, boolean isRight, boolean alignedX, boolean positive) {
        int color = Color.mixColors(0x470102, 0xCD0000, redstoneSignal);
        com.mojang.math.Axis rotationAxis = direction.getAxis().isHorizontal() ? com.mojang.math.Axis.YP : com.mojang.math.Axis.ZP;
        boolean flipRightCondition = direction.getAxis().isVertical() && (!positive ^ alignedX);
        boolean shouldFlip = isRight ^ flipRightCondition;
        float offset = shouldFlip ? 1/16.0f : -1/16.0f;

        ms.pushPose();
        //Local
        ms.translate(0.5, 0.5, 0.5);
        if (alignedX) {
            ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0f));
        }
        
        if (shouldFlip) {
            ms.mulPose(rotationAxis.rotationDegrees(180.0f));
        }
        ms.translate(-0.5, -0.5, -0.5);
        ms.translate(direction.getStepX() * offset, direction.getStepY() * offset, direction.getStepZ() * offset);
        sideOverlay.color(color).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutout()));
        ms.popPose();
    }

    private float getAngle(Vector3f aom) {
        float angle = aom.x;
        angle *= aom.z;
        angle += aom.y;
        angle = angle / 180f * (float) Math.PI;
        return angle;
    }
}
