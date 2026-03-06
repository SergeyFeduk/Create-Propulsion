package com.deltasf.createpropulsion.utility;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix3f;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.utility.math.DeltasOBB;
import com.deltasf.createpropulsion.utility.math.MathUtility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class OBBEntityFinder {
    private static final Quaterniond TEMP_LOCAL_QUATERNION = new Quaterniond();
    private static final Quaterniond TEMP_WORLD_QUATERNION = new Quaterniond();
    private static final Vector3d TEMP_VECTOR1 = new Vector3d();
    private static final Vector3d TEMP_VECTOR2 = new Vector3d();
    private static final Matrix3f TEMP_OBB_ROTATION = new Matrix3f();
    private static final Vector3f TEMP_OBB_CENTER = new Vector3f();
    private static final Vector3f TEMP_OBB_HALFEXT = new Vector3f();

    public static List<LivingEntity> getEntitiesInOrientedBox(Level level, BlockPos pos, Direction boxPrimaryAxis, Direction localDirection, Vec3 boxDimensions, Vec3 boxOffset) {
        TEMP_WORLD_QUATERNION.identity();
        TEMP_OBB_ROTATION.identity();

        Vec3i dirA = boxPrimaryAxis.getNormal();
        Vec3i dirB = localDirection.getNormal();
        TEMP_VECTOR1.set(dirA.getX(), dirA.getY(), dirA.getZ());
        TEMP_VECTOR2.set(dirB.getX(), dirB.getY(), dirB.getZ());
        TEMP_LOCAL_QUATERNION.rotateTo(TEMP_VECTOR1, TEMP_VECTOR2);

        Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
        if (ship != null) {
            ship.getTransform().getShipToWorldRotation().mul(TEMP_LOCAL_QUATERNION, TEMP_WORLD_QUATERNION);
        } else {
            TEMP_WORLD_QUATERNION.set(TEMP_LOCAL_QUATERNION);
        }

        double centerInShipX = pos.getX() + 0.5 + boxOffset.x;
        double centerInShipY = pos.getY() + 0.5 + boxOffset.y;
        double centerInShipZ = pos.getZ() + 0.5 + boxOffset.z;
        TEMP_VECTOR1.set(centerInShipX, centerInShipY, centerInShipZ);

        if (ship != null) {
            ship.getShipToWorld().transformPosition(TEMP_VECTOR1, TEMP_VECTOR2);
        } else {
            TEMP_VECTOR2.set(TEMP_VECTOR1);
        }

        //Broad phase
        double inflation = Math.max(boxDimensions.x, Math.max(boxDimensions.y, boxDimensions.z));
        AABB broadPhaseBox = AABB.ofSize(new Vec3(TEMP_VECTOR2.x, TEMP_VECTOR2.y, TEMP_VECTOR2.z), 0, 0, 0).inflate(inflation);
        List<LivingEntity> candidateEntities = level.getEntitiesOfClass(LivingEntity.class, broadPhaseBox);
        if (candidateEntities.isEmpty()) return List.of();

        //Narrow phase
        TEMP_OBB_CENTER.set((float) TEMP_VECTOR2.x, (float) TEMP_VECTOR2.y, (float) TEMP_VECTOR2.z);
        TEMP_OBB_HALFEXT.set((float) (boxDimensions.x * 0.5), (float) (boxDimensions.y * 0.5), (float) (boxDimensions.z * 0.5));
        MathUtility.quaternionToMatrix3f(TEMP_WORLD_QUATERNION, TEMP_OBB_ROTATION);

        List<LivingEntity> intersectingEntities = new ArrayList<>();
        for (LivingEntity entity : candidateEntities) {
            AABB bb = entity.getBoundingBox();
            float bx = (float) ((bb.minX + bb.maxX) * 0.5);
            float by = (float) ((bb.minY + bb.maxY) * 0.5);
            float bz = (float) ((bb.minZ + bb.maxZ) * 0.5);
            float bex = (float) ((bb.maxX - bb.minX) * 0.5);
            float bey = (float) ((bb.maxY - bb.minY) * 0.5);
            float bez = (float) ((bb.maxZ - bb.minZ) * 0.5);
            if (DeltasOBB.intersectsAABB(TEMP_OBB_CENTER, TEMP_OBB_HALFEXT, TEMP_OBB_ROTATION, bx, by, bz, bex, bey, bez)) {
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
