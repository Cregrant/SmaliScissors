package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Prefs;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.rule.types.RemoveCode;
import com.github.cregrant.smaliscissors.util.Misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class SmaliRemoveJob {
    private final Project project;
    private final ClassesPool pool;
    private final Patch patch;
    private final RemoveCode rule;
    private boolean stateModified;

    public SmaliRemoveJob(Project project, ClassesPool pool, Patch patch, RemoveCode rule) {
        this.project = project;
        this.pool = pool;
        this.patch = patch;
        this.rule = rule;
    }

    void remove(SmaliTarget initialTarget, State state) throws Exception {
        List<SmaliTarget> currentTargets = new ArrayList<>();
        currentTargets.add(initialTarget);
        do {
            List<SmaliTarget> newTargets = new ArrayList<>();
            for (final SmaliTarget target : currentTargets) {
                Set<SmaliClass> classes = new SmaliFilter(project, pool, state).separate(target);
                if (classes.isEmpty()) {
                    continue;
                }
                stateModified = true;
                state.removedTargets.add(target);

                final List<SmaliTarget> dependencies = Collections.synchronizedList(new ArrayList<SmaliTarget>());
                List<Future<?>> futures = new ArrayList<>(classes.size());
                final AtomicReference<Exception> exception = new AtomicReference<>();
                for (final SmaliClass smaliClass : classes) {
                    if (smaliClass.getRef().endsWith("Registrar;")) {
                        project.getSmaliKeeper().changeFirebaseAnalytics(patch, rule);
                        throw new IllegalStateException("Skipped to prevent some firebase errors.");
                    }
                    if (!rule.isInternal() && Prefs.logLevel.getLevel() == Prefs.Log.DEBUG.getLevel()) {
                        Main.out.println("Cleaning " + smaliClass);
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
                project.getExecutor().waitForFinish(futures);

                if (exception.get() != null) {
                    if (Prefs.logLevel.getLevel() == Prefs.Log.DEBUG.getLevel()) {
                        Main.out.println(Misc.stacktraceToString(exception.get()));
                    }
                    throw exception.get();
                }

                for (SmaliTarget dependency : dependencies) {
                    if (!dependency.isClass()) {
                        String parentClassRef = dependency.getRef().substring(0, dependency.getRef().indexOf(';') + 1);
                        if (state.removedTargets.contains(new SmaliTarget().setRef(parentClassRef))) {
                            continue;
                        }
                    } else if (state.removedTargets.contains(dependency)) {
                        continue;
                    }

                    newTargets.add(dependency);
                    if (!rule.isInternal() && Prefs.logLevel.getLevel() == Prefs.Log.DEBUG.getLevel()) {
                        Main.out.println("Also deleting " + dependency);
                    }
                }
                state.patchedClasses.removeAll(classes);
                state.patchedClasses.addAll(classes);
            }
            currentTargets = newTargets;
        } while (!currentTargets.isEmpty());
    }

    public boolean isStateModified() {
        return stateModified;
    }
}
