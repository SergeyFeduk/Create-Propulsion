package com.deltasf.createpropulsion.balloons.hot_air;

import java.util.UUID;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.ServerShip;

import java.lang.Math;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.atmosphere.AtmosphereData;
import com.deltasf.createpropulsion.atmosphere.DimensionAtmosphereManager;
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
        if (isInAirlessAtmosphere(level)) {
            balloon.hotAir = 0.0;
            balloon.isInvalid = true;
            return true;
        }

        if (balloon.isEmpty()) {
            return true; //Dead in a moment
        }
        
        SolverContext ctx = new SolverContext(level, balloon, group, registry, ship);

        calculateInjections(ctx);
        calculateGlobalLeak(ctx);
        calculateUpsideDownLeak(ctx);
        calculateHoleLeak(ctx);
        updateHotAir(ctx);
        handleInvalidation(ctx);

        return balloon.isInvalid && balloon.hotAir <= epsilon;
    }

    private static void calculateInjections(SolverContext ctx) {
        //Hai injections
        for(UUID id : ctx.balloon.supportHais) {
            AbstractHotAirInjectorBlockEntity hai = ctx.registry.getInjector(ctx.level, id);
            if (hai == null) continue; //May happen on hai destruction, before it got updated in registry
            double injection = hai.getInjectionAmount();
            ctx.hotAirChange += injection;
        }
    }

    private static void calculateGlobalLeak(SolverContext ctx) {
        //Global surface leak
        ctx.hotAirChange -= PropulsionConfig.BALLOON_SURFACE_LEAK_FACTOR.get() * ctx.catastrophicFailureModifier * ctx.surfaceArea * ctx.leakAdjustedFullness;
    }

    private static void calculateUpsideDownLeak(SolverContext ctx) {
        //Leak caused by ship being upside-down
        Vector3d up = new Vector3d(0.0, 1.0, 0.0).rotate(ctx.ship.getTransform().getShipToWorldRotation().normalize(new Quaterniond()), new Vector3d());
        double downness = up.dot(0.0, -1.0, 0.0);
        double leakAmountPercent = downRamp(downness, upsideDownThreshold);
        if (leakAmountPercent > 0.0) {
            double allowedRemaining = (1.0 - leakAmountPercent) * ctx.volume;
            if (ctx.hotAirAmount > allowedRemaining) {
                double baseRemoval = leakAmountPercent * ctx.hotAirAmount;
                double upsideDownLeak = baseRemoval * upsideDownLeakFactor;
                double maxRemovable = ctx.hotAirAmount - allowedRemaining;
                if (upsideDownLeak > maxRemovable) upsideDownLeak = maxRemovable;
                ctx.hotAirChange -= upsideDownLeak;
            }
        }
    }

    private static void calculateHoleLeak(SolverContext ctx) {
        //Hole leak based on y coordinate of each hole. We assume that hot air is evenlt distributed along all y levels
        double activeHoleCount = 0;
        for (BlockPos holePos : ctx.balloon.holes) {
            double interpolationValue = (holePos.getY() + 1.0) - ctx.pressureFloor;
            double activityFraction = org.joml.Math.clamp(0.0, 1.0, interpolationValue);
            activeHoleCount += activityFraction;
        }
        if (activeHoleCount > 0) {
            ctx.hotAirChange -= PropulsionConfig.BALLOON_HOLE_LEAK_FACTOR.get() * Math.pow(activeHoleCount, holeFactorExponent) * ctx.fullness;
        }
    }

    private static void updateHotAir(SolverContext ctx) {
        //Update hotAirAmount
        final double dt = 1 / 20.0; //For now a second will be the unit of time, may change
        ctx.balloon.hotAir = org.joml.Math.clamp(0, ctx.volume, ctx.hotAirAmount + ctx.hotAirChange * dt);
    }

    private static void handleInvalidation(SolverContext ctx) {
        //Handle invalidation
        boolean needsInvalidationCheck = ctx.balloon.isInvalid;
        if (ctx.isStructurallyFailed) {
            ctx.balloon.isInvalid = true;
        } else if (needsInvalidationCheck) {
            ctx.balloon.isInvalid = !BalloonRegistryUtility.isBalloonValid(ctx.balloon, ctx.group);
        }
    }

    private static class SolverContext {
        final Level level;
        final Balloon balloon;
        final HaiGroup group;
        final BalloonRegistry registry;
        final ServerShip ship;

        final double hotAirAmount;
        double hotAirChange = 0;
        final double volume;
        final double fullness;
        final double leakAdjustedFullness;
        final double surfaceArea;
        final boolean isStructurallyFailed;
        final double catastrophicFailureModifier;
        final double height;
        final double pressureFloor;

        SolverContext(Level level, Balloon balloon, HaiGroup group, BalloonRegistry registry, ServerShip ship) {
            this.level = level;
            this.balloon = balloon;
            this.group = group;
            this.registry = registry;
            this.ship = ship;

            this.hotAirAmount = balloon.hotAir;

            //Current volume and fullness
            this.volume = balloon.getVolumeSize();
            this.fullness = this.hotAirAmount / this.volume;
            this.leakAdjustedFullness = Math.max(this.fullness, 0.1); //Use max here so leak is still significant for almost empty balloons
            this.surfaceArea = surfaceAreaFactor * Math.pow(this.volume, 2.0/3.0);
            this.isStructurallyFailed = balloon.holes.size() >= this.surfaceArea * holeInvalidationThresholdPercent;
            this.catastrophicFailureModifier = this.isStructurallyFailed ? catastrophicLeakFactor : 1.0;

            AABB bounds = balloon.getAABB();
            this.height = bounds.maxY - bounds.minY;
            this.pressureFloor = bounds.maxY - (this.height * this.fullness) + 1e-6;
        }
    }

    private static boolean isInAirlessAtmosphere(Level level) {
        AtmosphereData atmosphere = DimensionAtmosphereManager.getData(level);
        return atmosphere.isAirless();
    }

    private static double downRamp(double v, double threshold) {
        if (v <= threshold) return 0.0;
        double denom = 1.0 - threshold;
        if (denom == 0.0) return 1.0;
        double t = (v - threshold) / denom;
        if (t <= 0.0) return 0.0;
        if (t >= 1.0) return 1.0;
        return t;
    }
}
