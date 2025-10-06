package com.deltasf.createpropulsion.propeller;

import java.util.List;

import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.propeller.blades.PropellerBladeItem;
import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.deltasf.createpropulsion.registries.PropulsionRenderTypes;
import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PropellerRenderer extends KineticBlockEntityRenderer<PropellerBlockEntity> {
    //TODO: Make configurable
    private static final float MIN_BLUR_DEG = 5.0f;
    private static final float TARGET_OPACITY = 0.65f;
    private static final float HEAD_TARGET_OPACITY = 0.9f;

    private static final float ANIMATION_DURATION = 0.3f;
    private static final float RPM_MAX_ACCELERATION = 500.0f;
    private static final float RPM_MIN_ACCELERATION = 3.0f;
    private static final float SMOOTHING_FACTOR = 2.0f;

    public PropellerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @SuppressWarnings("null")
    @Override
	protected void renderSafe(PropellerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Direction direction = be.getBlockState()
				.getValue(PropellerBlock.FACING);
		VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());

        int lightBehind = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(direction.getOpposite()));

        SuperByteBuffer shaftHalf = CachedBufferer.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), direction.getOpposite());

        //Shaft
        standardKineticRotationTransform(shaftHalf, be, lightBehind).renderInto(ms, vb);
        //Head
        renderHeadAndBlades(be, partialTicks, ms, buffer, light, overlay);
    }

    private void renderHeadAndBlades(PropellerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 localPos = Vec3.atCenterOf(be.getBlockPos());
        Vec3 blockPosWorld = VSGameUtilsKt.isBlockInShipyard(be.getLevel(), be.getBlockPos()) ? VSGameUtilsKt.toWorldCoordinates(be.getLevel(), localPos) : localPos;
        double distSqr = camera.getPosition().distanceToSqr(blockPosWorld);

        BlockState state = be.getBlockState();
        Direction direction = state.getValue(PropellerBlock.FACING);

        //Get models
        SuperByteBuffer headModel = CachedBufferer.partial(PropulsionPartialModels.PROPELLER_HEAD, state);
        SuperByteBuffer bladeModel = null;
        if (be.getBladeCount() > 0) { 
            ItemStack bladeStack = be.bladeInventory.getStackInSlot(0);
            if (!bladeStack.isEmpty() && bladeStack.getItem() instanceof PropellerBladeItem bladeItem) {
                PartialModel bladePartialModel = bladeItem.getModel();
                if (bladePartialModel != null) {
                    bladeModel = CachedBufferer.partial(bladePartialModel, state);
                }
            }
        }


        long timeNow = System.nanoTime();
        if (be.lastRenderTimeNanos == 0) { // First frame
            be.lastRenderTimeNanos = timeNow;
        }
        float deltaTimeSeconds = (timeNow - be.lastRenderTimeNanos) / 1000000000f;
        be.lastRenderTimeNanos = timeNow;

        //Interpolate RPM
        float targetRPM = be.getTargetRPM();
        float diff = targetRPM - be.visualRPM;
        if (Math.abs(diff) > 1e-3f) {
            float proportionalAccel = diff * SMOOTHING_FACTOR;
            float clampedAcceleration = Mth.clamp(Math.abs(proportionalAccel), RPM_MIN_ACCELERATION, RPM_MAX_ACCELERATION);
            
            float deltaRPM = Math.signum(diff) * clampedAcceleration * deltaTimeSeconds;

            if (Math.abs(deltaRPM) > Math.abs(diff)) {
                be.visualRPM = targetRPM;
            } else {
                be.visualRPM += deltaRPM;
            }

        }

        //Visual angle
        float degreesPerSecond = be.visualRPM * 6.0f;
        float angleChange = degreesPerSecond * deltaTimeSeconds;
        be.visualAngle = (be.visualAngle + angleChange) % 360.0f;

        int bladeCount = be.getBladeCount();
        if (be.animationStartTime > 0) {
            long timeSinceChange = timeNow - be.animationStartTime;
            float progress = (float) (timeSinceChange / (ANIMATION_DURATION * 1000000000f));
            progress = Mth.clamp(progress, 0.0f, 1.0f);
            progress = progress * progress * (3 - 2 * progress);
            for (int i = 0; i < be.targetBladeAngles.size(); i++) {
                float startAngle = be.prevBladeAngles.get(i);
                float endAngle = be.targetBladeAngles.get(i);
                be.renderedBladeAngles.set(i, AngleHelper.angleLerp(progress, startAngle, endAngle));
            }
            if (progress >= 1.0f) {
                be.animationStartTime = 0;
            }
        }

        //Blur m*th
        double blurRad = Math.PI * be.visualRPM / 30 * PropulsionConfig.PROPELLER_EXPOSURE_TIME.get();
        double blurDeg = blurRad * (180.0 / Math.PI);

        boolean shouldBlur = distSqr < (PropulsionConfig.PROPELLER_LOD_DISTANCE.get() * PropulsionConfig.PROPELLER_LOD_DISTANCE.get()) && blurDeg > MIN_BLUR_DEG;
        if (shouldBlur) {
            int N = Math.min(PropulsionConfig.PROPELLER_BLUR_MAX_INSTANCES.get(), Math.max(2, (int)Math.ceil(blurDeg / PropulsionConfig.PROPELLER_BLUR_SAMPLE_RATE.get())));
            //float alpha = 1.0f / (float)N;
            float alpha = (float) (1.0 - Math.pow(1.0 - TARGET_OPACITY, 1.0 / N));
            int alphaInt = (int)(alpha * 255);
            float angleStep = (float)blurDeg / (float)N;

            float headAlpha = (float) (1.0 - Math.pow(1.0 - HEAD_TARGET_OPACITY, 1.0 / N));
            int headAlphaInt = (int)(headAlpha * 255);

            VertexConsumer translucentVB = buffer.getBuffer(PropulsionRenderTypes.PROPELLER_BLUR);

            float stroboscopicAngle = be.visualAngle;
            if (bladeCount > 0) {
                float sectorAngle = 360f / bladeCount;
                stroboscopicAngle %= sectorAngle;
            }

            for (int i = 0; i < N; i++) {
                float rotationalAngle = stroboscopicAngle - (i * angleStep);
                renderHead(ms, translucentVB, light, overlay, headModel, direction, rotationalAngle, headAlphaInt);
                renderBlades(ms, translucentVB, light, overlay, bladeModel, direction, rotationalAngle, be.renderedBladeAngles, alphaInt);
            }
        } else {
            VertexConsumer cutoutVB = buffer.getBuffer(RenderType.cutoutMipped());
            renderHead(ms, cutoutVB, light, overlay, headModel, direction, be.visualAngle, 255);
            renderBlades(ms, cutoutVB, light, overlay, bladeModel, direction, be.visualAngle, be.renderedBladeAngles, 255);
        }
    }

    private void renderHead(PoseStack ms, VertexConsumer vb, int light, int overlay, SuperByteBuffer headModel, Direction direction, float rotationAngle, int alpha) {
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(direction.getRotation());
        ms.mulPose(Axis.XP.rotationDegrees(90));
        ms.mulPose(Axis.ZP.rotationDegrees(rotationAngle));
        ms.translate(-0.5, -0.5, -0.5);
        headModel.light(light).overlay(overlay).color(255, 255, 255, alpha).renderInto(ms, vb);
        ms.popPose();
    }

    private void renderBlades(PoseStack ms, VertexConsumer vb, int light, int overlay, SuperByteBuffer bladeModel, Direction direction, float kineticAngle, List<Float> placementAngles, int alpha) {
        if (placementAngles.isEmpty() || bladeModel == null) return;

        for (float currentAngle : placementAngles) {
            ms.pushPose();
            ms.translate(0.5, 0.5, 0.5);
            ms.mulPose(direction.getRotation());
            ms.mulPose(Axis.XP.rotationDegrees(90));
            ms.mulPose(Axis.ZP.rotationDegrees(kineticAngle));
            ms.mulPose(Axis.ZP.rotationDegrees(currentAngle));
            ms.translate(-0.5, -0.5, -0.5);
            bladeModel.light(light).overlay(overlay).color(255, 255, 255, alpha).renderInto(ms, vb);
            ms.popPose();
        }
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
