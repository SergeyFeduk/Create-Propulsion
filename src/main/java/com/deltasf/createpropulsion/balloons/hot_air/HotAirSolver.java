package com.deltasf.createpropulsion.balloons.hot_air;

import java.util.UUID;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.ServerShip;

import java.lang.Math;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.injectors.AbstractHotAirInjectorBlockEntity;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.utils.BalloonRegistryUtility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

//"Solver" word is a bit of an overkill, but it sounds cooler this way :D
public class HotAirSolver {
    static final double surfaceAreaFactor = 6;
    static final double epsilon = 0.1;
    static final double holeFactorExponent = 1.5;
    static final double holeInvalidationThresholdPercent = 0.25;
    static final double catastrophicLeakFactor = 1000.0;

    static final double upsideDownThreshold = -0.2;
    static final double upsideDownLeakFactor = 10.0;

    public static boolean tickBalloon(Level level, Balloon balloon, HaiGroup group, BalloonRegistry registry, ServerShip ship) {
        if (balloon.isEmpty()) {
            return true; //Dead in a moment
        }
        
        double hotAirAmount = balloon.hotAir;
        double hotAirChange = 0;
        
        //Hai injections
        for(UUID id : balloon.supportHais) {
            AbstractHotAirInjectorBlockEntity hai = registry.getInjector(level, id); 
            if (hai == null) continue; //May happen on hai destruction, before it got updated in registry
            double injection = hai.getInjectionAmount();
            hotAirChange += injection;
        }
        //Current volume and fullness
        double volume = balloon.getVolumeSize();
        double fullness = hotAirAmount / volume;
        double leakAdjustedFullness = Math.max(fullness, 0.1); //Use min here so leak is still significant for almost empty balloons
        double surfaceArea = surfaceAreaFactor * Math.pow(volume, 2.0/3.0);
        boolean isStructurallyFailed = balloon.holes.size() >= surfaceArea * holeInvalidationThresholdPercent;
        double catastrophicFailureModifier = isStructurallyFailed ? catastrophicLeakFactor : 1.0;

        //Global surface leak
        hotAirChange -= PropulsionConfig.BALLOON_SURFACE_LEAK_FACTOR.get() * catastrophicFailureModifier * surfaceArea * leakAdjustedFullness;
        //Leak caused by ship being upside-down or just angled too much
        Vector3d up = new Vector3d(0.0, 1.0, 0.0).rotate(ship.getTransform().getShipToWorldRotation().normalize(new Quaterniond()), new Vector3d());
        double downness = up.dot(0.0, -1.0, 0.0);
        double leakAmountPercent = downRamp(downness, upsideDownThreshold);
        if (leakAmountPercent > 0.0) {
            double allowedRemaining = (1.0 - leakAmountPercent) * volume;
            if (hotAirAmount > allowedRemaining) {
                double baseRemoval = leakAmountPercent * hotAirAmount;
                double upsideDownLeak = baseRemoval * upsideDownLeakFactor;
                double maxRemovable = hotAirAmount - allowedRemaining;
                if (upsideDownLeak > maxRemovable) upsideDownLeak = maxRemovable;
                hotAirChange -= upsideDownLeak;
            }
        }
        //Hole leak based on y coordinate of each hole. We assume that hot air is evenlt distributed along all y levels
        AABB bounds = balloon.getAABB();
        double maxY = bounds.maxY;
        double height = bounds.maxY - bounds.minY;
        double pressureFloor = maxY - (height * fullness) + 1e-6;

        double activeHoleCount = 0;
        for (BlockPos holePos : balloon.holes) {
            double interpolationValue = (holePos.getY() + 1.0) - pressureFloor;
            double activityFraction = org.joml.Math.clamp(0.0, 1.0, interpolationValue);
            activeHoleCount += activityFraction;
        }
        if (activeHoleCount > 0) {
            hotAirChange -= PropulsionConfig.BALLOON_HOLE_LEAK_FACTOR.get() * Math.pow(activeHoleCount, holeFactorExponent) * fullness;
        }
        
        //Update hotAirAmount
        final double dt = 1 / 20.0; //For now a second will be the unit of time, may change
        balloon.hotAir = org.joml.Math.clamp(0, volume, hotAirAmount + hotAirChange * dt);

        //Handle invalidation
        boolean needsInvalidationCheck = balloon.isInvalid;
        if (isStructurallyFailed) {
            balloon.isInvalid = true;
        } else if (needsInvalidationCheck) {
            balloon.isInvalid = !BalloonRegistryUtility.isBalloonValid(balloon, group);
        }

        if (balloon.isInvalid && balloon.hotAir <= epsilon) {
            return true;
        }

        return false;
    }

    public static double downRamp(double v, double threshold) {
        if (v <= threshold) return 0.0;
        double denom = 1.0 - threshold;
        if (denom == 0.0) return 1.0;
        double t = (v - threshold) / denom;
        if (t <= 0.0) return 0.0;
        if (t >= 1.0) return 1.0;
        return t;
    }
}
