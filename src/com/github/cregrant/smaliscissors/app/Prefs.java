package com.github.cregrant.smaliscissors.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class Prefs {
    public static String run_type = "";
    public static String projectPath = "";
    public static File patchesDir;
    public static String tempDir;
    //static boolean bigMemoryDevice = false;
    private static double versionConf = 0.01;
    static boolean rules_AEmode = true;
    static int verbose_level = 1;
    static boolean keepSmaliFilesInRAM = false;
    static boolean keepXmlFilesInRAM = false;

    void loadConf() {
/*        if (Runtime.getRuntime().maxMemory() > 200000000L) {
            bigMemoryDevice = true;
            OutStream.println("Big RAM device. Nice!");
        } else {
            OutStream.println("Low RAM device. Trying to survive...");
        }*/
        Properties props = new Properties();
        String settingsFilename = System.getProperty("user.dir") + File.separator + "config" + File.separator + "conf.txt";
        try {
            FileInputStream input = new FileInputStream(settingsFilename);
            props.load(input);
            input.close();
        }
        catch (Exception e) {
            OutStream.println("Error loading conf!");
        }
        try {
            if (props.size() == 0) {
                saveConf();
                OutStream.println("Config file broken or unreachable. Using default one.");
            }
            verbose_level = Integer.parseInt(props.getProperty("Verbose_level"));
            versionConf = Float.parseFloat(props.getProperty("Version"));
            rules_AEmode = Boolean.parseBoolean(props.getProperty("Rules_AEmode"));
            keepSmaliFilesInRAM = Boolean.parseBoolean(props.getProperty("Keep_smali_files_in_RAM"));
            keepXmlFilesInRAM = Boolean.parseBoolean(props.getProperty("Keep_xml_files_in_RAM"));
        }
        catch (Exception e) {
            OutStream.println("Error reading conf!");
        }
        if (Main.version - versionConf > 0.001) {
            new Prefs().upgradeConf();
        }
    }

    void saveConf() {
        try {
            FileOutputStream output = new FileOutputStream(System.getProperty("user.dir") + File.separator + "config" + File.separator + "conf.txt");
            Properties props = new Properties();
            props.put("Version", String.format("%.2f", versionConf).replace(',', '.'));
            props.put("Verbose_level", String.valueOf(verbose_level));
            props.put("Rules_AEmode", ((Boolean) rules_AEmode).toString());
            props.put("Keep_smali_files_in_RAM", ((Boolean) keepSmaliFilesInRAM).toString());
            props.put("Keep_xml_files_in_RAM", ((Boolean) keepXmlFilesInRAM).toString());
            props.store(output, "");
            output.close();
        }
        catch (Exception e) {
            OutStream.println("Error writing conf: " + e.getMessage());
        }
    }

    private void upgradeConf() {
        OutStream.println("Upgrading config file...");
        OutStream.println(versionConf + " --> 0.01");
        versionConf = 0.01f;
        this.saveConf();
        OutStream.println("Upgraded.");
    }
}