package com.github.cregrant.smaliscissors.functional.Utils.archivers;

import com.github.cregrant.smaliscissors.functional.Utils.BackgroundTasks;
import com.github.cregrant.smaliscissors.functional.Utils.Concurrent;
import com.github.cregrant.smaliscissors.util.IO;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class TarXzArchiver {

    public static byte[] createTarXz(File rootFolder, File file) {
        return createTarXz(rootFolder, Collections.singletonList(file));
    }

    public static byte[] createTarXz(File rootFolder, Collection<File> files) {
        Concurrent.compressSemaphore.acquireUninterruptibly();
        byte[] tarBytes = TarArchiver.createTar(rootFolder, files);
        byte[] tarXzBytes = createTarXz(tarBytes);
        Concurrent.compressSemaphore.release();
        return tarXzBytes;
    }

    public static void compressAndDelete(File rootFolder, Collection<File> files, String outputPath) throws Exception {
        IO.writeBytes(outputPath, createTarXz(rootFolder, files));
        for (File file : files) {
            IO.delete(file);
        }
    }

    public static byte[] createTarXz(byte[] bytes) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream(bytes.length);
             XZCompressorOutputStream xzo = new XZCompressorOutputStream(byteStream, 5)) {
            xzo.write(bytes);
            xzo.finish();
            return byteStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void extractTarXz2(byte[] bytes, File path) {
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
             XZCompressorInputStream xzi = new XZCompressorInputStream(byteStream);
             TarArchiveInputStream tarStream = new TarArchiveInputStream(xzi)) {

            BackgroundTasks tasks = new BackgroundTasks(Concurrent.WORKER);
            ArchiveEntry entry;
            while ((entry = tarStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                File file = new File(path, entry.getName());
                byte[] entryBytes = new byte[tarStream.available()];
                tarStream.read(entryBytes);

                Runnable r = () -> {
                    file.getParentFile().mkdirs();
                    IO.writeBytes(file.getPath(), entryBytes);
                };
                tasks.submitTask(r);
            }
            tasks.waitAndClear();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] extractXz(File archivePath) {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(archivePath), 1000000);
             XZCompressorInputStream xzi = new XZCompressorInputStream(bis)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1000000];
            int n;
            while ((n = xzi.read(buffer)) != -1) {
                bos.write(buffer, 0, n);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void extractTarXz(File archivePath, File outputDirectory) {
        byte[] tarBytes = extractXz(archivePath);
        TarArchiver.extractTar(tarBytes, outputDirectory);
    }

    public static ConcurrentHashMap<String, String> previewTarXz(byte[] bytes) {
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
             XZCompressorInputStream xzi = new XZCompressorInputStream(byteStream);
             ArchiveInputStream tarStream = new TarArchiveInputStream(xzi)) {
            ArchiveEntry entry;

            ConcurrentHashMap<String, String> result = new ConcurrentHashMap<>(10000);
            BackgroundTasks tasks = new BackgroundTasks(Concurrent.WORKER);
            while ((entry = tarStream.getNextEntry()) != null) {
                String path = entry.getName().replace('\\', '/');
                byte[] entryBytes = new byte[tarStream.available()];
                tarStream.read(entryBytes);

                Runnable r = () -> {
                    String content = new String(entryBytes, StandardCharsets.UTF_8);
                    result.put(path, content);
                };
                tasks.submitTask(r);
            }
            tasks.waitAndClear();
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
