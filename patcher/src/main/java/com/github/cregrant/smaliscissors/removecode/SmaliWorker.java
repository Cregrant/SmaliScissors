package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Prefs;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;
import com.github.cregrant.smaliscissors.rule.types.RemoveCode;
import com.github.cregrant.smaliscissors.rule.types.RemoveFiles;
import com.github.cregrant.smaliscissors.util.Misc;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class SmaliWorker {
    private static final boolean DEBUG_BENCHMARK = false;
    private static final boolean DEBUG_NOT_WRITE = false;
    private final Project project;
    private final Patch patch;
    private final RemoveCode rule;

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

            for (String path : rule.getTargets()) {
                SmaliTarget target = new SmaliTarget().setSkipPath(path);

                State newState;
                try {
                    newState = remove(target, currentState);
                } catch (Exception e) {
                    errorsNum++;
                    Main.out.println("Failed to remove " + target + " (" + e.getMessage() + ")");
                    continue;
                }

                //print only user defined rules
                if (rule.isInternal()) {
                    if (newState.removedTargets.size() > currentState.removedTargets.size()) {
                        crashReportersNum++;
                    }
                } else if (Prefs.logLevel.getLevel() <= Prefs.Log.INFO.getLevel()) {
                    if (newState.removedTargets.size() > currentState.removedTargets.size()) {
                        Main.out.println("Removed " + target);
                        patchedNum++;
                    } else {
                        //Main.out.println("Not found " + target);
                    }
                }
                currentState = newState;
            }
            project.getSmaliKeeper().keepClasses(currentState);

            if (DEBUG_BENCHMARK || DEBUG_NOT_WRITE) {
                System.out.println("\rLoop takes " + (System.currentTimeMillis() - l) + " ms\n");
            } else {
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

    private State remove(SmaliTarget initialTarget, State currentState) throws Exception {
        State newState = new State(currentState);
        List<SmaliTarget> currentTargets = new ArrayList<>();
        currentTargets.add(initialTarget);
        do {
            List<SmaliTarget> newTargets = new ArrayList<>();
            for (final SmaliTarget target : currentTargets) {
                Set<SmaliClass> classes = new SmaliFilter(project, newState).separate(target);
                if (classes.isEmpty()) {
                    continue;
                }
                newState.removedTargets.add(target);

                final List<SmaliTarget> dependencies = Collections.synchronizedList(new ArrayList<SmaliTarget>());
                List<Future<?>> futures = new ArrayList<>(classes.size());
                final AtomicReference<Exception> exception = new AtomicReference<>();
                for (final SmaliClass smaliClass : classes) {
                    if (smaliClass.getRef().endsWith("Registrar;")) {
                        project.getSmaliKeeper().changeFirebaseAnalytics(patch, rule);
                        throw new IllegalStateException("Skipped to prevent some firebase errors.");
                    }
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (exception.get() == null) {
                                    dependencies.addAll(smaliClass.clean(target));
                                }
                            } catch (Exception e) {
                                exception.set(e);
                            }

                        }
                    };
                    futures.add(project.getExecutor().submit(r));
                }
                project.getExecutor().compute(futures);

                if (exception.get() != null) {
                    if (Prefs.logLevel.getLevel() == Prefs.Log.DEBUG.getLevel()) {
                        Main.out.println(Misc.stacktraceToString(exception.get()));
                    }
                    throw exception.get();
                }

                for (SmaliTarget dependency : dependencies) {
                    if (!dependency.isClass()) {
                        String parentClassRef = dependency.getRef().substring(0, dependency.getRef().indexOf(';'));
                        if (newState.removedTargets.contains(new SmaliTarget().setRef(parentClassRef))) {
                            continue;
                        }
                    } else if (newState.removedTargets.contains(dependency)) {
                        continue;
                    }

                    newTargets.add(dependency);
                    if (!rule.isInternal() && Prefs.logLevel.getLevel() == Prefs.Log.DEBUG.getLevel()) {
                        Main.out.println("Also deleting " + dependency);
                    }
                }
                newState.patchedClasses.removeAll(classes);
                newState.patchedClasses.addAll(classes);
            }
            currentTargets = newTargets;
        } while (!currentTargets.isEmpty());

        return newState;
    }

    static class State {
        HashSet<SmaliFile> files;
        HashSet<SmaliFile> deletedFiles;
        HashSet<SmaliClass> patchedClasses;
        HashSet<SmaliTarget> removedTargets;

        State(List<SmaliFile> files) {
            this.files = new HashSet<>(files);
            this.deletedFiles = new HashSet<>();
            this.patchedClasses = new HashSet<>();
            this.removedTargets = new HashSet<>();
        }

        State(State state) {
            this.files = new HashSet<>(state.files);
            this.deletedFiles = new HashSet<>(state.deletedFiles);
            this.patchedClasses = new HashSet<>(state.patchedClasses);
            this.removedTargets = new HashSet<>(state.removedTargets);
        }
    }
}
