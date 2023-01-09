package com.github.cregrant.smaliscissors.common;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.util.IO;

import java.io.File;
import java.io.FilenameFilter;

public class ApkLocator {

    public String getApkPath(Project project) {
        if (Main.dex != null) {
            String apkPathSupplied = Main.dex.getApkPath();
            if (apkPathSupplied != null) {
                File file = new File(apkPathSupplied);
                if (file.exists()) {
                    return apkPathSupplied;
                }
            }
        }

        File[] files = new File(project.getPath()).listFiles();
        if (files != null) {
            for (File str : files) {
                if (str.getName().startsWith("apktool.")) {
                    return parseApktoolConfig(project, str);
                }
            }
        }
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

        String filename = content.substring(start + 2, end + 4);
        File parentFile = new File(project.getPath()).getParentFile();
        File apkFile = new File(parentFile, filename);
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
            return apkFiles[0].getPath();
        }
        return null;
    }
}
