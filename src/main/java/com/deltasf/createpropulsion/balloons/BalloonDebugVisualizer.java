package com.deltasf.createpropulsion.balloons;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.balloons.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.BalloonShipRegistry;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.debug.DebugRenderer;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.awt.Color;
import java.util.List;
import java.util.Collection;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BalloonDebugVisualizer {

    // Set to 'false' to see MAXAABBs and the final calculated balloon volume.
    // Set to 'true' to see the precise RLE union volume.
    private static final boolean DISPLAY_RLE_VOLUME = false;

    // Colors for the MAXAABB groups
    private static final Color[] GROUP_COLORS = new Color[] {
        new Color(255, 0, 0, 150),   // Red
        new Color(0, 255, 0, 150),   // Green
        new Color(0, 0, 255, 150),   // Blue
        new Color(255, 255, 0, 150), // Yellow
        new Color(0, 255, 255, 150), // Cyan
        new Color(255, 0, 255, 150)  // Magenta
    };

    // --- NEW COLOR PALETTE for the final, distinct balloons ---
    private static final Color[] BALLOON_COLORS = new Color[] {
        new Color(255, 165, 0),    // Orange
        new Color(255, 192, 203),  // Pink
        new Color(0, 250, 154),    // Medium Spring Green
        new Color(135, 206, 250),  // Light Sky Blue
        new Color(148, 0, 211),    // Dark Violet
        new Color(245, 245, 220)   // Beige
    };

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        BalloonShipRegistry shipRegistry = BalloonShipRegistry.get();
        if (shipRegistry == null) {
            return;
        }

        Collection<BalloonRegistry> allRegistries = shipRegistry.getRegistries();

        int groupIndex = 0;
        for (BalloonRegistry registry : allRegistries) {
            List<HaiGroup> groups = registry.getHaiGroups();
            if (groups.isEmpty()) continue;

            for (HaiGroup group : groups) {
                Color groupColor = GROUP_COLORS[groupIndex % GROUP_COLORS.length];

                if (DISPLAY_RLE_VOLUME) {
                    renderRleVolume(group, groupColor, groupIndex);
                } else {
                    renderDebugVisualization(group, groupColor, groupIndex);
                }

                groupIndex++;
            }
        }
    }

    /**
     * Renders the MAXAABBs for each HAI in a group, AND renders the final calculated
     * interior air blocks for any successfully discovered balloons with unique colors.
     */
    private static void renderDebugVisualization(HaiGroup group, Color color, int groupIndex) {
        // Part 1: Render the MAXAABBs for the group (same as before).
        for (BalloonRegistry.HaiData hai : group.getHais()) {
            String identifier = "hai_group_aabb_" + hai.id().toString();
            DebugRenderer.drawBox(identifier, hai.maxAABB(), color, 3);
        }

        // Part 2: Render the final calculated air blocks with unique colors per balloon.
        List<HaiGroup.Balloon> finalizedBalloons = group.getFinalizedBalloons();
        if (finalizedBalloons == null || finalizedBalloons.isEmpty()) {
            return;
        }

        int balloonIndex = 0;
        for (HaiGroup.Balloon balloon : finalizedBalloons) {
            // --- THIS IS THE FIX ---
            // Assign a unique color to each balloon found within the group.
            Color balloonColor = BALLOON_COLORS[balloonIndex % BALLOON_COLORS.length];

            for (BlockPos pos : balloon.interiorAir) {
                String identifier = "final_air_" + groupIndex + "_" + balloonIndex + "_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
                // Draw the box using the unique balloon color instead of white.
                DebugRenderer.drawBox(identifier, pos, balloonColor, 3);
            }
            balloonIndex++;
        }
    }


    private static void renderRleVolume(HaiGroup group, Color color, int groupIndex) {
        // ... (This method remains unchanged) ...
        AABB groupAABB = group.getGroupAABB();
        List<Pair<Integer, Integer>>[][] rleVolume = group.getRleVolume();

        if (groupAABB == null || rleVolume == null) {
            return;
        }
        for (int y = 0; y < rleVolume.length; y++) {
            for (int x = 0; x < rleVolume[y].length; x++) {
                List<Pair<Integer, Integer>> zIntervals = rleVolume[y][x];
                if (zIntervals == null || zIntervals.isEmpty()) {
                    continue;
                }
                for (int i = 0; i < zIntervals.size(); i++) {
                    Pair<Integer, Integer> zInterval = zIntervals.get(i);
                    AABB segmentAABB = new AABB(
                        groupAABB.minX + x,
                        groupAABB.minY + y,
                        zInterval.getFirst(),
                        groupAABB.minX + x + 1,
                        groupAABB.minY + y + 1,
                        zInterval.getSecond()
                    );
                    String identifier = "rle_" + groupIndex + "_" + x + "_" + y + "_" + i;
                    DebugRenderer.drawBox(identifier, segmentAABB, color, 3);
                }
            }
        }
    }
}