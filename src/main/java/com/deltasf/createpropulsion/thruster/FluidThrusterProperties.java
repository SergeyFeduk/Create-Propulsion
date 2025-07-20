package com.deltasf.createpropulsion.thruster;

import net.minecraft.network.FriendlyByteBuf;

public class FluidThrusterProperties {
    public float thrustMultiplier;
    public float consumptionMultiplier;
    
    public static final FluidThrusterProperties DEFAULT = new FluidThrusterProperties(1,1 );

    public FluidThrusterProperties(float thrustMultiplier, float consumptionMultiplier) {
        this.thrustMultiplier = thrustMultiplier;
        this.consumptionMultiplier = consumptionMultiplier;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(this.thrustMultiplier);
        buf.writeFloat(this.consumptionMultiplier);
    }

    public static FluidThrusterProperties decode(FriendlyByteBuf buf) {
        return new FluidThrusterProperties(buf.readFloat(), buf.readFloat());
    }    
}