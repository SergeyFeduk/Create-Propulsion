package com.deltasf.createpropulsion.propeller;

import org.joml.Vector3d;

import com.deltasf.createpropulsion.atmosphere.AtmosphereData;

public class PropellerData {
    private volatile float thrust;
    private volatile Vector3d direction;
    private volatile AtmosphereData atmosphere;

    public float getThrust() { return thrust; }
    public void setThrust(float thrust) { this.thrust = thrust; }
    
    public Vector3d getDirection() { return direction; }
    public void setDirection(Vector3d direction) { this.direction = direction; }
    
    public AtmosphereData getAtmosphere() {return atmosphere;};
    public void setAtmosphere(AtmosphereData atmosphere) { this.atmosphere = atmosphere; }
}
