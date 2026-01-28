package com.deltasf.createpropulsion.balloons.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.injectors.IHotAirInjector;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class BalloonSerializationUtil {

    public static byte[] serialize(Balloon balloon, BalloonRegistry registry) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(byteStream)) {
            LongOpenHashSet volume = balloon.getVolumeForSerialization();

            balloon.writeMetadata(out, registry);

            byte[] compressedVolume = BalloonCompressor.compress(volume);
            out.writeInt(volume.size());
            out.writeInt(compressedVolume.length);
            out.write(compressedVolume);
        }

        return byteStream.toByteArray();
    }

    public static Balloon deserialize(byte[] data, Level level, BalloonRegistry registry) throws IOException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
        try (DataInputStream in = new DataInputStream(byteStream)) {
            double hotAir = in.readDouble();
            int holeCount = in.readInt();
            int supportHaiCount = in.readInt();
            //Holes
            Set<BlockPos> holes = new HashSet<>();
            for(int i = 0; i < holeCount; i++) {
                holes.add(BlockPos.of(in.readLong()));
            }
            //SupportHaiPoses
            List<BlockPos> supportHaiPositions = new ArrayList<>(supportHaiCount);
            for (int i = 0; i < supportHaiCount; i++) {
                supportHaiPositions.add(BlockPos.of(in.readLong()));
            }

            //Volume metadata
            int volumeSize = in.readInt();
            int compressedVolumeSize = in.readInt();
            //Volume
            byte[] compressedVolume = in.readNBytes(compressedVolumeSize);
            long[] decompressedVolume = BalloonCompressor.decompress(compressedVolume, volumeSize);


            //Find a group for our lil balloon
            HaiGroup group = null;
            
            //By hai linking...
            for (BlockPos pos : supportHaiPositions) {
                BalloonRegistry.HaiData haiData = registry.getHaiAt(level, pos);
                if (haiData != null) {
                    group = registry.getGroupOf(haiData.id());
                    if (group != null) break; 
                }
            }

            //We are zombie, do geometric overlap check
            if (group == null) {
                //Calculate AABB of the balloon being loaded
                AABB balloonAABB = calculateAABB(decompressedVolume);
                
                if (balloonAABB != null) {
                    for (HaiGroup candidate : registry.getHaiGroups()) {
                        if (candidate.groupAABB != null && candidate.groupAABB.intersects(balloonAABB)) {
                            group = candidate;
                            break;
                        }
                    }
                }
            }

            //Nothing was found - new group it is
            if (group == null) {
                group = new HaiGroup();
                registry.getHaiGroups().add(group); 
            }

            Balloon balloon = group.createManagedBalloonFromSave(hotAir, holes, decompressedVolume, supportHaiPositions, level, registry);

            if (balloon == null) {
                System.out.println("Achtung! Achtung! We havent managed to recreate the balloon. This is bad.");
                return null; 
            }

            for(BlockPos haiPos : supportHaiPositions) {
                if (level.getBlockEntity(haiPos) instanceof IHotAirInjector injector) {
                    injector.onBalloonLoaded();
                }
            } 

            group.regenerateRLEVolume(level);
            return balloon;
        }
    }
    
    //TODO: Really should just serialize/deserialize already known aabb, but for now this suffices 
    private static AABB calculateAABB(long[] volume) {
        if (volume.length == 0) return null;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        
        for(long packed : volume) {
            int x = (int) (packed >> 38);
            if (x >= (1 << 25)) x -= (1 << 26);
            
            int y = (int) (packed & 0xFFFL);
            if (y >= (1 << 11)) y -= (1 << 12);
            
            int z = (int) ((packed >> 12) & 0x3FFFFFFL);
            if (z >= (1 << 25)) z -= (1 << 26);
            
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
            if (z < minZ) minZ = z;
            if (z > maxZ) maxZ = z;
        }
        
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }
}