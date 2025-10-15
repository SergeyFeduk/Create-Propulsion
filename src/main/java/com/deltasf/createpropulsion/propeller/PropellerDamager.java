package com.deltasf.createpropulsion.propeller;

import java.util.Optional;

import com.deltasf.createpropulsion.debug.PropulsionDebug;
import com.deltasf.createpropulsion.debug.routes.PropellerDebugRoute;
import com.deltasf.createpropulsion.propeller.blades.PropellerBladeItem;
import com.deltasf.createpropulsion.utility.AbstractAreaDamagerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;

public class PropellerDamager extends AbstractAreaDamagerBehaviour {
    private static final float MIN_SPEED_FOR_DAMAGE = 16.0f;

    public PropellerDamager(SmartBlockEntity be) {
        super(be);
    }

    private PropellerBlockEntity getPropeller() {
        return (PropellerBlockEntity) this.blockEntity;
    }

    @Override
    protected int getTickFrequency() {
        return 2;
    }

    @Override
    protected boolean shouldDamage() {
        PropellerBlockEntity propeller = getPropeller();
        return propeller.getBlade().isPresent()
            && Math.abs(propeller.getSpeed()) > MIN_SPEED_FOR_DAMAGE;
    }

    @Override
    protected DamageSource getDamageSource() {
        return getWorld().damageSources().generic();
    }

    @Override
    protected Optional<DamageZone> calculateDamageZone() {
        PropellerBlockEntity propeller = getPropeller();
        Optional<PropellerBladeItem> bladeOpt = propeller.getBlade();
        
        if (bladeOpt.isEmpty()) {
            return Optional.empty();
        }
        
        PropellerBladeItem blade = bladeOpt.get();
        
        // Get the damage zone from the blade
        AABB bladeDamageZone = blade.getDamageZone();
        Vec3 bladeOffset = blade.getDamageZoneOffset();
        
        Vec3 dimensions = new Vec3(
            bladeDamageZone.getXsize(),
            bladeDamageZone.getYsize(),
            bladeDamageZone.getZsize()
        );
        
        // Get the facing direction of the propeller
        Direction facing = propeller.getBlockState().getValue(PropellerBlock.FACING);

        if (facing == Direction.WEST) {
            bladeOffset = bladeOffset.multiply(-1, -1, -1);
        } else if (facing == Direction.SOUTH) {
            bladeOffset = new Vec3(bladeOffset.z, bladeOffset.y, bladeOffset.x);
        } else if (facing == Direction.NORTH) {
            bladeOffset = new Vec3(-bladeOffset.z, -bladeOffset.y, -bladeOffset.x);
        } else if (facing == Direction.UP) {
            bladeOffset = new Vec3(bladeOffset.y, bladeOffset.x, bladeOffset.z);
        } else if (facing == Direction.DOWN) {
            bladeOffset = new Vec3(-bladeOffset.y, -bladeOffset.x, -bladeOffset.z);
        }
        
        // Create the damage zone with the blade as context
        return Optional.of(new DamageZone(dimensions, bladeOffset, facing, Direction.SOUTH, blade));
    }

    @Override
    protected void applyDamage(LivingEntity entity, DamageSource source, DamageZone zone) {
        PropellerBladeItem blade = getPropeller().getBlade().get();
        float rpmPercent = Math.abs(getPropeller().getSpeed()) / PropellerBlockEntity.MAX_EFFECTIVE_SPEED;
        float baseDamage = 2.0f + rpmPercent * 3.0f;
        entity.hurt(source, baseDamage * blade.getDamageModifier());
    }

    @Override
    protected boolean shouldDebug() {
        return PropulsionDebug.isDebug(PropellerDebugRoute.DAMAGE);
    }

    @Override
    protected Color getDebugColor() {
        return Color.CYAN;
    }
}
