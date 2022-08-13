package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class SmaliFilter {

    Set<SmaliClass> separate(final Project project, final SmaliTarget target, SmaliWorker.State currentState) {
        final Set<SmaliClass> result = Collections.synchronizedSet(new HashSet<SmaliClass>(100));    //from files and classes
        List<Future<?>> futures = new ArrayList<>(currentState.files.size());

        Set<SmaliFile> deletedFiles = new HashSet<>(100);
        Set<SmaliClass> removedClasses = Collections.synchronizedSet(new HashSet<SmaliClass>(100));
        final Set<SmaliFile> acceptedFiles = Collections.synchronizedSet(new HashSet<SmaliFile>(100));

        for (final SmaliClass smaliClass : currentState.patchedClasses) {
            if (!target.isMethod() && target.isDeletionAllowed()) {
                if (!acceptPath(smaliClass.getFile().getPath(), target.getSkipPath())) {
                    removedClasses.add(smaliClass);
                    deletedFiles.add(smaliClass.getFile());
                    continue;
                }
            }

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    if (SmaliFilter.this.acceptBody(smaliClass.getNewBody(), target)) {
                        result.add(smaliClass);
                    }
                }
            };
            futures.add(project.getExecutor().submit(r));
        }
        currentState.patchedClasses.removeAll(removedClasses);

        final AtomicReference<Exception> error = new AtomicReference<>();
        if (!target.isMethod() && target.isDeletionAllowed()) {
            for (SmaliFile df : currentState.files) {
                if (!acceptPath(df.getPath(), target.getSkipPath())) {
                    deletedFiles.add(df);
                }
            }
        }
        currentState.files.removeAll(deletedFiles);
        currentState.deletedFiles.addAll(deletedFiles);

        if (target.isMethod() || !deletedFiles.isEmpty()) {
            for (final SmaliFile df : currentState.files) {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        String body = df.getBody();
                        if (!body.contains(target.getRef()) || error.get() != null) {
                            return;
                        }

                        if (body.charAt(body.indexOf('\n') - 1) == '\r')    //get rid of windows \r
                        {
                            body = body.replace("\r", "");
                        }

                        if (SmaliFilter.this.acceptBody(body, target)) {
                            acceptedFiles.add(df);
                            try {
                                result.add(new SmaliClass(project, df, body));
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
        if (error.get() != null) {
            throw new RuntimeException(error.get());
        }
        currentState.files.removeAll(acceptedFiles);
        return result;
    }

    private boolean acceptBody(String body, SmaliTarget target) {
        return target.containsInside(body);
    }

    private boolean acceptPath(String path, String target) {
        int pos = path.indexOf('/') + 1;
        return !path.startsWith(target, pos);
    }
}
