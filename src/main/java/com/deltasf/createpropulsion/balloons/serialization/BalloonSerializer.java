package com.deltasf.createpropulsion.balloons.serialization;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.nio.file.StandardCopyOption;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

public class BalloonSerializer {
    private BalloonSerializer() {}

    private static final int CURRENT_SERIALIZATION_VERSION = -1;
    private static final String MOD_DATA_FOLDER = "propulsion";
    private static final String BALLOON_DATA_FOLDER = "balloons";

    public static void loadForShip(ServerLevel level, long shipId) throws IOException {
        Path filePath = getShipDataPath(level, shipId);
        if (!Files.exists(filePath)) return;

        try (InputStream fileStream = Files.newInputStream(filePath); DataInputStream dataStream = new DataInputStream(fileStream)) {
            if (dataStream.available() < 4) return;

            int header = dataStream.readInt();

            if (header < 0 && header == -1) {
                int balloonCount = dataStream.readInt();
                
                for (int i = 0; i < balloonCount; i++) {
                    int length = dataStream.readInt();
                    byte[] balloonData = dataStream.readNBytes(length);
                    BalloonSerializationUtil.deserialize(balloonData, level);
                }
            } else {
                int firstLength = header;
                byte[] firstBalloonData = dataStream.readNBytes(firstLength);
                BalloonSerializationUtil.deserialize(firstBalloonData, level);

                while (dataStream.available() > 0) {
                    int nextLength = dataStream.readInt();
                    byte[] nextData = dataStream.readNBytes(nextLength);
                    BalloonSerializationUtil.deserialize(nextData, level);
                }
            }
        }
    }

    public static void saveForShip(ServerLevel level, long shipId) throws IOException {
        BalloonRegistry registry = BalloonShipRegistry.forShip(shipId);
        List<Balloon> balloons = registry.getBalloons();
        
        Path filePath = getShipDataPath(level, shipId);
        if (balloons.isEmpty()) {
            Files.deleteIfExists(filePath);
            return;
        }

        Path tempFilePath = filePath.resolveSibling(filePath.getFileName().toString() + ".tmp");

        try(OutputStream fileStream = Files.newOutputStream(tempFilePath); DataOutputStream dataStream = new DataOutputStream(fileStream)) {
            dataStream.writeInt(CURRENT_SERIALIZATION_VERSION); 
            dataStream.writeInt(balloons.size());

            for(Balloon balloon : balloons) {
                byte[] data = BalloonSerializationUtil.serialize(balloon, registry);
                dataStream.writeInt(data.length);
                dataStream.write(data);
            }
        }

        Files.move(tempFilePath, filePath, StandardCopyOption.REPLACE_EXISTING);
    }

    @SuppressWarnings("null")
    private static Path getShipDataPath(Level level, long shipId) throws IOException {
        if (level.getServer() == null) {
            throw new IOException("Cannot get save path on a client-side level.");
        }
        MinecraftServer server = level.getServer();
        Path worldSavePath = server.getWorldPath(LevelResource.ROOT);
        Path dataDir = worldSavePath.resolve(MOD_DATA_FOLDER).resolve(BALLOON_DATA_FOLDER);
        Files.createDirectories(dataDir);
        return dataDir.resolve("ship_" + shipId + ".dat");
    }
}
