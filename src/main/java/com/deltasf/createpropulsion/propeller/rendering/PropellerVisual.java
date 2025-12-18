package com.deltasf.createpropulsion.propeller.rendering;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.propeller.PropellerBlock;
import com.deltasf.createpropulsion.propeller.PropellerBlockEntity;
import com.deltasf.createpropulsion.propeller.blades.PropellerBladeItem;
import com.deltasf.createpropulsion.registries.PropulsionInstanceTypes;
import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.util.SmartRecycler;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class PropellerVisual extends KineticBlockEntityVisual<PropellerBlockEntity> implements SimpleDynamicVisual {
    private static final float PIVOT_X = PropellerRenderer.pivotX;
    private static final float PIVOT_Y = PropellerRenderer.pivotY;
    private static final float PIVOT_Z = PropellerRenderer.pivotZ;

    protected final RotatingInstance shaft;
    protected final Direction facing;

    private final SmartRecycler<Integer, OrientedInstance> sharpHead;
    private final SmartRecycler<Integer, PropellerBlurInstance> blurHead;
    private final SmartRecycler<PartialModel, OrientedInstance> sharpBlades;
    private final SmartRecycler<PartialModel, PropellerBlurInstance> blurBlades;

    private float lastRenderTimeSeconds;

    public PropellerVisual(VisualizationContext context, PropellerBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        this.facing = blockState.getValue(PropellerBlock.FACING);

        shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF)).createInstance();
        shaft.setup(blockEntity)
            .setPosition(getVisualPosition())
            .rotateToFace(Direction.SOUTH, facing.getOpposite())
            .setChanged();

        sharpHead = new SmartRecycler<>(key ->
            instancerProvider().instancer(InstanceTypes.ORIENTED, Models.partial(PropulsionPartialModels.PROPELLER_HEAD)).createInstance());

        blurHead = new SmartRecycler<>(key ->
            instancerProvider().instancer(PropulsionInstanceTypes.PROPELLER_BLUR, PropellerModels.getBlurred(PropulsionPartialModels.PROPELLER_HEAD)).createInstance());

        sharpBlades = new SmartRecycler<>(model -> 
            instancerProvider().instancer(InstanceTypes.ORIENTED, Models.partial(model)).createInstance());

        blurBlades = new SmartRecycler<>(model -> 
            instancerProvider().instancer(PropulsionInstanceTypes.PROPELLER_BLUR, PropellerModels.getBlurred(model)).createInstance());

        Level level = blockEntity.getLevel();
        if (level == null) return; 
        lastRenderTimeSeconds = AnimationTickHolder.getRenderTime() / 20.0f;
    }

    @Override
    public void beginFrame(Context ctx) {
        sharpHead.resetCount();
        blurHead.resetCount();
        sharpBlades.resetCount();
        blurBlades.resetCount();

        float time = AnimationTickHolder.getRenderTime(level);
        float timeSeconds = time / 20.0f;
        float deltaTimeSeconds = timeSeconds - lastRenderTimeSeconds;
        lastRenderTimeSeconds = timeSeconds;

        updateRPM(deltaTimeSeconds);
        updateVisualAngle(deltaTimeSeconds);
        updateBladeAnimations(timeSeconds);

        shaft.setup(blockEntity).setChanged();

        boolean shouldBlur = shouldBlur();
        PartialModel bladeModel = getBladeModel();
        boolean canBeBlurred = blockEntity.getBlade().map(PropellerBladeItem::canBeBlurred).orElse(true);

        int packedLight = computePackedLight();

        if (canBeBlurred && shouldBlur && PropulsionConfig.PROPELLER_ENABLE_BLUR.get()) {
            renderBlurred(bladeModel, packedLight);
        } else {
            renderSharp(bladeModel, packedLight);
        }

        sharpHead.discardExtra();
        blurHead.discardExtra();
        sharpBlades.discardExtra();
        blurBlades.discardExtra();
    }

    private void renderSharp(@Nullable PartialModel bladeModel, int light) {
        OrientedInstance head = sharpHead.get(0);
        transformHead(head, blockEntity.visualAngle, 255);
        head.light(light).setChanged();

        if (bladeModel != null && !blockEntity.renderedBladeAngles.isEmpty()) {
            for (float placementAngle : blockEntity.renderedBladeAngles) {
                OrientedInstance blade = sharpBlades.get(bladeModel);
                transformBlade(blade, blockEntity.visualAngle + placementAngle, 255);
                blade.light(light).setChanged();
            }
        }
    }

    private void renderBlurred(@Nullable PartialModel bladeModel, int light) {
        double blurRad = Math.PI * Math.abs(blockEntity.visualRPM) / 30 * PropulsionConfig.PROPELLER_EXPOSURE_TIME.get();
        double blurDeg = blurRad * (180.0 / Math.PI);

        int N = Math.min(PropulsionConfig.PROPELLER_BLUR_MAX_INSTANCES.get(), Math.max(2, (int)Math.ceil(blurDeg / PropulsionConfig.PROPELLER_BLUR_SAMPLE_RATE.get())));
        float alpha = (float) (1.0 - Math.pow(1.0 - PropellerRenderer.TARGET_OPACITY, 1.0 / N));
        int alphaInt = (int)(alpha * 255);
        float headAlpha = (float) (1.0 - Math.pow(1.0 - PropellerRenderer.HEAD_TARGET_OPACITY, 1.0 / N));
        int headAlphaInt = (int)(headAlpha * 255);
        
        float angleStep = (float)blurDeg / (float)N;
        float stroboscopicAngle = blockEntity.visualAngle;
        if (!blockEntity.renderedBladeAngles.isEmpty()) {
            float sectorAngle = 360f / blockEntity.renderedBladeAngles.size();
            stroboscopicAngle %= sectorAngle;
        }

        for (int i = 0; i < N; i++) {
            float rotationalAngle = stroboscopicAngle - (i * angleStep);

            PropellerBlurInstance head = blurHead.get(0);
            transformHead(head, rotationalAngle, headAlphaInt);
            head.light(light).setChanged();

            if (bladeModel != null) {
                for (float placementAngle : blockEntity.renderedBladeAngles) {
                    PropellerBlurInstance blade = blurBlades.get(bladeModel);
                    transformBlade(blade, rotationalAngle + placementAngle, alphaInt);
                    blade.light(light).setChanged();
                }
            }
        }
    }

    private void transformHead(Instance instance, float rotationAngle, int alpha) {
        Quaternionf q = getBaseRotation(rotationAngle);
        BlockPos bp = getVisualPosition();
        Vector3f pos = new Vector3f(bp.getX(), bp.getY(), bp.getZ());
        
        if (instance instanceof OrientedInstance) {
            ((OrientedInstance) instance).position(pos).rotation(q).color(255, 255, 255, alpha);
        } else if (instance instanceof PropellerBlurInstance) {
            ((PropellerBlurInstance) instance).position(pos).rotation(q).color(255, 255, 255, alpha);
        }
    }

    private void transformBlade(Instance instance, float totalAngle, int alpha) {
        double bladeAngle = PropulsionConfig.PROPELLER_BLADE_ANGLE.get();
        float pitchAngle = (float)bladeAngle * (blockEntity.IsClockwise() ? 1.0f : -1.0f);

        Quaternionf rotation = getBaseRotation(totalAngle);
        Quaternionf pitchRot = new Quaternionf().rotationY(Mth.DEG_TO_RAD * pitchAngle);
        Quaternionf finalRot = new Quaternionf(rotation).mul(pitchRot);

        Vector3f pivot = new Vector3f(PIVOT_X, PIVOT_Y, PIVOT_Z);
        Vector3f pivotRotated = new Vector3f(pivot).rotate(pitchRot);
        Vector3f localOffset = new Vector3f(pivot).sub(pivotRotated);
        localOffset.rotate(rotation);

        BlockPos bp = getVisualPosition();
        Vector3f finalPos = new Vector3f(bp.getX(), bp.getY(), bp.getZ()).add(localOffset);

        if (instance instanceof OrientedInstance) {
            ((OrientedInstance) instance).position(finalPos).rotation(finalRot).color(255, 255, 255, alpha);
        } else if (instance instanceof PropellerBlurInstance) {
            ((PropellerBlurInstance) instance).position(finalPos).rotation(finalRot).color(255, 255, 255, alpha);
        }
    }

    private Quaternionf getBaseRotation(float angle) {
        Quaternionf q = new Quaternionf(facing.getRotation());
        q.mul(new Quaternionf().rotationX(Mth.DEG_TO_RAD * 90));
        
        if (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            angle *= -1.0f;
        }
        q.mul(new Quaternionf().rotationZ(Mth.DEG_TO_RAD * angle));
        return q;
    }

    private void updateRPM(float deltaTimeSeconds) {
        float targetRPM = blockEntity.getTargetRPM();
        float diff = targetRPM - blockEntity.visualRPM;
        if (Math.abs(diff) > 1e-3f) {
            float proportionalAccel = diff * PropellerRenderer.SMOOTHING_FACTOR;
            float clampedAcceleration = Mth.clamp(Math.abs(proportionalAccel), PropellerRenderer.RPM_MIN_ACCELERATION, PropellerRenderer.RPM_MAX_ACCELERATION);
            float deltaRPM = Math.signum(diff) * clampedAcceleration * deltaTimeSeconds;
            if (Math.abs(deltaRPM) > Math.abs(diff)) blockEntity.visualRPM = targetRPM;
            else blockEntity.visualRPM += deltaRPM;
        }
    }

    private void updateVisualAngle(float deltaTimeSeconds) {
        float degreesPerSecond = blockEntity.visualRPM * 6.0f;
        float angleChange = degreesPerSecond * deltaTimeSeconds;
        blockEntity.visualAngle = (blockEntity.visualAngle + angleChange) % 360.0f;
    }

    private void updateBladeAnimations(float timeSeconds) {
        if (blockEntity.animationStartTime > 0) {
            float timeSinceChange = timeSeconds - blockEntity.animationStartTime;
            float progress = Mth.clamp(timeSinceChange / PropellerRenderer.ANIMATION_DURATION, 0.0f, 1.0f);
            progress = progress * progress * (3 - 2 * progress);
            for (int i = 0; i < blockEntity.targetBladeAngles.size(); i++) {
                float startAngle = blockEntity.prevBladeAngles.get(i);
                float endAngle = blockEntity.targetBladeAngles.get(i);
                blockEntity.renderedBladeAngles.set(i, AngleHelper.angleLerp(progress, startAngle, endAngle));
            }
            if (progress >= 1.0f) blockEntity.animationStartTime = 0;
        }
    }

    private boolean shouldBlur() {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 localPos = Vec3.atCenterOf(pos);
        Vec3 blockPosWorld = VSGameUtilsKt.isBlockInShipyard(level, pos) ? VSGameUtilsKt.toWorldCoordinates(level, localPos) : localPos;
        double distSqr = camera.getPosition().distanceToSqr(blockPosWorld);
        double blurRad = Math.PI * Math.abs(blockEntity.visualRPM) / 30 * PropulsionConfig.PROPELLER_EXPOSURE_TIME.get();
        double blurDeg = blurRad * (180.0 / Math.PI);
        return distSqr < (PropulsionConfig.PROPELLER_LOD_DISTANCE.get() * PropulsionConfig.PROPELLER_LOD_DISTANCE.get()) && blurDeg > PropellerRenderer.MIN_BLUR_DEG;
    }

    @Nullable
    private PartialModel getBladeModel() {
        return blockEntity.getBlade().map(PropellerBladeItem::getModel).orElse(null);
    }

    @Override
    public void updateLight(float partialTick) {
        int light = computePackedLight();
        shaft.light(light).setChanged();
    }

    @Override
    protected void _delete() {
        shaft.delete();
        sharpHead.delete();
        blurHead.delete();
        sharpBlades.delete();
        blurBlades.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(shaft);
    }
}