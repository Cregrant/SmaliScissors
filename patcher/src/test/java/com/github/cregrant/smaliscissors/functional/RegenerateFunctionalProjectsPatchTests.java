package com.github.cregrant.smaliscissors.functional;

import com.github.cregrant.smaliscissors.Flags;
import com.github.cregrant.smaliscissors.functional.Utils.TestProjectsManager;

import java.util.Scanner;

public class RegenerateFunctionalProjectsPatchTests {

    public static void main(String[] args) throws Exception {
        Flags.SMALI_DEBUG_BENCHMARK = false;
        Flags.SMALI_DEBUG_DO_NOT_WRITE = false;

        waitForConfirmation();
        TestProjectsManager manager = new TestProjectsManager(FunctionalProjectsPatchTest.getTestDir());
        manager.regenerateTestSuites();
        System.exit(0);
    }

    private static void waitForConfirmation() {
        System.out.println("WARNING! Any existing functional tests will be lost, " +
                "and the current project state will be used as a reference. Please type \"yes\" to continue:");
        Scanner br = new Scanner(System.in);
        while (true) {
            String inputString = br.nextLine();
            if (inputString == null || inputString.isEmpty()) {
                continue;
            }
            if (inputString.equals("yes")) {
                return;
            } else {
                System.out.println("yes?");
            }

        }
    }
}
