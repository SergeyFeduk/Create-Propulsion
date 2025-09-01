package com.deltasf.createpropulsion.balloons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class Balloon implements Iterable<BlockPos> {
    private final LongOpenHashSet volume = new LongOpenHashSet();
    public AABB bounds; 
    public Set<UUID> supportHais;
    public Set<BlockPos> holes = new HashSet<>();

    public Balloon(Collection<BlockPos> initialVolume, AABB bounds, Set<UUID> supportHais) {
        addAll(initialVolume);
        this.bounds = bounds;
        this.supportHais = supportHais;
    }

    //Api

    @Override
    public Iterator<BlockPos> iterator() {
        final LongIterator it = volume.iterator();
        return new Iterator<BlockPos>() {
            @Override
            public boolean hasNext() { return it.hasNext(); }
            @Override
            public BlockPos next() { return unpackPos(it.nextLong()); }
        };
    }

    public Iterable<BlockPos> getVolume() {
        return this;
    }

    public boolean isEmpty() { return volume.isEmpty(); }

    public int size() { return volume.size(); }

    public boolean contains(BlockPos pos) {
        return volume.contains(packPos(pos));
    }

    public boolean add(BlockPos pos) {
        return volume.add(packPos(pos));
    }

    public boolean remove(BlockPos pos) {
        return volume.remove(packPos(pos));
    }

    public void addAll(Collection<BlockPos> positions) {
        if (positions == null || positions.isEmpty()) return;
        for (BlockPos p : positions) volume.add(packPos(p));
    }

    public void mergeFrom(Balloon other) {
        if (other == null) return;
        final LongIterator it = other.volume.iterator();
        while (it.hasNext()) volume.add(it.nextLong());
    }

    public void addAllTo(Collection<BlockPos> out) {
        Objects.requireNonNull(out);
        final LongIterator it = volume.iterator();
        while (it.hasNext()) out.add(unpackPos(it.nextLong()));
    }

    public List<BlockPos> toList() {
        List<BlockPos> list = new ArrayList<>(volume.size());
        addAllTo(list);
        return list;
    }

    //Position packing

    private static long packPos(int x, int y, int z) {
        return (((long) x & 0x3FFFFFFL) << 38) | (((long) z & 0x3FFFFFFL) << 12) | ((long) y & 0xFFFL);
    }

    private static int unpackX(long packed) {
        int x = (int) (packed >> 38);
        if (x >= (1 << 25)) x -= (1 << 26);
        return x;
    }

    private static int unpackY(long packed) {
        int y = (int) (packed & 0xFFFL);
        if (y >= (1 << 11)) y -= (1 << 12);
        return y;
    }

    private static int unpackZ(long packed) {
        int z = (int) ((packed >> 12) & 0x3FFFFFFL);
        if (z >= (1 << 25)) z -= (1 << 26);
        return z;
    }

    private static BlockPos unpackPos(long packed) {
        return new BlockPos(unpackX(packed), unpackY(packed), unpackZ(packed));
    }

    private static long packPos(BlockPos pos) {
        return packPos(pos.getX(), pos.getY(), pos.getZ());
    }
}
