package com.deltasf.createpropulsion.heat;

public interface IHeatSource {
    int extractHeat(int amount, boolean simulate);

    int getHeatStored();

    int getMaxHeatStored();
}
