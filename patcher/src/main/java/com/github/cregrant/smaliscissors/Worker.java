package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.common.BackgroundWorker;
import com.github.cregrant.smaliscissors.util.Misc;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.currentTimeMillis;

public class Worker {
    private final BackgroundWorker executor = new BackgroundWorker();
    private ArrayList<Project> projects;
    private ArrayList<Patch> patches;

    public Worker(List<String> projectsList) {
        setProjects(projectsList);
    }

    void setProjects(List<String> projectsList) {
        projects = new ArrayList<>(projectsList.size());
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
        patches = new ArrayList<>(patchesList.size());
        for (String patch : patchesList) {
            patches.add(new Patch(patch));
        }
    }

    void addRemoveCodeRule(List<String> patchesList, String targets) {
        String[] splitTargets = targets.split("\\R");
        patches = new ArrayList<>(1);
        patches.add(new RemoveCodePatch(patchesList.get(0), splitTargets));
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
