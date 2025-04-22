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

    public void removeApplier(BlockPos pos){
        appliersMapping.remove(pos);
        //TODO: We can remove attachment by using  ship.saveAttachment(ForceInducedShip.class, null)
        //TODO: We should do this if there are no more appliers left on ship to clean up
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

    public static ForceInducedShip getOrCreate(ServerShip ship){
        return getOrCreateAsAttachment(ship);
    }

    public static ForceInducedShip get(Level level, BlockPos pos) {
        ServerShip ship = getShipAt((ServerLevel)level, pos);
        return ship != null ? getOrCreate(ship) : null;
    }

    private static ServerShip getShipAt(ServerLevel serverLevel, BlockPos pos){
        //Do not even dare to ask me
        ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos(serverLevel, pos);
        if (ship == null){
            ship = VSGameUtilsKt.getShipManagingPos(serverLevel, pos);
        }
        return ship;
    }
}
