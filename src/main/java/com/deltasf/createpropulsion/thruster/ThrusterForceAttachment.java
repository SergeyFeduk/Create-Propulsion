package com.deltasf.createpropulsion.thruster;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import org.valkyrienskies.core.api.ships.*;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

import com.deltasf.createpropulsion.utility.AttachmentUtils;

public final class ThrusterForceAttachment implements ShipPhysicsListener {
    public Map<BlockPos, ThrusterForceApplier> appliersMapping = new ConcurrentHashMap<>();
    public ThrusterForceAttachment() {}
    
    @Override
    public void physTick(@NotNull PhysShip physicShip, @NotNull PhysLevel physLevel) {
        PhysShipImpl ship = (PhysShipImpl)physicShip;
        appliersMapping.forEach((pos, applier) -> {
            applier.applyForces(pos, ship);
        });
    }

    public void addApplier(BlockPos pos, ThrusterForceApplier applier){
        appliersMapping.put(pos, applier);
    }

    public void removeApplier(ServerLevel level, BlockPos pos){
        appliersMapping.remove(pos);
        //Remove attachment by using passing null as attachment instance in order to clean up after ourselves
        // Potato note: actually, don't do this anymore
//        if (appliersMapping.isEmpty()) {
//            ServerShip ship = AttachmentUtils.getShipAt(level, pos);
//            if (ship instanceof LoadedServerShip loadedShip) {
//                // Remove attachment by passing null as the instance
//                loadedShip.setAttachment(ThrusterForceAttachment.class, null);
//            }
//        }
    }

    //Getters
    public static ThrusterForceAttachment getOrCreateAsAttachment(LoadedServerShip ship) {
        return AttachmentUtils.getOrCreate(ship, ThrusterForceAttachment.class, ThrusterForceAttachment::new);
    }

    public static ThrusterForceAttachment get(Level level, BlockPos pos) {
        return AttachmentUtils.get(level, pos, ThrusterForceAttachment.class, ThrusterForceAttachment::new);
    }
}
