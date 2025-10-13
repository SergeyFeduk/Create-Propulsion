package com.deltasf.createpropulsion.propeller.blades;

import org.joml.primitives.AABBi;

import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.AllItems;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CopperPropellerBladeItem extends PropellerBladeItem {
    private static final float size = 1.2f;
    private static final float thickness = 0.125f;
    private static final AABB damageAABB = new AABB(-size, -size, -thickness, size, size, thickness);
    private static final Vec3 damageOffset = new Vec3(0.25, 0, 0);

    private static final AABBi softObstructionRegion = new AABBi(-1,-1,0,1,1,4);

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

    @Override
    public float getFluidEfficiency() { return 1.0f; }

    @Override
    public float getAirEfficiency() { return 0.05f; }

    @Override
    public float getDamageModifier() { return 1.0f; }

    @Override
    public AABB getDamageZone() { return damageAABB; }

    @Override
    public Vec3 getDamageZoneOffset() { return damageOffset; }

    @Override
    public float getTorqueFactor() { return 1; }

    @Override
    public AABBi getSoftObstructionRegion() { return softObstructionRegion; }

    @Override
    public float getStressImpact() {
        return 16.0f;
    }

    public ItemStack getBreakDrop() {
        return new ItemStack(AllItems.COPPER_NUGGET);
    }
}
