package com.deltasf.createpropulsion.propeller.blades;

import org.joml.primitives.AABBi;

import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.jozufozu.flywheel.core.PartialModel;

import net.minecraft.world.phys.AABB;

public class AndesitePropellerBladeItem extends PropellerBladeItem {
    private static final AABB damageAABB = new AABB(0,0,0,0,0,0);
    private static final AABBi hardObstructionRegion = new AABBi(0,0,0,0,0,0);
    private static final AABBi softObstructionRegion = new AABBi(0,0,0,0,0,0);

    public AndesitePropellerBladeItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxBlades() {
        return 6;
    }

    @Override
    public float getGearRatio() {
        return 8; //Not sure if 8 is the best value, test
    }

    @Override
    public PartialModel getModel() {
        return PropulsionPartialModels.ANDESITE_BLADE;
    }

    @Override
    public boolean canBeBlurred() { return true; }

    @Override
    public boolean isBladeInverted() { return true; }

    @Override
    public float getFluidEfficiency() { return 0.8f; }

    @Override
    public float getAirEfficiency() { return 0.8f; }

    @Override
    public float getDamageModifier() { return 1.2f; }

    @Override
    public AABB getDamageZone() { return damageAABB; }

    @Override
    public float getTorqueFactor() { return 0; }

    @Override
    public AABBi getHardObstructionRegion() { return hardObstructionRegion; }

    @Override
    public AABBi getSoftObstructionRegion() { return softObstructionRegion; }
}
