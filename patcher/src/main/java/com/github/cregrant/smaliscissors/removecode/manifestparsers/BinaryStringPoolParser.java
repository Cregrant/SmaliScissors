package com.github.cregrant.smaliscissors.removecode.manifestparsers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;

/**
 * @author bilux (i.bilux@gmail.com)
 * <a href="https://github.com/ibilux/ApkBinaryDecode">...</a>
 */
public class BinaryStringPoolParser {
    private static final int RES_STRING_POOL_TYPE = 0x0001;

    public HashSet<String> parseXmlStrings(byte[] buf) {
        try {
            byte[] chunkType_buf2 = new byte[2];
            byte[] headerSize_buf2 = new byte[2];
            byte[] chunkSize_buf4 = new byte[4];
            ByteArrayInputStream in = new ByteArrayInputStream(buf);

            in.skip(8);
            in.read(chunkType_buf2);
            if (getShort(chunkType_buf2) == RES_STRING_POOL_TYPE) {
                in.read(headerSize_buf2);
                int headerSize = getShort(headerSize_buf2);

                in.read(chunkSize_buf4);
                int chunkSize = getInt(chunkSize_buf4);

                byte[] spBuf = new byte[chunkSize - 8];
                in.read(spBuf);

                return parseStringPool(spBuf, headerSize, chunkSize);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Binary AndroidManifest.xml has not been parsed. " +
                    "Suggestion: decompile with resources and try again.\n------------------");
        }
        return new HashSet<>();
    }

    private HashSet<String> parseStringPool(byte[] spBuf, int headerSize, int chunkSize) throws IOException {
        HashSet<String> stringPool = new HashSet<>();
        ByteArrayInputStream in = new ByteArrayInputStream(spBuf);

        // String pool header
        byte[] buf4 = new byte[4];
        in.read(buf4);

        int string_count = getInt(buf4);
        in.read(buf4);
        int style_count = getInt(buf4);
        in.skip(8);
        in.read(buf4);
        int style_start = getInt(buf4);

        // String pool data
        // Read index location of each String
        int[] string_indices = new int[string_count];
        if (string_count > 0) {
            for (int i = 0; i < string_count; i++) {
                in.read(buf4);
                string_indices[i] = getInt(buf4);
            }
        }

        if (style_count > 0) {
            // Skip Style
            in.skip(style_count * 4L);
        }

        // Read Strings
        for (int i = 0; i < string_count; i++) {
            int string_len;
            if (i == string_count - 1) {
                if (style_start == 0) {   // There is no Style span
                    // Length of the last string. Chunk Size - Start position of this String - Header - Len of Indices
                    string_len = chunkSize - string_indices[i] - headerSize - 4 * string_count;
                } else {
                    string_len = style_start - string_indices[i];
                }
            } else {
                string_len = string_indices[i + 1] - string_indices[i];
            }

            /*
             * Each String entry contains Length header (2 bytes to 4 bytes) + Actual String + [0x00]
             * Length header sometime contain duplicate values e.g. 20 20
             * Actual string sometime contains 00, which need to be ignored
             * Ending zero might be  2 byte or 4 byte
             */
            byte[] buf2 = new byte[2];
            in.read(buf2);
            int actual_str_len;
            if (buf2[0] == buf2[1]) {    // Its repeating, happens for Non-Manifest file. e.g. 20 20
                actual_str_len = buf2[0];
            } else {
                actual_str_len = getShort(buf2);
            }

            byte[] str_buf = new byte[actual_str_len];
            byte[] buf = new byte[string_len - 2]; // Skip 2 Length bytes, already read.
            in.read(buf);
            int j = 0;
            for (byte b : buf) {
                if (b != 0x00) {     // Skip 0x00
                    str_buf[j++] = b;
                }
            }
            String str = new String(str_buf);
            char firstChar = str.charAt(0);
            boolean isDigit = firstChar >= 48 && firstChar <= 57;
            //skip some non-classes strings
            if (!isDigit && !str.contains("/") && !str.contains("-") && str.contains(".")) {
                stringPool.add(str.replace('.', '/'));
            }
        }
        return stringPool;
    }

    public static short getShort(byte[] bytes) {
        return (short) (bytes[1] << 8 & 0xff00 | bytes[0] & 0xFF);
    }

    public static int getInt(byte[] bytes) {
        return bytes[3]
                << 24 & 0xff000000
                | bytes[2]
                << 16 & 0xff0000
                | bytes[1]
                << 8 & 0xff00
                | bytes[0] & 0xFF;
    }
}
