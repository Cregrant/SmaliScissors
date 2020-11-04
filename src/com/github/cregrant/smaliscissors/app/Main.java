package com.github.cregrant.smaliscissors.app;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import static java.lang.System.out;

public class Main {
    static final double version = 0.01;

    public static void main(String[] args) {
        ArrayList<String> zipArr = new ArrayList<>();
        ArrayList<String> projectsList = new ArrayList<>();
        if (args.length==1 && (args[0].equalsIgnoreCase("help") || args[0].contains("?")))
                out.println("Usage as module: add String(s) with full path to project and String(s) with full path to zip patches\n" +
                        "Append keepSmaliFilesInRAM or keepXmlFilesInRAM if you want to keep these files in RAM.\n" +
                        "Example ...main(sdcard/ApkEditor/decoded, sdcard/ApkEditor/patches/patch.zip, keepSmaliFilesInRAM");
        if (args.length!=0) {
            for (String str : args) {
                if (str.endsWith(".zip")) zipArr.add(str);
                else if (str.equalsIgnoreCase("keepSmaliFilesInRAM")) Prefs.keepSmaliFilesInRAM = true;
                else if (str.equalsIgnoreCase("keepXmlFilesInRAM")) Prefs.keepXmlFilesInRAM = true;
                else projectsList.add(str);
            }
            Prefs.run_type = "module";
            Prefs.patchesDir = new File(zipArr.get(0)).getParentFile();
            Prefs.projectPath = projectsList.get(0);
            Prefs.tempDir = Prefs.patchesDir + File.separator + "temp";
            runAsModule(projectsList, zipArr);
        }
        else {
            Prefs.run_type = "pc";
            Prefs.patchesDir = new File(System.getProperty("user.dir") + File.separator + "patches");
            Prefs.projectPath = "C:\\BAT\\_INPUT_APK";
            Prefs.tempDir = Prefs.patchesDir + File.separator + "temp";
            runOnPC();
        }
    }

    static void runOnPC() {
        ArrayList<String> projectsList = new ArrayList<>();
        new Prefs().loadConf();
        File projectPathFile = new File(Prefs.projectPath);
        if (!projectPathFile.isDirectory()) {
            out.println("Error loading projects folder\n" + projectPathFile);
            System.exit(1);
        }
        for (String MainDirFolder : Objects.requireNonNull(projectPathFile.list())) {
            File f = new File(projectPathFile + File.separator + MainDirFolder);
            if (!f.isDirectory()) continue;
            projectsList.add(MainDirFolder);
        }
        String msg = "\nSelect project. Enter = all. X - cancel. Example: 0 or 0 1 2 (means 0 and 1 and 2).";
        ArrayList<String> projectsToPatch = new Select().select(projectsList, msg, "No decompiled projects found");

        String patchResult;
        long startTimeTotal = System.currentTimeMillis();
        while (true) {
            for (String currentProjectPath : projectsToPatch) {
                if (currentProjectPath.equals("cancel")) break;
                patchResult = new ApplyPatch().doPatch(projectPathFile + File.separator + currentProjectPath, new ArrayList<>());
                if (patchResult.equals("error")) {
                    new IO().deleteAll(new File(Prefs.patchesDir + File.separator + "temp"));
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

    static void runAsModule(ArrayList<String> projectsList, ArrayList<String> zipArr) {
        if (projectsList.isEmpty() || zipArr.isEmpty()) {
            out.println("Empty project or patch list");
            throw new IndexOutOfBoundsException();
        }

        String patchResult;
        long startTimeTotal = System.currentTimeMillis();
            for (String currentProjectPath : projectsList) {
                patchResult = new ApplyPatch().doPatch(currentProjectPath, zipArr);
                if (patchResult.equals("error")) {
                    new IO().deleteAll(new File(Prefs.patchesDir + File.separator + "temp"));
                    out.println("ApplyPatch error occurred");
                }
            }
        out.println("All done in " + (System.currentTimeMillis() - startTimeTotal) + " ms");
        out.println("Good bye Sir.");
    }
}