package com.github.cregrant.smaliscissors;

import java.util.ArrayList;

import static java.lang.System.currentTimeMillis;

public class Worker {
    ArrayList<Project> projects;
    ArrayList<Patch> patches;

    public Worker(ArrayList<String> projectsList, ArrayList<String> patchesList) {
        load(projectsList, patchesList);
    }

    private void load(ArrayList<String> projectsList, ArrayList<String> patchesList) {
        projects = new ArrayList<>(projectsList.size());
        for (String project : projectsList)
            projects.add(new Project(project));

        patches = new ArrayList<>(patchesList.size());
        for (String patch : patchesList)
            patches.add(new Patch(patch));
        Prefs.tempDir = patches.get(0).getFile().getParentFile();
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
                if (Prefs.verbose_level == 0 && (Prefs.keepXmlFilesInRAM || Prefs.keepSmaliFilesInRAM))
                    Main.out.println("Writing changes to disk...");
                project.writeChanges();
                long end = currentTimeMillis();
                Main.out.println(project.getName() + " patched in " + (currentTimeMillis() - projectStartTime) + "ms." + "\n------------------");
            }
        } catch (Exception e) {
            StackTraceElement[] stack = e.getStackTrace();
            StringBuilder sb = new StringBuilder();
            sb.append("\nUnexpected error occured:\n\n");
            for (int i=0; i<6; i++) {
                sb.append(stack[i].toString()).append('\n');
            }
            Main.out.println(sb.toString());
        }

    }
}
