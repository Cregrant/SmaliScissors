package com.github.cregrant.smaliscissors.functional.Utils;

import com.github.cregrant.smaliscissors.Flags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;

public class TestProjectsManager {

    private static final Logger logger = LoggerFactory.getLogger(TestProjectsManager.class);
    private ArrayList<TestSuite> testSuites;
    private volatile boolean error;

    public TestProjectsManager(File testFolder) {
        loadTestSuites(testFolder);
    }

    public void runSuite(TestSuite suite) throws Exception {
        if (Flags.STOP_IF_TEST_FAILED && error) {
            throw new Exception("Skipped due to previous error");
        }

        try {
            suite.run();
        } catch (Exception e) {
            error = true;
            suite.deleteFiles();
            logger.error(suite + " execution exception:");
            throw e;
        }
        try {
            suite.check();
        } catch (Exception e) {
            error = true;
            suite.cleanupFiles();
            logger.error(suite + " generated wrong result.\n" + e);
            throw new InterruptedException();
        }
        logger.info(suite + " success.");
        suite.deleteFiles();
    }

    private static File[] getProjectDirs(File testFolder) {
        File[] projectDirs = testFolder.listFiles();
        if (projectDirs == null || projectDirs.length == 0) {
            throw new IllegalArgumentException("No test projects found");
        }
        return projectDirs;
    }

    private void loadTestSuites(File testFolder) {
        testSuites = new ArrayList<>();
        for (File rootDir : getProjectDirs(testFolder)) {
            if (rootDir.isFile()) {
                logger.warn("Unknown file inside a tests folder: " + rootDir.getPath());
                continue;
            }
            try {
                Object projectLock = new Object();
                for (TestPatch patch : getPatches(rootDir)) {
                    TestSuite testSuite = new TestSuite(new TestProject(rootDir, projectLock), patch);
                    testSuites.add(testSuite);
                }
            } catch (Exception e) {
                logger.error("Error loading " + rootDir + " test project");
                throw e;
            }
        }
    }

    public ArrayList<TestPatch> getPatches(File rootDir) {
        ArrayList<TestPatch> patches = new ArrayList<>();
        File[] patchDirs = rootDir.listFiles(file -> !file.getName().equals("source"));
        if (patchDirs == null || patchDirs.length == 0) {
            throw new IllegalArgumentException("Test patches not found inside " + rootDir);
        }
        for (File patchDir : patchDirs) {
            if (patchDir.isFile()) {
                logger.warn("Unknown file inside a test project folder: " + patchDir.getPath());
                continue;
            }
            patches.add(new TestPatch(patchDir));
        }
        return patches;
    }

    public void compressTestSuites() {
        BackgroundTasks tasks = new BackgroundTasks(Concurrent.LONG_WORKER);
        for (TestSuite suite : testSuites) {
            Runnable r = () -> {
                try {
                    suite.compressAll();
                } catch (Exception e) {
                    logger.error("Error compressing " + suite, e);
                }
            };
            tasks.submitTask(r);
        }
        tasks.waitAndClear();
    }

    public void regenerateTestSuites() {
        BackgroundTasks tasks = new BackgroundTasks(Concurrent.LONG_WORKER);
        for (TestSuite suite : testSuites) {
            Runnable r = () -> {
                try {
                    suite.regenerate();
                } catch (Exception e) {
                    logger.error("Error regenerating " + suite, e);
                }
            };
            tasks.submitTask(r);
        }
        tasks.waitAndClear();
    }

    public ArrayList<TestSuite> getTestSuites() {
        if (testSuites == null) {
            throw new IllegalStateException("Test projects loading was not called");
        }
        return testSuites;
    }

}
