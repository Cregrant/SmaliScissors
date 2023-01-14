package com.github.cregrant.smaliscissors.removecode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Gzip {

    private static final Logger logger = LoggerFactory.getLogger(Gzip.class);
    private final int originalLength;
    private byte[] theCompressedArray;

    public Gzip(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        originalLength = bytes.length;
        compress(bytes, 1);
    }

    private void compress(byte[] bytes, int bufferMultiplier) {
        byte[] output = new byte[bufferMultiplier * originalLength + 10];
        Deflater deflater = new Deflater(1);
        deflater.setInput(bytes);
        deflater.finish();
        int compressedDataLength = deflater.deflate(output);
        if (compressedDataLength >= output.length) {        //increase the buffer to prevent small string truncation
            compress(bytes, bufferMultiplier + 1);
            return;
        }

        theCompressedArray = Arrays.copyOf(output, compressedDataLength);
        deflater.end();
    }

    public String decompress() {
        Inflater inflater = new Inflater();
        inflater.setInput(theCompressedArray);
        byte[] result = new byte[originalLength];
        try {
            inflater.inflate(result);
        } catch (DataFormatException e) {
            logger.error("Error decompressing gzip", e);
        }
        inflater.end();
        return new String(result, StandardCharsets.UTF_8);
    }

    public float calcCompressRatio() {
        return (float) theCompressedArray.length / originalLength;
    }
}