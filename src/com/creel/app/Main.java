package com.creel.app;

import com.creel.misc.CompatibilityData;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class Main {
    static final double version = 0.01;

    public static void main(String[] args) {
        new Prefs().loadConf();
        ArrayList<String> projectsList = new ArrayList<>();
        String mainDir = new CompatibilityData().getHomeDir();
        File mainDirFile = new File(mainDir);
        if (!mainDirFile.isDirectory()) {
            System.out.println("Error loading projects folder");
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
        ArrayList<String> projectsToPatch = new Select().select(projectsList, msg);
        String patchResult = null;
        long startTimeTotal = System.currentTimeMillis();
        while (!projectsToPatch.get(0).equals("cancel")) {
            for (String currentProjectPath : projectsToPatch) {
                patchResult = Prefs.arch_device.equals("pc") ? Regex.doPatch(mainDir + File.separator + currentProjectPath) : Regex.doPatch(mainDir);
                if (patchResult.equals("error")) {
                    System.out.println("Some error occurred?");
                    continue;
                }
                if (!patchResult.equals("cancel")) continue;
                projectsToPatch.set(0, "cancel");
            }
            if (patchResult.equals("cancel")) continue;
            projectsToPatch = new Select().select(projectsList, msg);
        }
        System.out.println("All done in " + (System.currentTimeMillis() - startTimeTotal) + " ms");
        new Prefs().saveConf();
        System.out.println("Good bye Sir.");
    }
}