package com.github.cregrant.smaliscissors.common;

import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;

public class ApkLocator {

    private static final Logger logger = LoggerFactory.getLogger(ApkLocator.class);

    public String getApkPath(Project project) {
        File[] files = new File(project.getPath()).listFiles();
        if (files != null) {
            for (File str : files) {
                if (str.getName().startsWith("apktool.")) {
                    String apkPath = parseApktoolConfig(project, str);
                    if (apkPath != null) {
                        logger.debug("Apk path found in an apktool config: {}", apkPath);
                        return apkPath;
                    }
                    logger.debug("Apktool config does not contain an apk path");
                }
            }
        }
        logger.debug("Apktool config is not found");
        return parseParentFolder(project);
    }

    private String parseApktoolConfig(Project project, File config) {
        String content = IO.read(config.getPath());
        int end = content.indexOf(".apk");
        if (end == -1) {
            return null;
        }

        int start = content.lastIndexOf(':', end);
        if (start == -1) {
            return null;
        }
        if (content.charAt(start + 2) == '"') {   //some apktool versions wraps the filename in quotes
            start++;
        }

        String parsedString = content.substring(start + 2, end + 4).replace("\\/", "/");
        File apkFile = new File(parsedString);          //parsedString is a path
        if (apkFile.exists()) {
            return apkFile.getPath();
        }

        File parentFile = new File(project.getPath()).getParentFile();
        apkFile = new File(parentFile, parsedString);   //parsedString is a filename
        if (apkFile.exists()) {
            return apkFile.getPath();
        }
        return null;
    }

    private String parseParentFolder(Project project) {
        File projectFile = new File(project.getPath());
        final String projectName = projectFile.getName();
        File[] apkFiles = projectFile.getParentFile().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.equals(projectName + ".apk");
            }
        });
        if (apkFiles != null && apkFiles.length == 1) {
            logger.debug("Apk path found in the same directory as a project: {}", apkFiles[0].getPath());
            return apkFiles[0].getPath();
        }
        return null;
    }
}
