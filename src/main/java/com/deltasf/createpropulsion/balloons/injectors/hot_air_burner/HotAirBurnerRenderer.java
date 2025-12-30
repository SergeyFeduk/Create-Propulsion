package com.deltasf.createpropulsion.balloons.injectors.hot_air_burner;

import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class HotAirBurnerRenderer extends SmartBlockEntityRenderer<HotAirBurnerBlockEntity> {
    private static final int horizontalOffsetVoxels = 3;
    private static final int verticalOffsetVoxels = 1;

    public HotAirBurnerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
	protected void renderSafe(HotAirBurnerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        BlockState state = be.getBlockState();
        Direction direction = state.getValue(HotAirBurnerBlock.FACING).getOpposite();

        VertexConsumer solidVB = buffer.getBuffer(RenderType.solid());
        SuperByteBuffer leverModel = CachedBuffers.partial(PropulsionPartialModels.HOT_AIR_BURNER_LEVER, state);

        //Calculate translation & rotation
        int leverPosition = be.getLeverPosition();

        float horizontalOffset = leverPosition * horizontalOffsetVoxels / 16.0f;
        float verticalOffset = leverPosition * verticalOffsetVoxels / 16.0f;

        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(direction)));
        ms.translate(0, 0, 1.0);
        ms.translate(horizontalOffset, verticalOffset, 0);
        ms.translate(-0.5, -0.5, -0.5);
        leverModel.light(light).overlay(overlay).renderInto(ms, solidVB);
        ms.popPose();
    }
}
