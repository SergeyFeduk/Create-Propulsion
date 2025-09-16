package com.deltasf.createpropulsion.balloons.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.deltasf.createpropulsion.balloons.Balloon;

public class ManagedHaiSet implements Set<UUID>{
    private final Set<UUID> delegate;
    private final Balloon owner;
    private final Map<UUID, Balloon> map;

    public ManagedHaiSet(Balloon owner, Map<UUID, Balloon> map, Set<UUID> initialHais) {
        this.delegate = initialHais;
        this.owner = owner;
        this.map = map;

        // Perform the initial linking
        for (UUID haiId : this.delegate) {
            map.put(haiId, owner);
        }
    }

    @Override
    public boolean add(UUID haiId) {
        boolean changed = delegate.add(haiId);
        if (changed) {
            map.put(haiId, owner);
        }
        return changed;
    }

     @Override
    public boolean remove(Object o) {
        boolean changed = delegate.remove(o);
        if (changed && o instanceof UUID) {
            // Only remove from the map if it was mapped to THIS balloon
            map.remove((UUID) o, owner);
        }
        return changed;
    }

     @Override
    public void clear() {
        for (UUID haiId : delegate) {
            map.remove(haiId, owner);
        }
        delegate.clear();
    }

    @Override
    public boolean addAll(Collection<? extends UUID> c) {
        boolean changed = false;
        for (UUID id : c) {
            if (add(id)) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object id : c) {
            if (remove(id)) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return this.removeIf(element -> !c.contains(element));
    }

     @Override
    public int size() { return delegate.size(); }

    @Override
    public boolean isEmpty() { return delegate.isEmpty(); }

    @Override
    public boolean contains(Object o) { return delegate.contains(o); }

    @Override
    public Iterator<UUID> iterator() { return delegate.iterator(); }

    @Override
    public Object[] toArray() { return delegate.toArray(); }

    @Override
    public <T> T[] toArray(T[] a) { return delegate.toArray(a); }

    @Override
    public boolean containsAll(Collection<?> c) { return delegate.containsAll(c); }
}
