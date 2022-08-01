package com.github.cregrant;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class Config {
    static String verboseLevel = "INFO";
    static boolean allowCompression;

    public static void loadConf() {
        Properties props = new Properties();
        String settingsFilename = System.getProperty("user.dir") + "/config/conf.txt";
        try {
            FileInputStream input = new FileInputStream(settingsFilename);
            props.load(input);
            input.close();
        }
        catch (Exception e) {
            com.github.cregrant.smaliscissors.Main.out.println("Error loading conf!");
        }
        try {
            if (props.size() == 0) {
                saveConf();
                com.github.cregrant.smaliscissors.Main.out.println("Config file broken or unreachable. Using default one.");
            }
            verboseLevel = props.getProperty("Verbose level");
            allowCompression = Boolean.parseBoolean(props.getProperty("Allow compression"));
        }
        catch (Exception e) {
            com.github.cregrant.smaliscissors.Main.out.println("Error reading conf!");
            saveConf();
        }

    }

    private static void saveConf() {
        try {
            FileOutputStream output = new FileOutputStream(System.getProperty("user.dir") + "/config/conf.txt");
            Properties props = new Properties();
            props.put("Verbose level", String.valueOf(verboseLevel));
            props.put("Allow compression", Boolean.valueOf(allowCompression).toString());
            props.store(output, "");
            output.close();
        }
        catch (Exception e) {
            com.github.cregrant.smaliscissors.Main.out.println("Error writing conf: " + e.getMessage());
        }
    }
}
