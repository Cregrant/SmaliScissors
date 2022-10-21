package com.github.cregrant.smaliscissors.common;

import com.github.cregrant.smaliscissors.Project;

import java.io.*;
import java.util.Properties;

public class ProjectProperties {
    private final File propertiesFile;
    private final Properties properties;

    public ProjectProperties(Project project) {
        propertiesFile = new File(project.getPath() + "/PatcherProperties.txt");
        properties = new Properties();
        if (propertiesFile.exists()) {
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(propertiesFile))) {
                properties.load(bis);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String get(Property key) {
        return properties.getProperty(key.name());
    }

    public void set(Property key, String value) {
        properties.setProperty(key.name(), value);
    }

    public void save() {
        if (!properties.isEmpty()) {
            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(propertiesFile))) {
                properties.store(bos, null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public enum Property {
        firebase_crashlytics_patched,
        firebase_analytics_patched,
        removecode_action_type,
        removecode_action_count,
        last_removecode_target,
        targets_hash
    }
}
