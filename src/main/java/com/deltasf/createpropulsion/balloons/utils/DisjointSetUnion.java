package com.deltasf.createpropulsion.balloons.utils;

import java.util.HashMap;
import java.util.Map;

public class DisjointSetUnion {
    private final Map<Integer, Integer> parent = new HashMap<>();
    private final Map<Integer, Integer> rank = new HashMap<>();

    public void makeSet(int i) {
        parent.put(i, i);
        rank.put(i, 0);
    }

    public int find(int i) {
        if (parent.get(i) == i) {
            return i;
        }
        int root = find(parent.get(i));
        parent.put(i, root);
        return root;
    }

    public void union(int i, int j) {
        int rootI = find(i);
        int rootJ = find(j);
        if (rootI != rootJ) {
            if (rank.get(rootI) > rank.get(rootJ)) {
                parent.put(rootJ, rootI);
            } else {
                parent.put(rootI, rootJ);
                if (rank.get(rootI).equals(rank.get(rootJ))) {
                    rank.put(rootJ, rank.get(rootJ) + 1);
                }
            }
        }
    }
}