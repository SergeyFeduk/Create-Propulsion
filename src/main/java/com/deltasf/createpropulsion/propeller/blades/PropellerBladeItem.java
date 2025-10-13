package com.deltasf.createpropulsion.propeller.blades;

import org.joml.primitives.AABBi;

import com.jozufozu.flywheel.core.PartialModel;

import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class PropellerBladeItem extends Item {
    public PropellerBladeItem(Properties properties) {
        super(properties);
    }

    /**
     * Return the maxium amount of blades allowed per propeller
     **/
    public abstract int getMaxBlades();

    /**
     * Used to calculate visual RPM of the propeller 
     **/
    public abstract float getGearRatio();

    /**
     * Return a registered partial model for this blade. It's base cube must be centered exactly like it is centered for other blades
     **/
    public abstract PartialModel getModel();

    /**
     * Set this to true if blades should become blurred at high RPMS
     **/
    public abstract boolean canBeBlurred();

    /**
     * Set this to true if by default blade implementation should PULL. If false - blade will PUSH
     **/
    public boolean isBladeInverted() {
        return false;
    }

    /**
     * Return efficiency of this blade while propeller is in water
     **/
    public abstract float getFluidEfficiency();

    /**
     * Return efficiency of this blade while propeller is in air
     **/
    public abstract float getAirEfficiency();

    /**
     * Return the damage modifier for this blade
     **/
    public abstract float getDamageModifier();

    /**
     * Return an AABB relative to north-facing propeller in which entities will be damaged
     **/
    public abstract AABB getDamageZone();

    public abstract Vec3 getDamageZoneOffset();

    /**
     * Returns the multiplier for parasitic torque
     **/
    public abstract float getTorqueFactor();

    /**
     * Returns a region which MUST be free to make propeller function. Defined in blocks
     */
    public abstract AABBi getHardObstructionRegion();

    /**
     * Returns a region which should be free to have high efficiency. Defined in blocks
     */
    public abstract AABBi getSoftObstructionRegion();
}
