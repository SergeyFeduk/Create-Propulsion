package com.deltasf.createpropulsion.propeller;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.joml.Vector3d;

import com.deltasf.createpropulsion.atmosphere.AtmosphereData;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY
)
public class PropellerData {

    public PropellerData() {}

    private volatile float thrust;
    private volatile float torque;
    private volatile Vector3d direction;
    private volatile boolean invertDirection;
    private volatile AtmosphereData atmosphere;

    public float getThrust() { return thrust; }
    public void setThrust(float thrust) { this.thrust = thrust; }

    public float getTorque() { return torque; }
    public void setTorque(float torque) { this.torque = torque; }
    
    public Vector3d getDirection() { return direction; }
    public void setDirection(Vector3d direction) { this.direction = direction; }

    public boolean getInvertDirection() { return invertDirection; }
    public void setInvertDirection(boolean invertDirection) { this.invertDirection = invertDirection; }
    
    public AtmosphereData getAtmosphere() {return atmosphere;};
    public void setAtmosphere(AtmosphereData atmosphere) { this.atmosphere = atmosphere; }
}
