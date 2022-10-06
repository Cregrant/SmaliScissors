package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Prefs;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;
import com.github.cregrant.smaliscissors.rule.types.RemoveCode;
import com.github.cregrant.smaliscissors.rule.types.RemoveFiles;

import java.io.IOException;
import java.util.ArrayList;

public class SmaliWorker {
    private static final boolean DEBUG_BENCHMARK = false;
    private static final boolean DEBUG_NOT_WRITE = false;
    private final Project project;
    private final Patch patch;
    private final RemoveCode rule;
    private int bestLoopTime = Integer.MAX_VALUE;

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
            int crashReportersNum = 0;
            project.getSmaliKeeper().changeTargets(patch, rule);
            State newState = new State(currentState);

            for (String path : rule.getTargets()) {
                SmaliTarget target = new SmaliTarget().setSkipPath(path);

                SmaliRemoveJob job = new SmaliRemoveJob(project, patch, rule);
                try {
                    job.remove(target, newState);
                } catch (Exception e) {
                    errorsNum++;
                    Main.out.println("Failed to remove " + target + " (" + e.getMessage() + ")");
                    newState = new State(currentState);
                    continue;
                }

                //print only user defined rules
                if (rule.isInternal()) {
                    if (job.isStateModified()) {
                        crashReportersNum++;
                    }
                } else if (job.isStateModified() && Prefs.logLevel.getLevel() <= Prefs.Log.INFO.getLevel()) {
                    Main.out.println("Removed " + target);
                    patchedNum++;
                }
                if (job.isStateModified()) {
                    currentState = newState;
                    newState = new State(currentState);
                }
            }
            project.getSmaliKeeper().keepClasses(currentState);

            if (DEBUG_BENCHMARK) {
                int loopTime = (int) (System.currentTimeMillis() - l);
                bestLoopTime = Math.min(loopTime, bestLoopTime);
                System.out.println("\rLoop takes " + loopTime + " ms (" + bestLoopTime + " ms best)\n");
            } else if (!DEBUG_NOT_WRITE) {
                writeChanges(currentState);
            }

            if (rule.isInternal()) {
                if (crashReportersNum > 0) {
                    Main.out.println(crashReportersNum + " crash reporters deleted silently.");
                }
            } else if (Prefs.logLevel.getLevel() <= Prefs.Log.INFO.getLevel()) {
                Main.out.println(patchedNum + " targets patched and " + errorsNum + " failed.");
            }

        } while (DEBUG_BENCHMARK);
    }

    private void writeChanges(State state) {
        ArrayList<String> deletedList = new ArrayList<>(state.removedTargets.size());
        if (!state.deletedFiles.isEmpty()) {
            for (SmaliFile file : state.deletedFiles) {
                deletedList.add(file.getPath());
            }

            RemoveFiles removeFiles = new RemoveFiles();
            removeFiles.setTargets(deletedList);
            try {
                removeFiles.apply(project, patch);
            } catch (IOException e) {
                Main.out.println(e.getMessage());
                Main.out.println("Congratulations! Is your project broken now? How did you do that?");
            }
        }

        for (SmaliClass smaliClass : state.patchedClasses) {     //write changes
            smaliClass.getFile().setBody(smaliClass.getNewBody());
        }
    }

}
