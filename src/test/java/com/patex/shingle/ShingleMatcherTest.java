package com.patex.shingle;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShingleMatcherTest {

    private static Shingleable toShingleable(List<String> list) {
        Iterator<String> iterator = list.iterator();
        return new Shingleable() {
            @Override
            public int size() {
                return list.size() * 6;
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public String next() {
                return iterator.next();
            }

            @Override
            public void close() {
            }
        };
    }

    private static List<String> randomWords(int count) {
        Random random = new Random();
        return Stream.generate(() -> RandomStringUtils.randomAlphabetic(1 + random.nextInt(8))).
                limit(count).collect(Collectors.toList());
    }

    @Test
    public void shouldRejectDifferentlySizedObjectsWithoutFullOverlapCheck() {
        ShingleMatcher<List<String>, List<String>> matcher = ShingleMatcher.builder(ShingleMatcherTest::toShingleable).
                cache(0, 0, TimeUnit.MINUTES).
                similarity(0.7f).
                build();

        List<String> small = randomWords(20);
        List<String> big = new ArrayList<>(small);
        big.addAll(randomWords(200));

        assertFalse(matcher.isSimilar(small, big));
    }

    @Test
    public void shouldPersistComputedShinglerToExternalStorage() {
        Map<String, byte[]> backingStore = new HashMap<>();
        AtomicInteger saveCount = new AtomicInteger();

        ShingleCacheStorage<List<String>> storage = new ShingleCacheStorage<>() {
            @Override
            public InputStream load(List<String> key) {
                byte[] bytes = backingStore.get(String.join(" ", key));
                return bytes == null ? null : new ByteArrayInputStream(bytes);
            }

            @Override
            public void save(List<String> key, byte[] bytes) {
                saveCount.incrementAndGet();
                backingStore.put(String.join(" ", key), bytes);
            }
        };

        List<String> content = randomWords(50);
        List<String> similar = new ArrayList<>(content);
        similar.add(RandomStringUtils.random(5));

        ShingleMatcher<List<String>, List<String>> matcher = ShingleMatcher.builder(ShingleMatcherTest::toShingleable).
                storage(storage).
                cache(0, 0, TimeUnit.MINUTES).
                similarity(0.7f).
                build();

        assertTrue(matcher.isSimilar(content, similar));
        assertTrue("expected the newly computed shinglers to be persisted", saveCount.get() > 0);
        assertFalse("backing store should now hold at least one entry", backingStore.isEmpty());
    }
}
