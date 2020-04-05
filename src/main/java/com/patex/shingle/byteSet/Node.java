package com.patex.shingle.byteSet;

interface Node {

    Node getNext();

    byte[] toBytes();

    boolean isEqualsArray(byte[] key);
}
