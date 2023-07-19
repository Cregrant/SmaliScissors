package com.github.cregrant.smaliscissors.functional.Utils;

import com.github.cregrant.smaliscissors.util.IO;

import java.io.File;

public class TestSuite {

    private final TestProject project;
    private final TestPatch patch;

    public TestSuite(TestProject project, TestPatch patch) {
        this.project = project;
        this.patch = patch;
    }

    public void run() throws Exception {
        project.run(patch);
    }

    public void check() throws Exception {
        project.check(patch);
    }

    public void deleteFiles() {
        project.deleteFiles();
    }

    public void cleanupFiles() {
        project.cleanupFiles();
    }

    public void regenerate() throws Exception {
        patch.deletePatchedSourcesArchive();
        for (File file : patch.getPatchDirectory().getSources()) {
            IO.delete(file);
        }

        File patchRootFolder = patch.getPatchDirectory().getRootFolder();
        project.compress();
        project.extractSources(patchRootFolder);
        project.runOnDirectory(patchRootFolder, patch);
        project.cleanupFiles();
        patch.compress();
    }

    @Override
    public String toString() {
        return project + " + " + patch;
    }

    public void compressAll() {
        BackgroundTasks tasks = new BackgroundTasks(Concurrent.LONG_WORKER);
        tasks.submitTask(project::compress);
        tasks.submitTask(patch::compress);
        tasks.waitAndClear();
    }
}

