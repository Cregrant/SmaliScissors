package com.github.cregrant.smaliscissors.manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.zip.ZipFile;

public class BinaryParser {

    private static final Logger logger = LoggerFactory.getLogger(BinaryParser.class);
    private static final int END_DOC_TAG = 0x00100101;
    private static final int START_TAG = 0x00100102;
    private static final int END_TAG = 0x00100103;
    private final byte[] xml;

    public BinaryParser(String apkPath) {
        this.xml = readFile(apkPath);
    }

    public HashSet<String> getStrings() {
        int numbStrings = LEW(xml, 4 * 4);
        int sitOff = 0x24; // Offset of start of StringIndexTable
        int stOff = sitOff + numbStrings * 4; // StringTable follows
        int xmlTagOff = LEW(xml, 3 * 4); // Start from the offset in the 3rd
        for (int ii = xmlTagOff; ii < xml.length - 4; ii += 4) {
            if (LEW(xml, ii) == START_TAG) {
                xmlTagOff = ii;
                break;
            }
        }

        int off = xmlTagOff;
        HashSet<String> classes = new HashSet<>();
        try {
            while (off < xml.length - 3) {
                int tag0 = LEW(xml, off);

                if (tag0 == 0) {
                    classes.clear();    //parse error, should parse binary string pool instead
                    break;
                }

                if (tag0 == START_TAG) { // XML START TAG
                    int nameSi = LEW(xml, off + 5 * 4);
                    int numbAttrs = LEW(xml, off + 7 * 4); // Number of Attributes
                    off += 9 * 4; // Skip over 6+3 words of startTag data
                    String name = compXmlString(xml, sitOff, stOff, nameSi);

                    for (int ii = 0; ii < numbAttrs; ii++) {
                        int attrNameSi = LEW(xml, off + 4); // AttrName String
                        int attrValueSi = LEW(xml, off + 8); // AttrValue Str
                        off += 5 * 4; // Skip over the 5 words of an attribute
                        String attrName = compXmlString(xml, sitOff, stOff, attrNameSi);

                        if (name == null || attrName == null || !attrName.equals("name") || attrValueSi == -1) {
                            continue;
                        }

                        if (name.equals("activity") || name.equals("provider") || name.equals("service") ||
                                name.equals("receiver") || name.equals("meta-data")) {
                            String tmp = compXmlString(xml, sitOff, stOff, attrValueSi);
                            if (tmp != null) {
                                classes.add(tmp.replace('.', '/'));
                            }
                        }

                    }

                } else if (tag0 == END_TAG) {
                    off += 6 * 4; // Skip over 6 words of endTag data
                } else if (tag0 == END_DOC_TAG) {
                    break;
                } else {
                    off++;
                }
            }
        } catch (Exception ignored) {
        }
        if (classes.isEmpty()) {
            BinaryStringPoolParser parser = new BinaryStringPoolParser();
            classes = parser.parseXmlStrings(xml);
        }
        return classes;
    }

    private String compXmlString(byte[] xml, int sitOff, int stOff, int strInd) {
        if (strInd < 0) {
            return null;
        }
        int strOff = stOff + LEW(xml, sitOff + strInd * 4);
        return compXmlStringAt(xml, strOff);
    }

    private String compXmlStringAt(byte[] arr, int strOff) {
        int strLen = arr[strOff + 1] << 8 & 0xff00 | arr[strOff] & 0xff;
        byte[] chars = new byte[strLen];
        for (int ii = 0; ii < strLen; ii++) {
            chars[ii] = arr[strOff + 2 + ii * 2];
        }
        return new String(chars, StandardCharsets.UTF_8);
    }

    // LEW -- Return value of a Little Endian 32 bit word from the byte array at offset off.
    private int LEW(byte[] arr, int off) {
        return arr[off + 3] << 24 & 0xff000000 | arr[off + 2] << 16 & 0xff0000 | arr[off + 1] << 8 & 0xff00 | arr[off] & 0xFF;
    }

    private byte[] readFile(String apkPath) {
        byte[] buf;
        try (ZipFile zip = new ZipFile(apkPath)) {
            InputStream is = zip.getInputStream(zip.getEntry("AndroidManifest.xml"));
            buf = new byte[is.available()];
            is.read(buf);
        } catch (IOException e) {
            throw new IllegalStateException("Error loading AndroidManifest.xml: " + e.getMessage());
        }
        return buf;
    }
}