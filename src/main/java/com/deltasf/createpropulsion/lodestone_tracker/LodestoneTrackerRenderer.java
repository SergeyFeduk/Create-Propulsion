package com.deltasf.createpropulsion.lodestone_tracker;

import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.deltasf.createpropulsion.utility.Bakery;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.Color;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class LodestoneTrackerRenderer extends SafeBlockEntityRenderer<LodestoneTrackerBlockEntity> {
    public LodestoneTrackerRenderer(BlockEntityRendererProvider.Context context) { super();}

    @Override
    protected void renderSafe(LodestoneTrackerBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {        
        //Render compass
        ItemStack compass = blockEntity.getCompass();
        if (!compass.isEmpty()) {
            //This is to update compass every frame instead of every tick
            float targetAngle = blockEntity.getAngleFromCompass(compass);
            Direction facing = blockEntity.getCompassFacing();
            float facingAngle = facing.toYRot();
            int modelIndex = getIndexFromAngle(targetAngle - facingAngle);

            renderCompass(blockEntity.getLevel(), poseStack, bufferSource, light, overlay, compass, new Vec3(0.5,0.85f,0.5), modelIndex, facingAngle);
        }

        //Render partials
        BlockState blockState = blockEntity.getBlockState();
        VertexConsumer vertexBuffer = bufferSource.getBuffer(RenderType.cutout()); 
        SuperByteBuffer partialIndicatorModel = CachedBufferer.partial(PropulsionPartialModels.LODESTONE_TRACKER_INDICATOR, blockState);

        int powers[];
        if (blockEntity.IsInverted()) {
            powers = new int[]{
                blockEntity.powerSouth(),
                blockEntity.powerEast(),
                blockEntity.powerNorth(),
                blockEntity.powerWest()
            };
        } else {
            powers = new int[]{
                blockEntity.powerNorth(),
                blockEntity.powerWest(),
                blockEntity.powerSouth(),
                blockEntity.powerEast()
            };
        }

        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();

            //Rotate around main model based on index
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(i * 90.0f));
            poseStack.translate(-0.5, -0.5, -0.5); 

            float redstonePower = powers[i] / 15.0f;
            int color = Color.mixColors(0x470102, 0xCD0000, redstonePower);

            partialIndicatorModel.light(light)
                                 .color(color)
                                 .renderInto(poseStack, vertexBuffer);
            poseStack.popPose();
        }
    }

    private int getIndexFromAngle(float targetAngle) {
        //Angle is in degrees in range of 0..360 (non-normalized)
        //Model index is from 0 to 31
        //Compass is always pointing to north, and index 31 is pointing west
        final int TOTAL_INDICES = 32;
        final float SLICE_SIZE = 360.0f / TOTAL_INDICES;
        float normalizedAngle = (targetAngle % 360f + 360f) % 360f;
        int index = (int)(normalizedAngle / SLICE_SIZE);
        return index;
    }


    private static void renderCompass(Level level, PoseStack ms, MultiBufferSource buffer, int light, int overlay, 
        ItemStack item, Vec3 position, int modelIndex, float facingAngle) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        //Acquire target compass model
        modelIndex = Math.min(Math.max(0, modelIndex), 31);
        BakedModel model = Bakery.BAKED_COMPASS_MODELS[modelIndex];

        ms.pushPose();
        //Move and twist compass around to make it look fine
        ms.translate(position.x, position.y, position.z);
        ms.mulPose(Axis.YP.rotationDegrees(-facingAngle)); //Account for our facing
        ms.mulPose(Axis.XP.rotationDegrees(90.0f));
        ms.scale(0.5f, 0.5f, 0.5f);
        itemRenderer.render(item, ItemDisplayContext.FIXED, false, ms, buffer, light, overlay, model);

        ms.popPose();
    }
}
