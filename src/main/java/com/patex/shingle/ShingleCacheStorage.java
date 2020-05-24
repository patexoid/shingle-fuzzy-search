package com.patex.shingle;


import java.io.InputStream;

/**
 * Storage to keep byte {@link Shingleable} representation outside, can use anything like DB, files etc.
 * @param <KEY> key
 */
public interface ShingleCacheStorage<KEY> {

    InputStream load(KEY key);

    void save(KEY key, byte[] bytes);


}
