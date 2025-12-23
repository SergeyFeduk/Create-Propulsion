package com.deltasf.createpropulsion.impact_sensor;

import com.deltasf.createpropulsion.utility.AttachmentUtils;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ShipPhysicsListener;
import org.valkyrienskies.core.api.world.PhysLevel;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class ImpactSensorAttachment implements ShipPhysicsListener {
    @JsonIgnore
    private static final Map<Long, ImpactSensorAttachment> ACTIVE_ATTACHMENTS = new ConcurrentHashMap<>();

    public static ImpactSensorAttachment getById(long shipId) {
        return ACTIVE_ATTACHMENTS.get(shipId);
    }

    private final Map<Long, ImpactSensorData> sensors = new ConcurrentHashMap<>();
    
    @JsonIgnore
    private final Vector3d prevVelocity = new Vector3d();
    @JsonIgnore
    private final Queue<Vector3d> frameCollisionNormals = new ConcurrentLinkedQueue<>();
    @JsonIgnore
    private boolean initialized = false;

    public ImpactSensorAttachment() {}

    public void addSensor(BlockPos pos, ImpactSensorData data) {
        sensors.put(pos.asLong(), data);
    }

    public void removeSensor(BlockPos pos) {
        sensors.remove(pos.asLong());
    }

    public void recordCollisionNormal(Vector3dc normal) {
        frameCollisionNormals.offer(new Vector3d(normal));
    }

    @Override
    public void physTick(@NotNull PhysShip ship, @NotNull PhysLevel level) {
        ACTIVE_ATTACHMENTS.put(ship.getId(), this);

        Vector3dc currentVelocity = ship.getVelocity();
        
        if (!initialized) {
            prevVelocity.set(currentVelocity);
            initialized = true;
            frameCollisionNormals.clear();
            return;
        }

        if (!frameCollisionNormals.isEmpty()) {
            Vector3d deltaV = new Vector3d(currentVelocity).sub(prevVelocity);
            double maxProjectedImpact = 0.0;
            boolean hadSignificantImpact = false;

            while (!frameCollisionNormals.isEmpty()) {
                Vector3d normal = frameCollisionNormals.poll();
                double projectedMagnitude = Math.abs(deltaV.dot(normal));
                if (projectedMagnitude > maxProjectedImpact) {
                    maxProjectedImpact = projectedMagnitude;
                    hadSignificantImpact = true;
                }
            }

            if (hadSignificantImpact && !sensors.isEmpty()) {
                for (ImpactSensorData data : sensors.values()) {
                    data.setSignal(maxProjectedImpact);
                }
            }
        }

        prevVelocity.set(currentVelocity);
    }

    public static ImpactSensorAttachment getOrCreate(LoadedServerShip ship) {
        return AttachmentUtils.getOrCreate(ship, ImpactSensorAttachment.class, ImpactSensorAttachment::new);
    }

    public static ImpactSensorAttachment get(Level level, BlockPos pos) {
        return AttachmentUtils.get(level, pos, ImpactSensorAttachment.class, ImpactSensorAttachment::new);
    }
}
