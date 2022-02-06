package com.github.cregrant.smaliscissors.structures.rules;

import com.github.cregrant.smaliscissors.*;

import java.io.File;

public class Execute implements IRule {
    public String name;
    public String script;
    public String mainClass;
    public String entrance;
    public String param;
    public boolean isSmali = false;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean integrityCheckPassed() {
        return script != null && mainClass != null && entrance != null && param != null;
    }

    @Override
    public String nextRuleName() {
        return null;
    }

    @Override
    public boolean canBeMerged(IRule otherRule) {
        return false;
    }

    @Override
    public void apply(Project project, Patch patch) {
        String apkPath = new Scanner(project).getApkPath();
        if (apkPath == null)
            Main.out.println("ERROR: apk file not found.");
        String zipPath = patch.getFile().toString();
        String projectPath = project.getPath();
        Prefs.tempDir.mkdirs();
        String dexPath = Prefs.tempDir + File.separator + script;
        if (Main.dex != null)
            Main.dex.runDex(dexPath, entrance, mainClass, apkPath, zipPath, projectPath, param, Prefs.tempDir.toString());
        else
            Main.out.println("Dex executor is not present.");
        IO.delete(Prefs.tempDir);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:    EXECUTE_DEX.\n");
        if (name != null)
            sb.append("Name:  ").append(name).append('\n');
        return sb.toString();
    }
}
