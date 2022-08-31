package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.common.outer.DexExecutor;
import com.github.cregrant.smaliscissors.common.outer.SimpleOutStream;

import java.io.File;
import java.util.ArrayList;

public class Main {
    public static SimpleOutStream out;
    public static DexExecutor dex;


    public static void mainAsModule(String[] args, SimpleOutStream logger, DexExecutor dexExecutor) {
        if (args == null || args.length < 2) {
            Main.out.println("Usage as module: args - String(s) with full path to projects and String(s) with full path to zip patches\n" +
                    "Example: Main.main(sdcard/ApkEditor/decoded, sdcard/ApkEditor/patches/patch.zip");
            return;
        }
        out = logger == null ? getDefaultOutStream() : logger;
        dex = dexExecutor;
        ArrayList<String> zipList = new ArrayList<>(5);
        ArrayList<String> projectList = new ArrayList<>(5);
        String targets = "";

        for (String str : args) {
            if (str.endsWith(".zip")) {
                zipList.add(str);
            } else if (new File(str).exists()) {
                projectList.add(str);
            } else {
                targets = str;
/*                try {
                    Prefs.logLevel = Prefs.Log.valueOf(str);
                } catch (IllegalArgumentException e) {
                    out.println("Invalid log level. Using INFO.");
                }*/
            }
        }

        if (zipList.isEmpty() || projectList.isEmpty()) {
            out.println("Invalid input");
            return;
        }

        Worker worker = new Worker(projectList);
        if (targets.isEmpty()) {
            worker.setPatches(zipList);
        } else {
            worker.addRemoveCodeRule(zipList, targets);
        }
        worker.run();
        System.gc();
    }

//    public static void mainTest(String[] args) {
//        mainAsDex("",
//                "C:\\JAVA_projects\\SmaliScissors\\patches\\remove_code_all_discord.zip",
//                "C:\\BAT\\_INPUT_APK\\Discord_121.9_",
//                "com/android/installreferrer\n" +
//                        "com/google/android/gms/tagmanager\n" +
//                        "com/google/android/gms/ads\n" +
//                        "com/google/android/gms/analytics\n" +
//                        "com/google/android/gms/measurement\n" +
//                        "com/google/firebase/crash\n" +
//                        "com/google/firebase/analytics\n" +
//                        "com/google/firebase/firebase_analytics\n" +
//                        "com/adjust");
//    }

    //Run RemoveCode rule by executing this project as dex file (like in the ExecuteDex rule)
//    public static void mainAsDex(String apkPath, String zipPath, String projectPath, String param) {
//        String[] args = new String[]{projectPath, zipPath, param};
//        mainAsModule(args, null, null);
//    }

    private static SimpleOutStream getDefaultOutStream() {
        return new SimpleOutStream() {
            @Override
            public void println(Object x) {
                System.out.println(x);
            }
        };
    }
}