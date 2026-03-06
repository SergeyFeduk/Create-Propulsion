package com.deltasf.createpropulsion.utility.math;

import org.joml.Matrix3f;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector2f;
import org.joml.Vector3d;

import net.minecraft.core.Vec3i;

public class MathUtility {
    public static final double epsilon = 1e-5;
    
    //Very cool interpolation function stolen from Freya
    public static float expDecay(float a, float b, float decay, float dt) {
        return b + (a - b) * (float) Math.exp(-decay * dt);
    }

    public static void quaternionToMatrix3f(Quaterniond q, Matrix3f out) {
        double qx = q.x, qy = q.y, qz = q.z, qw = q.w;
        double lenSq = qx*qx + qy*qy + qz*qz + qw*qw;
        if (lenSq == 0.0) { out.identity(); return; }
        double inv = 1.0 / Math.sqrt(lenSq);
        double x = qx*inv, y = qy*inv, z = qz*inv, w = qw*inv;

        double xx = x*x, yy = y*y, zz = z*z;
        double xy = x*y, xz = x*z, yz = y*z;
        double wx = w*x, wy = w*y, wz = w*z;

        float m00 = (float)(1.0 - 2.0*(yy + zz));
        float m01 = (float)(2.0*(xy - wz));
        float m02 = (float)(2.0*(xz + wy));
        float m10 = (float)(2.0*(xy + wz));
        float m11 = (float)(1.0 - 2.0*(xx + zz));
        float m12 = (float)(2.0*(yz - wx));
        float m20 = (float)(2.0*(xz - wy));
        float m21 = (float)(2.0*(yz + wx));
        float m22 = (float)(1.0 - 2.0*(xx + yy));

        out.set(
            m00, m01, m02,
            m10, m11, m12,
            m20, m21, m22
        );
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
