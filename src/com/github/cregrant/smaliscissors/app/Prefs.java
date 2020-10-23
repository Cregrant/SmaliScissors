package com.github.cregrant.smaliscissors.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class Prefs {
    public static String arch_device = "";
    static boolean bigMemoryDevice = false;
    private static String versionType;
    private static double versionConf;
    static boolean rules_AEmode = true;
    static int verbose_level = 1;
    static final int max_thread_num = Runtime.getRuntime().availableProcessors();

    void loadConf() {
        if (Runtime.getRuntime().maxMemory() > 200000000L) {
            bigMemoryDevice = true;
            System.out.println("Big RAM device. Nice!");
        } else {
            System.out.println("Low RAM device. Trying to survive...");
        }
        Properties props = new Properties();
        String settingsFilename = System.getProperty("user.dir") + File.separator + "config" + File.separator + "conf.txt";
        try {
            FileInputStream input = new FileInputStream(settingsFilename);
            props.load(input);
            input.close();
        }
        catch (Exception e) {
            System.out.println("Error loading conf!");
        }
        try {
            verbose_level = Integer.parseInt(props.getProperty("Verbose_level"));
            versionConf = Float.parseFloat(props.getProperty("Version"));
            versionType = props.getProperty("Version_type");
            if (versionType.equals("a")) {
                System.out.print("Unstable version. Prepare your anus");
            }
            rules_AEmode = Boolean.parseBoolean(props.getProperty("Rules_AEmode"));
        }
        catch (Exception e) {
            System.out.println("Error reading conf!");
        }
        if (0.01 - versionConf > 0.001) {
            new Prefs().upgradeConf();
        }
    }

    void saveConf() {
        try {
            FileOutputStream output = new FileOutputStream(System.getProperty("user.dir") + File.separator + "config" + File.separator + "conf.txt");
            Properties props = new Properties();
            props.put("Version", String.format("%.2f", versionConf).replace(',', '.'));
            props.put("Version_type", String.valueOf(versionType));
            props.put("Verbose_level", String.valueOf(verbose_level));
            props.put("Rules_AEmode", rules_AEmode);
            props.store(output, "Config v0.01" + versionType);
            output.close();
        }
        catch (Exception e) {
            System.out.println("Error writing conf");
        }
    }

    private void upgradeConf() {
        System.out.println("Upgrading config file...");
        System.out.println(versionConf + " --> 0.01");
        versionConf = 0.01f;
        this.saveConf();
        System.out.println("Upgraded.");
    }
}