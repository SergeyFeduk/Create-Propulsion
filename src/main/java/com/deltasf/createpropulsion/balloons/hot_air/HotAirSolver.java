package com.deltasf.createpropulsion.balloons.hot_air;

import java.util.UUID;

import java.lang.Math;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.blocks.AbstractHotAirInjectorBlockEntity;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.utils.BalloonRegistryUtility;

import net.minecraft.world.level.Level;

//"Solver" word is a bit of an overkill, but it sounds cooler this way :D
public class HotAirSolver {
    static final double surfaceAreaFactor = 6;
    static final double epsilon = 0.1;
    static final double holeFactorExponent = 1.5;
    static final double holeInvalidationThresholdPercent = 0.25;
    static final double catastrophicLeakFactor = 1000.0;

    public static boolean tickBalloon(Level level, Balloon balloon, HaiGroup group, BalloonRegistry registry) {
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
        //Hole leak
        hotAirChange -= PropulsionConfig.BALLOON_HOLE_LEAK_FACTOR.get() * Math.pow(balloon.holes.size(), holeFactorExponent) * leakAdjustedFullness;
        
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
}
