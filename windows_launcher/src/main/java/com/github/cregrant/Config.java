package com.github.cregrant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class Config {

    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    static String verboseLevel = "INFO";

    public static void loadConf() {
        Properties props = new Properties();
        String settingsFilename = System.getProperty("user.dir") + "/config/conf.txt";
        try {
            FileInputStream input = new FileInputStream(settingsFilename);
            props.load(input);
            input.close();
        } catch (Exception e) {
            logger.error("Error loading conf!", e);
        }
        try {
            if (props.size() == 0) {
                saveConf();
                logger.warn("Config file broken or unreachable. Using default one.");
            }
            verboseLevel = props.getProperty("Verbose level");
        } catch (Exception e) {
            logger.error("Error reading conf!", e);
            saveConf();
        }

    }

    private static void saveConf() {
        try {
            FileOutputStream output = new FileOutputStream(System.getProperty("user.dir") + "/config/conf.txt");
            Properties props = new Properties();
            props.put("Verbose level", String.valueOf(verboseLevel));
            props.store(output, "");
            output.close();
        } catch (Exception e) {
            logger.error("Error writing conf!", e);
        }
    }
}
