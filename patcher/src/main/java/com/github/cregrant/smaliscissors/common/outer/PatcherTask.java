package com.github.cregrant.smaliscissors.common.outer;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.rule.types.RemoveCode;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

public class PatcherTask {

    private final String projectPath;
    private final ArrayList<String> patchPaths = new ArrayList<>();
    private final ArrayList<String> smaliPaths = new ArrayList<>();
    private String apkPath;

    public PatcherTask(String projectPath) {
        this.projectPath = projectPath;
    }

    public PatcherTask addPatchPath(String patchPath) {
        this.patchPaths.add(patchPath);
        return this;
    }

    public PatcherTask addPatchPaths(Collection<String> patchPaths) {
        this.patchPaths.addAll(patchPaths);
        return this;
    }

    public PatcherTask addApkPath(String apkPath) {
        this.apkPath = apkPath;
        return this;
    }

    public PatcherTask addSmaliPaths(Collection<String> smaliPaths) {
        this.smaliPaths.addAll(smaliPaths);
        return this;
    }

    public ArrayList<Patch> getPatches() {
        ArrayList<Patch> patches = new ArrayList<>();
        for (String patchPath : getPatchPaths()) {
            patches.add(new Patch(patchPath));
        }

        if (!smaliPaths.isEmpty()) {
            ArrayList<String> targetsList = new ArrayList<>();
            for (String s : smaliPaths) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    targetsList.add(trimmed);
                }
            }
            RemoveCode rule = new RemoveCode(targetsList);
            patches.add(new Patch(rule));
        }
        return patches;
    }

    public void validate() throws IllegalArgumentException {
        if (projectPath == null) {
            throw new IllegalArgumentException("Project path is null");
        } else if (!new File(projectPath).exists()) {
            throw new IllegalArgumentException("Project path is invalid: " + projectPath);
        } else if (!patchPaths.isEmpty()) {
            for (String patch : patchPaths) {
                if (!new File(patch).exists()) {
                    throw new IllegalArgumentException("Patch path is invalid: " + patch);
                }
            }
        } else if (apkPath != null && !new File(apkPath).exists()) {
            throw new IllegalArgumentException("Apk path is invalid: " + apkPath);
        } else if (patchPaths.isEmpty() && smaliPaths.isEmpty()) {
            throw new IllegalArgumentException("Both patch and smali paths are empty");
        }
    }

    public String getProjectPath() {
        return projectPath;
    }

    public ArrayList<String> getPatchPaths() {
        return patchPaths;
    }

    public String getApkPath() {
        return apkPath;
    }

    public ArrayList<String> getSmaliPaths() {
        return smaliPaths;
    }
}
