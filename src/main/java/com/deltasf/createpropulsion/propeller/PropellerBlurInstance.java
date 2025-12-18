package com.deltasf.createpropulsion.propeller;

import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.lib.instance.ColoredLitOverlayInstance;
import net.minecraft.core.Vec3i;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PropellerBlurInstance extends ColoredLitOverlayInstance {
    public float x;
    public float y;
    public float z;
    public final Quaternionf rotation = new Quaternionf();

    public PropellerBlurInstance(InstanceType<? extends PropellerBlurInstance> type, InstanceHandle handle) {
        super(type, handle);
    }

    public PropellerBlurInstance position(Vector3f pos) {
        return position(pos.x(), pos.y(), pos.z());
    }

    public PropellerBlurInstance position(Vec3i pos) {
        return position(pos.getX(), pos.getY(), pos.getZ());
    }

    public PropellerBlurInstance position(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public PropellerBlurInstance rotation(Quaternionf q) {
        this.rotation.set(q);
        return this;
    }
}