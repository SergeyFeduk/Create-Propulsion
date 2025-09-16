package com.deltasf.createpropulsion.balloons.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class BalloonSerializationUtil {
    //Format:

    //double hotAir
    //int holeCount
    //int supportHaiCount
    //long[] holes
    //long[] supportHaiPoses

    //int volumeSize
    //int compressedVolumeSize
    //long[] volume (it is not written here as we compress it externally)

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

    public static Balloon deserialize(byte[] data, Level level) throws IOException {
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

            if (supportHaiPositions.isEmpty()) return null;

            //Find concrete registry
            BlockPos firstHaiPos = supportHaiPositions.get(0);
            Ship ship = VSGameUtilsKt.getShipManagingPos(level, firstHaiPos);
            if (ship == null) {
                System.out.println("Achtung! Balloon orphaned, ship not found for HAI at " + firstHaiPos);
                return null;
            }
            BalloonRegistry registry = BalloonShipRegistry.forShip(ship.getId());

            //Volume metadata
            int volumeSize = in.readInt();
            int compressedVolumeSize = in.readInt();
            //Volume
            byte[] compressedVolume = in.readNBytes(compressedVolumeSize);
            long[] decompressedVolume = BalloonCompressor.decompress(compressedVolume, volumeSize);

            //Instantiating and re-linking stuff
            HaiGroup group = null;
            for (BlockPos pos : supportHaiPositions) {
                BalloonRegistry.HaiData haiData = registry.getHaiAt(level, pos);
                if (haiData != null) {
                    group = registry.getGroupOf(haiData.id());
                    if (group != null) break; // Found it
                }
            }

            if (group == null) {
                System.out.println("Achtung! Could not find a valid HaiGroup for balloon supported by HAIs at " + supportHaiPositions);
                return null;
            }

            Balloon balloon = group.createManagedBalloonFromSave(hotAir, holes, decompressedVolume, supportHaiPositions, level, registry);

            if (balloon == null) {
                 System.out.println("Achtung! Achtung! We havent managed to recreate the balloon. This is bad.");
            }

            return balloon;
        }
    }
}
