package com.patex.shingle;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.patex.shingle.byteSet.ByteHashSet;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Ca be used to determine how similar objects are
 */
class ShingleMatcher<T, ID> {

    private final Cache<ID, Shingler> cache;
    private final Function<T, Shingleable> mapFunc;
    private final Function<T, ID> idFunc;
    private final ShingleCache<T> shingleCache;
    private final LoadedShinglerFactory shinglerCreator;
    private final float similarity;

    public ShingleMatcher(Function<T, Shingleable> mapFunc, Function<T, ID> idFunc, int coef, int cacheSize,
                          int byteArraySize){
     this(mapFunc, idFunc, coef, cacheSize, byteArraySize, "MD5", 0.7f);
    }

    /**
     *
     * @param mapFunc Convert object to  {@link Shingleable}.
     * @param idFunc get unique id for function (used in caches)
     * @param coef how many shingles will be used in comparison. lover values is better comparison but resource
     *             consumption will be higher,
     *             e.g.
     *                 1 means each shingle will be used,
     *                 23 means that approximately each 23th shingle will be used, and memory consumption
     *                 will reduced 23 times, and it will work faster
     * @param cacheSize how many transformed to {@link Shingleable} objects will be kept in inner memory cache
     * @param byteArraySize to increase performance and decrease memory consumption, each shingle converts to byte
     *                      array using hash function, max value limited by hash algorithm eg for MD5 max value is 16.
     *                      For now memory consumption was optimized for 8 and 16 sizes
     *  @param hashAlgorithm algorithm to hash shingles
     *  @param similarity how similar should be object for {@link ShingleMatcher#isSimilar(java.lang.Object, java.lang.Object) }
     */
    public ShingleMatcher(Function<T, Shingleable> mapFunc, Function<T, ID> idFunc, int coef, int cacheSize,
                          int byteArraySize,String hashAlgorithm, float similarity) {
        this.mapFunc = mapFunc;
        this.idFunc = idFunc;
        shingleCache = new ShingleCache<>();
        cache = CacheBuilder.newBuilder().
                maximumSize(cacheSize).
                expireAfterAccess(10, TimeUnit.MINUTES).build();
        shinglerCreator = new LoadedShinglerFactory(coef, byteArraySize,
                () -> {
                    ThreadLocal<MessageDigest> digestTL = ThreadLocal.withInitial(() -> {
                        try {
                            return MessageDigest.getInstance(hashAlgorithm);
                        } catch (NoSuchAlgorithmException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    return bytes -> {
                        MessageDigest digest = digestTL.get();
                        byte[] result = digest.digest(bytes);
                        digest.reset();
                        return result;
                    };
                });
        this.similarity = similarity;
    }

    public ShingleMatcher(Function<T, Shingleable> mapFunc, Function<T, ID> idFunc, int coef, int cacheSize) {
        this(mapFunc, idFunc, coef, cacheSize, 16);
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
        int notmatch = smaller.size() / 5;
        for (byte[] shingleHash : smaller) {
            if (!bigger.contains(shingleHash)) {
                notmatch--;
                if (notmatch < 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private Shingler getShingler(T t) {
        ID id = idFunc.apply(t);
        try {
            return cache.get(id, () ->
                    shingleCache.getFromCache(t).orElseGet(() ->
                            createShingler(t)));
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Shingler createShingler(T t) {
        try {
            ByteHashSet shingleSet = shinglerCreator.createShingles(mapFunc.apply(t));
            LoadedShingler shingler = new LoadedShingler(shingleSet);
            shingleCache.saveToCache(shingler, t);
            return shingler;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void invalidate(T obj) {
        cache.invalidate(idFunc.apply(obj));
    }


    /**
     * External storage for shingle hash cache
     * @param storage {@link ShingleCacheStorage}
     */
    public void setStorage(ShingleCacheStorage<T> storage) {
        shingleCache.setStorage(storage);
    }
}
