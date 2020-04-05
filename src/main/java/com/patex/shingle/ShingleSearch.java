package com.patex.shingle;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Can be used to find similar objects
 */
public class ShingleSearch<T,ID> {

    private final Function<T, Collection<T>> preSearch;
    private final ShingleMatcher<T,ID> shingleMatcher;


    /**
     *
     * @param preSearch should return collection to be searched
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
    public ShingleSearch(Function<T, Collection<T>> preSearch,
                         Function<T, Shingleable> mapFunc,
                         Function<T, ID> idFunc, int coef, int cacheSize, int byteArraySize,
                         String hashAlgorithm, float similarity
    ) {
        this.preSearch = preSearch;
        shingleMatcher=new ShingleMatcher<>(mapFunc, idFunc, coef, cacheSize, byteArraySize, hashAlgorithm, similarity);
    }

    public ShingleSearch(Function<T, Collection<T>> preSearch,
                         Function<T, Shingleable> mapFunc,
                         Function<T, ID> idFunc, int coef, int cacheSize
    ) {
        this.preSearch = preSearch;
        shingleMatcher=new ShingleMatcher<>(mapFunc, idFunc, coef, cacheSize);
    }

    public Optional<T> findSimilar(T t) {
        return findSimilarStream(t).findFirst();
    }

    public Stream<T> findSimilarStream(T t) {
        return preSearch.apply(t).stream().filter(obj -> shingleMatcher.isSimilar(obj, t));
    }


    public void invalidate(T secondary) {
        shingleMatcher.invalidate(secondary);
    }

    public void setStorage(ShingleCacheStorage<T> storage) {
        shingleMatcher.setStorage(storage);
    }
}
