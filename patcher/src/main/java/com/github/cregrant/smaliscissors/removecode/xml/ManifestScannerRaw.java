package com.github.cregrant.smaliscissors.removecode.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.zip.ZipFile;

public class ManifestScannerRaw {
    public static int endDocTag = 0x00100101;
    public static int startTag = 0x00100102;
    public static int endTag = 0x00100103;

    public static HashSet<String> decompressXML(byte[] xml) {
        int numbStrings = LEW(xml, 4 * 4);
        int sitOff = 0x24; // Offset of start of StringIndexTable
        int stOff = sitOff + numbStrings * 4; // StringTable follows
        int xmlTagOff = LEW(xml, 3 * 4); // Start from the offset in the 3rd
        for (int ii = xmlTagOff; ii < xml.length - 4; ii += 4) {
            if (LEW(xml, ii) == startTag) {
                xmlTagOff = ii;
                break;
            }
        }

        int off = xmlTagOff;
        HashSet<String> classes = new HashSet<>();
        try {
            while (off < xml.length) {
                int tag0 = LEW(xml, off);

                if (tag0 == startTag) { // XML START TAG
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

                } else if (tag0 == endTag) {
                    off += 6 * 4; // Skip over 6 words of endTag data
                } else if (tag0 == endDocTag) {
                    break;
                } else {
                    off++;
                }
            }
        } catch (Exception ignored) {
        }
        if (classes.isEmpty()) {
            throw new IllegalStateException("Binary AndroidManifest.xml has not been parsed. Suggestion: decompile with resources and try again.");
        }
        return classes;
    }

    public static String compXmlString(byte[] xml, int sitOff, int stOff, int strInd) {
        if (strInd < 0) {
            return null;
        }
        int strOff = stOff + LEW(xml, sitOff + strInd * 4);
        return compXmlStringAt(xml, strOff);
    }

    public static String compXmlStringAt(byte[] arr, int strOff) {
        int strLen = arr[strOff + 1] << 8 & 0xff00 | arr[strOff] & 0xff;
        byte[] chars = new byte[strLen];
        for (int ii = 0; ii < strLen; ii++) {
            chars[ii] = arr[strOff + 2 + ii * 2];
        }
        return new String(chars);
    }

    // LEW -- Return value of a Little Endian 32 bit word from the byte array at offset off.
    public static int LEW(byte[] arr, int off) {
        return arr[off + 3] << 24 & 0xff000000 | arr[off + 2] << 16 & 0xff0000 | arr[off + 1] << 8 & 0xff00 | arr[off] & 0xFF;
    }

    public static HashSet<String> parse(String apkPath) {
        byte[] buf;
        try (ZipFile zip = new ZipFile(apkPath)) {
            InputStream is = zip.getInputStream(zip.getEntry("AndroidManifest.xml"));
            buf = new byte[is.available()];
            is.read(buf);
        } catch (IOException e) {
            return null;
        }

        return ManifestScannerRaw.decompressXML(buf);
    }
}