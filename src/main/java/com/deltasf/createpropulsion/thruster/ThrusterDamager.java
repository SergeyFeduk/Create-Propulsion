package com.deltasf.createpropulsion.thruster;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.debug.DebugRenderer;
import com.deltasf.createpropulsion.debug.PropulsionDebug;
import com.deltasf.createpropulsion.debug.routes.MainDebugRoute;
import com.deltasf.createpropulsion.utility.OBBEntityFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
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
        Direction plumeDirection = thruster.getBlockState().getValue(AbstractThrusterBlock.FACING).getOpposite();

        float visualPowerPercent = ((float)Math.max(thruster.getOverriddenPowerOrState(thruster.getBlockState()), LOWEST_POWER_THRSHOLD) - LOWEST_POWER_THRSHOLD) / 15.0f;
        float distanceByPower = Math.lerp(0.55f, 1.5f, visualPowerPercent);
        double potentialPlumeLength = thruster.getEmptyBlocks() * distanceByPower;

        Vec3 nozzlePos = getNozzlePosInWorld(plumeDirection);
        Vec3 worldPlumeDirection = getWorldPlumeDirection(plumeDirection);

        double correctedPlumeLength = performRaycastCheck(nozzlePos, worldPlumeDirection, potentialPlumeLength);
        if (correctedPlumeLength <= 0.01) return;

        Vec3 boxDimensions = new Vec3(1.4, 1.4, correctedPlumeLength);
        double plumeStartOffset = 0.8;
        double centerOffsetDistance = plumeStartOffset + (correctedPlumeLength / 2.0);
        Vec3i normal = plumeDirection.getNormal();
        Vec3 boxOffset = new Vec3(normal.getX(), normal.getY(), normal.getZ()).scale(centerOffsetDistance);

        List<LivingEntity> damageCandidates = OBBEntityFinder.getEntitiesInOrientedBox(
            thruster.getLevel(),
            thruster.getBlockPos(),
            Direction.SOUTH,
            plumeDirection,
            boxDimensions,
            boxOffset
        );


         if (PropulsionDebug.isDebug(MainDebugRoute.THRUSTER)) {
            // Debug logic remains largely the same, just simplified
            debugObb(plumeDirection, boxDimensions, boxOffset);
        }

        if (damageCandidates.isEmpty()) {
            return;
        }

        applyDamageToEntities(thruster.getLevel(), damageCandidates, nozzlePos, visualPowerPercent);
    }

    private Vec3 getNozzlePosInWorld(Direction plumeDirection) {
        BlockPos worldPosition = thruster.getBlockPos();
        Level level = thruster.getLevel();
        Vector3d thrusterCenterBlockShipCoords = VectorConversionsMCKt.toJOML(Vec3.atCenterOf(worldPosition));

        Quaterniond relativeRotation = new Quaterniond().rotateTo(new Vector3d(0, 0, 1), VectorConversionsMCKt.toJOMLD(plumeDirection.getNormal()));

        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        Vector3d thrusterCenterBlockWorld;
        Quaterniond obbRotationWorld;

        if (ship != null) {
            thrusterCenterBlockWorld = ship.getShipToWorld().transformPosition(thrusterCenterBlockShipCoords, new Vector3d());
            obbRotationWorld = ship.getTransform().getShipToWorldRotation().mul(relativeRotation, new Quaterniond());
        } else {
            thrusterCenterBlockWorld = thrusterCenterBlockShipCoords;
            obbRotationWorld = relativeRotation;
        }

        Vector3d nozzleOffsetLocal = new Vector3d(0, 0, 0.5);
        Vector3d nozzleOffsetWorld = obbRotationWorld.transform(nozzleOffsetLocal, new Vector3d());
        Vector3d thrusterNozzleWorldPos = thrusterCenterBlockWorld.add(nozzleOffsetWorld, new Vector3d());
        
        return VectorConversionsMCKt.toMinecraft(thrusterNozzleWorldPos);
    }

    private Vec3 getWorldPlumeDirection(Direction plumeDirection) {
        Level level = thruster.getLevel();
        BlockPos worldPosition = thruster.getBlockPos();
        Vector3d localPlumeVec = VectorConversionsMCKt.toJOMLD(plumeDirection.getNormal());

        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        if (ship != null) {
            ship.getTransform().getShipToWorldRotation().transform(localPlumeVec);
        }
        return VectorConversionsMCKt.toMinecraft(localPlumeVec);
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

    private void debugObb(Direction plumeDirection, Vec3 boxDimensions, Vec3 boxOffset) {
        Quaterniond worldOrientation = OBBEntityFinder.calculateWorldOrientation(thruster.getLevel(), thruster.getBlockPos(), Direction.SOUTH, plumeDirection);
        Vec3 worldCenter = OBBEntityFinder.calculateWorldCenter(thruster.getLevel(), thruster.getBlockPos(), boxOffset, worldOrientation);
        
        String identifier = "thruster_" + thruster.hashCode() + "_obb";
        Quaternionf debugRotation = new Quaternionf((float)worldOrientation.x, (float)worldOrientation.y, (float)worldOrientation.z, (float)worldOrientation.w);

        DebugRenderer.drawBox(identifier, worldCenter, boxDimensions, debugRotation, Color.ORANGE, false, TICKS_PER_ENTITY_CHECK + 1);
    }

    private void applyDamageToEntities(Level level, List<LivingEntity> damageCandidates, Vec3 nozzlePos, float visualPowerPercent) {
        DamageSource fireDamageSource = level.damageSources().onFire();
        for (Entity entity : damageCandidates) {
            if (entity.isRemoved() || entity.fireImmune()) continue;
            
            float invSqrDistance = visualPowerPercent * 8.0f / (float)Math.max(1, entity.position().distanceToSqr(nozzlePos));
            float damageAmount = 3 + invSqrDistance;

            entity.hurt(fireDamageSource, damageAmount);
            entity.setSecondsOnFire(3);
        }
    }
}
