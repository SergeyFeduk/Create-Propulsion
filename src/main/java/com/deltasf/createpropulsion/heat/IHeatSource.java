package com.deltasf.createpropulsion.heat;

public interface IHeatSource {
    float extractHeat(float amount, boolean simulate);

    void generateHeat(float amount);

    float getHeatStored();

    float getMaxHeatStored();
}
