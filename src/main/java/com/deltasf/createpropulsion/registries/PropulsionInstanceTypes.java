package com.deltasf.createpropulsion.registries;

import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.layout.FloatRepr;
import dev.engine_room.flywheel.api.layout.IntegerRepr;
import dev.engine_room.flywheel.api.layout.LayoutBuilder;
import dev.engine_room.flywheel.lib.instance.SimpleInstanceType;
import dev.engine_room.flywheel.lib.util.ExtraMemoryOps;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.system.MemoryUtil;

import com.deltasf.createpropulsion.propeller.rendering.PropellerBlurInstance;

public class PropulsionInstanceTypes {

    public static final InstanceType<PropellerBlurInstance> PROPELLER_OIT_BLUR = SimpleInstanceType.builder(PropellerBlurInstance::new)
        .cullShader(ResourceLocation.fromNamespaceAndPath("createpropulsion", "instance/cull/propeller_blur.glsl"))
        .vertexShader(ResourceLocation.fromNamespaceAndPath("createpropulsion", "instance/propeller_blur.vert"))
        .layout(LayoutBuilder.create()
            .vector("color", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
            .vector("light", IntegerRepr.SHORT, 2)
            .vector("overlay", IntegerRepr.SHORT, 2)
            .vector("pos", FloatRepr.FLOAT, 3)
            .vector("rotation", FloatRepr.FLOAT, 4)
            .build())
        .writer((ptr, instance) -> {
            //Color
            MemoryUtil.memPutByte(ptr, instance.red);
            MemoryUtil.memPutByte(ptr + 1, instance.green);
            MemoryUtil.memPutByte(ptr + 2, instance.blue);
            MemoryUtil.memPutByte(ptr + 3, instance.alpha);
            //Light & overlay
            ExtraMemoryOps.put2x16(ptr + 4, instance.light);
            ExtraMemoryOps.put2x16(ptr + 8, instance.overlay);
            //Position
            MemoryUtil.memPutFloat(ptr + 12, instance.x);
            MemoryUtil.memPutFloat(ptr + 16, instance.y);
            MemoryUtil.memPutFloat(ptr + 20, instance.z);
            //Rotation
            ExtraMemoryOps.putQuaternionf(ptr + 24, instance.rotation);
        })
        .build();

    public static final InstanceType<PropellerBlurInstance> PROPELLER_BLUR = SimpleInstanceType.builder(PropellerBlurInstance::new)
        .cullShader(ResourceLocation.fromNamespaceAndPath("createpropulsion", "instance/cull/propeller_blur.glsl"))
        .vertexShader(ResourceLocation.fromNamespaceAndPath("createpropulsion", "instance/propeller_blur.vert"))
        .layout(LayoutBuilder.create()
            .vector("color", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
            .vector("light", IntegerRepr.SHORT, 2)
            .vector("overlay", IntegerRepr.SHORT, 2)
            .vector("pos", FloatRepr.FLOAT, 3)
            .vector("rotation", FloatRepr.FLOAT, 4)
            .build())
        .writer((ptr, instance) -> {
            //Color
            MemoryUtil.memPutByte(ptr, instance.red);
            MemoryUtil.memPutByte(ptr + 1, instance.green);
            MemoryUtil.memPutByte(ptr + 2, instance.blue);
            MemoryUtil.memPutByte(ptr + 3, instance.alpha);
            //Light & overlay
            ExtraMemoryOps.put2x16(ptr + 4, instance.light);
            ExtraMemoryOps.put2x16(ptr + 8, instance.overlay);
            //Position
            MemoryUtil.memPutFloat(ptr + 12, instance.x);
            MemoryUtil.memPutFloat(ptr + 16, instance.y);
            MemoryUtil.memPutFloat(ptr + 20, instance.z);
            //Rotation
            ExtraMemoryOps.putQuaternionf(ptr + 24, instance.rotation);
        })
        .build();
    public static void register() {}
}