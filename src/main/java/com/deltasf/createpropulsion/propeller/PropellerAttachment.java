package com.deltasf.createpropulsion.propeller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ShipPhysicsListener;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

import com.deltasf.createpropulsion.utility.AttachmentUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class PropellerAttachment implements ShipPhysicsListener {
    public Map<Long, PropellerForceApplier> appliersMapping = new ConcurrentHashMap<>();
    PropellerAttachment() {}

    @Override
    public void physTick(@NotNull PhysShip physicShip, @NotNull PhysLevel physLevel) {
        PhysShipImpl ship = (PhysShipImpl)physicShip;
        appliersMapping.forEach((pos, applier) -> {
            applier.applyForces(BlockPos.of(pos), ship);
        });
    }

    public void addApplier(BlockPos pos, PropellerForceApplier applier) {
        appliersMapping.put(pos.asLong(), applier);
    }

    public void removeApplier(ServerLevel level, BlockPos pos) {
        appliersMapping.remove(pos.asLong());
    }

    //Getters
    public static PropellerAttachment getOrCreateAsAttachment(LoadedServerShip ship) {
        return AttachmentUtils.getOrCreate(ship, PropellerAttachment.class, PropellerAttachment::new);
    }

    public static PropellerAttachment get(Level level, BlockPos pos) {
        return AttachmentUtils.get(level, pos, PropellerAttachment.class, PropellerAttachment::new);
    }
}
