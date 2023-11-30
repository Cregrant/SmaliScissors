package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.DecompiledFile;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;
import com.github.cregrant.smaliscissors.rule.types.RemoveCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class SmaliRemoveJob {

    private static final Logger logger = LoggerFactory.getLogger(SmaliRemoveJob.class);
    private final Project project;
    private final ClassesPool pool;
    private final Patch patch;
    private final RemoveCode rule;
    private final Set<SmaliTarget> fieldsCanBeNull = Collections.synchronizedSet(new HashSet<SmaliTarget>());
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

        logger.debug("Started removing {}", initialTarget);
        while (!currentTargets.isEmpty()) {
            List<SmaliTarget> newTargets = new ArrayList<>();
            for (final SmaliTarget target : currentTargets) {
                newTargets.addAll(removeTarget(state, target));
            }
            currentTargets.clear();
            currentTargets.addAll(newTargets);

            if (currentTargets.isEmpty()) {
                currentTargets = findNullFields(state);
            }
        }
    }

    public void removeIfMatch(String matchTarget, State state) throws Exception {
        logger.debug("Started removing all that match {}", matchTarget);
        final List<SmaliTarget> results = getMatchedTargets(matchTarget);
        for (SmaliTarget target : results) {
            remove(target, state);
        }
    }

    private List<SmaliTarget> getMatchedTargets(String matchTarget) {
        final List<SmaliTarget> results = Collections.synchronizedList(new ArrayList<SmaliTarget>());
        final Pattern localMatchCompiled = Pattern.compile(patch.applyAssign(matchTarget));
        ArrayList<SmaliFile> files = project.getSmaliList();
        ArrayList<Future<?>> futures = new ArrayList<>(files.size());

        for (final DecompiledFile dFile : files) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    SmaliTarget classRef = SmaliFilter.getClassRef(dFile, localMatchCompiled);
                    if (classRef != null) {
                        results.add(classRef);
                    }
                }
            };
            futures.add(project.getExecutor().submit(r));
        }
        project.getExecutor().waitForFinish(futures);
        return results;
    }

    private List<SmaliTarget> findNullFields(final State state) {
        if (fieldsCanBeNull.isEmpty()) {
            return Collections.emptyList();
        }
        List<SmaliTarget> result = Collections.synchronizedList(new ArrayList<SmaliTarget>());
        for (final SmaliTarget targetField : fieldsCanBeNull) {
            State clonedState = new State(state);
            String fieldRef = targetField.getRef();
            Set<SmaliClass> classes = new SmaliFilter(project, pool, clonedState).separate(targetField);
            boolean getterExists = false;
            boolean setterExists = false;
            for (SmaliClass smaliClass : classes) {
                String body = smaliClass.getNewBody();
                int pos = body.indexOf(fieldRef);
                while (pos >= 0) {
                    char c;
                    int backPos = pos;

                    do {
                        c = body.charAt(--backPos);
                    } while (backPos > 0 && !(c == '\n' || c == '#' || c == '\"'));
                    if (c == '\n') {
                        getterExists = getterExists || body.startsWith("get", backPos + 6);
                        setterExists = setterExists || body.startsWith("put", backPos + 6);
                    }
                    if (getterExists && setterExists) {
                        break;
                    }
                    pos = body.indexOf(fieldRef, pos + 5);
                }
            }
            if (getterExists && !setterExists) {
                logger.debug("Also removing null field {}", targetField);
                result.add(targetField);
            }
        }
        fieldsCanBeNull.clear();
        return result;
    }

    private List<SmaliTarget> removeTarget(State state, final SmaliTarget target) throws Exception {
        Set<SmaliClass> classes = new SmaliFilter(project, pool, state).separate(target);
        if (classes.isEmpty()) {
            return Collections.emptyList();
        }
        stateModified = true;
        state.removedTargets.add(target);

        final List<SmaliTarget> dependencies = Collections.synchronizedList(new ArrayList<SmaliTarget>());
        List<Future<?>> futures = new ArrayList<>(classes.size());
        final AtomicReference<Exception> exception = new AtomicReference<>();
        for (final SmaliClass smaliClass : classes) {
            if (smaliClass.getRef().endsWith("Registrar;")) {
                project.getSmaliKeeper().changeFirebaseAnalytics(patch, rule);
                throw new IllegalStateException("causes app crash due to firebase errors");
            }
            if (!rule.isInternal()) {
                logger.debug("Cleaning {}", smaliClass);
            }
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (exception.get() == null) {
                            SmaliCleanResult result = smaliClass.clean(target);
                            dependencies.addAll(result.cascadeTargets);
                            fieldsCanBeNull.addAll(result.fieldsCanBeNull);
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

        List<SmaliTarget> cascadeTargets = new ArrayList<>();
        for (SmaliTarget dependency : dependencies) {
            cascadeTargets.add(dependency);
            if (!rule.isInternal()) {
                logger.debug("Also removing {}", dependency);
            }
        }
        return cascadeTargets;
    }

    boolean containsTargetFiles(SmaliTarget target, State state) {
        return !new SmaliFilter(project, pool, state).getPossibleTargetFiles(target).isEmpty();
    }

    public boolean isStateModified() {
        return stateModified;
    }
}
