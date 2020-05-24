package com.patex.shingle;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Can be used to find similar objects
 */
@Builder
@RequiredArgsConstructor
public class ShingleSearch<T,ID> {

    /**
     * Should return collection to be searched
     */
    private final Function<T, Collection<T>> preSearch;

    private final ShingleMatcher<T,ID> shingleMatcher;

    public Optional<T> findSimilar(T t) {
        return findSimilarStream(t).findFirst();
    }

    public Stream<T> findSimilarStream(T t) {
        return preSearch.apply(t).stream().filter(obj -> shingleMatcher.isSimilar(obj, t));
    }

    public void invalidate(T secondary) {
        shingleMatcher.invalidate(secondary);
    }

}
