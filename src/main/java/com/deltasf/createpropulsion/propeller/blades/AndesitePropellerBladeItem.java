package com.deltasf.createpropulsion.propeller.blades;

import org.joml.primitives.AABBi;

import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.AllItems;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AndesitePropellerBladeItem extends PropellerBladeItem {
    private static final float size = 0.8f;
    private static final float thickness = 0.125f;
    private static final AABB damageAABB = new AABB(-size, -size, -thickness, size, size, thickness);
    private static final Vec3 damageOffset = new Vec3(0.25, 0, 0);

    private static final AABBi softObstructionRegion = new AABBi(-1,-1,0,1,1,3);

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
    public float getFluidEfficiency() { return 0.5f; }

    @Override
    public float getAirEfficiency() { return 0.8f; }

    @Override
    public float getDamageModifier() { return 1.2f; }

    @Override
    public AABB getDamageZone() { return damageAABB; }

    @Override
    public Vec3 getDamageZoneOffset() { return damageOffset; }

    @Override
    public float getTorqueFactor() { return 0; }

    @Override
    public AABBi getSoftObstructionRegion() { return softObstructionRegion; }

    @Override
    public ItemStack getBreakDrop() {
        return new ItemStack(AllItems.ANDESITE_ALLOY);
    }
}
