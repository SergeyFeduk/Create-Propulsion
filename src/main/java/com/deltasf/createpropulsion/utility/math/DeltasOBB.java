package com.deltasf.createpropulsion.utility.math;

import org.joml.Matrix3f;
import org.joml.Vector3f;

public final class DeltasOBB {
    private DeltasOBB() {}

    public static boolean intersectsAABB(Vector3f obbCenter, Vector3f obbHalfExtents, Matrix3f obbRot, Vector3f aabbMin, Vector3f aabbMax) {
        float bx = (aabbMin.x + aabbMax.x) * 0.5f;
        float by = (aabbMin.y + aabbMax.y) * 0.5f;
        float bz = (aabbMin.z + aabbMax.z) * 0.5f;
        float bex = (aabbMax.x - aabbMin.x) * 0.5f;
        float bey = (aabbMax.y - aabbMin.y) * 0.5f;
        float bez = (aabbMax.z - aabbMin.z) * 0.5f;

        return intersectsAABB(obbCenter, obbHalfExtents, obbRot, bx, by, bz, bex, bey, bez);
    }

    public static boolean intersectsAABB(Vector3f obbCenter, Vector3f obbHalfExtents, Matrix3f obbRot, float bx, float by, float bz, float bex, float bey, float bez) {
        final float ax = obbCenter.x;
        final float ay = obbCenter.y;
        final float az = obbCenter.z;
        final float aex = obbHalfExtents.x;
        final float aey = obbHalfExtents.y;
        final float aez = obbHalfExtents.z;

        final float u0x = obbRot.m00();
        final float u0y = obbRot.m01();
        final float u0z = obbRot.m02();

        final float u1x = obbRot.m10();
        final float u1y = obbRot.m11();
        final float u1z = obbRot.m12();

        final float u2x = obbRot.m20();
        final float u2y = obbRot.m21();
        final float u2z = obbRot.m22();

        final float tx_world = bx - ax;
        final float ty_world = by - ay;
        final float tz_world = bz - az;

        final float R00 = u0x, R01 = u0y, R02 = u0z;
        final float R10 = u1x, R11 = u1y, R12 = u1z;
        final float R20 = u2x, R21 = u2y, R22 = u2z;

        final float EPS = 1e-6f;
        final float absR00 = Math.abs(R00) + EPS, absR01 = Math.abs(R01) + EPS, absR02 = Math.abs(R02) + EPS;
        final float absR10 = Math.abs(R10) + EPS, absR11 = Math.abs(R11) + EPS, absR12 = Math.abs(R12) + EPS;
        final float absR20 = Math.abs(R20) + EPS, absR21 = Math.abs(R21) + EPS, absR22 = Math.abs(R22) + EPS;

        final float t0 = tx_world * u0x + ty_world * u0y + tz_world * u0z;
        final float t1 = tx_world * u1x + ty_world * u1y + tz_world * u1z;
        final float t2 = tx_world * u2x + ty_world * u2y + tz_world * u2z;

        //SAT SLOP
        {
            final float ra = aex;
            final float rb = bex * absR00 + bey * absR01 + bez * absR02;
            if (Math.abs(t0) > ra + rb) return false;
        }
        {
            final float ra = aey;
            final float rb = bex * absR10 + bey * absR11 + bez * absR12;
            if (Math.abs(t1) > ra + rb) return false;
        }
        {
            final float ra = aez;
            final float rb = bex * absR20 + bey * absR21 + bez * absR22;
            if (Math.abs(t2) > ra + rb) return false;
        }
        {
            final float ra = aex * absR00 + aey * absR10 + aez * absR20;
            final float rb = bex;
            if (Math.abs(tx_world) > ra + rb) return false;
        }
        {
            final float ra = aex * absR01 + aey * absR11 + aez * absR21;
            final float rb = bey;
            if (Math.abs(ty_world) > ra + rb) return false;
        }
        {
            final float ra = aex * absR02 + aey * absR12 + aez * absR22;
            final float rb = bez;
            if (Math.abs(tz_world) > ra + rb) return false;
        }
        {
            final float tVal = Math.abs(t2 * R10 - t1 * R20);
            final float ra = aey * absR20 + aez * absR10;
            final float rb = bey * absR02 + bez * absR01;
            if (tVal > ra + rb) return false;
        }
        {
            final float tVal = Math.abs(t2 * R11 - t1 * R21);
            final float ra = aey * absR21 + aez * absR11;
            final float rb = bex * absR02 + bez * absR00;
            if (tVal > ra + rb) return false;
        }
        {
            final float tVal = Math.abs(t2 * R12 - t1 * R22);
            final float ra = aey * absR22 + aez * absR12;
            final float rb = bex * absR01 + bey * absR00;
            if (tVal > ra + rb) return false;
        }
        {
            final float tVal = Math.abs(t0 * R20 - t2 * R00);
            final float ra = aex * absR20 + aez * absR00;
            final float rb = bey * absR12 + bez * absR11;
            if (tVal > ra + rb) return false;
        }
        {
            final float tVal = Math.abs(t0 * R21 - t2 * R01);
            final float ra = aex * absR21 + aez * absR01;
            final float rb = bex * absR12 + bez * absR10;
            if (tVal > ra + rb) return false;
        }
        {
            final float tVal = Math.abs(t0 * R22 - t2 * R02);
            final float ra = aex * absR22 + aez * absR02;
            final float rb = bex * absR11 + bey * absR10;
            if (tVal > ra + rb) return false;
        }
        {
            final float tVal = Math.abs(t1 * R00 - t0 * R10);
            final float ra = aex * absR10 + aey * absR00;
            final float rb = bey * absR22 + bez * absR21;
            if (tVal > ra + rb) return false;
        }
        {
            final float tVal = Math.abs(t1 * R01 - t0 * R11);
            final float ra = aex * absR11 + aey * absR01;
            final float rb = bex * absR22 + bez * absR20;
            if (tVal > ra + rb) return false;
        }
        {
            final float tVal = Math.abs(t1 * R02 - t0 * R12);
            final float ra = aex * absR12 + aey * absR02;
            final float rb = bex * absR21 + bey * absR20;
            if (tVal > ra + rb) return false;
        }
        return true;
    }
}
