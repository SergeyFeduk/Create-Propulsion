package com.deltasf.createpropulsion.balloons.hot_air;

import java.util.UUID;

import java.lang.Math;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.utils.BalloonRegistryUtility;

//"Solver" word is a bit of an overkill, but it sounds cooler this way :D
public class HotAirSolver {
    static final double surfaceAreaFactor = 6;
    static final double epsilon = 1e-2;

    public static boolean tickBalloon(Balloon balloon, HaiGroup group, BalloonRegistry registry) {
        double hotAirAmount = balloon.hotAir;
        double hotAirChange = 0;
        //Handle invalidation
        if (balloon.isInvalid && hotAirAmount <= epsilon) {
            //Recheck validity
            balloon.isInvalid = !BalloonRegistryUtility.isBalloonValid(balloon, group);
            if (balloon.isInvalid) {
                //Bro did not survive this
                return true;
            }
        }
        //Hai injections
        for(UUID hai : balloon.supportHais) {
            //TODO: obtain actual hai object and get the injection amount
            double injection = 1;
            hotAirChange += injection;
        }
        //Current volume and fullness
        double volume = balloon.getVolumeSize();
        double fullness = hotAirAmount / volume;
        //Global surface leak
        double surfaceArea = surfaceAreaFactor * Math.pow(volume, 2.0/3.0);
        hotAirChange -= PropulsionConfig.BALLOON_SURFACE_LEAK_FACTOR.get() * surfaceArea * fullness;
        //Hole leak
        hotAirChange -= balloon.holes.size() * PropulsionConfig.BALLOON_HOLE_LEAK_FACTOR.get() * fullness;

        //Update hotAirAmount
        final double dt = 1 / 20.0; //For now a second will be the unit of time, may change
        balloon.hotAir = org.joml.Math.clamp(0, volume, hotAirAmount + hotAirChange * dt);

        return false;
    }
}
