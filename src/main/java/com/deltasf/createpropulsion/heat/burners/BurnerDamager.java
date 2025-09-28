package com.deltasf.createpropulsion.heat.burners;

import java.util.List;

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

        if (heatLevel == HeatLevel.KINDLED) {
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
                boxDimensions,
                boxOffset
        );

        if (entitiesToDamage.isEmpty()) {
            return false;
        }
        DamageSource fireDamageSource = level.damageSources().hotFloor();

        for (LivingEntity entity : entitiesToDamage) {
            entity.hurt(fireDamageSource, 3.0f);
        }

        return true;
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }
}
