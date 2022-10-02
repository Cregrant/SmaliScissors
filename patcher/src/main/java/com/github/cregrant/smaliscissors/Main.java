package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.common.outer.DexExecutor;
import com.github.cregrant.smaliscissors.common.outer.SimpleOutStream;

import java.io.File;
import java.util.ArrayList;

public class Main {
    public static SimpleOutStream out;
    public static DexExecutor dex;


    public static void mainAsModule(String[] args, SimpleOutStream logger, DexExecutor dexExecutor) {
        out = logger == null ? getDefaultOutStream() : logger;
        dex = dexExecutor;
        if (args == null || args.length < 2) {
            Main.out.println("Usage as module: args - String(s) with full path to projects and String(s) with full path to zip patches\n" +
                    "Example args: sdcard/ApkEditor/decoded, sdcard/ApkEditor/patches/patch.zip\n" +
                    "Or use as single REMOVE_CODE rule executor (project path(s) and targets separated by space inside quotes:\n" +
                    "sdcard/ApkEditor/decoded, \"com/folder1/\"\n" +
                    "sdcard/ApkEditor/decoded, \"com/folder1/ com/folder2/abc.smali\"");

            return;
        }
        ArrayList<String> zipList = new ArrayList<>(5);
        ArrayList<String> projectList = new ArrayList<>(5);
        String removeCodeTargets = "";

        for (String str : args) {
            boolean exists = new File(str).exists();
            if (str.endsWith(".zip") && exists) {
                zipList.add(str);
            } else if (exists) {
                projectList.add(str);
            } else if (str.endsWith("/") || str.endsWith(".smali")) {
                removeCodeTargets = str;
            } else {
                out.println("Unknown argument error: " + str);
                return;
            }

        }

        if ((removeCodeTargets.isEmpty() && zipList.isEmpty()) || projectList.isEmpty()) {
            out.println("Invalid input");
            return;
        }

        Worker worker = new Worker(projectList);
        if (removeCodeTargets.isEmpty()) {
            worker.setPatches(zipList);
        } else {
            worker.setSingleRemoveCodeRule(removeCodeTargets);
        }
        worker.run();
    }

    public static void main(String[] args) {
        mainAsModule(args, null, null);
    }

    private static SimpleOutStream getDefaultOutStream() {
        return new SimpleOutStream() {
            @Override
            public void println(Object x) {
                System.out.println(x);
            }
        };
    }
}