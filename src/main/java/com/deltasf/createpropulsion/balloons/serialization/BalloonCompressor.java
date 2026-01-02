package com.deltasf.createpropulsion.balloons.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class BalloonCompressor {
    public static byte[] compress(LongOpenHashSet positions) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflaterStream = new DeflaterOutputStream(byteStream)) {
            try (DataOutputStream dataStream = new DataOutputStream(deflaterStream)) {
                LongIterator it = positions.iterator();
                while (it.hasNext()) {
                    dataStream.writeLong(it.nextLong());
                }
            }
        }

        return byteStream.toByteArray();
    }
    
    public static byte[] compress(long[] positions) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflaterStream = new DeflaterOutputStream(byteStream)) {
            try (DataOutputStream dataStream = new DataOutputStream(deflaterStream)) {
                for(long pos : positions) {
                    dataStream.writeLong(pos);
                }
            }
        }

        return byteStream.toByteArray();
    }

    public static long[] decompress(byte[] data, int size) throws IOException {
        if (size == 0) return new long[0];
        long[] positions = new long[size];

        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
        try (InflaterInputStream inflaterStream = new InflaterInputStream(byteStream)) {
            try (DataInputStream dataStream = new DataInputStream(inflaterStream)) {
                for(int i = 0; i < size; i++) {
                    positions[i] = dataStream.readLong();
                }
            }
        }

        return positions;
    }
}
