package com.deltasf.createpropulsion.utility.math;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector2f;
import org.joml.Vector3d;

import com.simibubi.create.foundation.collision.Matrix3d;

import net.minecraft.core.Vec3i;

public class MathUtility {
    public static final double epsilon = 1e-5;
    
    //Very cool interpolation function stolen from Freya
    public static float expDecay(float a, float b, float decay, float dt) {
        return b + (a - b) * (float) Math.exp(-decay * dt);
    }

    public static Matrix3d createMatrixFromQuaternion(Quaterniond quaternion) {
        //TODO: Perhaps use accesstransformer for this?
        //I need to do this very ugly thing because create matrix class has its elements private
        double qx = quaternion.x;
        double qy = quaternion.y;
        double qz = quaternion.z;
        double qw = quaternion.w;
        double lengthSq = qx * qx + qy * qy + qz * qz + qw * qw;
        double invLength = 1.0 / Math.sqrt(lengthSq);

        double x = qx * invLength;
        double y = qy * invLength;
        double z = qz * invLength;
        double w = qw * invLength;
        double roll, pitch, yaw;

        // Singularity check
        double sinp = 2.0 * (w * y - z * x);

        if (Math.abs(sinp) > 0.999999) { // Gimbal lock prevention
            pitch = Math.PI / 2.0 * Math.signum(sinp);
            roll = Math.atan2(2.0 * (x * y + w * z), 1.0 - 2.0 * (y * y + z * z));
            yaw = 0.0;

        } else {
            roll = Math.atan2(2.0 * (w * x + y * z), 1.0 - 2.0 * (x * x + y * y));
            pitch = Math.asin(sinp);
            yaw = Math.atan2(2.0 * (w * z + x * y), 1.0 - 2.0 * (y * y + z * z));
        }

        Matrix3d resultMatrix = new Matrix3d();
        Matrix3d tempY = new Matrix3d();
        Matrix3d tempX = new Matrix3d();
        resultMatrix.asZRotation((float) yaw);
        tempY.asYRotation((float) pitch);
        resultMatrix.multiply(tempY);
        tempX.asXRotation((float) roll);
        resultMatrix.multiply(tempX);

        return resultMatrix;
    }

    public static Vector2f toHorizontalCoordinateSystem(Quaterniondc shipRotation) {
        Vector3d worldForwardDirection = new Vector3d();
        Vector3d LOCAL_SHIP_FORWARD_NEGATIVE_Z = new Vector3d(0.0, 0.0, -1.0);
        shipRotation.transform(LOCAL_SHIP_FORWARD_NEGATIVE_Z, worldForwardDirection);

        if (worldForwardDirection.lengthSquared() < 1.0e-12) {
            return new Vector2f(0.0f, 0.0f);
        }

        double horizontalDistance = Math.sqrt(worldForwardDirection.x * worldForwardDirection.x + worldForwardDirection.z * worldForwardDirection.z);

        float yaw;
        if (horizontalDistance < 1.0e-9) {
            yaw = 0.0f;
        } else {
            yaw = (float) Math.toDegrees(Math.atan2(worldForwardDirection.x, -worldForwardDirection.z));
        }

        float pitch;
        if (horizontalDistance < 1.0e-9) {
            if (worldForwardDirection.y > 0.0) {
                pitch = 90.0f;
            } else if (worldForwardDirection.y < 0.0) {
                pitch = -90.0f;
            } else {
                pitch = 0.0f;
            }
        } else {
            pitch = (float) Math.toDegrees(Math.atan2(worldForwardDirection.y, horizontalDistance));
        }

        return new Vector2f(yaw, pitch);
    }

    public static Vec3i AbsComponents(Vec3i value) {
        return new Vec3i(Math.abs(value.getX()), Math.abs(value.getY()), Math.abs(value.getZ()));
    }

    public static float sineInRange(float time, float bottom, float top) {
        float sin = (float) Math.sin(time);
        float normalized = (sin + 1.0f) * 0.5f;
        return bottom + normalized * (top - bottom);
    }
}
