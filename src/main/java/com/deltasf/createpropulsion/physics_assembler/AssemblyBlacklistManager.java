package com.deltasf.createpropulsion.physics_assembler;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.*;

public class AssemblyBlacklistManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final String DIRECTORY = "assembly_blocklist";
    
    private static final Set<Block> BLACKLIST = new HashSet<>();

    public AssemblyBlacklistManager() {
        super(GSON, DIRECTORY);
    }

    public static boolean isBlacklisted(Block block) {
        return BLACKLIST.contains(block);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> resources, @Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        profiler.push(CreatePropulsion.ID + ":Loading_assembly_blacklist");
        BLACKLIST.clear();
        parseBlockBlacklists(resources);
        profiler.pop();
    }

    private void parseBlockBlacklists(@Nonnull Map<ResourceLocation, JsonElement> resources) {
        Set<Block> combinedBlacklist = new HashSet<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                BlockListDefinition definition = GSON.fromJson(json, BlockListDefinition.class);
                if (definition.blacklist == null) continue;

                for (String blockId : definition.blacklist) {
                    ResourceLocation blockLocation = ResourceLocation.tryParse(blockId);
                    if (blockLocation == null) continue;

                    Block block = ForgeRegistries.BLOCKS.getValue(blockLocation);
                    if (block != null) {
                        combinedBlacklist.add(block);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[{}] Failed to parse blacklist from file {}: {}", CreatePropulsion.ID, fileId, e.getMessage());
            }
        }
        BLACKLIST.addAll(combinedBlacklist);
    }

    private static class BlockListDefinition {
        List<String> blacklist;
    }
}