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

@SuppressWarnings("null")
public class LodestoneTrackerRenderer extends SafeBlockEntityRenderer<LodestoneTrackerBlockEntity> {
    public LodestoneTrackerRenderer(BlockEntityRendererProvider.Context context) { super();}

    @Override
    protected void renderSafe(LodestoneTrackerBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
        ItemStack compass = blockEntity.getCompass();
        if (compass.isEmpty()) return;
        float y = 0.9f;
        renderItem(blockEntity.getLevel(), poseStack, bufferSource, light, overlay, compass, 0, new Vec3(0.5,y,0.5));
    }

    private static void renderItem(Level level, PoseStack ms, MultiBufferSource buffer, int light, int overlay, 
        ItemStack item, int angle, Vec3 position) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        //Acquire target compass model
        boolean test = Minecraft.getInstance().player.isCrouching();
        BakedModel model = Bakery.BAKED_COMPASS_MODELS[test ? 0 : 10];

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
