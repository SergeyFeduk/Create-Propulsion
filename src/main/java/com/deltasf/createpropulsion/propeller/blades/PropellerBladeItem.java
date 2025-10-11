package com.deltasf.createpropulsion.propeller.blades;

import com.jozufozu.flywheel.core.PartialModel;

import net.minecraft.world.item.Item;

public abstract class PropellerBladeItem extends Item {
    public PropellerBladeItem(Properties properties) {
        super(properties);
    }

    public abstract int getMaxBlades();

    public abstract float getGearRatio();

    public abstract PartialModel getModel();

    public abstract boolean canBeBlurred();
}
