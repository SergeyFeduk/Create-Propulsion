package com.deltasf.createpropulsion.balloons.hot_air;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import org.antlr.v4.parse.ANTLRParser.finallyClause_return;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Matrix3dc;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.ships.ShipForcesInducer;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.BalloonForceChunk;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.Balloon.ChunkKey;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.utility.AttachmentUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@SuppressWarnings("deprecation")
public class BalloonAttachment implements ShipForcesInducer {
    public BalloonAttachment() {}
    private final double epsilon = 1e-5;

    private double pressureAtSea = 1.225; 
    private double SCALE_HEIGHT = 200;

    private final double G = 9.81;

    @Override
    public void applyForces(@NotNull PhysShip physicShip) {
        List<Balloon> balloons = BalloonShipRegistry.forShip(physicShip.getId()).getBalloons();
        Matrix4dc shipToWorld = physicShip.getTransform().getShipToWorld();

        accumulatedForce.zero();
        accumulatedTorque.zero();

        for(Balloon balloon : balloons) {
            double fullness = balloon.hotAir / balloon.getVolumeSize();
            if (fullness <= epsilon) continue;
            calculateForcesForBalloon(shipToWorld, physicShip, balloon, fullness);
        }

        //Angular dampening
        Vector3d shipUpWorld = new Vector3d(0, 1, 0);
        shipToWorld.transformDirection(shipUpWorld, shipUpWorld);
        shipUpWorld.normalize();

        Vector3d worldUp = new Vector3d(0, 1, 0);
        Vector3d alignAxis = new Vector3d();
        shipUpWorld.cross(worldUp, alignAxis); // axis direction and magnitude ~ sin(angle)
        double alignMag = alignAxis.length();

        //P torque dampening
        if (alignMag > 1e-6) {
            alignAxis.normalize();
            Vector3d alignTorque = new Vector3d(alignAxis).mul(PropulsionConfig.BALLOON_ALIGNMENT_KP.get() * alignMag);
            accumulatedTorque.add(alignTorque);
        }
        
        //D torque dampening
        PhysShipImpl simpl = (PhysShipImpl)physicShip;
        Vector3dc angVel = simpl.getPoseVel().getOmega();

        if (angVel != null && angVel.lengthSquared() > 1e-9) {
            Matrix4dc worldToShip = physicShip.getTransform().getWorldToShip();
            Vector3d angVelShipSpace = new Vector3d();
            worldToShip.transformDirection(angVel, angVelShipSpace);
            Matrix3dc momentOfInertia = simpl.getInertia().getMomentOfInertiaTensor();
            momentOfInertia.transform(angVelShipSpace, angMomentumShipSpace);
            dampingTorqueShipSpace.set(angMomentumShipSpace).mul(-PropulsionConfig.BALLOON_ANGULAR_DAMPING.get());
            shipToWorld.transformDirection(dampingTorqueShipSpace, dampingTorqueWorldSpace);
            accumulatedTorque.add(dampingTorqueWorldSpace);
        }

        //Apply aggregated force and torque
        if (accumulatedForce.lengthSquared() > 1e-9) {
            physicShip.applyInvariantForce(accumulatedForce);
        }
        if (accumulatedTorque.lengthSquared() > 1e-9) {
            physicShip.applyInvariantTorque(accumulatedTorque);
        }
    }

    private void calculateForcesForBalloon(Matrix4dc shipToWorld, PhysShip physicShip, Balloon balloon, double fullness) {
        ConcurrentHashMap<ChunkKey, BalloonForceChunk> chunks = balloon.getChunkMap();

        Vector3dc shipCOMInShipSpace = physicShip.getTransform().getPositionInShip();
        shipToWorld.transformPosition(shipCOMInShipSpace.x(), shipCOMInShipSpace.y(), shipCOMInShipSpace.z(), shipCOMWorld);
        
        for (Map.Entry<ChunkKey, BalloonForceChunk> entry : chunks.entrySet()) {
            ChunkKey ck = entry.getKey();
            BalloonForceChunk chunk = entry.getValue();

            //Compute chunk world position
            double originX = ck.x() * (double)Balloon.CHUNK_SIZE;
            double originY = ck.y() * (double)Balloon.CHUNK_SIZE;
            double originZ = ck.z() * (double)Balloon.CHUNK_SIZE;
            double centerX = originX + (Balloon.CHUNK_SIZE - 1) / 2.0;
            double centerY = originY + (Balloon.CHUNK_SIZE - 1) / 2.0;
            double centerZ = originZ + (Balloon.CHUNK_SIZE - 1) / 2.0;
            double appShipX = centerX + chunk.centroidX;
            double appShipY = centerY + chunk.centroidY;
            double appShipZ = centerZ + chunk.centroidZ;
            shipToWorld.transformPosition(appShipX, appShipY, appShipZ, tmpWorldPos);

            //Calculate force magnitude
            double externalDensity = calculateExternalAirDensity(tmpWorldPos.y);
            double forceMagnitude = chunk.blockCount * externalDensity * G * PropulsionConfig.BALLOON_K_COEFFICIENT.get() * fullness;
            forceMagnitude = Math.max(0, forceMagnitude * PropulsionConfig.BALLOON_FORCE_COEFFICIENT.get());
            //Calculate force vector
            tmpForce.set(upWorld).mul(forceMagnitude);
            //Aggregate force and torque
            accumulatedForce.add(tmpForce);
            leverArmWorld.set(tmpWorldPos).sub(shipCOMWorld);
            leverArmWorld.cross(tmpForce, tmpForce); //tmpForce is reused to hold torque here
            accumulatedTorque.add(tmpForce);
        }
    }

    private double calculateExternalAirDensity(double altitude) {
        return pressureAtSea * Math.exp(-altitude/SCALE_HEIGHT);
    }

    private final Vector3d accumulatedForce = new Vector3d();
    private final Vector3d accumulatedTorque = new Vector3d();
    private final Vector3d tmpWorldPos = new Vector3d();
    private final Vector3d shipCOMWorld = new Vector3d();
    private final Vector3d leverArmWorld = new Vector3d();
    private final Vector3d upWorld = new Vector3d(0, 1, 0);
    private final Vector3d tmpForce = new Vector3d();

    //Angualr dampening
    private final Vector3d angMomentumShipSpace = new Vector3d();
    private final Vector3d dampingTorqueShipSpace = new Vector3d();
    private final Vector3d dampingTorqueWorldSpace = new Vector3d();

    public static BalloonAttachment getOrCreateAsAttachment(Level level, ServerShip ship){
        return AttachmentUtils.getOrCreate(ship, BalloonAttachment.class, () -> {
            BalloonAttachment attachment = new BalloonAttachment();
            return attachment;
        });
    }

    public static BalloonAttachment get(Level level, BlockPos pos) {
        return AttachmentUtils.get(level, pos, BalloonAttachment.class, () -> {
            BalloonAttachment attachment = new BalloonAttachment();
            return attachment;
        });
    }

    public static void ensureAttachmentExists(@Nonnull Level level, @Nonnull BlockPos pos) {
        get(level, pos);
    } 
}
