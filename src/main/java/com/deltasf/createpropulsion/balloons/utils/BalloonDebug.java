package com.deltasf.createpropulsion.balloons.utils;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry.HaiData;
import com.deltasf.createpropulsion.debug.DebugRenderer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.awt.Color;
import java.util.List;
import java.util.UUID;
import java.util.Collection;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BalloonDebug {
    private static final boolean IS_ENABLED = true;
    private static final float GOLDEN_RATIO_CONJUGATE = 0.61803398875f;

    private static final Color[] GROUP_COLORS = new Color[] {
        new Color(255, 0, 0),  
        new Color(0, 255, 0),   
        new Color(0, 0, 255),   
        new Color(255, 255, 0), 
        new Color(0, 255, 255), 
        new Color(255, 0, 255)
    };

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!IS_ENABLED || event.phase != TickEvent.Phase.END) {
            return;
        }

        BalloonShipRegistry shipRegistry = BalloonShipRegistry.get();
        if (shipRegistry == null) {
            return;
        }

        Collection<BalloonRegistry> allRegistries = shipRegistry.getRegistries();
        
        int groupIndex = 0;
        int balloonIndex = 0; // Index for coloring balloons
        for (BalloonRegistry registry : allRegistries) {
            List<HaiGroup> groups = registry.getHaiGroups();
            if (groups.isEmpty()) {
                continue;
            }

            for (HaiGroup group : groups) {
                Color baseColor = GROUP_COLORS[groupIndex % GROUP_COLORS.length];
                renderHaiGroupBounds(group, baseColor);
                
                for (Balloon balloon : group.balloons) {
                    // Generate a unique color
                    float hue = (balloonIndex * GOLDEN_RATIO_CONJUGATE) % 1.0f;
                    Color balloonColor = Color.getHSBColor(hue, 0.8f, 0.95f);
                    
                    renderBalloonVolume(balloon, balloonColor);
                    balloonIndex++;

                    //Render holes
                    for(BlockPos hole : balloon.holes) {
                        String identifier = hole.toShortString() + "_hole";
                        DebugRenderer.drawBox(identifier, new AABB(hole), Color.white, 3);
                    }
                }

                groupIndex++;
            }
        }
    }

    private static void renderBalloonVolume(Balloon balloon, Color color) {
        String balloonIdPrefix = "balloon_vol_" + balloon.hashCode() + "_";

        for (BlockPos pos : balloon) {
            String blockIdentifier = balloonIdPrefix + pos.asLong();
            DebugRenderer.drawBox(blockIdentifier, pos, color, 3);
        }
    }


    private static void renderHaiGroupBounds(HaiGroup group, Color baseColor) {
        Color haiAABBColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 80);
        for (HaiData hai : group.hais) {
            String haiIdentifier = "hai_aabb_" + hai.id().toString();
            DebugRenderer.drawBox(haiIdentifier, hai.aabb(), haiAABBColor, 3);
        }
    }

    public static void displayBlockFor(BlockPos pos, int ticks, Color color) {
        String ident = UUID.randomUUID().toString();
        DebugRenderer.drawBox(ident, pos, color, ticks);
    }
}
