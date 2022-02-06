package com.github.cregrant.smaliscissors;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class IO {

    public static String read(String path) {
        final FileInputStream is;
        String resultString = null;
        try {
            is = new FileInputStream(path);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            resultString = new String(buffer, StandardCharsets.UTF_8);
            is.close();
        } catch (FileNotFoundException e) {
            Main.out.println(path + ':' + " not found.");
            return "";
        } catch (IOException e) {
            e.printStackTrace();
            Main.out.println("Exiting to prevent file corruption");
            System.exit(1);
        }
        return resultString;
    }

    public static void write(String path, String content) {
        delete(new File(path));
        try {
            BufferedOutputStream bof = new BufferedOutputStream(new FileOutputStream(path));
            bof.write(content.getBytes(StandardCharsets.UTF_8));
            bof.flush();
            bof.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copy(String src, String dst) {
        src = src.trim();
        dst = dst.trim();
        File dstFile = new File(dst);
        File srcFile = new File(src);
        boolean srcIsFolder = srcFile.isDirectory();
        boolean dstIsFolder = dstFile.isDirectory();
        if (!srcIsFolder && dstIsFolder)    //someone is trying to copy file to name of folder bruh
            dst = dst + '/' + Regex.getFilename(src);    //append file name
        if (dstFile.exists())
            dstFile.delete();
        File dstFolder = dstFile.getParentFile();
        dstFolder.mkdirs();

        try (FileInputStream is = new FileInputStream(src);
             FileOutputStream os = new FileOutputStream(dst)) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1)
                os.write(buffer, 0, len);

        } catch (IOException e) {
            e.printStackTrace();
            Main.out.println("Error during copying file...\nFrom " + src + "\nTo " + dst);
        }
    }


    public static void copyOld(String src, String dst) {
        src = src.trim();
        dst = dst.trim();
        File dstFile = new File(dst);
        File srcFile = new File(src);
        boolean srcIsFolder = srcFile.isDirectory();
        boolean dstIsFolder = dstFile.isDirectory();
        if (!srcIsFolder && dstIsFolder)    //someone is trying to copy file to name of folder bruh
            dst = dst + '/' + Regex.getFilename(src);    //append file name
        if (dstFile.exists())
            dstFile.delete();
        File dstFolder = dstFile.getParentFile();
        dstFolder.mkdirs();

        try (FileInputStream is = new FileInputStream(src);
             FileOutputStream os = new FileOutputStream(dst)) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            os.write(buffer);
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
            Main.out.println("Error during copying file...\nFrom " + src + "\nTo " + dst);
        }
    }

    public static void delete(File file) {
        if (file.isDirectory()) {
            if (Objects.requireNonNull(file.list()).length == 0) {
                file.delete();
            } else {
                for (File child : Objects.requireNonNull(file.listFiles())) {
                    delete(child);
                }
                if (Objects.requireNonNull(file.list()).length == 0)
                    file.delete();
            }
        } else if (file.exists())
            file.delete();
    }
}