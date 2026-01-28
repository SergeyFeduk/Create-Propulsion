package com.deltasf.createpropulsion.thruster;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.network.PropulsionPackets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import com.deltasf.createpropulsion.network.SyncThrusterFuelsPacket;

public class ThrusterFuelManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final String DIRECTORY = "thruster_fuels";

    private static Map<Fluid, FluidThrusterProperties> fuelPropertiesMap = new HashMap<>();
    public static final TagKey<Fluid> FORGE_FUEL_TAG = TagKey.create(ForgeRegistries.FLUIDS.getRegistryKey(), ResourceLocation.fromNamespaceAndPath("forge", "fuel"));

    public static Map<Fluid, FluidThrusterProperties> getFuelPropertiesMap() { return fuelPropertiesMap; }

    public ThrusterFuelManager() {
        super(GSON, DIRECTORY);
    }

    @Nullable
    @SuppressWarnings("deprecation")
    public static FluidThrusterProperties getProperties(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return null;
        FluidThrusterProperties props = fuelPropertiesMap.get(fluid);
        if (props != null) {
            return props;
        }
        if (fluid.is(FORGE_FUEL_TAG)) return FluidThrusterProperties.DEFAULT;
        return null;
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> pObject, @Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        //Parse datapacks
        profiler.push(CreatePropulsion.ID + ":Loading_thruster_fuels");
        fuelPropertiesMap = parseFuelProperties(pObject);
        profiler.pop();
        //Update clients (happens only on /reload as on server start server instance is still null)
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isRunning()) {
            PropulsionPackets.sendToAll(SyncThrusterFuelsPacket.create(fuelPropertiesMap));
        }
    }

    public static void updateClient(Map<ResourceLocation, FluidThrusterProperties> fuelMap) {
        Map<Fluid, FluidThrusterProperties> newClientMap = new HashMap<>();
        fuelMap.forEach((rl, props) -> {
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(rl);
            if (fluid != null) {
                newClientMap.put(fluid, props);
            }
        });
        fuelPropertiesMap = newClientMap;
    }

    private Map<Fluid, FluidThrusterProperties> parseFuelProperties(@Nonnull Map<ResourceLocation, JsonElement> pObject) {
        Map<Fluid, FluidThrusterProperties> newMap = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation file = entry.getKey();
            JsonElement json = entry.getValue();

            //Parse fuel def
            ThrusterFuelDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> {LOGGER.error("[{}] Failed to parse thruster fuel definition from {}: {}", CreatePropulsion.ID, file, error);})
                .ifPresent(definition -> {
                    //There is a fuel that requires a mod but the mod is not present
                    if (definition.requiredMod().isPresent() && !ModList.get().isLoaded(definition.requiredMod().get())) {
                        return;
                    }
                    Fluid fluid = definition.getFluid();
                    //Fluid is not in registry
                    if (fluid == Fluids.EMPTY) {
                        return;
                    }
                    //Successfully load fuel
                    FluidThrusterProperties properties = new FluidThrusterProperties(
                        definition.thrustMultiplier(), 
                        definition.consumptionMultiplier());
                    newMap.put(fluid, properties);
                });
        }
        
        return newMap;
    }
}
