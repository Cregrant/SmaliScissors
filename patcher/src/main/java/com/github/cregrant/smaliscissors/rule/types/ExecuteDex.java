package com.github.cregrant.smaliscissors.rule.types;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.util.IO;

import java.io.IOException;
import java.util.ArrayList;

public class ExecuteDex implements Rule {
    private String name;
    private String script;
    private String mainClass;
    private String entrance;
    private String param;
    private boolean isSmali;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isValid() {
        return getScript() != null && getMainClass() != null && getEntrance() != null && getParam() != null;
    }

    @Override
    public boolean smaliNeeded() {
        return isSmali();
    }

    @Override
    public boolean xmlNeeded() {    //trying to guess
        return !isSmali();
    }

    @Override
    public String nextRuleName() {
        return null;
    }

    @Override
    public void apply(Project project, Patch patch) throws IOException {
        String apkPath = project.getApkPath();
        if (apkPath == null) {
            Main.out.println("Error: apk file not found. Dex script may fail.");
        }
        String zipPath = patch.getFile().toString();
        String projectPath = project.getPath();
        patch.createTempDir();
        ArrayList<String> extracted = IO.extract(patch.getFile(), patch.getTempDir().getPath(), getScript());
        if (extracted.size() != 1) {
            Main.out.println("Dex script extract error.");
            return;
        }
        String dexPath = extracted.get(0);
        if (Main.dex != null) {
            Main.dex.runDex(dexPath, getEntrance(), getMainClass(), apkPath, zipPath, projectPath, getParam(), patch.getTempDir().getPath());
        } else {
            Main.out.println("Dex executor is not present.");
        }
        patch.deleteTempDir();
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getEntrance() {
        return entrance;
    }

    public void setEntrance(String entrance) {
        this.entrance = entrance;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public boolean isSmali() {
        return isSmali;
    }

    public void setSmali(boolean smali) {
        isSmali = smali;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:  EXECUTE_DEX.\n");
        if (name != null) {
            sb.append("Name:  ").append(getName());
        }
        return sb.toString();
    }
}