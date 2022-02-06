package com.github.cregrant.smaliscissors;

import java.util.ArrayList;

public class Main {
    static final double version = 0.01;
    public static OutStream out;
    public static DexExecutor dex;

    public static void main(String[] args, OutStream logger, DexExecutor dexExecutor) {
        if (args.length < 2) {
            Main.out.println("Usage as module: add String(s) with full path to projects and String(s) with full path to zip patches\n" +
                    "Append keepSmaliFilesInRAM or keepXmlFilesInRAM if you want to keep these files in RAM.\n" +
                    "Example ...Main.main(sdcard/ApkEditor/decoded, sdcard/ApkEditor/patches/patch.zip, keepSmaliFilesInRAM");
            return;
        }

        out = logger;
        dex = dexExecutor;
        long startTime = System.currentTimeMillis();
        ArrayList<String> zipList = new ArrayList<>(5);
        ArrayList<String> projectList = new ArrayList<>(5);

        for (String str : args) {
            if (str.endsWith(".zip"))
                zipList.add(str);
            else if (str.equalsIgnoreCase("keepSmaliFilesInRAM"))
                Prefs.keepSmaliFilesInRAM = true;
            else if (str.equalsIgnoreCase("keepXmlFilesInRAM"))
                Prefs.keepXmlFilesInRAM = true;
            else projectList.add(str);
        }

        if (zipList.isEmpty() || projectList.isEmpty()) {
            out.println("Invalid input");
            return;
        }

        Worker worker = new Worker(projectList, zipList);
        worker.run();
        Main.out.println("All done in " + (System.currentTimeMillis() - startTime) + " ms");
        Main.out.println("Good bye Sir.");
    }
}