package com.github.cregrant.smaliscissors.app;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class Main {
    static final double version = 0.01;

    public static void main(String[] args) {
        long startTimeTotal = System.currentTimeMillis();
        ArrayList<String> zipArr = new ArrayList<>();
        ArrayList<String> projectsList = new ArrayList<>();
        if (args.length==1 && (args[0].equalsIgnoreCase("help") || args[0].contains("?")))
            OutStream.println("Usage as module: add String(s) with full path to project and String(s) with full path to zip patches\n" +
                    "Append keepSmaliFilesInRAM or keepXmlFilesInRAM if you want to keep these files in RAM.\n" +
                    "Example ...Main.main(sdcard/ApkEditor/decoded, sdcard/ApkEditor/patches/patch.zip, keepSmaliFilesInRAM");
        if (args.length!=0) {
            for (String str : args) {
                if (str.endsWith(".zip")) zipArr.add(str);
                else if (str.equalsIgnoreCase("keepSmaliFilesInRAM")) Prefs.keepSmaliFilesInRAM = true;
                else if (str.equalsIgnoreCase("keepXmlFilesInRAM")) Prefs.keepXmlFilesInRAM = true;
                else projectsList.add(str);
            }
            Prefs.run_type = "module";
            Prefs.patchesDir = new File(zipArr.get(0)).getParentFile();
            Prefs.tempDir = new File(Prefs.patchesDir + File.separator + "temp");
            runAsModule(projectsList, zipArr);
        }
        else {
            Prefs.run_type = "pc";
            Prefs.patchesDir = new File(System.getProperty("user.dir") + File.separator + "patches");
            File projectsHome = new File("C:\\BAT\\_INPUT_APK");
            Prefs.tempDir = new File(Prefs.patchesDir + File.separator + "temp");
            runOnPC(projectsHome);
        }
        OutStream.println("All done in " + (System.currentTimeMillis() - startTimeTotal) + " ms");
        OutStream.println("Good bye Sir.");
    }

    static void runOnPC(File projectsHome) {
        ArrayList<String> projectsList = new ArrayList<>();
        new Prefs().loadConf();
        if (!projectsHome.isDirectory()) {
            OutStream.println("Error loading projects folder\n" + projectsHome);
            System.exit(1);
        }

        File[] projectsArr = null;
        try {
            projectsArr = projectsHome.listFiles();
        } catch (NullPointerException e) {
            OutStream.println("Projects folder is empty\n" + projectsHome);
            System.exit(1);
        }
        for (File project : Objects.requireNonNull(projectsArr)) {
            if (project.isDirectory())
                projectsList.add(project.toString());
        }
        String msg = "\nSelect project. Enter = all. X - cancel. Example: 0 or 0 1 2 (means 0 and 1 and 2).";
        ArrayList<String> projectsToPatch = new Select().select(projectsList, msg, "No decompiled projects found");

        String patchResult;
        while (true) {
            for (String currentProjectPath : projectsToPatch) {
                Prefs.projectPath = currentProjectPath;
                if (Prefs.projectPath.equals("cancel")) break;
                patchResult = new ApplyPatch().doPatch(new ArrayList<>());
                if (patchResult.equals("error")) {
                    new IO().deleteAll(Prefs.tempDir);
                    OutStream.println("ApplyPatch error occurred");
                }
                if (patchResult.equals("cancel"))
                    projectsToPatch.set(0, "cancel");
            }
            if (projectsToPatch.get(0).equals("cancel")) break;
            projectsToPatch = new Select().select(projectsList, msg, "No decompiled projects found");
        }
        new Prefs().saveConf();
    }

    static void runAsModule(ArrayList<String> projectsList, ArrayList<String> zipArr) {
        if (projectsList.isEmpty() || zipArr.isEmpty()) {
            OutStream.println("Empty project or patch list");
            throw new IndexOutOfBoundsException();
        }

        String patchResult;
        for (String currentProjectPath : projectsList) {
            Prefs.projectPath = currentProjectPath;
            patchResult = new ApplyPatch().doPatch(zipArr);
            if (patchResult.equals("error")) {
                new IO().deleteAll(Prefs.tempDir);
                OutStream.println("ApplyPatch error occurred");
            }
        }
    }
}