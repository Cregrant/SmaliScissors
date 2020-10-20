package com.github.cregrant.smaliscissors.misc;

import com.github.cregrant.smaliscissors.app.Prefs;

import java.io.File;

public class CompatibilityData {
    public String getHomeDir() {
        final String arch = System.getProperty("os.arch");
        String mainDirPath = "";
        assert arch != null;
        if (arch.equals("amd64") || arch.equals("x86") || arch.equals("i386") || arch.equals("ppc")) {
            mainDirPath = "C:\\BAT\\_INPUT_APK";                 // путь
            Prefs.arch_device = "pc";
        }
        else if (arch.contains("aarch")){
            mainDirPath = Environment.getExternalStorageDirectory() + "/ApkEditor/decoded";
            Prefs.arch_device = "android";
        }
        else System.out.println("Unknown device? Contact aliens.");
        return mainDirPath;
    }

    public String getPatchesDir() {
        String dir;
        if (Prefs.arch_device.equals("pc")) dir = System.getProperty("user.dir")+File.separator+"patches";
        else dir = Environment.getExternalStorageDirectory() + "/ApkEditor/patches";
        return dir;
    }

    public  String getTempDir() {
        String dir;
        if (Prefs.arch_device.equals("pc")) dir = System.getProperty("user.dir")+File.separator+"patches" + File.separator + "temp";
        else dir = Environment.getExternalStorageDirectory() + "/ApkEditor/patches/temp";
        return dir;
    }

    //unused class
    private static class Environment {
        private Environment() {
        }

        static String getExternalStorageDirectory() {
            return "";
        }
    }
}