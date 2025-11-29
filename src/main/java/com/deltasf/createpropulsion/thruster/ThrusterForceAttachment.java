package com.deltasf.createpropulsion.thruster;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import org.valkyrienskies.core.api.ships.*;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

import com.deltasf.createpropulsion.utility.AttachmentUtils;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY
)
public final class ThrusterForceAttachment implements ShipPhysicsListener {
    public Map<Long, ThrusterForceApplier> appliersMapping = new ConcurrentHashMap<>();
    public ThrusterForceAttachment() {}
    
    @Override
    public void physTick(@NotNull PhysShip physicShip, @NotNull PhysLevel physLevel) {
        PhysShipImpl ship = (PhysShipImpl)physicShip;
        appliersMapping.forEach((pos, applier) -> {
            applier.applyForces(BlockPos.of(pos), ship);
        });
    }

    public void addApplier(BlockPos pos, ThrusterForceApplier applier){
        appliersMapping.put(pos.asLong(), applier);
    }

    public void removeApplier(ServerLevel level, BlockPos pos){
        appliersMapping.remove(pos.asLong());
    }

    //Getters
    public static ThrusterForceAttachment getOrCreateAsAttachment(LoadedServerShip ship) {
        return AttachmentUtils.getOrCreate(ship, ThrusterForceAttachment.class, ThrusterForceAttachment::new);
    }

    public static ThrusterForceAttachment get(Level level, BlockPos pos) {
        return AttachmentUtils.get(level, pos, ThrusterForceAttachment.class, ThrusterForceAttachment::new);
    }
}
