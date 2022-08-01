package com.github.cregrant.smaliscissors.smali;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Prefs;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.structures.common.SmaliFile;
import com.github.cregrant.smaliscissors.structures.rules.RemoveCode;
import com.github.cregrant.smaliscissors.structures.rules.RemoveFiles;
import com.github.cregrant.smaliscissors.structures.smali.SmaliClass;
import com.github.cregrant.smaliscissors.structures.smali.SmaliTarget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SmaliWorker {
    private final Project project;
    private final Patch patch;
    private final RemoveCode rule;
    private static final boolean DEBUG_BENCHMARK = true;
    private static final boolean DEBUG_NOT_WRITE = false;

    public SmaliWorker(Project project, Patch patch, RemoveCode rule) {
        this.project = project;
        this.patch = patch;
        this.rule = rule;
    }

    public void run() {
        do {
            long l = System.currentTimeMillis();
            State currentState = new State(project.getSmaliList());
            int errorsNum = 0;
            int patchedNum = 0;

            for (String path : rule.targets) {
                SmaliTarget target = new SmaliTarget();
                target.setSkipPath(path);

                State newState;
                try {
                    newState = remove(target, currentState);
                } catch (Exception e) {
                    errorsNum++;
                    Main.out.println("Failed to remove " + target + " (" + e.getMessage() + ")");
                    continue;
                }

                if (Prefs.logLevel.getLevel() <= Prefs.Log.INFO.getLevel()) {
                    if (newState.removed.size() > currentState.removed.size()) {
                        Main.out.println("Removed " + target);
                        patchedNum++;
                    }
                    else
                        Main.out.println("Not found " + target);
                }
                currentState = newState;
            }

            if (DEBUG_BENCHMARK || DEBUG_NOT_WRITE)
                System.out.println("\rLoop takes " + (System.currentTimeMillis() - l) + " ms\n");
            else
                writeChanges(currentState);

            if (Prefs.logLevel.getLevel() <= Prefs.Log.INFO.getLevel())
                Main.out.println(patchedNum + " targets patched and " + errorsNum + " failed.");

        } while (DEBUG_BENCHMARK);
    }

    private void writeChanges(State state) {
        ArrayList<String> deletedList = new ArrayList<>(state.removed.size());
        for (SmaliTarget target : state.removed)
            deletedList.add(target.getGlobPath());

        RemoveFiles removeFiles = new RemoveFiles();
        removeFiles.setTargets(deletedList);
        try {
            removeFiles.apply(project, patch);
        } catch (IOException e) {
            Main.out.println(e.getMessage());
            Main.out.println("Congratulations! Is your project broken now? How did you do that?");
        }

        for (SmaliClass smaliClass : state.classes) {     //write changes
            smaliClass.getFile().setBody(smaliClass.getNewBody());
        }
    }

    private State remove(SmaliTarget target, State currentState) {
        State newState = new State(currentState);
        List<SmaliClass> classes = new SmaliFilter().separate(target, newState.files, newState.classes);
        if (!classes.isEmpty() || newState.files.size() != currentState.files.size())
            newState.removed.add(target);

        for (SmaliClass smaliClass : classes) {
            List<String> dependencies = smaliClass.clean(target);
            newState.classes.remove(smaliClass);
            newState.classes.add(smaliClass);

            for (String s : dependencies) {
                SmaliTarget sideTarget = new SmaliTarget();
                sideTarget.setRef(s);
                if (newState.removed.contains(sideTarget))
                    continue;

                if (Prefs.logLevel.getLevel() == Prefs.Log.DEBUG.getLevel())
                    Main.out.println("Also deleting " + sideTarget);

                newState.removed.add(sideTarget);
                newState = remove(sideTarget, newState);
            }
        }
        return newState;
    }

    private static class State {
        ArrayList<SmaliFile> files;
        ArrayList<SmaliClass> classes;
        HashSet<SmaliTarget> removed;

        State(ArrayList<SmaliFile> files, ArrayList<SmaliClass> classes, HashSet<SmaliTarget> removed) {
            this.files = files;
            this.classes = classes;
            this.removed = removed;
        }

        State(List<SmaliFile> files) {
            this.files = new ArrayList<>(files);
            this.classes = new ArrayList<>();
            this.removed = new HashSet<>();
        }

        State(State state) {
            this.files = state.files;
            this.classes = state.classes;
            this.removed = state.removed;
        }
    }
}
