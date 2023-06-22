package com.github.cregrant.smaliscissors.functional.Utils;

import com.github.cregrant.smaliscissors.functional.Utils.archivers.TarXzArchiver;
import com.github.cregrant.smaliscissors.functional.Utils.directories.PatchDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TestPatch {

    public static final String PATCHED_SOURCES_FILENAME = "source_patched.tar.xz";
    private final String patchName;
    private final File patchedSourcesArchive;
    private final PatchDirectory patchDirectory;

    TestPatch(File patchDir) {
        this.patchDirectory = new PatchDirectory(patchDir);
        this.patchName = patchDir.getName();
        this.patchedSourcesArchive = new File(patchDir, PATCHED_SOURCES_FILENAME);

        if (patchDirectory.getPatchFile() == null && patchDirectory.getRemoveStrings().isEmpty()) {
            throw new IllegalArgumentException("Not zip patch nor txt file not found inside " + patchDir);
        }
    }

    public ConcurrentHashMap<String, String> getPatchedSources() {
        if (!patchedSourcesArchive.exists()) {
            throw new IllegalStateException(patchedSourcesArchive + " is not exists");
        }
        try {
            return TarXzArchiver.previewTarXz(Files.readAllBytes(patchedSourcesArchive.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void compress() {
        patchDirectory.compressIfNeeded(patchedSourcesArchive.getPath());
    }

    public PatchDirectory getPatchDirectory() {
        return patchDirectory;
    }

    public File getPatchedSourcesArchive() {
        return patchedSourcesArchive;
    }

    public File getPatchFile() {
        return patchDirectory.getPatchFile();
    }

    public List<String> getRemoveStrings() {
        return patchDirectory.getRemoveStrings();
    }

    @Override
    public String toString() {
        return patchName;
    }
}
