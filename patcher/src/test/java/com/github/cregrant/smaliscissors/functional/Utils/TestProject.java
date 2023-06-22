package com.github.cregrant.smaliscissors.functional.Utils;

import com.github.cregrant.smaliscissors.Patcher;
import com.github.cregrant.smaliscissors.common.outer.PatcherTask;
import com.github.cregrant.smaliscissors.functional.Utils.archivers.TarXzArchiver;
import com.github.cregrant.smaliscissors.functional.Utils.directories.SourceDirectory;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class TestProject {

    public static final String SOURCES_ARCHIVE_FILENAME = "source.tar.xz";
    private static final Logger logger = LoggerFactory.getLogger(TestProjectsManager.class);
    private final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private final ArrayList<File> deleteList = new ArrayList<>();
    private final ArrayList<TestPatch> patches = new ArrayList<>();
    private final String projectName;
    private final File sourcesArchive;
    private final SourceDirectory sourceDirectory;
    private final Object lock;      //same projects share the same lock
    private File extractedProjectDir;

    public TestProject(File baseDir, Object lock) {
        this.lock = lock;
        this.projectName = baseDir.getName();
        File sourceDir = new File(baseDir, "source");
        this.sourcesArchive = new File(sourceDir, SOURCES_ARCHIVE_FILENAME);
        this.sourceDirectory = new SourceDirectory(sourceDir);

        if (sourceDirectory.getApkFile() == null) {
            throw new IllegalArgumentException("Apk file not found inside " + sourceDir);
        }

        File[] patchDirs = baseDir.listFiles(file -> !file.getName().equals("source"));
        if (patchDirs == null || patchDirs.length == 0) {
            throw new IllegalArgumentException("Test patches not found inside " + sourceDir);
        }
        for (File patchDir : patchDirs) {
            if (patchDir.isFile()) {
                logger.warn("Unknown file inside a test project folder: " + patchDir.getPath());
                continue;
            }
            patches.add(new TestPatch(patchDir));
        }
    }

    public void run(TestPatch patch) throws Exception {
        temporaryFolder.create();
        extractedProjectDir = temporaryFolder.newFolder(projectName);
        extractSources(extractedProjectDir);
        runOnDirectory(extractedProjectDir, patch);
    }

    public void runOnDirectory(File projectDir, TestPatch patch) throws Exception {
        deleteList.add(new File(projectDir, "PatcherProperties.txt"));

        PatcherTask task = new PatcherTask(projectDir.getPath())
                .addApkPath(sourceDirectory.getApkFile().getPath());
        if (patch.getPatchFile() != null) {
            task.addPatchPath(patch.getPatchFile().getPath());
        } else {
            task.addSmaliPaths(patch.getRemoveStrings());
        }
        task.validate();

        Concurrent.runPatcherSemaphore.acquireUninterruptibly();
        new Patcher(null, null, Collections.singletonList(task)).run();
        Concurrent.runPatcherSemaphore.release();
    }

    public void cleanupFiles() {
        deleteList.sort(Comparator.comparingInt(file -> -file.getPath().length()));
        deleteList.forEach(File::delete);
    }

    public void deleteFiles() {
        temporaryFolder.delete();
    }

    public void check(TestPatch patch) throws Exception {
        //Map<path, content>
        //fixme make two maps and compare
        Map<String, String> sourceMap = patch.getPatchedSources();
        AtomicReference<Exception> exception = new AtomicReference<>();

        FileVisitor<Path> visitor = new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
                deleteList.add(filePath.toFile());
                try {
                    String shortPath = extractedProjectDir.toPath().relativize(filePath).toString().replace('\\', '/');
                    if (shortPath.equals("PatcherProperties.txt")) {
                        return FileVisitResult.CONTINUE;
                    }
                    compareFiles(sourceMap.get(shortPath), filePath.toFile());
                } catch (Exception e) {
                    exception.set(e);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(extractedProjectDir.toPath(), visitor);
        if (exception.get() != null) {
            throw exception.get();
        }
    }

    private void compareFiles(String targetBody, File file) throws IOException {
        if (!file.exists()) {
            throw new IOException(file + " is missing.");
        } else if (targetBody == null) {
            throw new IOException(file + " is unexpected.");
        }

        String receivedBody = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        if (!targetBody.equals(receivedBody)) {
            throw new IOException(file + " content mismatch.\n\n" +
                    "TARGET--------------------------\n" + targetBody +
                    "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" +
                    "RECEIVED------------------------\n" + receivedBody);
        }
    }

    public void extractSources(File dstDir) {
        TarXzArchiver.extractTarXz(sourcesArchive, dstDir);
    }

    public synchronized void compress() {
        synchronized (lock) {
            sourceDirectory.compressIfNeeded(sourcesArchive.getPath());
        }
    }

    public SourceDirectory getSourceDirectory() {
        return sourceDirectory;
    }

    public File getSourcesArchive() {
        return sourcesArchive;
    }

    public ArrayList<TestPatch> getPatches() {
        return patches;
    }

    @Override
    public String toString() {
        return projectName;
    }
}
