package com.patex.shingle;


import com.google.common.collect.EvictingQueue;
import com.patex.shingle.byteSet.ByteHashSet;
import com.patex.shingle.byteSet.ByteSetFactory;
import com.patex.shingle.config.LangConfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Function;

/**
 *
 */
class LoadedShinglerFactory {

    private final int coef;
    private final Function<byte[], byte[]> hashFunc;
    private final LangConfig config;
    private final int byteArraySize;
    private final int shingleSize;

    LoadedShinglerFactory(int coef, int byteArraySize,
                          Function<byte[], byte[]> hashFunc, LangConfig config, int shingleSize) {
        this.coef = coef;
        this.hashFunc = hashFunc;
        this.byteArraySize = byteArraySize;
        this.config = config;
        this.shingleSize = shingleSize;
    }

    public ByteHashSet createShingles(Shingleable shingleable) {
        ShingleIterator shingleIterator = new ShingleIterator(shingleable);
        int skip = shingleSize;
        while (shingleIterator.hasNext() && skip-- > 1) {
            shingleIterator.next();
        }
        int size = shingleable.size() / config.getAverageWordLength() / 2 / coef;
        ByteHashSet byteSet = ByteSetFactory.createByteSet(size, this.byteArraySize);
        while (shingleIterator.hasNext()) {
            byte[] bytes = shingleIterator.next();
            byte d = 0;
            for (byte b : bytes) {
                d ^= b;
            }
            if (d % coef == 0) {
                byteSet.add(bytes);
            }
        }
        return byteSet;
    }

    @SuppressWarnings("UnstableApiUsage")
    private class ShingleIterator implements Iterator<byte[]> {
        private final Shingleable shingleable;
        private final List<String> chunk = new ArrayList<>();
        private final EvictingQueue<String> words = EvictingQueue.create(shingleSize);

        ShingleIterator(Shingleable shingleable) {
            this.shingleable = shingleable;
            loadNextChunk();
        }

        @Override
        public boolean hasNext() {
            return shingleable.hasNext() || !chunk.isEmpty();
        }

        @Override
        public byte[] next() {
            words.add(chunk.remove(0));
            if (chunk.isEmpty()) {
                loadNextChunk();
            }
            return hashFunc.apply(words.toString().getBytes());
        }

        private void loadNextChunk() {
            while (shingleable.hasNext() && chunk.isEmpty()) {
                StringTokenizer st = new StringTokenizer(shingleable.next(), config.getDelimiters());
                while (st.hasMoreTokens()) {
                    String word = config.normalize(st.nextToken());
                    if (!config.getSkipWords().contains(word)) {
                        chunk.add(word);
                    }
                }
            }
        }
    }
}
