package org.carl.utils;

import java.math.BigInteger;
import java.util.*;

public class SimHashIndex {
    private final int numSegments;
    private final int segmentBitSize;
    private final Map<String, List<SimHashEntry>> index = new HashMap<>();

    public SimHashIndex(int numSegments) {
        this.numSegments = numSegments;
        this.segmentBitSize = 64 / numSegments;
    }

    public void add(String text, BigInteger simHash) {
        String[] keys = makeKeys(simHash);
        SimHashEntry entry = new SimHashEntry(text, simHash);

        for (String key : keys) {
            index.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
        }
    }

    public List<SimHashEntry> query(BigInteger simHash, int maxHammingDistance) {
        Set<SimHashEntry> candidates = new HashSet<>();
        for (String key : makeKeys(simHash)) {
            List<SimHashEntry> bucket = index.getOrDefault(key, Collections.emptyList());
            for (SimHashEntry entry : bucket) {
                if (SimHash.hammingDistance(simHash, entry.simHash) <= maxHammingDistance) {
                    candidates.add(entry);
                }
            }
        }
        return new ArrayList<>(candidates);
    }

    private String[] makeKeys(BigInteger simHash) {
        String[] keys = new String[numSegments];
        String bin = String.format("%64s", simHash.toString(2)).replace(' ', '0');
        for (int i = 0; i < numSegments; i++) {
            int start = i * segmentBitSize;
            keys[i] = bin.substring(start, start + segmentBitSize) + "#" + i;
        }
        return keys;
    }

    public static class SimHashEntry {
        public final String text;
        public final BigInteger simHash;

        public SimHashEntry(String text, BigInteger simHash) {
            this.text = text;
            this.simHash = simHash;
        }

        @Override
        public int hashCode() {
            return Objects.hash(simHash);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SimHashEntry)) return false;
            return simHash.equals(((SimHashEntry) obj).simHash);
        }
    }
}
