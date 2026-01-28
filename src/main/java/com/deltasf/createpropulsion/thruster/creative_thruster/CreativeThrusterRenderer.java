package com.deltasf.createpropulsion.thruster.creative_thruster;

import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Vector3f;

public class CreativeThrusterRenderer extends SmartBlockEntityRenderer<CreativeThrusterBlockEntity> {
    public CreativeThrusterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(CreativeThrusterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        BlockState state = be.getBlockState();
        Direction facing = state.getValue(CreativeThrusterBlock.FACING);
        Direction placementFacing = state.getValue(CreativeThrusterBlock.PLACEMENT_FACING);

        if (facing.getAxis() == placementFacing.getAxis()) {
            return;
        }

        SuperByteBuffer bracket = CachedBuffers.partial(PropulsionPartialModels.CREATIVE_THRUSTER_BRACKET, state);
        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());

        float angle = getBracketAngle(facing, placementFacing);

        ms.pushPose();
        
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(facing.getRotation());
        ms.mulPose(Axis.XP.rotationDegrees(90));
        ms.mulPose(Axis.ZP.rotationDegrees(angle));
        ms.translate(-0.5, -0.5, -0.5);

        bracket.light(light).overlay(overlay).renderInto(ms, vb);

        ms.popPose();
    }

    //:)
    private float getBracketAngle(Direction facing, Direction placementFacing) {
        Vector3f local = new Vector3f(placementFacing.step());
        local.rotate(facing.getRotation().conjugate());
        local.rotateX((float) Math.toRadians(-90));
        double targetAngle = Math.toDegrees(Math.atan2(local.y, local.x));
        return (float) (targetAngle + 90);
    }
}