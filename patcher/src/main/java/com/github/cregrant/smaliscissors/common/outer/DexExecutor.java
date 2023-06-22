package com.github.cregrant.smaliscissors.common.outer;

public interface DexExecutor {

    void runDex(String dexPath, String entrance, String mainClass, String apkPath, String zipPath, String projectPath, String param, String tempDir) throws Exception;
}
