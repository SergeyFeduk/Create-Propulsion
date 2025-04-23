package com.deltasf.createpropulsion.ship;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ShipForcesInducer;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@SuppressWarnings("deprecation")
public class ForceInducedShip implements ShipForcesInducer {
    public Map<BlockPos, IForceApplier> appliersMapping = new ConcurrentHashMap<>();
    public ForceInducedShip() {}
    
    @Override
    public void applyForces(@NotNull PhysShip physicShip){
        PhysShipImpl ship = (PhysShipImpl)physicShip;
        appliersMapping.forEach((pos, applier) -> {
            applier.applyForces(pos, ship);
        });
    }

    public void addApplier(BlockPos pos, IForceApplier applier){
        appliersMapping.put(pos, applier);
    }

    public void removeApplier(ServerLevel level, BlockPos pos){
        appliersMapping.remove(pos);
        //Remove attachment by using passing null as attachment instance in order to clean up after ourselves
        if (appliersMapping.isEmpty()) {
            getShipAt(level, pos).saveAttachment(ForceInducedShip.class, null);
        }
    }

    //Getters
    public static ForceInducedShip getOrCreateAsAttachment(ServerShip ship){
        ForceInducedShip attachment = ship.getAttachment(ForceInducedShip.class);
        if (attachment == null) {
            attachment = new ForceInducedShip();
            ship.saveAttachment(ForceInducedShip.class, attachment);
        }
        return attachment;
    }

    public static ForceInducedShip get(Level level, BlockPos pos) {
        ServerShip ship = getShipAt((ServerLevel)level, pos);
        return ship != null ? getOrCreateAsAttachment(ship) : null;
    }

    private static ServerShip getShipAt(ServerLevel serverLevel, BlockPos pos){
        //For some reason (which I don't remember anymore) it has to be just like that
        ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos(serverLevel, pos);
        if (ship == null){
            ship = VSGameUtilsKt.getShipManagingPos(serverLevel, pos);
        }
        return ship;
    }
}
