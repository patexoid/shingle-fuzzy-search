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

    private static final int HEADER_SIZE = 8;

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
            int bufferSize = (int) Math.min(1024L * 1024, Math.max(1, (long) size * byteArrayLength));
            byte[] buffer = new byte[bufferSize];
            int bufferReadOff = 0;
            int shinglesRead = 0;
            while (true) {
                int readBytesCount = is.read(buffer, bufferReadOff, buffer.length - bufferReadOff);
                if (readBytesCount == -1) {
                    if (bufferReadOff != 0) {
                        log.warn("Warning broken cache for key: {}, truncated mid-shingle", key);
                        return Optional.empty();
                    }
                    break;
                }
                readBytesCount += bufferReadOff;
                bufferReadOff = 0;
                int position = 0;
                while (readBytesCount - position >= byteArrayLength) {
                    byte[] shingle = new byte[byteArrayLength];
                    System.arraycopy(buffer, position, shingle, 0, byteArrayLength);
                    position += byteArrayLength;
                    set.add(shingle);
                    shinglesRead++;
                }
                if (position < readBytesCount) {
                    bufferReadOff = readBytesCount - position;
                    System.arraycopy(buffer, position, buffer, 0, bufferReadOff);
                }
            }
            if (shinglesRead != size) {
                log.warn("Warning broken cache for key: {}, expected {} shingles but read {}", key, size, shinglesRead);
                return Optional.empty();
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
        int capacity = HEADER_SIZE + shingler.size() * shingler.getByteArraySize();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(capacity);
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
