package com.patex.shingle;

import java.io.Closeable;
import java.util.Iterator;

/**
 * All objects should implement or be transformed to this
 *  Iterator can return whole text on first request or
 *  text parts one by one(can be text lines with several words splitted by spaces and punctuations), it was done to save memory
 */
public interface Shingleable extends Iterator<String>, Closeable {

    /**
     * Approximate object size, measure unit should be same for same object types it's used to determine bigger object
     * @return approximate object size
     */
    int size();
}
