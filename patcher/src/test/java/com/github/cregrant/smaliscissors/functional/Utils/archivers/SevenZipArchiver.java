package com.github.cregrant.smaliscissors.functional.Utils.archivers;

import com.github.cregrant.smaliscissors.functional.Utils.BackgroundTasks;
import com.github.cregrant.smaliscissors.functional.Utils.Concurrent;
import com.github.cregrant.smaliscissors.util.IO;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class SevenZipArchiver {

    public static void createSevenZip(File rootFolder, File file, File outputFile) {
        createSevenZip(rootFolder, Collections.singletonList(file), outputFile);
    }

    public static void compressAndDelete(File rootFolder, Collection<File> files, File outputFile) throws Exception {
        createSevenZip(rootFolder, files, outputFile);
        for (File file : files) {
            IO.delete(file);
        }
    }

    public static void createSevenZip(File rootFolder, Collection<File> files, File outputFile) {
        Concurrent.compressSemaphore.acquireUninterruptibly();
        try (SevenZOutputFile sevenZOutputFile = new SevenZOutputFile(outputFile)) {
            for (File file : files) {
                String shortPath = rootFolder.toPath().relativize(file.toPath()).toString();
                SevenZArchiveEntry entry = sevenZOutputFile.createArchiveEntry(file, shortPath);
                sevenZOutputFile.putArchiveEntry(entry);
                sevenZOutputFile.closeArchiveEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Concurrent.compressSemaphore.release();
    }

    public static void extractSevenZip(byte[] bytes, File path) {
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
             XZCompressorInputStream xzi = new XZCompressorInputStream(byteStream);
             ArchiveInputStream tarStream = new TarArchiveInputStream(xzi)) {

            BackgroundTasks tasks = new BackgroundTasks(Concurrent.WORKER);
            TarArchiver.extractTar(IO.readBytes("C:\\JAVA_projects\\SmaliScissors\\test\\Basic\\test_add_file\\source_patched.tar"), new File("C:\\JAVA_projects\\SmaliScissors\\test\\Basic\\test_add_file\\"));
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

    public static ConcurrentHashMap<String, String> previewTarXz(byte[] bytes) {
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
             XZCompressorInputStream xzi = new XZCompressorInputStream(byteStream);
             ArchiveInputStream tarStream = new TarArchiveInputStream(xzi)) {
            ArchiveEntry entry;

            ConcurrentHashMap<String, String> result = new ConcurrentHashMap<>(10000);
            BackgroundTasks tasks = new BackgroundTasks(Concurrent.WORKER);
            while ((entry = tarStream.getNextEntry()) != null) {
                String path = entry.getName();
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
