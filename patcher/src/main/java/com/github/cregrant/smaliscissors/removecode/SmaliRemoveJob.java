package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.rule.types.RemoveCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class SmaliRemoveJob {

    private static final Logger logger = LoggerFactory.getLogger(SmaliRemoveJob.class);
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
                    if (!rule.isInternal()) {
                        logger.debug("Cleaning {}", smaliClass);
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
                    logger.debug("", exception.get());
                    throw exception.get();
                }

                for (SmaliTarget dependency : dependencies) {
                    newTargets.add(dependency);
                    if (!rule.isInternal()) {
                        logger.debug("Also deleting {}", dependency);
                    }
                }
            }
            currentTargets = newTargets;
        } while (!currentTargets.isEmpty());
    }

    boolean containsTargetFiles(SmaliTarget target, State state) {
        return !new SmaliFilter(project, pool, state).getPossibleTargetFiles(target).isEmpty();
    }

    public boolean isStateModified() {
        return stateModified;
    }
}
