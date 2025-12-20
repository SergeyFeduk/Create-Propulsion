package com.deltasf.createpropulsion.heat.burners.liquid;

import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlock;
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
import net.minecraft.world.level.block.state.BlockState;

public class LiquidBurnerRenderer extends SmartBlockEntityRenderer<LiquidBurnerBlockEntity> {
    public LiquidBurnerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
	protected void renderSafe(LiquidBurnerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        Direction direction = state.getValue(AbstractBurnerBlock.FACING).getOpposite();

        VertexConsumer solidVB = buffer.getBuffer(RenderType.cutoutMipped());
        SuperByteBuffer leverModel = CachedBuffers.partial(PropulsionPartialModels.LIQUID_BURNER_FAN, state);

        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(direction)));
        ms.translate(-0.5, -0.5, -0.5);
        leverModel.light(light).overlay(overlay).renderInto(ms, solidVB);
        ms.popPose();
    }
}
