package com.deltasf.createpropulsion.utility;

import java.util.ArrayList;
import java.util.List;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.utility.math.MathUtility;
import com.simibubi.create.foundation.collision.Matrix3d;
import com.simibubi.create.foundation.collision.OrientedBB;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class OBBEntityFinder {
    public static List<LivingEntity> getEntitiesInOrientedBox(Level level, BlockPos pos, Direction boxPrimaryAxis, Direction localDirection, Vec3 boxDimensions, Vec3 boxOffset) {
        Quaterniond worldOrientation = calculateWorldOrientation(level, pos, boxPrimaryAxis, localDirection);
        Vec3 worldCenter = calculateWorldCenter(level, pos, boxOffset, worldOrientation);

        //Broad phase
        double inflation = Math.max(boxDimensions.x, Math.max(boxDimensions.y, boxDimensions.z));
        AABB broadPhaseBox = AABB.ofSize(worldCenter, 0, 0, 0).inflate(inflation);
        List<LivingEntity> candidateEntities = level.getEntitiesOfClass(LivingEntity.class, broadPhaseBox);

        if (candidateEntities.isEmpty()) {
            return List.of();
        }

        //Narrow phase
        Vec3 halfExtents = boxDimensions.scale(0.5);
        Matrix3d rotationMatrix = MathUtility.createMatrixFromQuaternion(worldOrientation);
        OrientedBB obb = new OrientedBB(worldCenter, halfExtents, rotationMatrix);

        List<LivingEntity> intersectingEntities = new ArrayList<>();
        for (LivingEntity entity : candidateEntities) {
            if (obb.intersect(entity.getBoundingBox()) != null) {
                intersectingEntities.add(entity);
            }
        }

        return intersectingEntities;
    }

    public static Quaterniond calculateWorldOrientation(Level level, BlockPos pos, Direction boxPrimaryAxis, Direction localDirection) {
        Quaterniond localRotation = new Quaterniond().rotateTo(
            VectorConversionsMCKt.toJOMLD(boxPrimaryAxis.getNormal()), 
            VectorConversionsMCKt.toJOMLD(localDirection.getNormal())
        );

        Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
        if (ship != null) {
            return ship.getTransform().getShipToWorldRotation().mul(localRotation, new Quaterniond());
        } else {
            return localRotation;
        }
    }

    public static Quaterniond calculateWorldOrientation(Ship ship, Direction boxPrimaryAxis, Direction localDirection) {
        Quaterniond localRotation = new Quaterniond().rotateTo(
            VectorConversionsMCKt.toJOMLD(boxPrimaryAxis.getNormal()), 
            VectorConversionsMCKt.toJOMLD(localDirection.getNormal())
        );

        if (ship != null) {
            return ship.getTransform().getShipToWorldRotation().mul(localRotation, new Quaterniond());
        } else {
            return localRotation;
        }
    }


    public static Vec3 calculateWorldCenter(Level level, BlockPos pos, Vec3 localOffset, Quaterniond worldOrientation) {
        Vector3d blockCenterInShip = VectorConversionsMCKt.toJOML(Vec3.atCenterOf(pos));
        Vector3d centerInShip = blockCenterInShip.add(VectorConversionsMCKt.toJOML(localOffset), new Vector3d());

        Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
        if (ship != null) {
            Vector3d worldPos = ship.getShipToWorld().transformPosition(centerInShip, new Vector3d());
            return VectorConversionsMCKt.toMinecraft(worldPos);
        } else {
            return VectorConversionsMCKt.toMinecraft(centerInShip);
        }
    }
}
