package com.deltasf.createpropulsion.propeller.blades;

import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.jozufozu.flywheel.core.PartialModel;

public class WoodenPropellerBladeItem extends PropellerBladeItem {
    public WoodenPropellerBladeItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxBlades() {
        return 4;
    }

    @Override
    public float getGearRatio() {
        return 8;
    }

    @Override
    public PartialModel getModel() {
        return PropulsionPartialModels.WOODEN_BLADE;
    }

    @Override
    public boolean canBeBlurred() { return true; }

    @Override
    public boolean isBladeInverted() { return true; }
}
