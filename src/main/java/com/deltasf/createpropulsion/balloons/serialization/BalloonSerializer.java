package com.deltasf.createpropulsion.balloons.serialization;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

    private static final String MOD_DATA_FOLDER = "propulsion";
    private static final String BALLOON_DATA_FOLDER = "balloons";

    public static void loadForShip(ServerLevel level, long shipId) throws IOException {
        //Load compoundData from disk
        List<byte[]> compoundData = new ArrayList<>();
        Path filePath = getShipDataPath(level, shipId);

        if (!Files.exists(filePath)) {
            return; //Ship does not have any balloons on it, skip it
        }

        try (InputStream fileStream = Files.newInputStream(filePath); DataInputStream dataStream = new DataInputStream(fileStream)) {
            while (dataStream.available() > 0) {
                int length = dataStream.readInt();
                byte[] balloonData = dataStream.readNBytes(length);
                if (balloonData.length != length) {
                    throw new IOException("What the actual fuck is happening? We expected " + length + " bytes but got " + balloonData.length);
                }
                compoundData.add(balloonData);
            }
        }

        //Deserialize all balloons from compoundData
        for(byte[] balloonData : compoundData) {
            BalloonSerializationUtil.deserialize(balloonData, level);
        }
    }

    public static void saveForShip(ServerLevel level, long shipId) throws IOException {
        BalloonRegistry registry = BalloonShipRegistry.forShip(shipId);
        //Serialize balloons into compoundData
        List<Balloon> balloons = registry.getBalloons();
        List<byte[]> compoundData = new ArrayList<>();
        for(Balloon balloon : balloons) {
            try {
                byte[] data = BalloonSerializationUtil.serialize(balloon, registry);
                compoundData.add(data);
            } catch (IOException e) {
                throw new IOException("Failed to serialize a balloon: ", e);
            }
        }
        //Save compoundData to disk
        Path filePath = getShipDataPath(level, shipId);
        if (compoundData == null || compoundData.isEmpty()) {
            Files.deleteIfExists(filePath);
            return;
        }

        Path tempFilePath = filePath.resolveSibling(filePath.getFileName().toString() + ".tmp");

        try(OutputStream fileStream = Files.newOutputStream(tempFilePath); DataOutputStream dataStream = new DataOutputStream(fileStream)) {
            for(byte[] balloonData : compoundData) {
                dataStream.writeInt(balloonData.length);
                dataStream.write(balloonData);
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
