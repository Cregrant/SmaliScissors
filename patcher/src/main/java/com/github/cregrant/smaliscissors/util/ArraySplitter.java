package com.github.cregrant.smaliscissors.util;

public class ArraySplitter {

    private final int arrayLength;
    private final int chunkSize;
    private int currentPos;

    public ArraySplitter(Object[] array) {
        this(array, Runtime.getRuntime().availableProcessors());
    }

    public ArraySplitter(Object[] array, int partsNum) {
        arrayLength = array.length;
        chunkSize = arrayLength / partsNum + partsNum;
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
