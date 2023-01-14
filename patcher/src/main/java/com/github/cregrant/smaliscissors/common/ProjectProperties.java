package com.github.cregrant.smaliscissors.common;

import com.github.cregrant.smaliscissors.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

public class ProjectProperties {

    private static final Logger logger = LoggerFactory.getLogger(ProjectProperties.class);
    private final File propertiesFile;
    private final Properties properties;

    public ProjectProperties(Project project) {
        propertiesFile = new File(project.getPath() + "/PatcherProperties.txt");
        properties = new Properties(getDefaults());
        if (propertiesFile.exists()) {
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(propertiesFile))) {
                properties.load(bis);
            } catch (IOException e) {
                logger.error("Error accessing PatcherProperties.txt inside the project");
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

    private Properties getDefaults() {
        Properties props = new Properties();
        props.setProperty(Property.firebase_crashlytics_patched.name(), "false");
        props.setProperty(Property.firebase_analytics_patched.name(), "false");
        props.setProperty(Property.removecode_action_type.name(), "");
        props.setProperty(Property.removecode_action_count.name(), "0");
        props.setProperty(Property.last_removecode_target.name(), "");
        props.setProperty(Property.targets_hash.name(), "0");
        return props;
    }

    public void save() {
        if (!properties.isEmpty()) {
            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(propertiesFile))) {
                properties.store(bos, null);
                logger.debug("PatcherProperties.txt saved");
            } catch (IOException e) {
                logger.error("Error saving PatcherProperties.txt inside the project");
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
