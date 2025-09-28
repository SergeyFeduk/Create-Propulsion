package com.deltasf.createpropulsion.heat;

public interface IHeatConsumer {
    boolean isActive();

    float getOperatingThreshold();
    
    float consumeHeat(float maxAvailable, boolean simulate);
}
