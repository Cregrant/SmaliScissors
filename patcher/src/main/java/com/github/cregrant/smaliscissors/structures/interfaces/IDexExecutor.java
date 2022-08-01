package com.github.cregrant.smaliscissors.structures.interfaces;

public interface IDexExecutor {

    void runDex(String dexPath, String entrance, String mainClass, String apkPath, String zipPath, String projectPath, String param, String tempDir);

    String getApkPath();
}
