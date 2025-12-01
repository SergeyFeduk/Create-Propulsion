package com.deltasf.createpropulsion.registries;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;

public class PropulsionMassProvider implements DataProvider {
    private final PackOutput packOutput;

    public PropulsionMassProvider(PackOutput packOutput) {
        this.packOutput = packOutput;
    }

    @Override
    public CompletableFuture<?> run(@Nonnull CachedOutput cache) {
        List<MassEntry> entries = new ArrayList<>();

        for (PropulsionBlocks.EnvelopeColor color : PropulsionBlocks.EnvelopeColor.values()) {
            ResourceLocation envelopeId = PropulsionBlocks.getEnvelope(color).getId();
            entries.add(new MassEntry(envelopeId.toString(), 10.0, 0.2));
            ResourceLocation shaftId = PropulsionBlocks.getEnvelopedShaft(color).getId();
            entries.add(new MassEntry(shaftId.toString(), 10.0, 0.2));
        }

        JsonArray jsonArray = new JsonArray();
        for (MassEntry entry : entries) {
            JsonObject object = new JsonObject();
            object.addProperty("block", entry.block);
            object.addProperty("mass", entry.mass);
            object.addProperty("friction", entry.friction);
            jsonArray.add(object);
        }

        Path path = this.packOutput.getOutputFolder()
            .resolve("data")
            .resolve(CreatePropulsion.ID)
            .resolve("vs_mass")
            .resolve("envelope.json");
        return DataProvider.saveStable(cache, jsonArray, path);
    }

    @Override
    public String getName() {
        return "Propulsion mass provider";
    }

    private static class MassEntry {
        final String block;
        final double mass;
        final double friction;

        public MassEntry(String block, double mass, double friction) {
            this.block = block;
            this.mass = mass;
            this.friction = friction;
        }
    }
}
