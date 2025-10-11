package com.deltasf.createpropulsion.propeller.blades;

import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.jozufozu.flywheel.core.PartialModel;

public class CopperPropellerBladeItem extends PropellerBladeItem {
    public CopperPropellerBladeItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxBlades() {
        return 3;
    }

    @Override
    public float getGearRatio() {
        return 0.3f;
    }

    @Override
    public PartialModel getModel() {
        return PropulsionPartialModels.COPPER_BLADE;
    }

    @Override
    public boolean canBeBlurred() { return false; }
}
