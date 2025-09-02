package com.deltasf.createpropulsion.thruster;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.debug.DebugRenderer;
import com.deltasf.createpropulsion.debug.PropulsionDebug;
import com.deltasf.createpropulsion.debug.routes.MainDebugRoute;
import com.deltasf.createpropulsion.registries.PropulsionCommands;
import com.deltasf.createpropulsion.utility.MathUtility;
import com.simibubi.create.foundation.collision.Matrix3d;
import com.simibubi.create.foundation.collision.OrientedBB;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.joml.Math;
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
        //Broad phase
        float visualPowerPercent = ((float)Math.max(thruster.getOverriddenPowerOrState(thruster.getBlockState()), LOWEST_POWER_THRSHOLD) - LOWEST_POWER_THRSHOLD) / 15.0f;
        float distanceByPower = Math.lerp(0.55f,1.5f, visualPowerPercent);
        Direction plumeDirection = thruster.getBlockState().getValue(AbstractThrusterBlock.FACING).getOpposite();
        AABB plumeAABB = calculateAabb(plumeDirection, distanceByPower);
        List<Entity> damageCandidates = thruster.getLevel().getEntities(null, plumeAABB);

        if (PropulsionDebug.isDebug(MainDebugRoute.THRUSTER)) {
            debugObb(plumeDirection, distanceByPower);
        }

        if (damageCandidates.isEmpty()) {
            return;
        }

        //Narrow phase
        NozzleInfo nozzleInfo = calculateNozzleInfo(plumeDirection);
        Vector3d localPlumeVec = new Vector3d(0, 0, 1);
        nozzleInfo.obbRotationWorldJOML().transform(localPlumeVec);
        Vec3 worldPlumeDirection = VectorConversionsMCKt.toMinecraft(localPlumeVec);

        double potentialPlumeLength = thruster.getEmptyBlocks() * distanceByPower;
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

        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        if (ship != null) {
            thrusterCenterBlockWorldJOML = ship.getShipToWorld().transformPosition(thrusterCenterBlockShipCoordsJOMLD, new Vector3d());
            obbRotationWorldJOML = ship.getTransform().getShipToWorldRotation().mul(relativeRotationJOML, new Quaterniond());
        } else {
            thrusterCenterBlockWorldJOML = thrusterCenterBlockShipCoordsJOMLD;
            obbRotationWorldJOML = relativeRotationJOML;
        }

        Vector3d nozzleOffsetLocal = new Vector3d(0, 0, 0.5);
        Vector3d nozzleOffsetWorld = obbRotationWorldJOML.transform(nozzleOffsetLocal, new Vector3d());
        Vector3d thrusterNozzleWorldPos = thrusterCenterBlockWorldJOML.add(nozzleOffsetWorld, new Vector3d());
        Vec3 thrusterNozzleWorldPosMC = VectorConversionsMCKt.toMinecraft(thrusterNozzleWorldPos);

        return new NozzleInfo(thrusterNozzleWorldPosMC, obbRotationWorldJOML, thrusterCenterBlockWorldJOML);
    }

    @SuppressWarnings("null")
    private double performRaycastCheck(Vec3 nozzlePos, Vec3 worldPlumeDirection, double maxDistance) {
        Level level = thruster.getLevel();
        Vec3 endPos = nozzlePos.add(worldPlumeDirection.scale(maxDistance));

        var clipContext = new net.minecraft.world.level.ClipContext(
            nozzlePos,
            endPos,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            null
        );

        net.minecraft.world.phys.BlockHitResult hitResult = level.clip(clipContext);

        if (hitResult.getType() == net.minecraft.world.phys.BlockHitResult.Type.BLOCK) {
            return nozzlePos.distanceTo(hitResult.getLocation());
        }

        return maxDistance;
    }

    private ObbCalculationResult calculateObb(Direction plumeDirection, double plumeLength, NozzleInfo nozzleInfo) {
        double plumeStartOffset = 0.8;
        double centerOffsetDistance = plumeStartOffset + (plumeLength / 2.0);
        Vector3d relativeCenterOffsetJOML = VectorConversionsMCKt.toJOMLD(plumeDirection.getNormal()).mul(centerOffsetDistance);

        BlockPos worldPosition = thruster.getBlockPos();
        Level level = thruster.getLevel();
        Vector3d thrusterCenterBlockShipCoordsJOMLD = VectorConversionsMCKt.toJOML(Vec3.atCenterOf(worldPosition));

        Vector3d obbCenterWorldJOML;
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        if (ship != null) {
            obbCenterWorldJOML = ship.getShipToWorld().transformPosition(relativeCenterOffsetJOML.add(thrusterCenterBlockShipCoordsJOMLD, new Vector3d()), new Vector3d());
        } else {
            obbCenterWorldJOML = nozzleInfo.thrusterCenterBlockWorldJOML().add(relativeCenterOffsetJOML, new Vector3d());
        }

        Vector3d plumeHalfExtentsJOML = new Vector3d(0.7, 0.7, plumeLength / 2.0);
        Vec3 plumeCenterMC = VectorConversionsMCKt.toMinecraft(obbCenterWorldJOML);
        Vec3 plumeHalfExtentsMC = VectorConversionsMCKt.toMinecraft(plumeHalfExtentsJOML);
        Matrix3d plumeRotationMatrix = MathUtility.createMatrixFromQuaternion(nozzleInfo.obbRotationWorldJOML());
        OrientedBB plumeOBB = new OrientedBB(plumeCenterMC, plumeHalfExtentsMC, plumeRotationMatrix);

        return new ObbCalculationResult(plumeLength, nozzleInfo.thrusterNozzleWorldPosMC(), plumeOBB, obbCenterWorldJOML, plumeHalfExtentsJOML, nozzleInfo.obbRotationWorldJOML());
    }

    private AABB calculateAabb(Direction plumeDirection, float distanceByPower) {
        BlockPos worldPosition = thruster.getBlockPos();
        BlockPos blockBehind = worldPosition.relative(plumeDirection);
        int aabbEndOffset = (int)Math.floor(thruster.getEmptyBlocks() * distanceByPower) + 1;
        BlockPos blockEnd = worldPosition.relative(plumeDirection, aabbEndOffset);
        return new AABB(blockBehind).minmax(new AABB(blockEnd)).inflate(1.0);
    }

    private void applyDamageToEntities(Level level, List<Entity> damageCandidates, ObbCalculationResult obbResult, float visualPowerPercent) {
        DamageSource fireDamageSource = level.damageSources().onFire();
        for (Entity entity : damageCandidates) {
            if (entity.isRemoved() || entity.fireImmune()) continue;
            AABB entityAABB = entity.getBoundingBox();
            if (obbResult.plumeOBB.intersect(entityAABB) != null) {
                float invSqrDistance = visualPowerPercent * 8.0f / (float)Math.max(1, entity.position().distanceToSqr(obbResult.thrusterNozzleWorldPosMC));
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

        double potentialPlumeLength = thruster.getEmptyBlocks() * distanceByPower;
        double correctedPlumeLength = performRaycastCheck(nozzleInfo.thrusterNozzleWorldPosMC(), worldPlumeDirection, potentialPlumeLength);
        if (correctedPlumeLength <= 0.01) return;

        ObbCalculationResult obbResult = calculateObb(plumeDirection, correctedPlumeLength, nozzleInfo);

        String identifier = "thruster_" + thruster.hashCode() + "_obb";
        Quaternionf debugRotation = new Quaternionf((float)obbResult.obbRotationWorldJOML.x, (float)obbResult.obbRotationWorldJOML.y, (float)obbResult.obbRotationWorldJOML.z, (float)obbResult.obbRotationWorldJOML.w);
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
