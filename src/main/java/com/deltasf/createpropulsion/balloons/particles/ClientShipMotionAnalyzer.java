package com.deltasf.createpropulsion.balloons.particles;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.valkyrienskies.core.api.ships.ClientShip;

public class ClientShipMotionAnalyzer {
    private static final int SMOOTHING_WINDOW = 3;
    private static final double DT = 0.05;
    private static final double INV_DT = 1.0 / DT;

    //History for smoothing
    private final Vector3d[] linearAccelerationHistory = new Vector3d[SMOOTHING_WINDOW];
    private final Vector3d[] angularAccelerationHistory = new Vector3d[SMOOTHING_WINDOW];
    private int historyIndex = 0;

    private final Vector3d previousVelocity = new Vector3d();
    private final Vector3d previousOmega = new Vector3d();
    private boolean initialized = false;

    //Outputs
    public final Vector3f linearInertia = new Vector3f();
    public final Vector3f angularInertia = new Vector3f();
    public final Vector3f worldUpInLocal = new Vector3f();
    
    //Cache
    private final Vector3d tmpLinearAcceleration = new Vector3d();
    private final Vector3d tmpAngularAcceleration = new Vector3d();
    private final Vector3d tmpAverageLinear = new Vector3d();
    private final Vector3d tmpAverageAngular = new Vector3d();
    private final Quaterniond tmpQuaternion = new Quaterniond();
    private final Vector3d tmpUp = new Vector3d();

    public ClientShipMotionAnalyzer() {
        for (int i = 0; i < SMOOTHING_WINDOW; i++) {
            linearAccelerationHistory[i] = new Vector3d();
            angularAccelerationHistory[i] = new Vector3d();
        }
    }

    public void tick(ClientShip ship) {
        if (!initialized) {
            previousVelocity.set(ship.getVelocity());
            previousOmega.set(ship.getAngularVelocity());
            initialized = true;
            return;
        }

        tmpLinearAcceleration.set(ship.getVelocity()).sub(previousVelocity).mul(INV_DT);
        tmpAngularAcceleration.set(ship.getAngularVelocity()).sub(previousOmega).mul(INV_DT);

        //Smooth acceleration
        linearAccelerationHistory[historyIndex].set(tmpLinearAcceleration);
        angularAccelerationHistory[historyIndex].set(tmpAngularAcceleration);
        historyIndex = (historyIndex + 1) % SMOOTHING_WINDOW;

        //Reset accumulators
        tmpAverageLinear.zero();
        tmpAverageAngular.zero();
        
        for (int i = 0; i < SMOOTHING_WINDOW; i++) {
            tmpAverageLinear.add(linearAccelerationHistory[i]);
            tmpAverageAngular.add(angularAccelerationHistory[i]);
        }
        tmpAverageLinear.div(SMOOTHING_WINDOW);
        tmpAverageAngular.div(SMOOTHING_WINDOW);

        //WTS rotation
        tmpQuaternion.set(ship.getTransform().getShipToWorldRotation()).conjugate();
        
        //Transform the acceleration vectors into local space
        tmpQuaternion.transform(tmpAverageLinear);
        tmpQuaternion.transform(tmpAverageAngular);

        linearInertia.set((float)-tmpAverageLinear.x, (float)-tmpAverageLinear.y, (float)-tmpAverageLinear.z);
        angularInertia.set((float)-tmpAverageAngular.x, (float)-tmpAverageAngular.y, (float)-tmpAverageAngular.z);

        previousVelocity.set(ship.getVelocity());
        previousOmega.set(ship.getAngularVelocity());

        //Local world-space upwards
        tmpUp.set(0.0, 1.0, 0.0);
        tmpQuaternion.transform(tmpUp);
        worldUpInLocal.set((float)tmpUp.x, (float)tmpUp.y, (float)tmpUp.z);
    }
}