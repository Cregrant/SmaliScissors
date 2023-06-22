package com.github.cregrant.smaliscissors.functional.Utils.archivers;

import com.github.cregrant.smaliscissors.functional.Utils.BackgroundTasks;
import com.github.cregrant.smaliscissors.functional.Utils.Concurrent;
import com.github.cregrant.smaliscissors.util.IO;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;

public class TarArchiver {

    public static byte[] createTar(File rootFolder, File file) {
        return createTar(rootFolder, Collections.singletonList(file));
    }

    public static byte[] createTar(File rootFolder, Collection<File> files) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream(200000000);
             TarArchiveOutputStream outputStream = new TarArchiveOutputStream(byteStream)) {

            outputStream.setAddPaxHeadersForNonAsciiNames(true);
            outputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            files.forEach(parent -> addFiles(rootFolder, parent, outputStream));
            outputStream.finish();
            return byteStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void extractTar(byte[] tarBytes, File path) {
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(tarBytes);
             ArchiveInputStream tarStream = new TarArchiveInputStream(byteStream)) {
            ArchiveEntry entry;
            while ((entry = tarStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                File f = new File(path, entry.getName());
                f.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(f);
                     BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                    IOUtils.copy(tarStream, bos);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void addFiles(File rootFolder, File initialFile, TarArchiveOutputStream outputStream) {
        Queue<File> queue = new ArrayDeque<>(30000);
        queue.add(initialFile);
        BackgroundTasks tasks = new BackgroundTasks(Concurrent.WORKER);
        while (!queue.isEmpty()) {
            File currentFile = queue.poll();
            if (currentFile.isDirectory()) {
                File[] files = currentFile.listFiles();
                if (files == null) {
                    continue;
                }

                for (File subFile : files) {
                    if (subFile.isDirectory()) {
                        queue.add(subFile);
                    } else {
                        tasks.submitTask(() -> addFileToStream(rootFolder, subFile, outputStream));
                    }
                }
            } else {
                tasks.submitTask(() -> addFileToStream(rootFolder, currentFile, outputStream));
            }
        }
        tasks.waitAndClear();
    }

    private static void addFileToStream(File rootFolder, File file, TarArchiveOutputStream outputStream) {
        byte[] content = IO.readBytes(file.getPath());
        String shortPath = rootFolder.toPath().relativize(file.toPath()).toString();
        try {
            TarArchiveEntry entry = new TarArchiveEntry(shortPath);
            entry.setSize(file.length());
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (outputStream) {
                outputStream.putArchiveEntry(entry);
                outputStream.write(content);
                outputStream.closeArchiveEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
