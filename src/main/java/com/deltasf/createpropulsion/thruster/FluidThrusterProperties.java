package com.deltasf.createpropulsion.thruster;

public class FluidThrusterProperties {
    public float thrustMultiplier;
    public float consumptionMultiplier;
    
    public static final FluidThrusterProperties DEFAULT = new FluidThrusterProperties(1,1 );

    public FluidThrusterProperties(float thrustMultiplier, float consumptionMultiplier) {
        this.thrustMultiplier = thrustMultiplier;
        this.consumptionMultiplier = consumptionMultiplier;
    }
}