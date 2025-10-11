package com.deltasf.createpropulsion.reaction_wheel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ShipForcesInducer;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import com.deltasf.createpropulsion.utility.AttachmentUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

@SuppressWarnings("deprecation")
public class ReactionWheelAttachment implements ShipForcesInducer {
    public Map<BlockPos, ReactionWheelForceApplier> appliersMapping = new ConcurrentHashMap<>();
    ReactionWheelAttachment() {}

    @Override
    public void applyForces(@NotNull PhysShip physicShip) {
        PhysShipImpl ship = (PhysShipImpl) physicShip;
        appliersMapping.forEach((pos, applier) -> {
            applier.applyForces(pos, ship);
        });
    }

    public void addApplier(BlockPos pos, ReactionWheelForceApplier applier) {
        appliersMapping.put(pos, applier);
    }

    public void removeApplier(ServerLevel level, BlockPos pos) {
        appliersMapping.remove(pos);
        // Remove attachment by using passing null as attachment instance in order to clean up after ourselves
        if (appliersMapping.isEmpty()) {
            ServerShip ship = AttachmentUtils.getShipAt(level, pos);
            if (ship != null) {
                // Remove attachment by passing null as the instance
                ship.saveAttachment(ReactionWheelAttachment.class, null);
            }
        }
    }

    // Getters
    public static ReactionWheelAttachment getOrCreateAsAttachment(ServerShip ship) {
        return AttachmentUtils.getOrCreate(ship, ReactionWheelAttachment.class, ReactionWheelAttachment::new);
    }

    public static ReactionWheelAttachment get(Level level, BlockPos pos) {
        return AttachmentUtils.get(level, pos, ReactionWheelAttachment.class, ReactionWheelAttachment::new);
    }
}
