package com.deltasf.createpropulsion.thruster;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.debug.DebugRenderer;
import com.deltasf.createpropulsion.utility.MathUtility;
import com.simibubi.create.foundation.collision.Matrix3d;
import com.simibubi.create.foundation.collision.OrientedBB;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.joml.*;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.awt.*;
import java.util.List;

public class ThrusterDamager {
    private static final int TICKS_PER_ENTITY_CHECK = 5;
    private static final int LOWEST_POWER_THRSHOLD = 5;

    private final AbstractThrusterBlockEntity thruster;

    public ThrusterDamager(AbstractThrusterBlockEntity thruster) {
        this.thruster = thruster;
    }

    public void tick(int currentTick) {
        if (!shouldDamageEntities()) return;

        if (currentTick % TICKS_PER_ENTITY_CHECK == 0) {
            doEntityDamageCheck();
        }
    }

    private boolean shouldDamageEntities() {
        return PropulsionConfig.THRUSTER_DAMAGE_ENTITIES.get()
                && thruster.isPowered()
                && thruster.isWorking();
    }

    @SuppressWarnings("null")
    private void doEntityDamageCheck() {
        float visualPowerPercent = ((float) Math.max(thruster.getOverriddenPowerOrState(thruster.getBlockState()), LOWEST_POWER_THRSHOLD) - LOWEST_POWER_THRSHOLD) / 15.0f;
        float distanceByPower = Math.lerp(0.55f, 1.5f, visualPowerPercent);
        Direction plumeDirection = thruster.getBlockState().getValue(AbstractThrusterBlock.FACING).getOpposite();

        // Broad phase
        AABB plumeAABB = calculateAabb(plumeDirection, distanceByPower);
        List<Entity> damageCandidates = thruster.getLevel().getEntities(null, plumeAABB);

        if (CreatePropulsion.debug) {
            debugObb(plumeDirection, distanceByPower);
        }

        if (damageCandidates.isEmpty()) {
            return;
        }

        // Narrow phase
        NozzleInfo nozzleInfo = calculateNozzleInfo(plumeDirection);
        Vector3d localPlumeVec = new Vector3d(0, 0, 1);
        nozzleInfo.obbRotationWorldJOML().transform(localPlumeVec);
        Vec3 worldPlumeDirection = VectorConversionsMCKt.toMinecraft(localPlumeVec);

        // Calculate potential plume length in world space
        double potentialPlumeLength;
        Ship ship = VSGameUtilsKt.getShipManagingPos(thruster.getLevel(), thruster.getBlockPos());
        if (ship != null) {
            double plumeLengthShip = thruster.getEmptyBlocks() * distanceByPower;
            Vec3i normal = plumeDirection.getNormal();
            Vector3d plumeDisplacementShip = new Vector3d(normal.getX(), normal.getY(), normal.getZ()).mul(plumeLengthShip);
            Vector3d plumeDisplacementWorld = ship.getShipToWorld().transformDirection(plumeDisplacementShip, new Vector3d());
            potentialPlumeLength = plumeDisplacementWorld.length();
        } else {
            potentialPlumeLength = thruster.getEmptyBlocks() * distanceByPower;
        }

        double correctedPlumeLength = performRaycastCheck(nozzleInfo.thrusterNozzleWorldPosMC(), worldPlumeDirection, potentialPlumeLength);
        if (correctedPlumeLength <= 0.01) return;
        ObbCalculationResult obbResult = calculateObb(plumeDirection, correctedPlumeLength, nozzleInfo);
        applyDamageToEntities(thruster.getLevel(), damageCandidates, obbResult, visualPowerPercent);
    }

    private NozzleInfo calculateNozzleInfo(Direction plumeDirection) {
        Quaterniond relativeRotationJOML = new Quaterniond().rotateTo(new Vector3d(0, 0, 1), VectorConversionsMCKt.toJOMLD(plumeDirection.getNormal()));

        BlockPos worldPosition = thruster.getBlockPos();
        Level level = thruster.getLevel();
        Vector3d thrusterCenterBlockShipCoordsJOMLD = VectorConversionsMCKt.toJOML(Vec3.atCenterOf(worldPosition));

        Vector3d thrusterCenterBlockWorldJOML;
        Quaterniond obbRotationWorldJOML;
        Vector3d nozzleOffsetWorld;

        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        if (ship != null) {
            thrusterCenterBlockWorldJOML = ship.getShipToWorld().transformPosition(thrusterCenterBlockShipCoordsJOMLD, new Vector3d());
            obbRotationWorldJOML = ship.getTransform().getShipToWorldRotation().mul(relativeRotationJOML, new Quaterniond());
            Vector3d nozzleOffsetLocal = new Vector3d(0, 0, 0.5);
            Vector3d nozzleOffsetShip = relativeRotationJOML.transform(nozzleOffsetLocal, new Vector3d());
            nozzleOffsetWorld = ship.getShipToWorld().transformDirection(nozzleOffsetShip, new Vector3d());

        } else {
            thrusterCenterBlockWorldJOML = thrusterCenterBlockShipCoordsJOMLD;
            obbRotationWorldJOML = relativeRotationJOML;

            Vector3d nozzleOffsetLocal = new Vector3d(0, 0, 0.5);
            nozzleOffsetWorld = obbRotationWorldJOML.transform(nozzleOffsetLocal, new Vector3d());
        }

        Vector3d thrusterNozzleWorldPos = thrusterCenterBlockWorldJOML.add(nozzleOffsetWorld, new Vector3d());
        Vec3 thrusterNozzleWorldPosMC = VectorConversionsMCKt.toMinecraft(thrusterNozzleWorldPos);
        return new NozzleInfo(thrusterNozzleWorldPosMC, obbRotationWorldJOML, thrusterCenterBlockWorldJOML);
    }

    @SuppressWarnings("null")
    private double performRaycastCheck(Vec3 nozzlePos, Vec3 worldPlumeDirection, double maxDistance) {
        Level level = thruster.getLevel();
        Vec3 endPos = nozzlePos.add(worldPlumeDirection.scale(maxDistance));

        var clipContext = new ClipContext(
                nozzlePos,
                endPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
        );

        BlockHitResult hitResult = level.clip(clipContext);

        if (hitResult.getType() == BlockHitResult.Type.BLOCK) {
            return nozzlePos.distanceTo(hitResult.getLocation());
        }

        return maxDistance;
    }

    private ObbCalculationResult calculateObb(Direction plumeDirection, double plumeLength, NozzleInfo nozzleInfo) {
        Vector3d plumeWorldDir = new Vector3d(0, 0, 1);
        nozzleInfo.obbRotationWorldJOML().transform(plumeWorldDir);
        Vector3d nozzlePosWorld = VectorConversionsMCKt.toJOML(nozzleInfo.thrusterNozzleWorldPosMC());
        Vector3d obbCenterWorldJOML = nozzlePosWorld.add(plumeWorldDir.mul(plumeLength / 2.0), new Vector3d());

        Vector3d plumeHalfExtentsJOML;
        Level level = thruster.getLevel();
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, thruster.getBlockPos());

        if (ship != null) {
            Vector3d plumeDirShip = VectorConversionsMCKt.toJOMLD(plumeDirection.getNormal());
            Vector3d temp = new Vector3d(1.0, 0.0, 0.0);
            if (java.lang.Math.abs(plumeDirShip.dot(temp)) > 0.99) {
                temp.set(0.0, 1.0, 0.0);
            }
            Vector3d rightVecShip = plumeDirShip.cross(temp, new Vector3d()).normalize();
            Vector3d upVecShip = plumeDirShip.cross(rightVecShip, new Vector3d()).normalize();

            Vector3d halfExtentX_ship = rightVecShip.mul(0.7);
            Vector3d halfExtentY_ship = upVecShip.mul(0.7);

            Vector3d halfExtentX_world = ship.getShipToWorld().transformDirection(halfExtentX_ship, new Vector3d());
            Vector3d halfExtentY_world = ship.getShipToWorld().transformDirection(halfExtentY_ship, new Vector3d());

            plumeHalfExtentsJOML = new Vector3d(halfExtentX_world.length(), halfExtentY_world.length(), plumeLength / 2.0);
        } else {
            plumeHalfExtentsJOML = new Vector3d(0.7, 0.7, plumeLength / 2.0);
        }

        Vec3 plumeCenterMC = VectorConversionsMCKt.toMinecraft(obbCenterWorldJOML);
        Vec3 plumeHalfExtentsMC = VectorConversionsMCKt.toMinecraft(plumeHalfExtentsJOML);
        Matrix3d plumeRotationMatrix = MathUtility.createMatrixFromQuaternion(nozzleInfo.obbRotationWorldJOML());
        OrientedBB plumeOBB = new OrientedBB(plumeCenterMC, plumeHalfExtentsMC, plumeRotationMatrix);

        return new ObbCalculationResult(plumeLength, nozzleInfo.thrusterNozzleWorldPosMC(), plumeOBB, obbCenterWorldJOML, plumeHalfExtentsJOML, nozzleInfo.obbRotationWorldJOML());
    }

    private AABB calculateAabb(Direction plumeDirection, float distanceByPower) {
        Level level = thruster.getLevel();
        BlockPos worldPosition = thruster.getBlockPos();
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);

        if (ship == null) {
            BlockPos blockBehind = worldPosition.relative(plumeDirection);
            int aabbEndOffset = (int) Math.floor(thruster.getEmptyBlocks() * distanceByPower) + 1;
            BlockPos blockEnd = worldPosition.relative(plumeDirection, aabbEndOffset);
            return new AABB(blockBehind).minmax(new AABB(blockEnd)).inflate(1.0);
        }

        double plumeLengthShipSpace = thruster.getEmptyBlocks() * distanceByPower + 1.0;
        Vector3d startPosShip = VectorConversionsMCKt.toJOML(Vec3.atCenterOf(worldPosition.relative(plumeDirection)));

        Vec3i normal = plumeDirection.getNormal();
        Vector3d plumeDisplacementShip = new Vector3d(normal.getX(), normal.getY(), normal.getZ()).mul(plumeLengthShipSpace);
        Vector3d endPosShip = startPosShip.add(plumeDisplacementShip, new Vector3d());

        Vector3d startPosWorld = ship.getShipToWorld().transformPosition(startPosShip, new Vector3d());
        Vector3d endPosWorld = ship.getShipToWorld().transformPosition(endPosShip, new Vector3d());

        AABB plumeAABB = new AABB(VectorConversionsMCKt.toMinecraft(startPosWorld), VectorConversionsMCKt.toMinecraft(endPosWorld));

        Vector3dc scaling = ship.getTransform().getShipToWorldScaling();
        double maxScale = java.lang.Math.max(scaling.x(), java.lang.Math.max(scaling.y(), scaling.z()));
        return plumeAABB.inflate(1.5 * maxScale);
    }

    private void applyDamageToEntities(Level level, List<Entity> damageCandidates, ObbCalculationResult obbResult, float visualPowerPercent) {
        DamageSource fireDamageSource = level.damageSources().onFire();
        for (Entity entity : damageCandidates) {
            if (entity.isRemoved() || entity.fireImmune()) continue;
            AABB entityAABB = entity.getBoundingBox();
            if (obbResult.plumeOBB.intersect(entityAABB) != null) {
                float invSqrDistance = visualPowerPercent * 8.0f / (float) Math.max(1, entity.position().distanceToSqr(obbResult.thrusterNozzleWorldPosMC));
                float damageAmount = 3 + invSqrDistance;

                entity.hurt(fireDamageSource, damageAmount);
                entity.setSecondsOnFire(3);
            }
        }
    }

    private void debugObb(Direction plumeDirection, float distanceByPower) {
        NozzleInfo nozzleInfo = calculateNozzleInfo(plumeDirection);
        Vector3d localPlumeVec = new Vector3d(0, 0, 1);
        nozzleInfo.obbRotationWorldJOML().transform(localPlumeVec);
        Vec3 worldPlumeDirection = VectorConversionsMCKt.toMinecraft(localPlumeVec);

        double potentialPlumeLength;
        Ship ship = VSGameUtilsKt.getShipManagingPos(thruster.getLevel(), thruster.getBlockPos());
        if (ship != null) {
            double plumeLengthShip = thruster.getEmptyBlocks() * distanceByPower;
            Vec3i normal = plumeDirection.getNormal();
            Vector3d plumeDisplacementShip = new Vector3d(normal.getX(), normal.getY(), normal.getZ()).mul(plumeLengthShip);
            Vector3d plumeDisplacementWorld = ship.getShipToWorld().transformDirection(plumeDisplacementShip, new Vector3d());
            potentialPlumeLength = plumeDisplacementWorld.length();
        } else {
            potentialPlumeLength = thruster.getEmptyBlocks() * distanceByPower;
        }

        double correctedPlumeLength = performRaycastCheck(nozzleInfo.thrusterNozzleWorldPosMC(), worldPlumeDirection, potentialPlumeLength);
        if (correctedPlumeLength <= 0.01) return;

        ObbCalculationResult obbResult = calculateObb(plumeDirection, correctedPlumeLength, nozzleInfo);

        String identifier = "thruster_" + thruster.hashCode() + "_obb";
        Quaternionf debugRotation = new Quaternionf((float) obbResult.obbRotationWorldJOML.x, (float) obbResult.obbRotationWorldJOML.y, (float) obbResult.obbRotationWorldJOML.z, (float) obbResult.obbRotationWorldJOML.w);
        Vec3 debugSize = new Vec3(obbResult.plumeHalfExtentsJOML.x * 2, obbResult.plumeHalfExtentsJOML.y * 2, obbResult.plumeHalfExtentsJOML.z * 2);
        Vec3 debugCenter = VectorConversionsMCKt.toMinecraft(obbResult.obbCenterWorldJOML);

        DebugRenderer.drawBox(identifier, debugCenter, debugSize, debugRotation, Color.ORANGE, false, TICKS_PER_ENTITY_CHECK + 1);
    }

    private record ObbCalculationResult(
            double plumeLength,
            Vec3 thrusterNozzleWorldPosMC,
            OrientedBB plumeOBB,
            Vector3d obbCenterWorldJOML,
            Vector3d plumeHalfExtentsJOML,
            Quaterniond obbRotationWorldJOML
    ) {}

    private record NozzleInfo(
            Vec3 thrusterNozzleWorldPosMC,
            Quaterniond obbRotationWorldJOML,
            Vector3d thrusterCenterBlockWorldJOML
    ) {}
}