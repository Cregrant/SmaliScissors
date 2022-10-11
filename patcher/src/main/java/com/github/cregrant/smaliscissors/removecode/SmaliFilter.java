package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class SmaliFilter {
    private final Project project;
    private final State currentState;
    private final List<Future<?>> futures;
    private final Set<SmaliFile> deletedFiles;
    private final Set<SmaliClass> result;

    public SmaliFilter(Project project, State currentState) {
        this.project = project;
        this.currentState = currentState;
        futures = new ArrayList<>(currentState.files.size());
        deletedFiles = new HashSet<>(100);
        result = Collections.synchronizedSet(new HashSet<SmaliClass>(100));
    }

    Set<SmaliClass> separate(final SmaliTarget target) {
        processPatchedClasses(target);
        removeTargetFiles(target);
        scanAllFiles(target);
        return result;
    }

    private void scanAllFiles(final SmaliTarget target) {
        final Set<SmaliFile> acceptedFiles = Collections.synchronizedSet(new HashSet<SmaliFile>(100));
        final AtomicReference<Exception> error = new AtomicReference<>();
        if (!target.isClass() || !deletedFiles.isEmpty()) {
            for (final SmaliFile df : currentState.files) {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        String body = df.getBody();
                        if (!body.contains(target.getRef()) || error.get() != null) {
                            return;
                        }

                        if (body.charAt(body.indexOf('\n') - 1) == '\r') {    //get rid of windows \r
                            body = body.replace("\r", "");
                        }

                        if (acceptBody(body, target)) {
                            acceptedFiles.add(df);
                            try {
                                result.add(new SmaliClass(df, body));
                            } catch (Exception e) {
                                error.set(e);
                            }
                        }
                    }
                };
                futures.add(project.getExecutor().submit(r));
            }
        }
        project.getExecutor().compute(futures);
        currentState.files.removeAll(acceptedFiles);
        if (error.get() != null) {
            throw new RuntimeException(error.get());
        }
    }

    private void removeTargetFiles(SmaliTarget target) {
        if (target.isClass()) {
            for (SmaliFile df : currentState.files) {
                if (acceptPath(df.getPath(), target.getSkipPath())) {
                    deletedFiles.add(df);
                }
            }
            currentState.files.removeAll(deletedFiles);
            currentState.deletedFiles.addAll(deletedFiles);
        }
    }

    private void processPatchedClasses(final SmaliTarget target) {
        for (Iterator<SmaliClass> iterator = currentState.patchedClasses.iterator(); iterator.hasNext(); ) {
            final SmaliClass smaliClass = iterator.next();
            if (target.isClass()) {
                if (acceptPath(smaliClass.getFile().getPath(), target.getSkipPath())) {
                    iterator.remove();
                    deletedFiles.add(smaliClass.getFile());
                    continue;
                }
            }

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    if (acceptClassBody(smaliClass, target)) {
                        result.add(smaliClass);
                    }
                }
            };
            futures.add(project.getExecutor().submit(r));
        }
        project.getExecutor().compute(futures);
        futures.clear();
    }

    private boolean acceptStringBody(String body, SmaliTarget target) {
        return target.containsInside(body);
    }

    private boolean acceptClassBody(SmaliClass smaliClass, SmaliTarget target) {
        for (ClassPart part : smaliClass.getBodyParts()) {
            if (target.containsInside(part.getText())) {
                return true;
            }
        }
        return false;
    }

    private boolean acceptPath(String path, String target) {
        int pos = path.indexOf('/') + 1;
        return path.startsWith(target, pos);
    }
}
