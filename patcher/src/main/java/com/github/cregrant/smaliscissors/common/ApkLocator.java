package com.github.cregrant.smaliscissors.common;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.util.IO;
import com.github.cregrant.smaliscissors.util.Regex;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

public class ApkLocator {

    public String getApkPath(Project project) {
        String apkPathSupplied = Main.dex.getApkPath();
        if (apkPathSupplied != null) {
            File file = new File(apkPathSupplied);
            if (file.exists()) {
                return apkPathSupplied;
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
        Pattern pattern = Pattern.compile(".{0,5}apkFile.+?(?:\": \"|: )(.+?\\.apk)(?:\",)?");
        try {
            String scannedPath = Regex.matchSingleLine(IO.read(config.getPath()), pattern);
            if (scannedPath == null) {
                return null;
            }

            File parentFile = new File(project.getPath()).getParentFile();
            File apkFile = new File(parentFile + File.separator + scannedPath);
            if (apkFile.exists()) {
                return apkFile.getPath();
            }
            apkFile = new File(scannedPath);
            if (apkFile.exists()) {
                return apkFile.getPath();
            }
        } catch (Exception ignored) {
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
        if (apkFiles!= null && apkFiles.length == 1)
            return apkFiles[0].getPath();
        return null;
    }
}
