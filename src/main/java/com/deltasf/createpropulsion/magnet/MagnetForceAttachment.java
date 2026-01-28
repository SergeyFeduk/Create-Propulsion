package com.deltasf.createpropulsion.magnet;

import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3ic;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ShipPhysicsListener;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.utility.AttachmentUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class MagnetForceAttachment implements ShipPhysicsListener {
    @JsonIgnore
    public volatile Level level;
    public MagnetForceAttachment() {}
    
    @Override
    public void physTick(@NotNull PhysShip physicShip, @NotNull PhysLevel physLevel) {
        PhysShipImpl ship = (PhysShipImpl) physicShip;
        if (this.level == null) {
            return;
        }
        List<MagnetPair> pairs = MagnetRegistry.forLevel(level).getPairsForShip(ship.getId());
        if (pairs.isEmpty()) {
            return;
        }

        var transform = ship.getTransform();
        var shipCOM = transform.getPositionInShip();

        accumulatedForce.zero();
        accumulatedTorque.zero();
        for(MagnetPair pair : pairs) {
            calculateInteraction(pair, ship, shipCOM, transform, accumulatedForce, accumulatedTorque);
        }
        //Cuz doing this individually for some reason breaks things
        if (accumulatedForce.lengthSquared() > 1e-9) { 
            ship.applyInvariantForce(accumulatedForce);
        }
        if (accumulatedTorque.lengthSquared() > 1e-9) {
            ship.applyInvariantTorque(accumulatedTorque);
        }

    }

    //Physics fun

    private double force_distance_factor(double distance) {
        return 1 / (distance * distance * distance * distance);
    }

    private double torque_distance_factor(double distance) {
        return 1 / (distance * distance * distance * distance); //Yes, should be x^3, but this feels better
    }

    //Physics not fun

    private final Vector3d localPosA_absolute_shipspace = new Vector3d();
    private final Vector3d worldPosA = new Vector3d();
    private final Vector3d m_A_hat = new Vector3d();

    private final Vector3d worldPosB = new Vector3d();
    private final Vector3d m_B_hat = new Vector3d();
    private final Vector3d localPosB_shipspace = new Vector3d();

    private final Vector3d r_AB_vec = new Vector3d();
    private final Vector3d r_BA_vec = new Vector3d();
    private final Vector3d r_BA_hat = new Vector3d();

    private final Vector3d forceOnA = new Vector3d();
    private final Vector3d forceTerm1 = new Vector3d();
    private final Vector3d forceTerm2 = new Vector3d();
    private final Vector3d forceTerm3 = new Vector3d();

    private final Vector3d torqueOnA_dipole = new Vector3d();
    private final Vector3d torqueCross_mA_mB = new Vector3d();
    private final Vector3d torqueCross_mA_rBA = new Vector3d();
    private final Vector3d torqueTerm2_scaled = new Vector3d();

    private final Vector3d worldLeverArmA = new Vector3d();
    private final Vector3d leverArmA_shipSpace = new Vector3d();
    private final Vector3d normalToWorld = new Vector3d();

    //Variables
    private final double MIN_INTERACTION_DISTANCE_SQ = 0.5;
    private final double MAGNET_INTERACTION_CONSTANT = 10000;
    private static final double POINT_FIVE = 0.5;

    private final Vector3d accumulatedForce = new Vector3d();
    private final Vector3d accumulatedTorque = new Vector3d();
    private final Vector3d tempTorqueFromForce = new Vector3d();


    private void calculateInteraction(MagnetPair pair, PhysShipImpl shipA, Vector3dc ACOM, ShipTransform transformA, 
        Vector3d totalForceAcc,
        Vector3d totalTorqueAcc) {
        //Calculate interaction power
        double powerA = pair.localPower;
        double powerB = pair.otherPower;
        if (powerA <= 0 || powerB <= 0) return; //Interaction power product is zero
        double normalizedPowerProduct = (powerA / 15.0) * (powerB / 15.0);
        double effectiveInteractionConstant = MAGNET_INTERACTION_CONSTANT * normalizedPowerProduct * PropulsionConfig.REDSTONE_MAGNET_POWER_MULTIPLIER.get();
        
        localPosA_absolute_shipspace.set(
            pair.localPos.getX() + POINT_FIVE,
            pair.localPos.getY() + POINT_FIVE,
            pair.localPos.getZ() + POINT_FIVE
        );
        //World-space position of magnet A
        transformA.getShipToWorld().transformPosition(localPosA_absolute_shipspace, worldPosA);
        //World-space normalized direction of magnet A moment
        toWorldDirection(transformA, pair.localDir, m_A_hat);

        LoadedShip shipB_loaded = null;

        if (pair.otherShipId == -1) { //Magnet B is on the world grid
            worldPosB.set(
                pair.otherPos.getX() + POINT_FIVE,
                pair.otherPos.getY() + POINT_FIVE,
                pair.otherPos.getZ() + POINT_FIVE
            );
            m_B_hat.set(pair.otherDir.x(), pair.otherDir.y(), pair.otherDir.z());
            if (m_B_hat.lengthSquared() < 1e-9) {
                System.err.println("Magnet otherDir (world) is zero for magnet at " + pair.otherPos.toString() +
                                ". This will result in NaN forces/torques.");
            }
            m_B_hat.normalize();
        } else { //Magnet B is on another ship
            shipB_loaded = getShipById(this.level, pair.otherShipId);
            if (shipB_loaded == null) return; //Other ship not found or not loaded

            ShipTransform transformB = shipB_loaded.getTransform();
            localPosB_shipspace.set(
                pair.otherPos.getX() + POINT_FIVE,
                pair.otherPos.getY() + POINT_FIVE,
                pair.otherPos.getZ() + POINT_FIVE
            );
            transformB.getShipToWorld().transformPosition(localPosB_shipspace, worldPosB);
            toWorldDirection(transformB, pair.otherDir, m_B_hat);
        }

        //Distance coefficients
        worldPosB.sub(worldPosA, r_AB_vec);
        double rLengthSquared = r_AB_vec.lengthSquared();

        if (rLengthSquared > MagnetRegistry.magnetRangeSquared) return;

        double effectiveRSquared = Math.max(rLengthSquared, MIN_INTERACTION_DISTANCE_SQ);
        double effectiveR = Math.sqrt(effectiveRSquared);

        if (effectiveR <= 1e-8) return;

        r_AB_vec.negate(r_BA_vec);
        r_BA_vec.normalize(effectiveR, r_BA_hat);

        double forceCoeff = 3.0 * effectiveInteractionConstant * force_distance_factor(effectiveR);
        double torqueCoeff = effectiveInteractionConstant * torque_distance_factor(effectiveR);

        //Dots
        double dot_mA_rBA = m_A_hat.dot(r_BA_hat);
        double dot_mB_rBA = m_B_hat.dot(r_BA_hat);
        double dot_mA_mB = m_A_hat.dot(m_B_hat);

        //Calculater force
        m_B_hat.mul(dot_mA_rBA, forceTerm1);
        m_A_hat.mul(dot_mB_rBA, forceTerm2);
        double termF3_scalar = dot_mA_mB - 5.0 * dot_mA_rBA * dot_mB_rBA;
        r_BA_hat.mul(termF3_scalar, forceTerm3);

        forceTerm1.add(forceTerm2, forceOnA);
        forceOnA.add(forceTerm3);
        forceOnA.mul(forceCoeff);

        //Calculate torque
        m_A_hat.cross(m_B_hat, torqueCross_mA_mB);
        m_A_hat.cross(r_BA_hat, torqueCross_mA_rBA);

        torqueCross_mA_rBA.mul(-3.0 * dot_mB_rBA, torqueTerm2_scaled);

        torqueCross_mA_mB.add(torqueTerm2_scaled, torqueOnA_dipole);
        torqueOnA_dipole.mul(torqueCoeff);
        torqueOnA_dipole.negate();

        if (!forceOnA.isFinite() || !torqueOnA_dipole.isFinite()) {
            return;
        }

        //Accumulate force and torque (dipole + force lever)
        localPosA_absolute_shipspace.sub(ACOM, leverArmA_shipSpace);
        
        transformA.getShipToWorld().transformDirection(leverArmA_shipSpace, worldLeverArmA);
        worldLeverArmA.cross(forceOnA, tempTorqueFromForce);

        totalForceAcc.add(forceOnA);
        totalTorqueAcc.add(torqueOnA_dipole);
        totalTorqueAcc.add(tempTorqueFromForce.mul(PropulsionConfig.REDSTONE_MAGNET_FORCE_INDUCED_TORQUE_MULTIPLIER.get()));
    }

    //Utility
    
    private void toWorldDirection(ShipTransform transform, Vector3ic blockNormal, Vector3d destWorldDir) {
        normalToWorld.set(blockNormal);
        transform.getShipToWorld().transformDirection(normalToWorld, destWorldDir);
        if (destWorldDir.lengthSquared() < 1e-10) {
            destWorldDir.zero();
            return;
        }
        destWorldDir.normalize();
    }    
    
    public static LoadedShip getShipById(Level level, long shipId) {
        return VSGameUtilsKt.getShipWorldNullable(level).getLoadedShips().getById(shipId);
    }

    //As you can see the calculations are done only for magnet A. This sucks as magnet B would have to do 90% of the same calculations when calculating pair (B; A) 
    //It is possible to reduce the amount of calculations by a factor of 2 by introducing centralized cache for the whole physics thread
    //By caching result(A, B) after its calculation we can skip almost all math for (B, A) and just reuse inverted result(A, B), as F(A, B) = -F(B, A)
    //This would require having a centralized cache and a way to subsribe to the start of physics tick, which I did not figure out yet
    //Or, instead of physics tick start - check if all pairs were cached - and clean up after (tho this is less desirable)

    public static MagnetForceAttachment getOrCreateAsAttachment(Level level, LoadedServerShip ship){
        return AttachmentUtils.getOrCreate(ship, MagnetForceAttachment.class, () -> {
            MagnetForceAttachment attachment = new MagnetForceAttachment();
            attachment.level = level;
            return attachment;
        });
    }

    public static MagnetForceAttachment get(Level level, BlockPos pos) {
        return AttachmentUtils.get(level, pos, MagnetForceAttachment.class, () -> {
            MagnetForceAttachment attachment = new MagnetForceAttachment();
            attachment.level = level;
            return attachment;
        });
    }

    public static void ensureAttachmentExists(@Nonnull Level level, @Nonnull BlockPos pos) {
        get(level, pos);
    } 
}
