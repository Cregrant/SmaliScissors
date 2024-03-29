package com.github.cregrant.smaliscissors.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class IO {

    private static final Logger logger = LoggerFactory.getLogger(IO.class);

    public static String read(String path) {
        String resultString;
        try (FileInputStream is = new FileInputStream(path)) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            resultString = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return resultString;
    }

    public static byte[] readBytes(String path) {
        try (FileInputStream is = new FileInputStream(path)) {
            byte[] content = new byte[is.available()];
            is.read(content);
            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void write(String path, String content) {
        File file = new File(path);
        file.delete();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeBytes(String path, byte[] content) {
        File file = new File(path);
        file.delete();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void copy(String src, String dst) {
        src = src.trim();
        dst = dst.trim();
        File dstFile = new File(dst);
        File srcFile = new File(src);
        if (!srcFile.isDirectory() && dstFile.isDirectory()) {    //someone is trying to copy file to name of folder
            dst = dst + '/' + srcFile.getName();    //append file name
        }
        if (dstFile.exists()) {
            dstFile.delete();
        }
        File dstFolder = dstFile.getParentFile();
        dstFolder.mkdirs();

        try (FileInputStream is = new FileInputStream(src);
             FileOutputStream os = new FileOutputStream(dst)) {

            byte[] buffer = new byte[16384];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }

        } catch (IOException e) {
            logger.error("Error during copying file...\nFrom " + src + "\nTo " + dst, e);
        }
    }

    public static void delete(File file) throws IOException {
        try {
            if (file.isDirectory()) {
                if (Objects.requireNonNull(file.list()).length == 0) {
                    file.delete();
                } else {
                    for (File child : Objects.requireNonNull(file.listFiles())) {
                        delete(child);
                    }
                    if (Objects.requireNonNull(file.list()).length == 0) {
                        file.delete();
                    }
                }
            } else if (file.exists()) {
                file.delete();
            }
        } catch (NullPointerException e) {
            throw new IOException("Deleting " + file + "caused IO error.");
        }
    }

    public static ArrayList<String> extract(File zipFile, String dstPath, String exactName) {
        ArrayList<String> extractedPathList = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                File filePath = mergePath(dstPath + File.separator, zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    filePath.mkdirs();
                } else {
                    if (zipEntry.getName().equals("patch.txt") || (exactName != null && !zipEntry.getName().equals(exactName))) {
                        continue;
                    }
                    filePath.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream(filePath);
                    byte[] buffer = new byte[16384];
                    int len;
                    while ((len = zis.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    extractedPathList.add(filePath.getPath());
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return extractedPathList;
    }

    private static File mergePath(String dstFolder, String toMerge) {
        String[] dstPath = dstFolder.replace('\\', '/').split("/");
        String[] newPath = toMerge.replace('\\', '/').split("/");
        String[] fullPath = Arrays.copyOf(dstPath, dstPath.length + newPath.length);
        System.arraycopy(newPath, 0, fullPath, dstPath.length, newPath.length);     //fullPath = dstPath + newPath

        StringBuilder sb = new StringBuilder(dstFolder.length() + toMerge.length());
        String prevStr = "";
        for (String str : fullPath) {
            if (str.equals(prevStr)) {
                continue;
            }
            sb.append(str).append(File.separator);
            prevStr = str;
        }
        return new File(sb.toString());
    }
}