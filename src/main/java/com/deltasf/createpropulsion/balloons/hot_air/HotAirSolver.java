package com.deltasf.createpropulsion.balloons.hot_air;

import java.util.UUID;

import java.lang.Math;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;

//"Solver" word is a bit of overkill, but it sounds cooler this way :D
public class HotAirSolver {
    static final double surfaceLeakfactor = 1e-4;
    static final double surfaceAreaFactor = 6;
    static final double holeLeakFactor = 0.2;

    public static void tickBalloon(Balloon balloon) {
        double hotAirAmount = balloon.hotAir;
        double hotAirChange = 0;
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
        //TODO: For now we approximate area as it is a cube. I'll replace it with sphere approximation later 
        double surfaceArea = surfaceAreaFactor * Math.pow(volume, 2.0/3.0);
        hotAirChange -= surfaceLeakfactor * surfaceArea * fullness;
        //Hole leak
        hotAirChange -= balloon.holes.size() * holeLeakFactor * fullness;

        //Update hotAirAmount
        final double dt = 1 / 20.0; //For now a second will be the unit of time, may change
        balloon.hotAir = org.joml.Math.clamp(0, volume, hotAirAmount + hotAirChange * dt);
    }
}
