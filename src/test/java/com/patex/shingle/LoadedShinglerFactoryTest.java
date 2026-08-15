package com.patex.shingle;

import com.patex.shingle.byteSet.ByteHashSet;
import com.patex.shingle.config.LangConfig;
import org.junit.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LoadedShinglerFactoryTest {

    private static final LangConfig NO_SKIP_WORDS_CONFIG = langConfig(Collections.emptySet());

    private static LangConfig langConfig(Set<String> skipWords) {
        return new LangConfig() {
            @Override
            public String normalize(String s) {
                return s.toLowerCase();
            }

            @Override
            public Set<String> getSkipWords() {
                return skipWords;
            }

            @Override
            public int getAverageWordLength() {
                return 5;
            }

            @Override
            public String getDelimiters() {
                return " ";
            }
        };
    }

    private static byte[] md5(byte[] bytes) {
        try {
            return MessageDigest.getInstance("MD5").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private static Shingleable toShingleable(String text) {
        Iterator<String> words = Arrays.asList(text.split(" ")).iterator();
        return new Shingleable() {
            @Override
            public int size() {
                return text.length();
            }

            @Override
            public boolean hasNext() {
                return words.hasNext();
            }

            @Override
            public String next() {
                return words.next();
            }

            @Override
            public void close() {
            }
        };
    }

    @Test
    public void shouldProduceSlidingWindowCountOfShingles() {
        LoadedShinglerFactory factory =
                new LoadedShinglerFactory(1, 16, LoadedShinglerFactoryTest::md5, NO_SKIP_WORDS_CONFIG, 3);

        // 6 distinct words, window size 3 => 6 - 3 + 1 = 4 shingles
        ByteHashSet shingles = factory.createShingles(toShingleable("one two three four five six"));

        assertEquals(4, shingles.getSize());
    }

    @Test
    public void shouldProduceNoShinglesWhenFewerWordsThanShingleSize() {
        LoadedShinglerFactory factory =
                new LoadedShinglerFactory(1, 16, LoadedShinglerFactoryTest::md5, NO_SKIP_WORDS_CONFIG, 5);

        ByteHashSet shingles = factory.createShingles(toShingleable("one two three"));

        assertEquals(0, shingles.getSize());
    }

    @Test
    public void shouldFilterOutConfiguredSkipWords() {
        LoadedShinglerFactory withSkipWords = new LoadedShinglerFactory(1, 16, LoadedShinglerFactoryTest::md5,
                langConfig(Set.of("the", "a")), 2);
        LoadedShinglerFactory withoutSkipWords =
                new LoadedShinglerFactory(1, 16, LoadedShinglerFactoryTest::md5, NO_SKIP_WORDS_CONFIG, 2);

        // once "the"/"a" are filtered out, both texts reduce to the same word sequence
        ByteHashSet filtered = withSkipWords.createShingles(toShingleable("the cat sat on a mat"));
        ByteHashSet alreadyClean = withoutSkipWords.createShingles(toShingleable("cat sat on mat"));

        assertEquals(alreadyClean.getSize(), filtered.getSize());
        for (byte[] shingle : alreadyClean) {
            assertTrue(filtered.contains(shingle));
        }
    }

    @Test
    public void shouldNormalizeWordsBeforeHashing() {
        LoadedShinglerFactory factory =
                new LoadedShinglerFactory(1, 16, LoadedShinglerFactoryTest::md5, NO_SKIP_WORDS_CONFIG, 3);

        ByteHashSet upperCase = factory.createShingles(toShingleable("ONE TWO THREE FOUR"));
        ByteHashSet lowerCase = factory.createShingles(toShingleable("one two three four"));

        assertEquals(lowerCase.getSize(), upperCase.getSize());
        for (byte[] shingle : lowerCase) {
            assertTrue(upperCase.contains(shingle));
        }
    }

    @Test
    public void shouldDownsampleShinglesByCoef() {
        LoadedShinglerFactory full = new LoadedShinglerFactory(1, 16, LoadedShinglerFactoryTest::md5,
                NO_SKIP_WORDS_CONFIG, 3);
        LoadedShinglerFactory downsampled = new LoadedShinglerFactory(4, 16, LoadedShinglerFactoryTest::md5,
                NO_SKIP_WORDS_CONFIG, 3);

        String text = "a b c d e f g h i j k l m n o p q r s t";
        ByteHashSet fullSet = full.createShingles(toShingleable(text));
        ByteHashSet sampledSet = downsampled.createShingles(toShingleable(text));

        assertTrue(sampledSet.getSize() < fullSet.getSize());
        for (byte[] shingle : sampledSet) {
            assertTrue(fullSet.contains(shingle));
        }
    }
}
