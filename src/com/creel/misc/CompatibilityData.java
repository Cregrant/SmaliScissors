package com.creel.misc;

import com.creel.app.Prefs;
import java.io.File;

public class CompatibilityData {
    public String getHomeDir() {
        String arch = System.getProperty("os.arch");
        String mainDirPath = "";
        if (arch.equals("amd64") || arch.equals("x86") || arch.equals("i386") || arch.equals("ppc")) {
            mainDirPath = "C:\\BAT\\_INPUT_APK";
            Prefs.arch_device = "pc";
        } else if (arch.contains("aarch")) {
            mainDirPath = Environment.getExternalStorageDirectory() + "/ApkEditor/decoded";
            Prefs.arch_device = "android";
        } else {
            System.out.println("Unknown device? Contact aliens.");
        }
        return mainDirPath;
    }

    public String getPatchesDir() {
        return Prefs.arch_device.equals("pc") ? System.getProperty("user.dir") + File.separator + "patches" : Environment.getExternalStorageDirectory() + "/ApkEditor/patches";
    }

    private static class Environment {
        private Environment() {
        }

        static String getExternalStorageDirectory() {
            return "";
        }
    }
}