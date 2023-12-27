package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Patch;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.removecode.classparts.ClassMethod;
import com.github.cregrant.smaliscissors.removecode.classparts.ClassPart;
import com.github.cregrant.smaliscissors.rule.types.RemoveCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

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
        Set<SmaliTarget> currentTargets = new HashSet<>();
        currentTargets.add(initialTarget);

        while (!currentTargets.isEmpty()) {
            HashSet<SmaliTarget> newTargets = new HashSet<>();
            for (final SmaliTarget target : currentTargets) {
                newTargets.addAll(removeTarget(state, target));
            }
            currentTargets = newTargets;

            if (currentTargets.isEmpty()) {
                currentTargets = findNullFields(state);
            }
        }
    }

    private Set<SmaliTarget> findNullFields(final State state) {
        if (fieldsCanBeNull.isEmpty()) {
            return Collections.emptySet();
        }
        HashSet<SmaliTarget> nullFields = new HashSet<>();
        for (final SmaliTarget targetField : fieldsCanBeNull) {
            String fieldRef = targetField.getRef();
            Set<SmaliClass> classes = new SmaliFilter(project, pool, state).separateDoNotChangeState(targetField);
            boolean getterExists = false;
            boolean setterExists = false;
            for (SmaliClass smaliClass : classes) {
                for (ClassPart part : smaliClass.getBodyParts()) {
                    if (!(part instanceof ClassMethod)) {
                        continue;
                    }
                    String body = ((ClassMethod) part).getBody();
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
            }
            if (getterExists && !setterExists) {
                logger.debug("Also removing null field {}", targetField);
                nullFields.add(targetField);
            }
        }
        fieldsCanBeNull.clear();
        return nullFields;
    }

    private List<SmaliTarget> removeTarget(State state, final SmaliTarget target) throws Exception {
        Set<SmaliClass> classes = new SmaliFilter(project, pool, state).separate(target);
        if (classes.isEmpty()) {
            return Collections.emptyList();
        }
        logger.debug("Started removing {}", target);
        stateModified = true;
        state.removedTargets.add(target);

        final List<SmaliTarget> dependencies = Collections.synchronizedList(new ArrayList<SmaliTarget>());
        List<Future<?>> futures = new ArrayList<>(classes.size());
        final AtomicReference<Exception> exception = new AtomicReference<>();
        for (final SmaliClass smaliClass : classes) {
            if (smaliClass.getRef().endsWith("Registrar;")) {
                project.getSmaliKeeper().changeFirebaseAnalytics(patch, rule);
                throw new IllegalStateException("preventing app crash due to firebase errors");
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
            logger.debug("removeTarget failed", exception.get());
            throw exception.get();
        }

        if (!rule.isInternal()) {
            for (SmaliTarget dependency : dependencies) {
                logger.debug("Also removing {}", dependency);
            }
        }
        return dependencies;
    }

    public boolean isStateModified() {
        return stateModified;
    }
}
