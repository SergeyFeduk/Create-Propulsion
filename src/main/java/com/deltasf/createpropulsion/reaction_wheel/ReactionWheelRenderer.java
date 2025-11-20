package com.deltasf.createpropulsion.reaction_wheel;

import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class ReactionWheelRenderer extends KineticBlockEntityRenderer<ReactionWheelBlockEntity> {

    public ReactionWheelRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @SuppressWarnings("null")
    @Override
    protected void renderSafe(ReactionWheelBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Direction direction = be.getBlockState().getValue(ReactionWheelBlock.FACING);
        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
        int lightBehind = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(direction));
        SuperByteBuffer shaftHalf = CachedBufferer.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), direction);

        //Shaft
        standardKineticRotationTransform(shaftHalf, be, lightBehind).renderInto(ms, vb);
        //Core
        renderCore(be, partialTicks, ms, buffer, light, overlay);
    }

    private void renderCore(ReactionWheelBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        Direction direction = state.getValue(ReactionWheelBlock.FACING);

        SuperByteBuffer coreModel = CachedBufferer.partial(PropulsionPartialModels.REACTION_WHEEL_CORE, state);
        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());

        //TODO: Very likely that wheel weird shading is caused by its rotation. Maybe try remove disableDiffuse and change rotation (270 or smth like that?)
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(direction.getRotation());
        ms.mulPose(Axis.XP.rotationDegrees(90));
        ms.translate(-0.5, -0.5, -0.5);
        coreModel.disableDiffuse().light(light).overlay(overlay).color(255, 255, 255, 255).renderInto(ms, vb);
        ms.popPose();
    }
}
