package com.deltasf.createpropulsion.propeller;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.atmosphere.AtmoshpereHelper;
import com.deltasf.createpropulsion.atmosphere.AtmosphereData;

import net.minecraft.core.BlockPos;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY
)
public class PropellerForceApplier {
    private PropellerData data;

    private Vector3d relativePos = new Vector3d();
    private final Vector3d worldForceDirection = new Vector3d();
    private final Vector3d worldForce = new Vector3d();
    private final Vector3d parallelForce = new Vector3d();
    private final Vector3d perpendicularForce = new Vector3d();
    private Vector3d velocityDirection = new Vector3d();

    @JsonIgnore
    private static final Vector3d scaledForce_temp1 = new Vector3d();
    @JsonIgnore
    private static final Vector3d scaledForce_temp2 = new Vector3d();
    @JsonIgnore
    private static final Vector3d scaledForce_temp3 = new Vector3d();

    public PropellerForceApplier(PropellerData data){
        this.data = data;
    }

    //Jackson constructor
    public PropellerForceApplier() {}

    public void applyForces(BlockPos pos, PhysShipImpl ship) {
        float thrust = data.getThrust();
        float torque = data.getTorque(); 
        if (thrust == 0 && torque == 0) return;
        AtmosphereData atmosphere = data.getAtmosphere();
        if (atmosphere == null) return;

        final double maxSpeed = PropulsionConfig.PROPELLER_MAX_SPEED.get();
        final double forceMultiplier = PropulsionConfig.PROPELLER_POWER_MULTIPLIER.get();
        thrust *= forceMultiplier;
        //Direction from ship space to world space
        final ShipTransform transform = ship.getTransform();
        final Vector3dc shipCenterOfMass = transform.getPositionInShip(); 
        relativePos = VectorConversionsMCKt.toJOMLD(pos)
            .add(0.5, 0.5, 0.5)
            .sub(shipCenterOfMass);

        //Positioning and density
        transform.getShipToWorld().transformDirection(data.getDirection(), worldForceDirection);
        Vector3d worldPos = transform.getShipToWorld().transformPosition(VectorConversionsMCKt.toJOMLD(pos));
        double externalAirDensity = AtmoshpereHelper.calculateExternalAirDensity(atmosphere, worldPos.y, true);
        //Torque calculation and application
        if (torque != 0) {
            Vector3d torqueVector = new Vector3d(worldForceDirection).normalize().mul(torque);
            torqueVector.mul(externalAirDensity * PropulsionConfig.PROPELLER_TORQUE_EFFECT_MULTIPLIER.get());
            ship.applyInvariantTorque(torqueVector);
        }        

        worldForceDirection.normalize().mul(data.getInvertDirection() ? -1.0 : 1.0);
        worldForce.set(worldForceDirection).mul(thrust).mul(externalAirDensity);
        final Vector3dc linearVelocity = ship.getVelocity();
        if (linearVelocity.lengthSquared() >= maxSpeed * maxSpeed) {
            double dot = worldForce.dot(linearVelocity);
            if (dot > 0) {
                double forceLengthSq = worldForce.lengthSquared();
                if (forceLengthSq > 1e-9) { 
                    velocityDirection = velocityDirection.set(linearVelocity).normalize();
                    double parallelMagnitude = worldForce.dot(velocityDirection);
                    parallelForce.set(velocityDirection).mul(parallelMagnitude);
                    perpendicularForce.set(worldForce).sub(parallelForce);
                    ship.applyInvariantForceToPos(perpendicularForce, relativePos); 
                    applyScaledForce(ship, linearVelocity, parallelForce, maxSpeed); 
                }
                return;
            }
        }
        ship.applyInvariantForceToPos(worldForce, relativePos);
    }

    private static void applyScaledForce(PhysShipImpl ship, Vector3dc linearVelocity, Vector3d forceToScale, double maxSpeed){
        var currentServer = ValkyrienSkiesMod.getCurrentServer();
        if (currentServer == null) return;

        var pipeline = VSGameUtilsKt.getVsPipeline(currentServer);
        double physTps = pipeline.computePhysTps();
        if (physTps <= 0) return;
        double deltaTime = 1.0 / physTps;
        double mass = ship.getMass();
        if (mass <= 0) return;

        forceToScale.mul(deltaTime / mass, scaledForce_temp1);
        linearVelocity.add(scaledForce_temp1, scaledForce_temp2);
        scaledForce_temp2.normalize(maxSpeed, scaledForce_temp3);
        scaledForce_temp3.sub(linearVelocity, scaledForce_temp1);
        scaledForce_temp1.mul(mass / deltaTime, scaledForce_temp2);
        ship.applyInvariantForce(scaledForce_temp2);
    }
}
