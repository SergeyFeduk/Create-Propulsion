package com.deltasf.createpropulsion.utility.math;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;

// Based on: https://gist.github.com/alksily/7a85a1898e65c936f861ee93516e397d perlin noise implementation by alksily

public class PerlinNoise {
    private double seed;
    private int[] p;
    private int[] permutation;
    private final List<NoiseOctave> octaves;

    public static final List<NoiseOctave> MONO = List.of(new NoiseOctave(200, 0.03));

    public PerlinNoise(double seed, List<NoiseOctave> octaves) {
        this.seed = seed;
        this.octaves = (octaves == null) ? MONO : new ArrayList<>(octaves);
        init();
    }

    public PerlinNoise(List<NoiseOctave> octaves) {
        this.seed = new Random().nextGaussian() * 255;
        this.octaves = (octaves == null) ? MONO : new ArrayList<>(octaves);
        init();
    }

    private void init() {
        this.p = new int[512];
        this.permutation = new int[] {151,160,137,91,90,15,131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,190,6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,88,237,149,56,87,174,20,125,136,171,168,68,175,74,165,71,134,139,48,27,166,77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,102,143,54,65,25,63,161,1,216,80,73,209,76,132,187,208,89,18,169,200,196,135,130,116,188,159,86,164,100,109,198,173,186,3,64,52,217,226,250,124,123,5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,223,183,170,213,119,248,152,2,44,154,163,70,221,153,101,155,167,43,172,9,129,22,39,253,19,98,108,110,79,113,224,232,178,185,112,104,218,246,97,228,251,34,242,193,238,210,144,12,191,179,162,241,81,51,145,235,249,14,239,107,49,192,214,31,181,199,106,157,184,84,204,176,115,121,50,45,127,4,150,254,138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180};
        for (int i = 0; i < 256; i++) {
            p[256 + i] = p[i] = permutation[i];
        }
    }

    public void setSeed(double seed) {
        this.seed = seed;
    }

    public double getSeed() {
        return this.seed;
    }

    public double noise(double x, double y, double z) {
        double value = 0.0;
        double totalMagnitude = 0.0;
        for (NoiseOctave o : octaves) {
            double scale = o.scale();
            double mag = o.magnitude();
            if (scale <= 0) continue;
            value += smoothNoise(x / scale, y / scale, z / scale) * mag;
            totalMagnitude += mag;
        }
        if (totalMagnitude == 0.0) return 0.0;
        return value / totalMagnitude;
    }

    private double smoothNoise(double x, double y, double z) {
        x += this.seed;
        y += this.seed;
        z += this.seed;

        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        int Z = (int) Math.floor(z) & 255;

        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);

        double u = fade(x);
        double v = fade(y);
        double w = fade(z);

        int A = p[X] + Y;
        int AA = p[A] + Z;
        int AB = p[A + 1] + Z;
        int B = p[X + 1] + Y;
        int BA = p[B] + Z;
        int BB = p[B + 1] + Z;

        return lerp(w, 
            lerp(v, lerp(u, grad(p[AA], x, y, z), grad(p[BA], x - 1, y, z)),
                    lerp(u, grad(p[AB], x, y - 1, z), grad(p[BB], x - 1, y - 1, z))),

            lerp(v, lerp(u, grad(p[AA + 1], x, y, z - 1), grad(p[BA + 1], x - 1, y, z - 1)),
                    lerp(u, grad(p[AB + 1], x, y - 1, z - 1), grad(p[BB + 1], x - 1, y - 1, z - 1))));
    }

    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
