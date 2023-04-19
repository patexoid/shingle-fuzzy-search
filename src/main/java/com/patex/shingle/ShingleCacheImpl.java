package com.patex.shingle;

import com.patex.shingle.byteSet.ByteHashSet;
import com.patex.shingle.byteSet.ByteSetFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
class ShingleCacheImpl<KEY> implements ShingleCache<KEY> {

    private final ShingleCacheStorage<KEY> storage;

    @Override
    public Optional<Shingler> get(KEY key) throws IOException {
        try (InputStream is = storage.load(key)) {
            if (is == null) {
                return Optional.empty();
            }
            int size = readInt(is);
            int byteArrayLength = readInt(is);
            ByteHashSet set = ByteSetFactory.createByteSet(size, byteArrayLength);
            byte[] buffer = new byte[1024 * 1024];
            int bufferReadOff = 0;
            while (true) {
                int readBytesCount = is.read(buffer, bufferReadOff, buffer.length - bufferReadOff);
                if (readBytesCount == -1) {
                    if (bufferReadOff != 0) {
                        log.warn("Warning broken cache for key: {}", key);
                        return Optional.empty();
                    }
                    break;
                }
                readBytesCount += bufferReadOff;
                bufferReadOff = 0;
                int position = 0;
                while (readBytesCount - position >= byteArrayLength) {
                    byte[] shingle = new byte[byteArrayLength];
                    for (int i = 0; i < byteArrayLength; i++) {
                        shingle[i] = buffer[position++];
                    }
                    set.add(shingle);
                }
                if (position < readBytesCount) {
                    int i = 0;
                    for (; i < byteArrayLength && position < readBytesCount; i++) {
                        buffer[i] = buffer[position++];
                    }
                    bufferReadOff = i;
                }
            }
            LoadedShingler shingler = new LoadedShingler(set);
            return Optional.of(shingler);
        }
    }

    private int readInt(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4;
    }

    public void put(KEY key, Shingler shingler) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeInt(baos, shingler.size());
        writeInt(baos, shingler.getByteArraySize());
        for (byte[] bytes : shingler) {
            baos.write(bytes);
        }
        storage.save(key, baos.toByteArray());
    }

    private void writeInt(OutputStream out, int v) throws IOException {
        out.write((v >>> 24) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 8) & 0xFF);
        out.write(v & 0xFF);
    }
}
