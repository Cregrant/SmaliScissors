package com.github.cregrant.smaliscissors.util;

public class ArraySplitter {

    private final int arrayLength;
    private final int chunkSize;
    private int currentPos;

    public ArraySplitter(Object[] array) {
        arrayLength = array.length;
        int cores = Runtime.getRuntime().availableProcessors();
        chunkSize = arrayLength / cores + cores;
    }

    public boolean hasNext() {
        return currentPos < arrayLength;
    }

    public int chunkStart() {
        return currentPos;
    }

    public int chunkEnd() {
        currentPos = Math.min(currentPos + chunkSize, arrayLength);
        return currentPos;
    }
}
