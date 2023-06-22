package com.github.cregrant.smaliscissors.functional.Utils;

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
        if (error) {
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

    private void loadTestSuites(File testFolder) {
        File[] projectDirs = testFolder.listFiles();
        if (projectDirs == null || projectDirs.length == 0) {
            throw new IllegalArgumentException("No test projects found");
        }

        testSuites = new ArrayList<>();
        for (File rootDir : projectDirs) {
            if (rootDir.isFile()) {
                logger.warn("Unknown file inside a tests folder: " + rootDir.getPath());
                continue;
            }
            try {
                Object projectLock = new Object();
                TestProject project = new TestProject(rootDir, projectLock);
                for (TestPatch patch : project.getPatches()) {
                    TestSuite testSuite = new TestSuite(project, patch);
                    testSuites.add(testSuite);
                }
            } catch (Exception e) {
                logger.error("Error loading " + rootDir + " test project");
                throw e;
            }
        }
    }

    public void compressTestSuites() {
        BackgroundTasks tasks = new BackgroundTasks(Concurrent.LONG_WORKER);
        for (TestSuite suite : testSuites) {
            Runnable r = () -> {
                try {
                    suite.compress();
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
