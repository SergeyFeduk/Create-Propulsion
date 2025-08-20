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
import java.util.EnumSet;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BalloonDebugVisualizer {

    public enum DisplayMode {
        MAXAABBS,       // Renders the initial maxAABB for each HAI.
        BALLOON_VOLUME, // Renders the final calculated interior air volume of each balloon.
        RLE_VOLUME,     // Renders the optimized Run-Length Encoded scan volume.
        SHELL_VOLUME    // Renders the collected shell blocks of each balloon.
    }

    private static final EnumSet<DisplayMode> ACTIVE_MODES = EnumSet.of(
        DisplayMode.MAXAABBS,
        DisplayMode.SHELL_VOLUME,
        DisplayMode.BALLOON_VOLUME
    );

    // --- COLOR PALETTES ---
    private static final Color SHELL_COLOR = Color.WHITE;

    private static final Color[] GROUP_COLORS = new Color[] {
        new Color(255, 0, 0, 150),   // Red
        new Color(0, 255, 0, 150),   // Green
        new Color(0, 0, 255, 150),   // Blue
        new Color(255, 255, 0, 150), // Yellow
        new Color(0, 255, 255, 150), // Cyan
        new Color(255, 0, 255, 150)  // Magenta
    };

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
        if (event.phase != TickEvent.Phase.END || ACTIVE_MODES.isEmpty()) {
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

                if (ACTIVE_MODES.contains(DisplayMode.MAXAABBS)) {
                    renderMaxAabbs(group, groupColor);
                }
                if (ACTIVE_MODES.contains(DisplayMode.BALLOON_VOLUME)) {
                    renderBalloonVolume(group, groupIndex);
                }
                if (ACTIVE_MODES.contains(DisplayMode.RLE_VOLUME)) {
                    renderRleVolume(group, groupColor, groupIndex);
                }
                if (ACTIVE_MODES.contains(DisplayMode.SHELL_VOLUME)) {
                    //renderShellVolume(group, groupIndex);
                }

                groupIndex++;
            }
        }
    }

    /**
     * Renders the MAXAABBs for each HAI in a group.
     */
    private static void renderMaxAabbs(HaiGroup group, Color color) {
        for (BalloonRegistry.HaiData hai : group.getHais()) {
            String identifier = "max_aabb_" + hai.id().toString();
            DebugRenderer.drawBox(identifier, hai.maxAABB(), color, 3);
        }
    }

    /**
     * Renders the precise Run-Length Encoded union volume for a group.
     */
    private static void renderRleVolume(HaiGroup group, Color color, int groupIndex) {
        AABB groupAABB = group.getGroupAABB();
        List<Pair<Integer, Integer>>[][] rleVolume = group.getRleVolume();

        if (groupAABB == null || rleVolume == null) {
            return;
        }
        for (int y = 0; y < rleVolume.length; y++) {
            if (rleVolume[y] == null) continue;
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
                        zInterval.getSecond() + 1 // Add 1 to maxZ for correct AABB rendering
                    );
                    String identifier = "rle_" + groupIndex + "_" + x + "_" + y + "_" + i;
                    DebugRenderer.drawBox(identifier, segmentAABB, color, 3);
                }
            }
        }
    }

    private static void renderBalloonVolume(HaiGroup group, int groupIndex) {
    List<HaiGroup.Balloon> balloons = group.getFinalizedBalloons();
    if (balloons == null || balloons.isEmpty()) {
        return;
    }

    int balloonIndex = 0;
    for (HaiGroup.Balloon balloon : balloons) {
        // Cycle through the balloon color palette for each separate balloon
        Color balloonColor = BALLOON_COLORS[balloonIndex % BALLOON_COLORS.length];

        for (BlockPos pos : balloon.interiorAir) {
            // Create a unique identifier for each block to prevent flickering
            String identifier = "balloon_vol_" + groupIndex + "_" + balloonIndex + "_" + pos.hashCode();
            // Create a 1x1x1 AABB at the block's position
            AABB blockAABB = new AABB(pos);
            
            DebugRenderer.drawBox(identifier, blockAABB, balloonColor, 3);
        }
        balloonIndex++;
    }
}

}