package com.deltasf.createpropulsion.heat.burners.liquid;

import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlock;
import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class LiquidBurnerRenderer extends SmartBlockEntityRenderer<LiquidBurnerBlockEntity> {
    private static final float FAN_SPEED = 0.5f;

    public LiquidBurnerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
	protected void renderSafe(LiquidBurnerBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = blockEntity.getBlockState();
        Level level = blockEntity.getLevel();
        if (level == null) return;
        Direction direction = state.getValue(AbstractBurnerBlock.FACING).getOpposite();

        VertexConsumer solidVB = buffer.getBuffer(RenderType.cutoutMipped());
        SuperByteBuffer fanModel = CachedBuffers.partial(PropulsionPartialModels.LIQUID_BURNER_FAN, state);

        //Fan rotation
        float time = AnimationTickHolder.getRenderTime(level);
        if (blockEntity.lastRenderTime == -1) blockEntity.lastRenderTime = time;
        float dt = time - blockEntity.lastRenderTime;
        blockEntity.lastRenderTime = time;

        if (blockEntity.isFanSpinning()) {
            blockEntity.fanAngle += dt * FAN_SPEED;
            blockEntity.fanAngle %= (float) (Math.PI * 2);
        }

        //Guh
        ms.pushPose();
        
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(direction)));

        ms.translate(0, -2/16.0f, 0);
        ms.mulPose(Axis.XP.rotation(-blockEntity.fanAngle));
        ms.translate(0, 2/16.0f, 0);
        
        ms.translate(-0.5, -0.5, -0.5);

        fanModel.light(light).overlay(overlay).renderInto(ms, solidVB);
        ms.popPose();
    }
}
