package com.patex.shingle;

import com.patex.shingle.byteSet.ByteHashSet;
import com.patex.shingle.byteSet.ByteSetFactory;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.junit.Assert.assertTrue;

public class ShingleCacheImplTest {

    @Test
    public void shouldSaveAndReturn() throws IOException {
        ShingleCacheImpl<String> shingleCache = new ShingleCacheImpl<>(new ShingleCacheStorage<>() {
            private final Map<String, byte[]> storage = new HashMap<>();

            @Override
            public InputStream load(String s) {
                return new ByteArrayInputStream(storage.get(s)) {
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
            public void save(String s, byte[] bytes) {
                storage.put(s, bytes);
            }
        });

        ByteHashSet byteSet = ByteSetFactory.createByteSet(10, 8);
        for (int i = 0; i < 10; i++) {
            byte[] b = new byte[8];
            new Random().nextBytes(b);
            byteSet.add(b);
        }

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
}
