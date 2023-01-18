package com.github.cregrant.smaliscissors.console;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class InteractiveChoice {

    private final String rootProjectsDir;
    private final String rootPatchesDir;
    private ArrayList<String> projectsList;
    private ArrayList<String> patchesList;
    private boolean error;

    public InteractiveChoice(Args parsedArgs) {
        rootProjectsDir = parsedArgs.getProjects().get(0);
        rootPatchesDir = parsedArgs.getPatches().get(0);
    }

    public void showSelection() {
        Scanner scanner = new Scanner(new File(rootProjectsDir), new File(rootPatchesDir));
        if (scanner.scanFailed()) {
            return;
        }

        String msg = "\nSelect a project. Enter = all. x = exit. For example, 0 or 0 1 2 (which means 0 and 1 and 2).";
        Picker projectsPicker = new Picker(scanner.getScannedProjects(), msg);
        projectsList = projectsPicker.getChoice();
        if (projectsList.isEmpty()) {
            error = true;
            return;
        }

        String msg2 = "\nNow select a patch:";
        Picker patchesPicker = new Picker(scanner.getScannedPatches(), msg2);
        patchesList = patchesPicker.getChoice();
        if (projectsList.isEmpty()) {
            error = true;
        }
    }

    public boolean isChoiceFailed() {
        return error;
    }

    public List<String> getProjectsList() {
        return projectsList;
    }

    public List<String> getPatchesList() {
        return patchesList;
    }
}
