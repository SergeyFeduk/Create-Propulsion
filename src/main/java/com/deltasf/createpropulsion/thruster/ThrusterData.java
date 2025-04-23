package com.deltasf.createpropulsion.thruster;

import org.joml.Vector3d;

public class ThrusterData {
    private volatile float thrust;
    public float getThrust() { return thrust; }
    public void setThrust(float thrust) { this.thrust = thrust; }
    //Direction in ship space. Expected to be normalized
    private volatile Vector3d direction;
    public Vector3d getDirection() { return direction; }
    public void setDirection(Vector3d direction) { this.direction = direction; }
}
