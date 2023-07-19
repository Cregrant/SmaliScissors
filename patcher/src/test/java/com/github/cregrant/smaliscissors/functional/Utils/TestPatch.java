package com.github.cregrant.smaliscissors.functional.Utils;

import com.github.cregrant.smaliscissors.functional.Utils.archivers.TarXzArchiver;
import com.github.cregrant.smaliscissors.functional.Utils.directories.PatchDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TestPatch {

    private final String patchName;
    private final PatchDirectory patchDirectory;

    public TestPatch(File patchDir) {
        this.patchDirectory = new PatchDirectory(patchDir);
        this.patchName = patchDir.getName();
    }

    public ConcurrentHashMap<String, String> getPatchedSources() {
        File patchedSourcesArchive = patchDirectory.getSourcesArchive();
        if (!patchedSourcesArchive.exists()) {
            throw new IllegalStateException("Compressed sources is not detected:" + patchedSourcesArchive);
        }
        try {
            return TarXzArchiver.previewTarXz(Files.readAllBytes(patchedSourcesArchive.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void compress() {
        patchDirectory.compressIfNeeded(patchDirectory.getSourcesArchive().getPath());
    }

    public PatchDirectory getPatchDirectory() {
        return patchDirectory;
    }

    public void deletePatchedSourcesArchive() {
        patchDirectory.getSourcesArchive().delete();
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
