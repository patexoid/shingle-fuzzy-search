package com.patex.shingle;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.patex.shingle.byteSet.ByteHashSet;
import com.patex.shingle.config.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Ca be used to determine how similar objects are
 */


public class ShingleMatcher<T, ID> {

    private final Cache<ID, Shingler> cache;
    private final Function<T, Shingleable> mapFunc;
    private final Function<T, ID> idFunc;
    private final ShingleCache<ID> shingleCache;
    private final LoadedShinglerFactory shinglerCreator;
    private final float similarity;

    private ShingleMatcher(Cache<ID, Shingler> cache, Function<T, Shingleable> mapFunc, Function<T, ID> idFunc,
                           ShingleCache<ID> shingleCache, LoadedShinglerFactory shinglerCreator, float similarity) {
        this.cache = cache;
        this.mapFunc = mapFunc;
        this.idFunc = idFunc;
        this.shingleCache = shingleCache;
        this.shinglerCreator = shinglerCreator;
        this.similarity = similarity;
    }

    public static <ID, T> Builder<ID, T> builder(Function<T, Shingleable> mapFunc, Function<T, ID> idFunc) {
        return new Builder<>(mapFunc, idFunc);
    }

    public static <T> Builder<T, T> builder(Function<T, Shingleable> mapFunc) {
        return builder(mapFunc, Function.identity());
    }

    public boolean isSimilar(T first, T second) {
        Shingler firstS = getShingler(first);
        Shingler secondS = getShingler(second);
        return isSimilar(firstS, secondS);
    }

    private boolean isSimilar(Shingler first, Shingler second) {
        Shingler bigger, smaller;
        if (first.size() > second.size()) {
            bigger = first;
            smaller = second;
        } else {
            smaller = first;
            bigger = second;
        }
        if (((float) smaller.size()) / ((float) bigger.size()) < similarity) {
            return false;
        }
        int notMatch = (int) (smaller.size() * (1 - similarity));
        for (byte[] shingleHash : smaller) {
            if (!bigger.contains(shingleHash)) {
                notMatch--;
                if (notMatch < 0) {
                    return false;
                }
            }
        }
        return true;
    }

    @SneakyThrows
    private Shingler getShingler(T t) {
        ID id = idFunc.apply(t);
        return cache.get(id, () -> shingleCache.get(id).orElse(createShingler(id, t)));
    }

    private Shingler createShingler(ID id, T t) throws IOException {
        ByteHashSet shingleSet = shinglerCreator.createShingles(mapFunc.apply(t));
        LoadedShingler shingler = new LoadedShingler(shingleSet);
        shingleCache.put(id, shingler);
        return shingler;
    }

    public void invalidate(T obj) {
        cache.invalidate(idFunc.apply(obj));
    }

    @Accessors(chain = true, fluent = true)
    @RequiredArgsConstructor
    public static class Builder<ID, T> {

        /**
         * Convert object to  {@link Shingleable}.
         */
        private final Function<T, Shingleable> mapFunc;
        /**
         * get unique id for function (used in caches)
         */
        private final Function<T, ID> idFunc;
        /**
         * External storage for shingle cache
         */
        @Setter
        ShingleCacheStorage<ID> storage;
        @Setter
        private Configuration config = Configuration.getDefaultConfig();
        @Setter
        private String lang;
        /**
         * how long shingle can be in memory cache
         */
        private long cacheDuration = 10;
        /**
         * Cache time unit
         */
        @Setter
        private TimeUnit cacheTimeUnit = TimeUnit.MINUTES;

        /**
         * how many transformed to {@link Shingleable} objects will be kept in memory cache
         */
        private int cacheSize = 100;

        /**
         * How many shingles will be used in comparison. lover values is better comparison but resource
         * consumption will be higher,
         * e.g.
         * 1 means each shingle will be used,
         * 23 means that approximately each 23th shingle will be used, and memory consumption
         * will reduced 23 times, and it will work faster, and accuracy is still good enough for most cases
         * <p>
         * Default is 1
         */
        @Setter
        private int coef = 1;

        /**
         * to increase performance and decrease memory consumption, each shingle converts to byte
         * array using hash function, max value limited by hash algorithm eg for MD5 max value is 16.
         * For now memory consumption was optimized for 8 and 16 sizes.
         * There is no validation or transformation for non 8 and 16 sizes, make sure that your hash function return
         * byte array with specified size, for 8 and 16 byte array can be bigger
         * <p/>
         * Default 16
         */
        @Setter
        private int byteArraySize = 16;

        /**
         * Algorithm to hash shingles
         * <p/>
         * Default MD5
         */
        @Setter
        private String hashAlgorithm = "MD5";

        /**
         * how similar should be object for {@link ShingleMatcher#isSimilar(java.lang.Object, java.lang.Object) }
         * <p/>
         * Default 0.7
         */
        @Setter
        private float similarity = 0.7f;

        /**
         * How many words should be in shingle
         * <p/>
         * Default 10
         */
        @Setter
        private int shingleSize = 10;

        private static Function<byte[], byte[]> hash(String hashAlgorithm) {
            ThreadLocal<MessageDigest> digestTL = ThreadLocal.withInitial(() -> createDigest(hashAlgorithm));
            return bytes -> {
                MessageDigest digest = digestTL.get();
                byte[] result = digest.digest(bytes);
                digest.reset();
                return result;
            };
        }

        @SneakyThrows
        private static MessageDigest createDigest(String hashAlgorithm) {
            return MessageDigest.getInstance(hashAlgorithm);
        }

        public Builder<ID, T> cache(int cacheSize, long cacheDuration, TimeUnit cacheTimeunit) {
            this.cacheSize = cacheSize;
            this.cacheDuration = cacheDuration;
            this.cacheTimeUnit = cacheTimeunit;
            return this;
        }

        public ShingleMatcher<T, ID> build() {
            Cache<ID, Shingler> cache = CacheBuilder.newBuilder().
                    maximumSize(cacheSize).
                    expireAfterAccess(cacheDuration, cacheTimeUnit).build();
            ShingleCache<ID> shingleCache;
            if (storage != null) {
                shingleCache = new ShingleCacheImpl<>(storage);
            } else {
                shingleCache = new ShingleCache<>() {
                    @Override
                    public Optional<Shingler> get(ID id) {
                        return Optional.empty();
                    }

                    @Override
                    public void put(ID id, Shingler shingler) {

                    }
                };
            }

            LoadedShinglerFactory factory = new LoadedShinglerFactory(coef, byteArraySize,
                    hash(hashAlgorithm), config.getLangConfig(lang), shingleSize);
            return new ShingleMatcher<>(cache, mapFunc, idFunc, shingleCache, factory, similarity);
        }
    }
}
