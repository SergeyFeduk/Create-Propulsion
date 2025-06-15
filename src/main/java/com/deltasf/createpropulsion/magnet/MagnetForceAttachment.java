package com.deltasf.createpropulsion.magnet;

import java.util.List;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3ic;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ShipForcesInducer;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.PropulsionConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

@SuppressWarnings("deprecation")
public class MagnetForceAttachment implements ShipForcesInducer {
    public volatile Level level;
    public MagnetForceAttachment() {}
    
    @Override
    public void applyForces(@NotNull PhysShip physicShip) {
        PhysShipImpl ship = (PhysShipImpl) physicShip;
        List<MagnetPair> pairs = MagnetRegistry.get().forLevel(level).getPairsForShip(ship.getId());
        if (pairs.isEmpty()) {
            return;
        }

        var transform = ship.getTransform();
        var shipCOM = transform.getPositionInShip();

        _accumulatedForce.zero();
        _accumulatedTorque.zero();
        for(MagnetPair pair : pairs) {
            calculateInteraction(pair, ship, shipCOM, transform, _accumulatedForce, _accumulatedTorque);
        }
        //Cuz doing this individually for some reason breaks things
        if (_accumulatedForce.lengthSquared() > 1e-9) { 
            ship.applyInvariantForce(_accumulatedForce);
        }
        if (_accumulatedTorque.lengthSquared() > 1e-9) {
            ship.applyInvariantTorque(_accumulatedTorque);
        }

    }

    //Physics fun

    private double force_distance_factor(double distance) {
        return 1 / (distance * distance * distance * distance);
        //return smoothDippedBetaDecay(distance, 3, 4.4, 2.6, 0.1, -0.2) / 4;
    }

    private double torque_distance_factor(double distance) {
        return 1 / (distance * distance * distance * distance);
        //return smoothDippedBetaDecay(distance, 2, 4.4, 2.6, 0.1, -0.2) / 4;
    } 

    /*private double smoothDippedBetaDecay(double distance, double exponent, double strength, double width, double center, double offset) {
        double q = distance + offset;
        double base = 1 / java.lang.Math.pow(q, exponent); //JOML has no pow !?
        double bump = strength * (1 - 1 / (1 + Math.exp(-width * (q - center)))); // Sigmoid
        return base * (1 - bump);
    }*/

    //Physics not fun

    private final Vector3d _localPosA_absolute_shipspace = new Vector3d();
    private final Vector3d _worldPosA = new Vector3d();
    private final Vector3d _m_A_hat = new Vector3d();

    private final Vector3d _worldPosB = new Vector3d();
    private final Vector3d _m_B_hat = new Vector3d();
    private final Vector3d _localPosB_shipspace = new Vector3d();

    private final Vector3d _r_AB_vec = new Vector3d();
    private final Vector3d _r_BA_vec = new Vector3d();
    private final Vector3d _r_BA_hat = new Vector3d();

    private final Vector3d _forceOnA = new Vector3d();
    private final Vector3d _forceTerm1 = new Vector3d();
    private final Vector3d _forceTerm2 = new Vector3d();
    private final Vector3d _forceTerm3 = new Vector3d();

    private final Vector3d _torqueOnA_dipole = new Vector3d();
    private final Vector3d _torqueCross_mA_mB = new Vector3d();
    private final Vector3d _torqueCross_mA_rBA = new Vector3d();
    private final Vector3d _torqueTerm2_scaled = new Vector3d();

    private final Vector3d _worldLeverArmA = new Vector3d();
    private final Vector3d _leverArmA_shipSpace = new Vector3d();
    private final Vector3d _normalToWorld = new Vector3d();

    //Variables
    private final double MIN_INTERACTION_DISTANCE_SQ = (0.5 * 1);
    private final double MAGNET_INTERACTION_CONSTANT = 10000;
    private static final double POINT_FIVE = 0.5;

    private final Vector3d _accumulatedForce = new Vector3d();
    private final Vector3d _accumulatedTorque = new Vector3d();
    private final Vector3d _tempTorqueFromForce = new Vector3d();


    @SuppressWarnings("null")
        private void calculateInteraction(MagnetPair pair, PhysShipImpl shipA, Vector3dc ACOM, ShipTransform transformA, 
        Vector3d totalForceAcc,
        Vector3d totalTorqueAcc) {
        //Calculate interaction power
        double powerA = pair.localPower;
        double powerB = pair.otherPower;
        if (powerA <= 0 || powerB <= 0) return; //Interaction power product is zero
        double normalizedPowerProduct = (powerA / 15.0) * (powerB / 15.0);
        double effectiveInteractionConstant = MAGNET_INTERACTION_CONSTANT * normalizedPowerProduct * PropulsionConfig.REDSTONE_MAGNET_POWER_MULTIPLIER.get();
        
        _localPosA_absolute_shipspace.set(
            pair.localPos.getX() + POINT_FIVE,
            pair.localPos.getY() + POINT_FIVE,
            pair.localPos.getZ() + POINT_FIVE
        );
        // World-space position of magnet A
        transformA.getShipToWorld().transformPosition(_localPosA_absolute_shipspace, _worldPosA);
        // World-space normalized direction of magnet A moment
        toWorldDirection(transformA, pair.localDir, _m_A_hat);

        LoadedShip shipB_loaded = null;

        if (pair.otherShipId == -1) { // Magnet B is on the world grid
            _worldPosB.set(
                pair.otherPos.getX() + POINT_FIVE,
                pair.otherPos.getY() + POINT_FIVE,
                pair.otherPos.getZ() + POINT_FIVE
            );
            _m_B_hat.set(pair.otherDir.x(), pair.otherDir.y(), pair.otherDir.z());
            if (_m_B_hat.lengthSquared() < 1e-9) {
                System.err.println("Magnet otherDir (world) is zero for magnet at " + pair.otherPos.toString() +
                                ". This will result in NaN forces/torques.");
            }
            _m_B_hat.normalize();
        } else { // Magnet B is on another ship
            shipB_loaded = getShipById(this.level, pair.otherShipId);
            if (shipB_loaded == null) return; // Other ship not found or not loaded

            ShipTransform transformB = shipB_loaded.getTransform();
            _localPosB_shipspace.set(
                pair.otherPos.getX() + POINT_FIVE,
                pair.otherPos.getY() + POINT_FIVE,
                pair.otherPos.getZ() + POINT_FIVE
            );
            transformB.getShipToWorld().transformPosition(_localPosB_shipspace, _worldPosB);
            toWorldDirection(transformB, pair.otherDir, _m_B_hat);
        }

        //Distance coefficients
        _worldPosB.sub(_worldPosA, _r_AB_vec);
        double rLengthSquared = _r_AB_vec.lengthSquared();

        if (rLengthSquared > MagnetRegistry.magnetRangeSquared) return;

        double effectiveRSquared = Math.max(rLengthSquared, MIN_INTERACTION_DISTANCE_SQ);
        double effectiveR = Math.sqrt(effectiveRSquared);

        if (effectiveR <= 1e-8) return;

        _r_AB_vec.negate(_r_BA_vec);
        _r_BA_vec.normalize(effectiveR, _r_BA_hat);

        double forceCoeff = 3.0 * effectiveInteractionConstant * force_distance_factor(effectiveR);
        double torqueCoeff = effectiveInteractionConstant * torque_distance_factor(effectiveR);

        //Dots
        double dot_mA_rBA = _m_A_hat.dot(_r_BA_hat);
        double dot_mB_rBA = _m_B_hat.dot(_r_BA_hat);
        double dot_mA_mB = _m_A_hat.dot(_m_B_hat);

        //Calculater force
        _m_B_hat.mul(dot_mA_rBA, _forceTerm1);
        _m_A_hat.mul(dot_mB_rBA, _forceTerm2);
        double termF3_scalar = dot_mA_mB - 5.0 * dot_mA_rBA * dot_mB_rBA;
        _r_BA_hat.mul(termF3_scalar, _forceTerm3);

        _forceTerm1.add(_forceTerm2, _forceOnA);
        _forceOnA.add(_forceTerm3);
        _forceOnA.mul(forceCoeff);

        //Calculate torque
        _m_A_hat.cross(_m_B_hat, _torqueCross_mA_mB);
        _m_A_hat.cross(_r_BA_hat, _torqueCross_mA_rBA);

        _torqueCross_mA_rBA.mul(-3.0 * dot_mB_rBA, _torqueTerm2_scaled);

        _torqueCross_mA_mB.add(_torqueTerm2_scaled, _torqueOnA_dipole);
        _torqueOnA_dipole.mul(torqueCoeff);
        _torqueOnA_dipole.negate();

        if (!_forceOnA.isFinite() || !_torqueOnA_dipole.isFinite()) {
            return;
        }

        //Accumulate force and torque (dipole + force lever)
        _localPosA_absolute_shipspace.sub(ACOM, _leverArmA_shipSpace);
        
        transformA.getShipToWorld().transformDirection(_leverArmA_shipSpace, _worldLeverArmA);
        _worldLeverArmA.cross(_forceOnA, _tempTorqueFromForce);

        totalForceAcc.add(_forceOnA);
        totalTorqueAcc.add(_torqueOnA_dipole);
        totalTorqueAcc.add(_tempTorqueFromForce.mul(PropulsionConfig.REDSTONE_MAGNET_FORCE_INDUCED_TORQUE_MULTIPLIER.get()));
    }

    //Utility
    
    private void toWorldDirection(ShipTransform transform, Vector3ic blockNormal, Vector3d destWorldDir) {
        _normalToWorld.set(blockNormal);
        transform.getShipToWorld().transformDirection(_normalToWorld, destWorldDir);
        if (destWorldDir.lengthSquared() < 1e-10) {
            destWorldDir.zero();
            return;
        }
        destWorldDir.normalize();
    }    
    
    public static LoadedShip getShipById(Level level, long shipId) {
        return VSGameUtilsKt.getShipWorldNullable(level).getLoadedShips().getById(shipId);
    }

    // As you can see the calculations are done only for magnet A. This sucks as magnet B would have to do 90% of the same calculations when calculating pair (B; A) 
    // It is possible to reduce the amount of calculations by a factor of 2 by introducing centralized cache for the whole physics thread
    // By caching result(A, B) after its calculation we can skip almost all math for (B, A) and just reuse inverted result(A, B), as F(A, B) = -F(B, A)
    // This would require having a centralized cache and a way to subsribe to the start of physics tick, which I did not figure out yet
    // Or, instead of physics tick start - check if all pairs were cached - and clean up after (tho this is less desirable)

    //CODE DUPLICATION!!!!!!!!!

    public static MagnetForceAttachment getOrCreateAsAttachment(Level level, ServerShip ship){
        MagnetForceAttachment attachment = ship.getAttachment(MagnetForceAttachment.class);
        if (attachment == null) {
            attachment = new MagnetForceAttachment();
            attachment.level = level;
            ship.saveAttachment(MagnetForceAttachment.class, attachment);
        }
        return attachment;
    }

    public static MagnetForceAttachment get(Level level, BlockPos pos) {
        ServerShip ship = getShipAt((ServerLevel)level, pos);
        return ship != null ? getOrCreateAsAttachment(level, ship) : null;
    }

    private static ServerShip getShipAt(ServerLevel serverLevel, BlockPos pos){
        ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos(serverLevel, pos);
        if (ship == null){
            ship = VSGameUtilsKt.getShipManagingPos(serverLevel, pos);
        }
        return ship;
    }

    public static void ensureAttachmentExists(@Nonnull Level level, @Nonnull BlockPos pos) {
        ServerShip ship = getShipAt((ServerLevel) level, pos);
        if (ship != null) {
            MagnetForceAttachment attachment = ship.getAttachment(MagnetForceAttachment.class);
            if (attachment == null) {
                attachment = new MagnetForceAttachment();
                attachment.level = level;
                ship.saveAttachment(MagnetForceAttachment.class, attachment);
            }
        }
    } 
}
