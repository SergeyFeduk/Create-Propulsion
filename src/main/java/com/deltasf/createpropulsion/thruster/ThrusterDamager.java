package com.deltasf.createpropulsion.thruster;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.debug.PropulsionDebug;
import com.deltasf.createpropulsion.debug.routes.MainDebugRoute;
import com.deltasf.createpropulsion.utility.AbstractAreaDamagerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.awt.Color;
import java.util.Optional;

public class ThrusterDamager extends AbstractAreaDamagerBehaviour {
    private static final int LOWEST_POWER_THRSHOLD = 5;

    private record ThrusterDamageContext(Vec3 nozzlePos, float visualPowerPercent) {}

    public ThrusterDamager(SmartBlockEntity be) {
        super(be);
    }

    private AbstractThrusterBlockEntity getThruster() {
        return (AbstractThrusterBlockEntity) this.blockEntity;
    }

    @Override
    protected int getTickFrequency() {
        return 5;
    }

    @Override
    protected boolean shouldDamage() {
        return PropulsionConfig.THRUSTER_DAMAGE_ENTITIES.get()
            && getThruster().isPowered()
            && getThruster().isWorking();
    }

    @Override
    protected DamageSource getDamageSource() {
        return getWorld().damageSources().onFire();
    }

    @Override
    protected Optional<DamageZone> calculateDamageZone() {
        AbstractThrusterBlockEntity thruster = getThruster();
        Direction plumeDirection = thruster.getBlockState().getValue(AbstractThrusterBlock.FACING).getOpposite();

        float visualPowerPercent = ((float)Math.max(thruster.getOverriddenPowerOrState(thruster.getBlockState()), LOWEST_POWER_THRSHOLD) - LOWEST_POWER_THRSHOLD) / 15.0f;
        float distanceByPower = Math.lerp(0.55f, 1.5f, visualPowerPercent);
        double potentialPlumeLength = thruster.getEmptyBlocks() * distanceByPower;
        
        Vec3 nozzlePos = getNozzlePosInWorld(plumeDirection);
        Vec3 worldPlumeDirection = getWorldPlumeDirection(plumeDirection);

        double correctedPlumeLength = performRaycastCheck(nozzlePos, worldPlumeDirection, potentialPlumeLength);
        if (correctedPlumeLength <= 0.01) {
            return Optional.empty();
        }

        Vec3 boxDimensions = new Vec3(1.4, 1.4, correctedPlumeLength);
        double plumeStartOffset = 0.8;
        double centerOffsetDistance = plumeStartOffset + (correctedPlumeLength / 2.0);
        Vec3 boxOffset = Vec3.atLowerCornerOf(plumeDirection.getNormal()).scale(centerOffsetDistance);
        
        ThrusterDamageContext context = new ThrusterDamageContext(nozzlePos, visualPowerPercent);

        return Optional.of(new DamageZone(boxDimensions, boxOffset, plumeDirection, Direction.SOUTH, context));
    }

    @Override
    protected void applyDamage(LivingEntity entity, DamageSource source, DamageZone zone) {
        ThrusterDamageContext context = (ThrusterDamageContext) zone.context();
        
        float invSqrDistance = context.visualPowerPercent() * 8.0f / (float)java.lang.Math.max(1, entity.position().distanceToSqr(context.nozzlePos()));
        float damageAmount = 3 + invSqrDistance;

        entity.hurt(source, damageAmount);
        entity.setSecondsOnFire(3);
    }

    @Override
    protected boolean shouldDebug() {
        return PropulsionDebug.isDebug(MainDebugRoute.THRUSTER);
    }

    @Override
    protected Color getDebugColor() {
        return Color.ORANGE;
    }

    private Vec3 getNozzlePosInWorld(Direction plumeDirection) {
        BlockPos worldPosition = getThruster().getBlockPos();
        Level level = getThruster().getLevel();
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
        Level level = getThruster().getLevel();
        BlockPos worldPosition = getThruster().getBlockPos();
        Vector3d localPlumeVec = VectorConversionsMCKt.toJOMLD(plumeDirection.getNormal());

        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        if (ship != null) {
            ship.getTransform().getShipToWorldRotation().transform(localPlumeVec);
        }
        return VectorConversionsMCKt.toMinecraft(localPlumeVec);
    }


    @SuppressWarnings("null")
    private double performRaycastCheck(Vec3 nozzlePos, Vec3 worldPlumeDirection, double maxDistance) {
        Level level = getThruster().getLevel();
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
}
