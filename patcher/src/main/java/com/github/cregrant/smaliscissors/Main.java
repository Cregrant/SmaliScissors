package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.structures.interfaces.IDexExecutor;
import com.github.cregrant.smaliscissors.structures.interfaces.IOutStream;

import java.util.ArrayList;

public class Main {
    public static IOutStream out;
    public static IDexExecutor dex;

    public static void main(String[] args, IOutStream logger, IDexExecutor dexExecutor) {
        if (args.length < 2) {
            Main.out.println("Usage as module: add String(s) with full path to projects and String(s) with full path to zip patches\n" +
                    "Example: Main.main(sdcard/ApkEditor/decoded, sdcard/ApkEditor/patches/patch.zip, keepSmaliFilesInRAM");
            return;
        }
        out = logger;
        dex = dexExecutor;
        ArrayList<String> zipList = new ArrayList<>(5);
        ArrayList<String> projectList = new ArrayList<>(5);

        for (String str : args) {
            if (str.endsWith(".zip"))
                zipList.add(str);
            else if (str.contains("/") || str.contains("\\"))
                projectList.add(str);
            else {
                try {
                    Prefs.logLevel = Prefs.Log.valueOf(str);
                } catch (IllegalArgumentException e) {
                    out.println("Invalid log level. Reverting to INFO.");
                }
            }
        }

        if (zipList.isEmpty() || projectList.isEmpty()) {
            out.println("Invalid input");
            return;
        }

        Worker worker = new Worker(projectList, zipList);
        worker.run();
        System.gc();
    }
}