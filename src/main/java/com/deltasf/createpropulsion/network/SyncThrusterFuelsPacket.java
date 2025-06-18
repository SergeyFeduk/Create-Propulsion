package com.deltasf.createpropulsion.network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.deltasf.createpropulsion.thruster.FluidThrusterProperties;
import com.deltasf.createpropulsion.thruster.ThrusterFuelManager;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class SyncThrusterFuelsPacket {
    private final Map<ResourceLocation, FluidThrusterProperties> fuelMap;

    public static SyncThrusterFuelsPacket create(Map<Fluid, FluidThrusterProperties> mapToSync) {
        Map<ResourceLocation, FluidThrusterProperties> networkSafeMap = new HashMap<>();
        mapToSync.forEach((fluid, props) -> {
            ResourceLocation key = ForgeRegistries.FLUIDS.getKey(fluid);
            if (key != null) {
                networkSafeMap.put(key, props);
            }
        });
        return new SyncThrusterFuelsPacket(networkSafeMap);
    }

    private SyncThrusterFuelsPacket(Map<ResourceLocation, FluidThrusterProperties> fuelMap) {
        this.fuelMap = fuelMap;
    }

    public static SyncThrusterFuelsPacket decode(FriendlyByteBuf buf) {
        Map<ResourceLocation, FluidThrusterProperties> map = buf.readMap(FriendlyByteBuf::readResourceLocation, FluidThrusterProperties::decode);
        return new SyncThrusterFuelsPacket(map);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeMap(this.fuelMap, FriendlyByteBuf::writeResourceLocation, (b, props) -> props.encode(b));
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ThrusterFuelManager.updateClient(this.fuelMap);
        });
        context.setPacketHandled(true);
    }
}
