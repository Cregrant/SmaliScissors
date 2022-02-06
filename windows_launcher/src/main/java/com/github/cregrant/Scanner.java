package com.github.cregrant;

import java.io.File;
import java.util.ArrayList;

import static com.github.cregrant.Main.out;

public class Scanner {
    File projectsFolder;
    File patchesFolder;

    public ArrayList<String> getScannedProjects() {
        return scannedProjects;
    }

    public ArrayList<String> getScannedPatches() {
        return scannedPatches;
    }

    ArrayList<String> scannedProjects;
    ArrayList<String> scannedPatches;

    public Scanner(File projectsFolder, File patchesFolder) {
        this.projectsFolder = projectsFolder;
        this.patchesFolder = patchesFolder;
        scannedProjects = getProjects();
        scannedPatches = getPatches();
    }

    boolean scanFailed() {
        return scannedProjects.isEmpty() || scannedPatches.isEmpty();
    }

    private ArrayList<String> getProjects() {
        if (projectsFolder == null || !projectsFolder.isDirectory()) {
            out.println("Error loading projects folder\n" + projectsFolder);
            return new ArrayList<>(0);
        }

        File[] projects = projectsFolder.listFiles();
        if (projects == null || projects.length == 0) {
            out.println("Projects folder is empty\n" + projectsFolder);
            return new ArrayList<>(0);
        }

        ArrayList<String> result = new ArrayList<>();
        for (File project : projects) {
            if (project.isDirectory()) {
                String[] subfolders = project.list();
                if (subfolders == null)
                    continue;
                for (String subfolder : subfolders) {
                    if (subfolder.equals("res") || subfolder.equals("smali")) {
                        result.add(project.toString());
                        break;
                    }
                }
            }
        }
        if (result.isEmpty())
            out.println("No decompiled projects found!");
        return result;
    }

    private ArrayList<String> getPatches() {
        if (patchesFolder == null || !patchesFolder.isDirectory()) {
            out.println("Error loading patches folder\n" + patchesFolder);
            return new ArrayList<>(0);
        }

        File[] patches = patchesFolder.listFiles();
        if (patches == null || patches.length == 0) {
            out.println("Patches folder is empty\n" + patchesFolder);
            return new ArrayList<>(0);
        }

        ArrayList<String> result = new ArrayList<>(patches.length);
        for (File zip : patches) {
            if (zip.toString().endsWith(".zip"))
                result.add(zip.toString());
        }
        return result;
    }
}
