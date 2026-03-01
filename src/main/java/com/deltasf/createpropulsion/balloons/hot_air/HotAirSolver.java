package com.deltasf.createpropulsion.balloons.hot_air;

import java.util.ArrayList;
import java.util.List;
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
import com.deltasf.createpropulsion.balloons.injectors.IHotAirInjector;
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

        if (balloon.isEmpty()) { return true; } //Dead in a moment
        
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
        for(UUID id : ctx.balloon.getSupportHais()) {
            IHotAirInjector hai = ctx.registry.getInjector(ctx.level, id);
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
        double minY = ctx.balloon.getAABB().minY;

        for (BlockPos holePos : ctx.balloon.getHoles()) {
            double interpolationValue = (holePos.getY() + 1.0) - ctx.pressureFloor;
            double activityFraction = org.joml.Math.clamp(0.0, 1.0, interpolationValue);

            double relativePos = 0.0;
            if (ctx.height > 0) {
                relativePos = (holePos.getY() - minY) / ctx.height;
            }
            double heightBoost = 1.0 + org.joml.Math.clamp(0.0, 1.0, relativePos); //Relative height factor (higher the hole in balloon -> higher the leak factor)

            activeHoleCount += activityFraction * heightBoost;
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
        if (ctx.isStructurallyFailed) {
            ctx.balloon.isInvalid = true;
        } else {
            //Re-check validity against the group
            boolean isValid = BalloonRegistryUtility.isBalloonValid(ctx.balloon, ctx.group);

            if (isValid && ctx.balloon.isSupportHaisEmpty() && ctx.group.hais.isEmpty()) {
                if (ctx.hotAirAmount <= epsilon) { 
                    isValid = false;
                }
            }
            
            ctx.balloon.isInvalid = !isValid;
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
            this.isStructurallyFailed = balloon.getHoles().size() >= this.surfaceArea * holeInvalidationThresholdPercent;
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

    public static double predictSteadyHotAir(Level level, Balloon balloon, HaiGroup group, BalloonRegistry registry, ServerShip ship, double tolerance) {
        if (balloon == null || registry == null || ship == null) return 0.0;
        return predictSteadyHotAir(new SolverContext(level, balloon, group, registry, ship), tolerance);
    }

    //Predicts hot air amount that satisfies (injection - leak = 0)
    //Uses bisection (cus ramp & exponents -> no closed-form solution)
    public static double predictSteadyHotAir(SolverContext ctx, double HotAirTolerance) {
        if (ctx == null) return 0.0;
        final double volume = ctx.volume;
        if (volume <= 0.0) return 0.0;

        //Accumulate injection
        double totalInjection = 0.0;
        for (UUID id : ctx.balloon.getSupportHais()) {
            IHotAirInjector injector = ctx.registry.getInjector(ctx.level, id);
            if (injector == null) continue;
            totalInjection += injector.getInjectionAmount();
        }

        final double surfaceLeakCoefficient = PropulsionConfig.BALLOON_SURFACE_LEAK_FACTOR.get() * ctx.catastrophicFailureModifier * ctx.surfaceArea;
        final double holeLeakFactor = PropulsionConfig.BALLOON_HOLE_LEAK_FACTOR.get();
        final double height = ctx.height;

        final List<BlockPos> holeList = new ArrayList<>(ctx.balloon.getHoles());
        final int holeCount = holeList.size();
        final double[] deltaYs = new double[holeCount];
        final double[] weights = new double[holeCount];

        final double boundsMaxY = ctx.balloon.getAABB().maxY;
        final double boundsMinY = ctx.balloon.getAABB().minY;

        for (int i = 0; i < holeCount; i++) {
            BlockPos hp = holeList.get(i);
            deltaYs[i] = (hp.getY() + 1.0) - boundsMaxY;
            double relativePos = 0.0;
            if (height > 0.0) relativePos = (hp.getY() - boundsMinY) / height;
            weights[i] = 1.0 + org.joml.Math.clamp(0.0, 1.0, relativePos);
        }

        final double downness = new Vector3d(0.0, 1.0, 0.0)
            .rotate(ctx.ship.getTransform().getShipToWorldRotation().normalize(new Quaterniond()), new Vector3d())
            .dot(0.0, -1.0, 0.0);
        final double upsideDownLeakPercent = downRamp(downness, upsideDownThreshold);

        //Check endpoints
        double phiAtZero = evaluatePhi(totalInjection, surfaceLeakCoefficient, holeLeakFactor, height, deltaYs, weights, holeCount, upsideDownLeakPercent, 0.0, volume);
        if (phiAtZero <= 0.0) return 0.0;
        double phiAtOne = evaluatePhi(totalInjection, surfaceLeakCoefficient, holeLeakFactor, height, deltaYs, weights, holeCount, upsideDownLeakPercent, 1.0, volume);
        if (phiAtOne >= 0.0) return volume;

        //Determine bisection iterations from tolerance
        double fullnessTolerance = (HotAirTolerance <= 0.0) ? 1e-12 : (HotAirTolerance / volume);
        if (fullnessTolerance <= 0.0) fullnessTolerance = 1e-12;
        int iterations = (int) Math.ceil(Math.log(1.0 / fullnessTolerance) / Math.log(2.0));
        iterations = Math.max(4, Math.min(iterations, 60));

        double low = 0.0;
        double high = 1.0;
        for (int iter = 0; iter < iterations; iter++) {
            double mid = 0.5 * (low + high);
            double phiMid = evaluatePhi(totalInjection, surfaceLeakCoefficient, holeLeakFactor, height, deltaYs, weights, holeCount, upsideDownLeakPercent, mid, volume);
            if (phiMid > 0.0) low = mid; else high = mid;
        }
        double fullness = 0.5 * (low + high);
        double predictedHotAir = fullness * volume;
        if (predictedHotAir < 0.0) predictedHotAir = 0.0;
        if (predictedHotAir > volume) predictedHotAir = volume;
        return predictedHotAir;
    }

    private static double evaluatePhi(double totalInjection, double surfaceLeakCoefficient, double holeLeakFactor,
                                      double height, double[] deltaYs, double[] weights, int holeCount,
                                      double upsideDownLeakPercent, double fullness, double volume) {
        double leakAdjustedFullness = Math.max(fullness, 0.1);
        double globalLeak = surfaceLeakCoefficient * leakAdjustedFullness;

        double activeHoleCount = 0.0;
        for (int i = 0; i < holeCount; i++) {
            double raw = deltaYs[i] + height * fullness;
            double activity = org.joml.Math.clamp(0.0, 1.0, raw);
            activeHoleCount += weights[i] * activity;
        }
        double holeLeak = holeLeakFactor * fullness * Math.pow(activeHoleCount, holeFactorExponent);

        double upsideDownLeakFraction = 0.0;
        if (upsideDownLeakPercent > 0.0) {
            double allowedRemainingFraction = (1.0 - upsideDownLeakPercent);
            if (fullness > allowedRemainingFraction) {
                double baseRemovalFraction = upsideDownLeakPercent * fullness;
                double candidate = baseRemovalFraction * upsideDownLeakFactor;
                double maxRemovableFraction = fullness - allowedRemainingFraction;
                upsideDownLeakFraction = Math.min(candidate, maxRemovableFraction);
            }
        }

        return totalInjection - (globalLeak + holeLeak + upsideDownLeakFraction * volume);
    }
}
