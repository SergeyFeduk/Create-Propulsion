package com.deltasf.createpropulsion.lodestone_tracker;

import com.deltasf.createpropulsion.utility.Bakery;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("null") //Silence
public class LodestoneTrackerRenderer extends SafeBlockEntityRenderer<LodestoneTrackerBlockEntity> {
    public LodestoneTrackerRenderer(BlockEntityRendererProvider.Context context) { super();}

    @Override
    protected void renderSafe(LodestoneTrackerBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
        ItemStack compass = blockEntity.getCompass();
        if (compass.isEmpty()) return;
        //This is to update compass every frame instead of every tick. Better visual responsivness
        float targetAngle = blockEntity.getAngleFromCompass(compass);
        int modelIndex = getIndexFromAngle(targetAngle);
        renderCompass(blockEntity.getLevel(), poseStack, bufferSource, light, overlay, compass, 0, new Vec3(0.5,0.85f,0.5), modelIndex);
    }

    private int getIndexFromAngle(float targetAngle) {
        //Angle is in degrees in range of 0..360
        //Model index is from 0 to 31
        //Compass is always pointing to north, and index 31 is pointing west
        final int TOTAL_INDICES = 32;
        final float SLICE_SIZE = 360.0f / TOTAL_INDICES;

        int index = (int)(targetAngle / SLICE_SIZE);
        return index;
    }


    private static void renderCompass(Level level, PoseStack ms, MultiBufferSource buffer, int light, int overlay, 
        ItemStack item, int angle, Vec3 position, int modelIndex) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        //Acquire target compass model
        modelIndex = Math.min(Math.max(0, modelIndex), 31);
        BakedModel model = Bakery.BAKED_COMPASS_MODELS[modelIndex];

        ms.pushPose();
        //Move and twist compass around to make it look fine
        ms.translate(position.x, position.y, position.z);
        ms.mulPose(Axis.XP.rotationDegrees(90.0f));
        ms.mulPose(Axis.ZP.rotationDegrees(180.0f));
        ms.scale(0.5f, 0.5f, 0.5f);
        itemRenderer.render(item, ItemDisplayContext.FIXED, false, ms, buffer, light, overlay, model);

        ms.popPose();
    }
}
