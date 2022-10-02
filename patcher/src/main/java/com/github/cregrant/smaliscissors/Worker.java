package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.common.BackgroundWorker;
import com.github.cregrant.smaliscissors.rule.types.RemoveCode;
import com.github.cregrant.smaliscissors.util.Misc;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.currentTimeMillis;

public class Worker {
    private final BackgroundWorker executor = new BackgroundWorker();
    private final ArrayList<Project> projects = new ArrayList<>(5);
    private final ArrayList<Patch> patches = new ArrayList<>(5);

    public Worker(List<String> projectsList) {
        setProjects(projectsList);
    }

    void setProjects(List<String> projectsList) {
        for (String path : projectsList) {
            try {
                Project project = new Project(path, executor);
                projects.add(project);
            } catch (Exception e) {
                Main.out.println("Error: skipping project \"" + path + "\"! (" + e.getMessage() + ")");
            }
        }
    }

    void setPatches(List<String> patchesList) {
        for (String patchString : patchesList) {
            patches.add(new Patch(patchString));
        }
    }

    void setSingleRemoveCodeRule(String targets) {
        String[] splitTargets = targets.split(" ");
        RemoveCode rule = new RemoveCode();
        ArrayList<String> targetsList = new ArrayList<>(splitTargets.length);
        for (String s : splitTargets) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                targetsList.add(trimmed);
            }
        }
        rule.setTargets(targetsList);
        patches.add(new Patch(rule));
    }

    void run() {
        executor.start();
        try {
            long globalStartTime = currentTimeMillis();
            for (Project project : projects) {
                long projectStartTime = currentTimeMillis();
                Main.out.println("Project - " + project.getName());
                for (Patch patch : patches) {
                    project.scan(patch.isSmaliNeeded(), patch.isXmlNeeded());
                    Main.out.println("Patch - " + patch.getName() + "\n");
                    long patchStartTime = currentTimeMillis();
                    project.applyPatch(patch);
                    Main.out.println(patch.getName() + " applied in " + (currentTimeMillis() - patchStartTime) + "ms.");
                }
                project.writeChanges();
                Main.out.println(project.getName() + " finished in " + (currentTimeMillis() - projectStartTime) + "ms." + "\n------------------");
            }
            Main.out.println("Tasks completed in " + (currentTimeMillis() - globalStartTime) + "ms." + "\n------------------");
        } catch (FileNotFoundException e) {
            Main.out.println(e.getMessage());
            Main.out.println("Note: probably patch require some files that haven't been decompiled yet.");
        } catch (Exception e) {
            Main.out.println(Misc.stacktraceToString(e));
        } finally {
            executor.stop();
        }
    }
}
