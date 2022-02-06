package com.github.cregrant.smaliscissors;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Gzip {
    public byte[] theCompressedArray;
    int originalLength;

    public Gzip(String s) {
        compress(s);
    }

    public void compress(String string) {
        byte[] input = string.getBytes(StandardCharsets.UTF_8);
        originalLength = input.length;
        byte[] output;
        if (originalLength > 200)
            output = new byte[input.length];
        else
            output = new byte[2 * input.length];
        Deflater compresser = new Deflater(3);
        compresser.setInput(input);
        compresser.finish();
        int compressedDataLength = compresser.deflate(output);
        theCompressedArray = Arrays.copyOf(output, compressedDataLength);
        compresser.end();
    }

    public String decompress() {
        Inflater decompresser = new Inflater();
        decompresser.setInput(theCompressedArray);
        byte[] result = new byte[originalLength];
        try {
            decompresser.inflate(result);
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
        decompresser.end();
        return new String(result, StandardCharsets.UTF_8);
    }
}