package com.deltasf.createpropulsion.physics_assembler;

import com.deltasf.createpropulsion.physics_assembler.AssemblyGaugeOverlayRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AssemblyGaugeUsedPacket {

    private final AABB selection;

    public AssemblyGaugeUsedPacket(AABB selection) {
        this.selection = selection;
    }

    public AssemblyGaugeUsedPacket(FriendlyByteBuf buf) {
        this.selection = new AABB(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                                  buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(selection.minX);
        buf.writeDouble(selection.minY);
        buf.writeDouble(selection.minZ);
        buf.writeDouble(selection.maxX);
        buf.writeDouble(selection.maxY);
        buf.writeDouble(selection.maxZ);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            AssemblyGaugeOverlayRenderer.triggerFlash(this.selection);
        }));
        context.get().setPacketHandled(true);
    }
}
