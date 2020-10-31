package com.github.cregrant.smaliscissors.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import static java.lang.System.out;

public class Prefs {
    public static String arch_device = "";
    static boolean bigMemoryDevice = false;
    private static String versionType = "s";
    private static double versionConf = 0.01;
    static boolean rules_AEmode = true;
    static int verbose_level = 1;
    static final int max_thread_num = Runtime.getRuntime().availableProcessors();
    static boolean keepSmaliFilesInRAM = false;
    static boolean keepXmlFilesInRAM = false;

    void loadConf() {
        if (Runtime.getRuntime().maxMemory() > 200000000L) {
            bigMemoryDevice = true;
            out.println("Big RAM device. Nice!");
        } else {
            out.println("Low RAM device. Trying to survive...");
        }
        Properties props = new Properties();
        String settingsFilename = System.getProperty("user.dir") + File.separator + "config" + File.separator + "conf.txt";
        try {
            FileInputStream input = new FileInputStream(settingsFilename);
            props.load(input);
            input.close();
        }
        catch (Exception e) {
            out.println("Error loading conf!");
        }
        try {
            if (props.size() == 0) {
                saveConf();
                out.println("Config file broken or unreachable. Using default one.");
            }
            verbose_level = Integer.parseInt(props.getProperty("Verbose_level"));
            versionConf = Float.parseFloat(props.getProperty("Version"));
//            versionType = props.getProperty("Version_type");
//            if (versionType.equals("a")) {
//                out.print("Unstable version. Prepare uranus");
//            }
            rules_AEmode = Boolean.parseBoolean(props.getProperty("Rules_AEmode"));
            keepSmaliFilesInRAM = Boolean.parseBoolean(props.getProperty("Keep_smali_files_in_RAM"));
            keepXmlFilesInRAM = Boolean.parseBoolean(props.getProperty("Keep_xml_files_in_RAM"));
        }
        catch (Exception e) {
            out.println("Error reading conf!");
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
            //props.put("Version_type", String.valueOf(versionType));
            props.put("Verbose_level", String.valueOf(verbose_level));
            props.put("Rules_AEmode", ((Boolean) rules_AEmode).toString());
            props.put("Keep_smali_files_in_RAM", ((Boolean) keepSmaliFilesInRAM).toString());
            props.put("Keep_xml_files_in_RAM", ((Boolean) keepXmlFilesInRAM).toString());
            props.store(output, "");
            output.close();
        }
        catch (Exception e) {
            out.println("Error writing conf: " + e.getMessage());
        }
    }

    private void upgradeConf() {
        out.println("Upgrading config file...");
        out.println(versionConf + " --> 0.01");
        versionConf = 0.01f;
        this.saveConf();
        out.println("Upgraded.");
    }
}