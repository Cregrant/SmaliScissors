package com.github.cregrant.smaliscissors;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.currentTimeMillis;

public class Worker {
    ArrayList<Project> projects;
    ArrayList<Patch> patches;

    public Worker(List<String> projectsList, List<String> patchesList) {
        load(projectsList, patchesList);
    }

    private void load(List<String> projectsList, List<String> patchesList) {
        projects = new ArrayList<>(projectsList.size());
        for (String project : projectsList)
            projects.add(new Project(project));

        patches = new ArrayList<>(patchesList.size());
        for (String patch : patchesList)
            patches.add(new Patch(patch));
    }

    void run() {
        try {
            for (Project project : projects) {
                long projectStartTime = currentTimeMillis();
                Main.out.println("Project - " + project.getName());
                for (Patch patch : patches) {
                    project.scan(patch.smaliNeeded, patch.xmlNeeded);
                    Main.out.println("Patch - " + patch.getName() + "\n");
                    long patchStartTime = currentTimeMillis();
                    project.applyPatch(patch);
                    Main.out.println(patch.getName() + " patched in " + (currentTimeMillis() - patchStartTime) + "ms.");
                }
                project.writeChanges();
                Main.out.println(project.getName() + " patched in " + (currentTimeMillis() - projectStartTime) + "ms." + "\n------------------");
            }
        } catch (FileNotFoundException e) {
            Main.out.println(e.getMessage());
            Main.out.println("Note: probably patch require some files that haven't been decompiled yet.");
        } catch (Exception e) {
            StackTraceElement[] stack = e.getStackTrace();
            StringBuilder sb = new StringBuilder();
            sb.append("\nUnexpected error occured:\n\n");
            int limit = Math.min(stack.length, 6);
            for (int i = 0; i < limit; i++) {
                sb.append(stack[i].toString()).append('\n');
            }
            Main.out.println(sb.toString());
        }
    }
}
