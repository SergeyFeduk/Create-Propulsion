package com.deltasf.createpropulsion.propeller.blades;

import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.primitives.AABBi;

public class BladeProperties {
    public static final BladeProperties DEFAULT = new BladeProperties();

    public int maxBlades = 1;
    public float gearRatio = 1f;
    public String model = "createpropulsion:block/missing";
    public boolean canBeBlurred = false;
    public boolean isBladeInverted = false;
    public float fluidEfficiency = 0f;
    public float airEfficiency = 0f;
    public float damageModifier = 0f;
    public double[] damageZone = {0, 0, 0, 0, 0, 0};
    public double[] damageZoneOffset = {0, 0, 0};
    public float torqueFactor = 0f;
    public int[] softObstructionRegion = {0, 0, 0, 0, 0, 0};
    public float stressImpact = 8.0f;
    public String breakDrop = "minecraft:air";

    private transient PartialModel cachedModel;
    private transient AABB cachedDamageZone;
    private transient Vec3 cachedOffset;
    private transient AABBi cachedObstruction;

    public PartialModel getModel() {
        if (cachedModel == null) {
            cachedModel = PropulsionPartialModels.BLADE_MODELS.getOrDefault(model, PropulsionPartialModels.WOODEN_BLADE);
        }
        return cachedModel;
    }

    public AABB getDamageZone() {
        if (cachedDamageZone == null) cachedDamageZone = new AABB(damageZone[0], damageZone[1], damageZone[2], damageZone[3], damageZone[4], damageZone[5]);
        return cachedDamageZone;
    }

    public Vec3 getDamageZoneOffset() {
        if (cachedOffset == null) cachedOffset = new Vec3(damageZoneOffset[0], damageZoneOffset[1], damageZoneOffset[2]);
        return cachedOffset;
    }

    public AABBi getSoftObstructionRegion() {
        if (cachedObstruction == null) cachedObstruction = new AABBi((int)softObstructionRegion[0], (int)softObstructionRegion[1], (int)softObstructionRegion[2], (int)softObstructionRegion[3], (int)softObstructionRegion[4], (int)softObstructionRegion[5]);
        return cachedObstruction;
    }

    public ItemStack getBreakDrop() {
        String[] parts = breakDrop.split(":");
        ResourceLocation rl = parts.length == 2 
            ? ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]) 
            : ResourceLocation.fromNamespaceAndPath("minecraft", breakDrop);
        return new ItemStack(ForgeRegistries.ITEMS.getValue(rl));
    }
}
