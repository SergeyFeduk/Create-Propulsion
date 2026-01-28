package com.deltasf.createpropulsion.reaction_wheel;

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

public final class ReactionWheelAttachment implements ShipPhysicsListener {
    public Map<Long, ReactionWheelForceApplier> appliersMapping = new ConcurrentHashMap<>();
    ReactionWheelAttachment() {}

    @Override
    public void physTick(@NotNull PhysShip physicShip, @NotNull PhysLevel physLevel) {
        PhysShipImpl ship = (PhysShipImpl) physicShip;
        appliersMapping.forEach((pos, applier) -> {
            applier.applyForces(BlockPos.of(pos), ship);
        });
    }

    public void addApplier(BlockPos pos, ReactionWheelForceApplier applier) {
        appliersMapping.put(pos.asLong(), applier);
    }

    public void removeApplier(ServerLevel level, BlockPos pos) {
        appliersMapping.remove(pos.asLong());
    }

    //Getters
    public static ReactionWheelAttachment getOrCreateAsAttachment(LoadedServerShip ship) {
        return AttachmentUtils.getOrCreate(ship, ReactionWheelAttachment.class, ReactionWheelAttachment::new);
    }

    public static ReactionWheelAttachment get(Level level, BlockPos pos) {
        return AttachmentUtils.get(level, pos, ReactionWheelAttachment.class, ReactionWheelAttachment::new);
    }
}
