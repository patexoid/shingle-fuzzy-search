package com.patex.shingle;

import com.patex.shingle.byteSet.ByteHashSet;
import com.patex.shingle.byteSet.ByteSetFactory;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShingleCacheImplTest {

    private final Map<String, byte[]> storage = new HashMap<>();

    private ShingleCacheImpl<String> newCache() {
        return new ShingleCacheImpl<>(new ShingleCacheStorage<>() {
            @Override
            public InputStream load(String key) {
                byte[] bytes = storage.get(key);
                return bytes == null ? null : new ByteArrayInputStream(bytes);
            }

            @Override
            public void save(String key, byte[] bytes) {
                storage.put(key, bytes);
            }
        });
    }

    private ShingleCacheImpl<String> newCappedReadCache() {
        return new ShingleCacheImpl<>(new ShingleCacheStorage<>() {
            @Override
            public InputStream load(String key) {
                byte[] bytes = storage.get(key);
                return bytes == null ? null : new ByteArrayInputStream(bytes) {
                    @Override
                    public synchronized int read(byte[] b, int off, int len) {
                        if (len > 61) {
                            len = 61;
                        }
                        return super.read(b, off, len);
                    }
                };
            }

            @Override
            public void save(String key, byte[] bytes) {
                storage.put(key, bytes);
            }
        });
    }

    private ByteHashSet randomByteSet(int count, int byteArraySize) {
        ByteHashSet byteSet = ByteSetFactory.createByteSet(count, byteArraySize);
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            byte[] b = new byte[byteArraySize];
            random.nextBytes(b);
            byteSet.add(b);
        }
        return byteSet;
    }

    @Test
    public void shouldSaveAndReturn() throws IOException {
        ShingleCacheImpl<String> shingleCache = newCappedReadCache();

        ByteHashSet byteSet = randomByteSet(10, 8);

        shingleCache.put("key", new LoadedShingler(byteSet));
        Optional<Shingler> keyO = shingleCache.get("key");
        assertTrue(keyO.isPresent());
        for (byte[] bytes : byteSet) {
            assertTrue(keyO.get().contains(bytes));
        }
        for (byte[] bytes : keyO.get()) {
            assertTrue(byteSet.contains(bytes));
        }
    }

    @Test
    public void shouldReturnEmptyWhenNoEntryForKey() throws IOException {
        ShingleCacheImpl<String> shingleCache = newCache();

        assertFalse(shingleCache.get("missing").isPresent());
    }

    @Test
    public void shouldSaveAndReturnEmptyShingler() throws IOException {
        ShingleCacheImpl<String> shingleCache = newCache();

        ByteHashSet byteSet = ByteSetFactory.createByteSet(0, 8);
        shingleCache.put("key", new LoadedShingler(byteSet));

        Optional<Shingler> keyO = shingleCache.get("key");
        assertTrue(keyO.isPresent());
        assertEquals(0, keyO.get().size());
        assertFalse(keyO.get().iterator().hasNext());
    }

    @Test
    public void shouldReturnEmptyWhenCacheTruncatedMidShingle() throws IOException {
        ShingleCacheImpl<String> shingleCache = newCache();

        ByteHashSet byteSet = randomByteSet(10, 8);
        shingleCache.put("key", new LoadedShingler(byteSet));

        byte[] full = storage.get("key");
        // cut off in the middle of the last 8-byte shingle
        storage.put("key", Arrays.copyOf(full, full.length - 3));

        assertFalse(shingleCache.get("key").isPresent());
    }

    @Test
    public void shouldReturnEmptyWhenCacheTruncatedOnShingleBoundary() throws IOException {
        ShingleCacheImpl<String> shingleCache = newCache();

        ByteHashSet byteSet = randomByteSet(10, 8);
        shingleCache.put("key", new LoadedShingler(byteSet));

        byte[] full = storage.get("key");
        // drop exactly the last whole shingle: fewer shingles than the header declares,
        // but still byte-aligned, so no partial trailing shingle is left over
        storage.put("key", Arrays.copyOf(full, full.length - 8));

        assertFalse(shingleCache.get("key").isPresent());
    }
}