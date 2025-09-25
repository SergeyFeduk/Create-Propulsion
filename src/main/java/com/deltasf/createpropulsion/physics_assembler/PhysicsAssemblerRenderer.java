package com.deltasf.createpropulsion.physics_assembler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class PhysicsAssemblerRenderer extends SafeBlockEntityRenderer<PhysicsAssemblerBlockEntity> {

    public PhysicsAssemblerRenderer(BlockEntityRendererProvider.Context context) {
        super();
    }

    @Override
    protected void renderSafe(PhysicsAssemblerBlockEntity assembler, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
        ItemStack gaugeStack = assembler.getGaugeStack();
        if (gaugeStack.isEmpty()) {
            return;
        }
        //Assembly gauge inside is rendered always
        renderItem(poseStack, gaugeStack, assembler, bufferSource, light, overlay);

        //Outline is rendered only when player looks at block
        Minecraft mc = Minecraft.getInstance();
        HitResult hitResult = mc.hitResult;

        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            if (blockHitResult.getBlockPos().equals(assembler.getBlockPos())) {
                renderSelectionOutline(assembler);
            }
        }

    }

    private void renderItem(PoseStack poseStack, ItemStack gaugeStack, PhysicsAssemblerBlockEntity assembler, MultiBufferSource bufferSource, int light, int overlay) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel bakedModel = itemRenderer.getModel(gaugeStack, assembler.getLevel(), null, 0);

        poseStack.pushPose();
        poseStack.translate(0.5, 1.1625, 0.5);
        poseStack.scale(0.6f, 0.6f, 0.6f);

        float rotation = assembler.getGaugeRotation();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        itemRenderer.render(gaugeStack, ItemDisplayContext.FIXED, false, poseStack, bufferSource, light, overlay, bakedModel);

        poseStack.popPose();
    }

    private void renderSelectionOutline(PhysicsAssemblerBlockEntity assembler) {
        ItemStack gaugeStack = assembler.getGaugeStack();
        if (gaugeStack.isEmpty()) {
            return;
        }

        BlockPos posA = AssemblyGaugeItem.getPosA(gaugeStack);
        BlockPos posB = AssemblyGaugeItem.getPosB(gaugeStack);

        if (posA == null || posB == null) {
            return;
        }

        AABB selectionBox = new AABB(posA).minmax(new AABB(posB));

        AssemblyUtility.renderOutline(
            "assembler_selection",
            selectionBox,
            AssemblyUtility.PASSIVE_COLOR,
            1 / 16f,
            true
        );
    }

}
