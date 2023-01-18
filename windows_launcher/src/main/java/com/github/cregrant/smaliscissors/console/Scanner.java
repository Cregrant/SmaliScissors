package com.github.cregrant.smaliscissors.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;

public class Scanner {

    private static final Logger logger = LoggerFactory.getLogger(Scanner.class);
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
            logger.error("Error loading projects folder\n" + projectsFolder);
            return new ArrayList<>(0);
        }

        File[] projects = projectsFolder.listFiles();
        if (projects == null || projects.length == 0) {
            logger.error("Projects folder is empty\n" + projectsFolder);
            return new ArrayList<>(0);
        }

        ArrayList<String> result = new ArrayList<>();
        for (File project : projects) {
            if (project.isDirectory()) {
                String[] subfolders = project.list();
                if (subfolders == null) {
                    continue;
                }
                for (String subfolder : subfolders) {
                    if (subfolder.equals("res") || subfolder.equals("smali")) {
                        result.add(project.toString());
                        break;
                    }
                }
            }
        }
        if (result.isEmpty()) {
            logger.error("No decompiled projects found!");
        }
        return result;
    }

    private ArrayList<String> getPatches() {
        if (patchesFolder == null || !patchesFolder.isDirectory()) {
            logger.error("Error loading patches folder\n" + patchesFolder);
            return new ArrayList<>(0);
        }

        File[] patches = patchesFolder.listFiles();
        if (patches == null || patches.length == 0) {
            logger.error("Patches folder is empty\n" + patchesFolder);
            return new ArrayList<>(0);
        }

        ArrayList<String> result = new ArrayList<>(patches.length);
        for (File zip : patches) {
            if (zip.toString().endsWith(".zip")) {
                result.add(zip.toString());
            }
        }
        return result;
    }
}
