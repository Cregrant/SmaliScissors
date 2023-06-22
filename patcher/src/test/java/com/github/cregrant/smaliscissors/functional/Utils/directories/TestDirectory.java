package com.github.cregrant.smaliscissors.functional.Utils.directories;

import com.github.cregrant.smaliscissors.functional.Utils.archivers.TarXzArchiver;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class TestDirectory {

    protected final File rootFolder;
    protected ArrayList<File> sources;
    protected File sourcesArchive;

    protected TestDirectory(File rootFolder) {
        this.rootFolder = rootFolder;
    }

    public abstract void rescan();

    protected File[] getFiles() {
        File[] files = rootFolder.listFiles();
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Invalid path: " + rootFolder.getPath());
        }
        return files;
    }

    public void compressIfNeeded(String archivePath) {
        rescan();
        if (sources.isEmpty()) {
            return;
        }
        if (sourcesArchive != null) {
            throw new IllegalArgumentException("Detected both uncompressed sources and compressed archive inside " +
                    rootFolder);
        }

        try {
            TarXzArchiver.compressAndDelete(rootFolder, sources, archivePath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        sources.clear();
        sourcesArchive = new File(archivePath);
    }

    public ArrayList<File> getSources() {
        return sources;
    }

    protected ArrayList<File> scanSources(File[] files) {
        ArrayList<File> result = new ArrayList<>(Arrays.asList(files));
        result.remove(sourcesArchive);
        return result;
    }

    protected File scanFile(File[] files, String extension) {
        for (File file : files) {
            if (file.getPath().endsWith(extension)) {
                return file;
            }
        }
        return null;
    }

    public File getSourcesArchive() {
        return sourcesArchive;
    }

    public File getRootFolder() {
        return rootFolder;
    }
}
