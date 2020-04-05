package com.patex.shingle;


import java.io.InputStream;

/**
 * Storage to keep byte {@link Shingleable} representation outside, can use anything like DB, files etc.
 * @param <T> key
 */
public interface ShingleCacheStorage<T> {

    InputStream load(T t);

    void save(byte[] bytes, T t);


}
