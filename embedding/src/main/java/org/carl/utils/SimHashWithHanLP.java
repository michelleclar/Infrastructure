package org.carl.utils;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;
import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class SimHashWithHanLP {

    private final static int hashBitCount = 64;
    private final static Segment segment = HanLP.newSegment().enableIndexMode(true);

    public static List<String> segmentText(String text) {
        List<Term> terms = segment.seg(text);
        return terms.stream()
                .map(term -> term.word)
                .filter(word -> !word.trim().isEmpty())
                .collect(Collectors.toList());
    }

    public static long calculateSimHash(String text, boolean useTermFrequency) {
        List<String> words = segmentText(text);

        int[] v = new int[hashBitCount];
        Map<String, Integer> termFrequencies = new HashMap<>();

        if (useTermFrequency) {
            for (String word : words) {
                termFrequencies.put(word, termFrequencies.getOrDefault(word, 0) + 1);
            }
        }

        for (String word : words) {
            long wordHash = Hashing.murmur3_128().hashString(word, StandardCharsets.UTF_8).asLong();
            int weight = useTermFrequency ? termFrequencies.get(word) : 1;

            for (int i = 0; i < hashBitCount; i++) {
                if (((wordHash >>> i) & 1) == 1) {
                    v[i] += weight;
                } else {
                    v[i] -= weight;
                }
            }
        }

        long simHash = 0L;
        for (int i = 0; i < hashBitCount; i++) {
            if (v[i] > 0) {
                simHash |= (1L << i);
            }
        }
        return simHash;
    }

    public static int hammingDistance(long hash1, long hash2) {
        return Long.bitCount(hash1 ^ hash2);
    }
}
