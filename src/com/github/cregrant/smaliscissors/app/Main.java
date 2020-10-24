package com.github.cregrant.smaliscissors.app;

import com.github.cregrant.smaliscissors.misc.CompatibilityData;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import static java.lang.System.out;

public class Main {
    static final double version = 0.01;

    public static void main(String[] args) {
        new Prefs().loadConf();
        ArrayList<String> projectsList = new ArrayList<>();
        String mainDir = new CompatibilityData().getHomeDir();
        File mainDirFile = new File(mainDir);
        if (!mainDirFile.isDirectory()) {
            out.println("Error loading projects folder");
            System.exit(1);
        }
        if (Prefs.arch_device.equals("android")) {
            projectsList.add(mainDir);
        }
        if (projectsList.isEmpty()) {
            for (String MainDirFolder : Objects.requireNonNull(mainDirFile.list())) {
                File f = new File(mainDir + File.separator + MainDirFolder);
                if (!f.isDirectory()) continue;
                projectsList.add(MainDirFolder);
            }
        }
        String msg = "\nSelect project. Enter = all. X - cancel. Example: 0 or 0 1 2 (means 0 and 1 and 2).";
        ArrayList<String> projectsToPatch = new Select().select(projectsList, msg, "No decompiled projects found");
        String patchResult;
        long startTimeTotal = System.currentTimeMillis();
        while (true) {
            for (String currentProjectPath : projectsToPatch) {
                if (currentProjectPath.equals("cancel")) break;
                patchResult = Prefs.arch_device.equals("pc") ? new ApplyPatch().doPatch(mainDir + File.separator + currentProjectPath) : new ApplyPatch().doPatch(mainDir);
                if (patchResult.equals("error")) {
                    new IO().deleteAll(new File(new CompatibilityData().getPatchesDir() + File.separator + "temp"));
                    out.println("ApplyPatch error occurred");
                }
                if (!patchResult.equals("cancel")) continue;
                projectsToPatch.set(0, "cancel");
            }
            if (projectsToPatch.get(0).equals("cancel")) break;
            projectsToPatch = new Select().select(projectsList, msg, "No decompiled projects found");
        }
        out.println("All done in " + (System.currentTimeMillis() - startTimeTotal) + " ms");
        new Prefs().saveConf();
        out.println("Good bye Sir.");
    }
}