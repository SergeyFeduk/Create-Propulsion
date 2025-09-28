package com.deltasf.createpropulsion.heat.burners;

import java.awt.Color;
import java.util.List;

import org.joml.Quaterniond;
import org.joml.Quaternionf;

import com.deltasf.createpropulsion.debug.DebugRenderer;
import com.deltasf.createpropulsion.debug.PropulsionDebug;
import com.deltasf.createpropulsion.debug.routes.MainDebugRoute;
import com.deltasf.createpropulsion.heat.burners.solid.SolidBurnerBlockEntity;
import com.deltasf.createpropulsion.utility.OBBEntityFinder;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BurnerDamager extends BlockEntityBehaviour {
    public static final BehaviourType<BurnerDamager> TYPE = new BehaviourType<>();

    private static final int DAMAGE_INTERVAL = 5;
    private int damageCooldown;

    public BurnerDamager(SmartBlockEntity be) { super(be); }

    @Override
    public void tick() {
        super.tick();

        Level level = getWorld();
        if (level.isClientSide()) {
            return;
        }

        if (damageCooldown > 0) {
            damageCooldown--;
            return;
        }

        SolidBurnerBlockEntity burner = (SolidBurnerBlockEntity) blockEntity;
        HeatLevel heatLevel = burner.getBlockState().getValue(AbstractBurnerBlock.HEAT);

        if (heatLevel == HeatLevel.KINDLED && level.getBlockState(getPos().above()).isAir()) {

            if (PropulsionDebug.isDebug(MainDebugRoute.BURNER)) {
                debugObb();
            }

            if (applyDamage(level)) {
                damageCooldown = DAMAGE_INTERVAL;
            }
        }
    }

    private boolean applyDamage(Level level) {
        Vec3 boxDimensions = new Vec3(0.9, 0.1, 0.9);
        Vec3 boxOffset = new Vec3(0, 0.5, 0);

        List<LivingEntity> entitiesToDamage = OBBEntityFinder.getEntitiesInOrientedBox(
                getWorld(),
                getPos(),
                Direction.UP,
                Direction.UP,
                boxDimensions,
                boxOffset
        );

        if (entitiesToDamage.isEmpty()) {
            return false;
        }
        DamageSource fireDamageSource = level.damageSources().hotFloor();

        for (LivingEntity entity : entitiesToDamage) {
            if (entity.isRemoved() || entity.fireImmune()) continue;
            entity.hurt(fireDamageSource, 3.0f);
        }

        return true;
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    private void debugObb() {
        Vec3 boxDimensions = new Vec3(0.9, 0.1, 0.9);
        Vec3 boxOffset = new Vec3(0, 0.5, 0);

        Quaterniond worldOrientation = OBBEntityFinder.calculateWorldOrientation(
            getWorld(), 
            getPos(), 
            Direction.UP, 
            Direction.UP
        );
        Vec3 worldCenter = OBBEntityFinder.calculateWorldCenter(
            getWorld(), 
            getPos(), 
            boxOffset, 
            worldOrientation
        );
        
        String identifier = "burner_" + blockEntity.hashCode() + "_obb";
        Quaternionf debugRotation = new Quaternionf((float)worldOrientation.x, (float)worldOrientation.y, (float)worldOrientation.z, (float)worldOrientation.w);
        
        DebugRenderer.drawBox(identifier, worldCenter, boxDimensions, debugRotation, Color.RED, false, 2);
    }
}
