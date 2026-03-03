package com.deltasf.createpropulsion.propeller.blades;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.primitives.AABBi;

public class PropellerBladeItem extends Item {
    private final ResourceLocation bladeId;

    public PropellerBladeItem(Properties properties, ResourceLocation bladeId) {
        super(properties);
        this.bladeId = bladeId;
    }

    private BladeProperties getData() {
        return BladeDataManager.get(bladeId);
    }

    public int getMaxBlades() { return getData().maxBlades; }
    public float getGearRatio() { return getData().gearRatio; }
    public PartialModel getModel() { return getData().getModel(); }
    public boolean canBeBlurred() { return getData().canBeBlurred; }
    public boolean isBladeInverted() { return getData().isBladeInverted; }
    public float getFluidEfficiency() { return getData().fluidEfficiency; }
    public float getAirEfficiency() { return getData().airEfficiency; }
    public float getDamageModifier() { return getData().damageModifier; }
    public AABB getDamageZone() { return getData().getDamageZone(); }
    public Vec3 getDamageZoneOffset() { return getData().getDamageZoneOffset(); }
    public float getTorqueFactor() { return getData().torqueFactor; }
    public AABBi getSoftObstructionRegion() { return getData().getSoftObstructionRegion(); }
    public float getStressImpact() { return getData().stressImpact; }
    public ItemStack getBreakDrop() { return getData().getBreakDrop(); }
}
