package com.deltasf.createpropulsion.heat;

public interface IHeatSource {
    int extractHeat(int amount, boolean simulate);

    void generateHeat(int amount);

    int getHeatStored();

    int getMaxHeatStored();
}
