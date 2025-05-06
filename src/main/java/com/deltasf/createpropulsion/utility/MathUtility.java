package com.deltasf.createpropulsion.utility;

import org.joml.Quaterniond;

import com.simibubi.create.foundation.collision.Matrix3d;

public class MathUtility {
    
    public static Matrix3d createMatrixFromQuaternion(Quaterniond quaternion) {
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
}
