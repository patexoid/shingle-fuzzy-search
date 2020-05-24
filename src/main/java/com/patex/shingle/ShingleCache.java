package com.patex.shingle;

import java.io.IOException;
import java.util.Optional;

public interface ShingleCache<KEY> {
    Optional<Shingler> get(KEY key) throws IOException;

    void put(KEY key, Shingler shingler) throws IOException;
}
