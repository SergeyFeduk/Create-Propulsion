package com.deltasf.createpropulsion.balloons.injectors.hot_air_pump;

import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class HotAirPumpRenderer extends KineticBlockEntityRenderer<HotAirPumpBlockEntity>  {
    public HotAirPumpRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    //TODO: 
    @Override
    protected void renderSafe(HotAirPumpBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        
        //TODO: Fan
        SuperByteBuffer cogModel = CachedBuffers.partialFacing(PropulsionPartialModels.HOT_AIR_PUMP_COG, state, Direction.NORTH);
        SuperByteBuffer membraneModel = CachedBuffers.partialFacing(PropulsionPartialModels.HOT_AIR_PUMP_MEMBRANE, state, Direction.NORTH);
        SuperByteBuffer meshModel = CachedBuffers.partialFacing(PropulsionPartialModels.HOT_AIR_PUMP_MESH, state, Direction.NORTH);
        
        standardKineticRotationTransform(cogModel, be, light).renderInto(ms, buffer.getBuffer(RenderType.solid()));

        VertexConsumer cutoutBuffer = buffer.getBuffer(RenderType.cutout());

        membraneModel.light(light).overlay(overlay).renderInto(ms, cutoutBuffer);
        meshModel.light(light).overlay(overlay).renderInto(ms, cutoutBuffer);
    }
}
