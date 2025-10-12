package com.deltasf.createpropulsion.propeller;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.propeller.blades.PropellerBladeItem;
import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.deltasf.createpropulsion.registries.PropulsionRenderTypes;
import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.Material;
import com.jozufozu.flywheel.api.MaterialGroup;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.core.Materials;
import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.core.materials.oriented.OrientedData;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityInstance;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;
import com.simibubi.create.foundation.utility.AngleHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

public class PropellerRenderInstance extends KineticBlockEntityInstance<PropellerBlockEntity> implements DynamicInstance {
    protected final Direction facing;
    protected RotatingData shaft;
    private PartialModel currentBladeModel;

    protected Instancer<OrientedData> headInstancerCutout;
    protected Instancer<OrientedData> bladeInstancerCutout;
    protected Instancer<OrientedData> headInstancerBlur;
    protected Instancer<OrientedData> bladeInstancerBlur;

    private final List<OrientedData> transientInstances = new ArrayList<>();
    private final Vector3f pivot = new Vector3f(PropellerRenderer.pivotX,PropellerRenderer.pivotY,PropellerRenderer.pivotZ);

    public PropellerRenderInstance(MaterialManager materialManager, PropellerBlockEntity blockEntity) {
        super(materialManager, blockEntity);
        this.facing = this.blockState.getValue(PropellerBlock.FACING);

        shaft = getRotatingMaterial().getModel(AllPartialModels.SHAFT_HALF, blockState, this.facing.getOpposite()).createInstance();
        setup(shaft);

        MaterialGroup cutoutGroup = materialManager.defaultCutout();
        MaterialGroup blurGroup = materialManager.transparent(PropulsionRenderTypes.PROPELLER_BLUR);

        Material<OrientedData> orientedMaterialCutout = cutoutGroup.material(Materials.ORIENTED);
        Material<OrientedData> orientedMaterialBlur = blurGroup.material(Materials.ORIENTED);

        headInstancerCutout = orientedMaterialCutout.getModel(PropulsionPartialModels.PROPELLER_HEAD, blockState);
        headInstancerBlur = orientedMaterialBlur.getModel(PropulsionPartialModels.PROPELLER_HEAD, blockState);

        recreateBladeInstancers();
    }

    private void recreateBladeInstancers() {
        bladeInstancerCutout = null;
        bladeInstancerBlur = null;

        currentBladeModel = getBladeModel();
        if (currentBladeModel != null) {
            MaterialGroup cutoutGroup = materialManager.defaultCutout();
            MaterialGroup blurGroup = materialManager.transparent(PropulsionRenderTypes.PROPELLER_BLUR);
            
            Material<OrientedData> orientedMaterialCutout = cutoutGroup.material(Materials.ORIENTED);
            Material<OrientedData> orientedMaterialBlur = blurGroup.material(Materials.ORIENTED);

            bladeInstancerCutout = orientedMaterialCutout.getModel(currentBladeModel, blockState);
            bladeInstancerBlur = orientedMaterialBlur.getModel(currentBladeModel, blockState);
        }
    }

    private PartialModel getBladeModel() {
        return blockEntity.getBlade()
            .map(PropellerBladeItem::getModel)
            .orElse(null);
    }

    private boolean canBeBlurred() {
        return blockEntity.getBlade()
                .map(PropellerBladeItem::canBeBlurred)
                .orElse(true);
    }

    @Override
    public void beginFrame() {
        updateRotation(shaft);
        clearTransientInstances();

        if (getBladeModel() != currentBladeModel) {
            recreateBladeInstancers();
        }

        long timeNow = System.nanoTime();
        if (blockEntity.lastRenderTimeNanos == 0) blockEntity.lastRenderTimeNanos = timeNow;
        float deltaTimeSeconds = (timeNow - blockEntity.lastRenderTimeNanos) / 1.0e9f;
        blockEntity.lastRenderTimeNanos = timeNow;

        float targetRPM = blockEntity.getTargetRPM();
        float diff = targetRPM - blockEntity.visualRPM;
        if (Math.abs(diff) > 1e-3f) {
            float proportionalAccel = diff * PropellerRenderer.SMOOTHING_FACTOR;
            float clampedAcceleration = Mth.clamp(Math.abs(proportionalAccel), PropellerRenderer.RPM_MIN_ACCELERATION, PropellerRenderer.RPM_MAX_ACCELERATION);
            float deltaRPM = Math.signum(diff) * clampedAcceleration * deltaTimeSeconds;
            if (Math.abs(deltaRPM) > Math.abs(diff)) blockEntity.visualRPM = targetRPM;
            else blockEntity.visualRPM += deltaRPM;
        }

        float degreesPerSecond = blockEntity.visualRPM * 6.0f;
        float angleChange = degreesPerSecond * deltaTimeSeconds;
        blockEntity.visualAngle = (blockEntity.visualAngle + angleChange) % 360.0f;

        if (blockEntity.animationStartTime > 0) {
            long timeSinceChange = timeNow - blockEntity.animationStartTime;
            float progress = Mth.clamp((float)(timeSinceChange / (PropellerRenderer.ANIMATION_DURATION * 1.0e9f)), 0.0f, 1.0f);
            progress = progress * progress * (3 - 2 * progress);
            for (int i = 0; i < blockEntity.targetBladeAngles.size(); i++) {
                float startAngle = blockEntity.prevBladeAngles.get(i);
                float endAngle = blockEntity.targetBladeAngles.get(i);
                blockEntity.renderedBladeAngles.set(i, AngleHelper.angleLerp(progress, startAngle, endAngle));
            }
            if (progress >= 1.0f) blockEntity.animationStartTime = 0;
        }

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 localPos = Vec3.atCenterOf(pos);
        Vec3 blockPosWorld = VSGameUtilsKt.isBlockInShipyard(this.blockEntity.getLevel(), pos) ? VSGameUtilsKt.toWorldCoordinates(this.blockEntity.getLevel(), localPos) : localPos;
        double distSqr = camera.getPosition().distanceToSqr(blockPosWorld);

        double blurRad = Math.PI * blockEntity.visualRPM / 30 * PropulsionConfig.PROPELLER_EXPOSURE_TIME.get();
        double blurDeg = Math.toDegrees(blurRad);

        boolean shouldBlur = distSqr < Math.pow(PropulsionConfig.PROPELLER_LOD_DISTANCE.get(), 2) && blurDeg > PropellerRenderer.MIN_BLUR_DEG;
        boolean canBeBlurred = canBeBlurred();

        if (canBeBlurred && shouldBlur) {
            renderBlurred(blurDeg);
        } else {
            renderSharp();
        }
    }

    private void clearTransientInstances() {
        for (OrientedData instance : transientInstances) {
            instance.delete();
        }
        transientInstances.clear();
    }

    private void renderSharp() {
        OrientedData headData = headInstancerCutout.createInstance();
        transformHeadInstance(headData, blockEntity.visualAngle, (byte) 255);
        transientInstances.add(headData); 

        if (bladeInstancerCutout != null) {
            for (Float placementAngle : blockEntity.renderedBladeAngles) {
                OrientedData bladeData = bladeInstancerCutout.createInstance();
                transformBladeInstance(bladeData, blockEntity.visualAngle + placementAngle, (byte) 255);
                transientInstances.add(bladeData); 
            }
        }
    }

    private void renderBlurred(double blurDeg) {
        int N = Math.min(PropulsionConfig.PROPELLER_BLUR_MAX_INSTANCES.get(), Math.max(2, (int)Math.ceil(blurDeg / PropulsionConfig.PROPELLER_BLUR_SAMPLE_RATE.get())));
        float alpha = (float) (1.0 - Math.pow(1.0 - PropellerRenderer.TARGET_OPACITY, 1.0 / N));
        int alphaInt = (int)(alpha * 255);
        float headAlpha = (float) (1.0 - Math.pow(1.0 - PropellerRenderer.HEAD_TARGET_OPACITY, 1.0 / N));
        int headAlphaInt = (int)(headAlpha * 255);
        float angleStep = (float)blurDeg / N;

        float stroboscopicAngle = blockEntity.visualAngle;
        if (!blockEntity.renderedBladeAngles.isEmpty()) {
            float sectorAngle = 360f / blockEntity.renderedBladeAngles.size();
            stroboscopicAngle %= sectorAngle;
        }

        for (int i = 0; i < N; i++) {
            float rotationalOffset = -i * angleStep;
            OrientedData headData = headInstancerBlur.createInstance();
            transformHeadInstance(headData, rotationalOffset + stroboscopicAngle, (byte) headAlphaInt);
            transientInstances.add(headData); 

            if (bladeInstancerBlur != null) {
                for (Float placementAngle : blockEntity.renderedBladeAngles) {
                    OrientedData bladeData = bladeInstancerBlur.createInstance();
                    transformBladeInstance(bladeData, rotationalOffset + stroboscopicAngle + placementAngle, (byte) alphaInt);
                    transientInstances.add(bladeData); 
                }
            }
        }
    }

    private void transformHeadInstance(OrientedData instanceData, float totalAngle, byte alpha) {
        Quaternionf rotation = getRotation(totalAngle);
        
        instanceData.setPosition(getInstancePosition())
                .setRotation(rotation)
                .setColor((byte) 255, (byte) 255, (byte) 255, alpha);

        relight(pos, instanceData);
    }

    private void transformBladeInstance(OrientedData instanceData, float totalAngle, byte alpha) {
        double bladeAngle = PropulsionConfig.PROPELLER_BLADE_ANGLE.get();
        float pitchAngle = (float)bladeAngle * (blockEntity.isClockwise ? 1.0f : -1.0f);
        
        Quaternionf rotation = getRotation(totalAngle);
        Quaternionf rPitch = Axis.YP.rotationDegrees(pitchAngle);
        Quaternionf rFinal = new Quaternionf(rotation).mul(rPitch);

        Vector3f pivotRotated = new Vector3f(pivot).rotate(rPitch);
        Vector3f localOffset = new Vector3f(pivot).sub(pivotRotated);
        Vector3f worldOffset = localOffset.rotate(rotation);

        Vector3f instancePos = VectorConversionsMCKt.toJOMLF((Vec3i)getInstancePosition());
        Vector3f finalPos = new Vector3f(instancePos.x, instancePos.y, instancePos.z).add(worldOffset);
        
        instanceData.setPosition(finalPos)
                .setRotation(rFinal)
                .setColor((byte) 255, (byte) 255, (byte) 255, alpha);

        relight(pos, instanceData);
    }

    private Quaternionf getRotation(float angle) {
        Quaternionf q = new Quaternionf(this.facing.getRotation());
        q.mul(Axis.XP.rotationDegrees(90));
        q.mul(Axis.ZP.rotationDegrees(angle));
        return q;
    }

    @Override
    public void updateLight() {
        super.updateLight();
        relight(pos, this.shaft);
    }

    @Override
    public void remove() {
        shaft.delete();
        clearTransientInstances();
    }
}
