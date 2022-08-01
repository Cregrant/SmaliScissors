package com.github.cregrant.smaliscissors.structures.rules;

import com.github.cregrant.smaliscissors.*;
import com.github.cregrant.smaliscissors.utils.IO;
import com.github.cregrant.smaliscissors.utils.Scanner;

import java.io.IOException;
import java.util.ArrayList;

public class Dex implements IRule {
    public String name;
    public String script;
    public String mainClass;
    public String entrance;
    public String param;
    public boolean isSmali;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean integrityCheckPassed() {
        return script != null && mainClass != null && entrance != null && param != null;
    }

    @Override
    public boolean smaliNeeded() {
        return isSmali;
    }

    @Override
    public boolean xmlNeeded() {    //trying to guess
        return !isSmali;
    }

    @Override
    public String nextRuleName() {
        return null;
    }

    @Override
    public void apply(Project project, Patch patch) throws IOException {
        String apkPath = new Scanner(project).getApkPath();
        if (apkPath == null) {
            Main.out.println("Error: apk file not found. Dex script skipped.");
            return;
        }
        String zipPath = patch.getFile().toString();
        String projectPath = project.getPath();
        patch.createTempDir();
        ArrayList<String> extracted = IO.extract(patch.getFile(), patch.getTempDir().getPath(), script);
        if (extracted.size() != 1) {
            Main.out.println("Dex script extract error.");
            return;
        }
        String dexPath = extracted.get(0);
        if (Main.dex != null)
            Main.dex.runDex(dexPath, entrance, mainClass, apkPath, zipPath, projectPath, param, patch.getTempDir().getPath());
        else
            Main.out.println("Dex executor is not present.");
        patch.deleteTempDir();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:  EXECUTE_DEX.\n");
        if (name != null)
            sb.append("Name:  ").append(name);
        return sb.toString();
    }
}
