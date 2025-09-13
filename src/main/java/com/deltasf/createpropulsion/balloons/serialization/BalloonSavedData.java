package com.deltasf.createpropulsion.balloons.serialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

//TODO: Remove, this all is wrong
/*public class BalloonSavedData extends SavedData {
    private static final String DATA_NAME = CreatePropulsion.ID + "_balloons";

    private final List<byte[]> rawBalloonData = new ArrayList<>();
    //private boolean needsProcessing = false;

    public static BalloonSavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();

        BalloonSavedData data = storage.computeIfAbsent(BalloonSavedData::load, BalloonSavedData::new, DATA_NAME);

        /*if (data.needsProcessing) {
            data.processLoadedData(level);
            data.needsProcessing = false;
        }

        return data;
    }

    public static BalloonSavedData load(CompoundTag nbt) {
        BalloonSavedData savedData = new BalloonSavedData();
        ListTag balloonsTag = nbt.getList("balloons", Tag.TAG_BYTE_ARRAY);

        for(int i = 0; i < balloonsTag.size(); i++) {
            Tag tag = balloonsTag.get(i);

            if (tag instanceof ByteArrayTag bat) {
                savedData.rawBalloonData.add(bat.getAsByteArray());
            }
        }

        //savedData.needsProcessing = !savedData.rawBalloonData.isEmpty();
        return savedData;
    }

    public void processLoadedData(ServerLevel level) {
        //BalloonShipRegistry.get().reset(); //TODO: Reset per dimension

        for(byte[] balloonData : rawBalloonData) {
            try {
                BalloonSerializationUtil.deserialize(balloonData, level);
            } catch (IOException e) {
                System.out.println("Failed to deserialize balloon: " + e);
            }
        }
        System.out.println("Loaded balloons");

        rawBalloonData.clear();
    }

    @Override
    public CompoundTag save(@Nonnull CompoundTag nbt) {
        ListTag balloonsTag = new ListTag();
        for(BalloonRegistry registry : BalloonShipRegistry.get().getRegistries()) {
            for(Balloon balloon : registry.getBalloons()) {
                try {
                    byte[] data = BalloonSerializationUtil.serialize(balloon, registry);
                    balloonsTag.add(new ByteArrayTag(data));
                } catch (IOException e) {
                    System.out.println("Failed to serialize balloon: " + e);
                }
            }
        }

        System.out.println("Saved balloons");
        nbt.put("balloons", balloonsTag);
        return nbt;
    }

}
*/