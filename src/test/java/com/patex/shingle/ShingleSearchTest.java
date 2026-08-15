package com.patex.shingle;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShingleSearchTest {

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

    private static ShingleMatcher<List<String>, List<String>> newMatcher() {
        return ShingleMatcher.builder(ShingleSearchTest::toShingleable).
                cache(0, 0, TimeUnit.MINUTES).
                similarity(0.7f).
                build();
    }

    @Test
    public void shouldFindSimilarAmongCandidates() {
        List<String> content = randomWords(100);
        List<String> similar = new ArrayList<>(content);
        similar.add(RandomStringUtils.random(5));
        List<String> different = randomWords(100);

        ShingleSearch<List<String>, List<String>> search = ShingleSearch.<List<String>, List<String>>builder().
                preSearch(t -> Arrays.asList(different, similar)).
                shingleMatcher(newMatcher()).
                build();

        Optional<List<String>> result = search.findSimilar(content);
        assertTrue(result.isPresent());
        assertEquals(similar, result.get());
    }

    @Test
    public void shouldReturnEmptyWhenNoCandidateIsSimilar() {
        List<String> content = randomWords(100);
        List<String> different = randomWords(100);

        ShingleSearch<List<String>, List<String>> search = ShingleSearch.<List<String>, List<String>>builder().
                preSearch(t -> List.of(different)).
                shingleMatcher(newMatcher()).
                build();

        assertFalse(search.findSimilar(content).isPresent());
    }

    @Test
    public void shouldStreamAllMatchingCandidates() {
        List<String> content = randomWords(100);
        List<String> similarA = new ArrayList<>(content);
        similarA.add(0, RandomStringUtils.random(5));
        List<String> similarB = new ArrayList<>(content);
        similarB.add(RandomStringUtils.random(5));
        List<String> different = randomWords(100);

        ShingleSearch<List<String>, List<String>> search = ShingleSearch.<List<String>, List<String>>builder().
                preSearch(t -> Arrays.asList(different, similarA, similarB)).
                shingleMatcher(newMatcher()).
                build();

        long matchCount = search.findSimilarStream(content).count();
        assertEquals(2, matchCount);
    }

    @Test
    public void shouldContinueWorkingAfterInvalidate() {
        List<String> content = randomWords(100);
        List<String> similar = new ArrayList<>(content);
        similar.add(RandomStringUtils.random(5));

        ShingleSearch<List<String>, List<String>> search = ShingleSearch.<List<String>, List<String>>builder().
                preSearch(t -> List.of(similar)).
                shingleMatcher(newMatcher()).
                build();

        assertTrue(search.findSimilar(content).isPresent());
        search.invalidate(content);
        assertTrue(search.findSimilar(content).isPresent());
    }
}
