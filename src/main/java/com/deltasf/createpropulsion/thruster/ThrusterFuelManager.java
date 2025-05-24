package com.deltasf.createpropulsion.thruster;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public class ThrusterFuelManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final String DIRECTORY = "thruster_fuels";

    private Map<Fluid, FluidThrusterProperties> fuelPropertiesMap = new HashMap<>();
    public static final TagKey<Fluid> FORGE_FUEL_TAG = TagKey.create(ForgeRegistries.FLUIDS.getRegistryKey(), new ResourceLocation("forge", "fuel"));

    private static ThrusterFuelManager instance;

    public ThrusterFuelManager() {
        super(GSON, DIRECTORY);
        instance = this;
    }

    public static ThrusterFuelManager getInstance() {
        if (instance == null) throw new IllegalStateException("ThrusterFuelManager instance is not initialized. Calling it too early?");
        return instance;
    }

    @Nullable
    @SuppressWarnings("deprecation")
    public FluidThrusterProperties getProperties(Fluid fluid) {
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
        profiler.push(CreatePropulsion.ID + ":Loading_thruster_fuels");
        Map<Fluid, FluidThrusterProperties> newMap = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation file = entry.getKey();
            JsonElement json = entry.getValue();

            // Parse fuel def
            ThrusterFuelDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> {LOGGER.error("[{}] Failed to parse thruster fuel definition from {}: {}", CreatePropulsion.ID, file, error);})
                .ifPresent(definition -> {
                    //There is a fuel that requires a mod but the mod is not present
                    if (definition.requiredMod().isPresent() && !ModList.get().isLoaded(definition.requiredMod().get())) {
                        LOGGER.info("Not loaded {}: no mod", file.toString());
                        return;
                    }
                    Fluid fluid = definition.getFluid();
                    //Fluid is not in registry
                    if (fluid == Fluids.EMPTY) {
                        LOGGER.info("Not Loaded {}: Empty fluid", file.toString());
                        return;
                    }
                    //Successfully load fuel
                    FluidThrusterProperties properties = new FluidThrusterProperties(
                        definition.thrustMultiplier(), 
                        definition.consumptionMultiplier());
                    newMap.put(fluid, properties);
                    LOGGER.info("Loaded {}", file.toString());
                });
                
        }

        this.fuelPropertiesMap = newMap;
        profiler.pop();
    }
}
