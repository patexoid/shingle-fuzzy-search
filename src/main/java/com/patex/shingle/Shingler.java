package com.patex.shingle;

interface Shingler extends Iterable<byte[]>{

    int size();

    boolean contains(byte[] shingleHash);

    int getByteArraySize();
}
