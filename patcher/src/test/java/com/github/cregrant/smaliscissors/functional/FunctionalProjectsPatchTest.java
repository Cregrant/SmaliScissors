package com.github.cregrant.smaliscissors.functional;

import com.github.cregrant.smaliscissors.functional.Utils.TestProjectsManager;
import com.github.cregrant.smaliscissors.removecode.SmaliWorker;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.stream.Stream;

class FunctionalProjectsPatchTest {

    public static File getTestDir() throws FileNotFoundException {
        File testDir = new File(getHomeDir(), "test");
        if (!testDir.exists()) {
            throw new FileNotFoundException(testDir + " is not found");
        }
        return testDir;
    }

    private static File getHomeDir() {
        File userDir = new File(System.getProperty("user.dir"));
        File gradlew = new File(userDir, "gradlew");
        while (!gradlew.exists()) {
            File previousFolder = gradlew.getParentFile().getParentFile();
            if (previousFolder == null) {
                throw new IllegalStateException("Unknown home directory: " + userDir);
            }
            gradlew = new File(previousFolder, "gradlew");
        }
        return gradlew.getParentFile();
    }

    @TestFactory
    Stream<DynamicTest> patchTestProjects() throws Exception {
        SmaliWorker.DEBUG_BENCHMARK = false;
        SmaliWorker.DEBUG_NOT_WRITE = false;

        TestProjectsManager manager = new TestProjectsManager(getTestDir());
        manager.compressTestSuites();

        return manager.getTestSuites()
                .stream()
                .map(suite -> DynamicTest.dynamicTest(suite.toString(), () -> manager.runSuite(suite)));
    }
}