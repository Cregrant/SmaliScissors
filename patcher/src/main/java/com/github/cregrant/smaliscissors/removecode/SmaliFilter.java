package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;
import com.github.cregrant.smaliscissors.removecode.classparts.ClassPart;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class SmaliFilter {
    private final Project project;
    private final ClassesPool pool;
    private final State currentState;
    private final List<Future<?>> futures;
    private final Set<SmaliFile> deletedFiles;
    private final Set<SmaliClass> result;

    public SmaliFilter(Project project, ClassesPool pool, State currentState) {
        this.project = project;
        this.pool = pool;
        this.currentState = currentState;
        futures = new ArrayList<>(currentState.files.size());
        deletedFiles = new HashSet<>(100);
        result = Collections.synchronizedSet(new HashSet<SmaliClass>(100));
    }

    Set<SmaliClass> separate(final SmaliTarget target) {
        processPatchedClasses(target);
        removeTargetFiles(target);
        if (!pool.getMap().isEmpty()) {
            scanAllFilesPooled(target);
        } else {
            scanAllFiles(target);
        }
        return result;
    }

    private void scanAllFilesPooled(final SmaliTarget target) {
        final Set<SmaliFile> acceptedFiles = Collections.synchronizedSet(new HashSet<SmaliFile>(100));
        final AtomicReference<Exception> error = new AtomicReference<>();
        if (!target.isClass() || !deletedFiles.isEmpty()) {
            String targetRef = target.getRef();
            String classRef = targetRef;
            int endPos = targetRef.indexOf(';');
            if (endPos != -1 && endPos != targetRef.length() - 1) {
                classRef = targetRef.substring(0, targetRef.indexOf(';') + 1);
            }

            Set<SmaliFile> scheduledSet = new HashSet<SmaliFile>(100);
            for (Map.Entry<String, ArrayList<SmaliFile>> entry : pool.getMap().entrySet()) {
                if (entry.getKey().startsWith(classRef)) {          //get all classes that call classRef
                    for (SmaliFile file : entry.getValue()) {
                        if (currentState.files.contains(file)) {    //accept if it hasn't been loaded yet
                            scheduledSet.add(file);
                        }
                    }
                }
            }
            for (final SmaliFile df : scheduledSet) {
                scheduleFileScan(acceptedFiles, target, df, error);
            }
        }
        project.getExecutor().waitForFinish(futures);
        futures.clear();
        currentState.files.removeAll(acceptedFiles);
        if (error.get() != null) {
            throw new RuntimeException(error.get());
        }
    }

    private void scheduleFileScan(final Set<SmaliFile> acceptedFiles, final SmaliTarget target, final SmaliFile df, final AtomicReference<Exception> error) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    SmaliClass smaliClass = scanFile(df, target);
                    if (smaliClass != null && error.get() == null) {
                        acceptedFiles.add(df);
                        result.add(smaliClass);
                    }
                } catch (Exception e) {
                    error.set(e);
                }
            }
        };
        futures.add(project.getExecutor().submit(r));
    }

    private void scanAllFiles(final SmaliTarget target) {
        final Set<SmaliFile> acceptedFiles = Collections.synchronizedSet(new HashSet<SmaliFile>(100));
        final AtomicReference<Exception> error = new AtomicReference<>();
        if (!target.isClass() || !deletedFiles.isEmpty()) {
            for (final SmaliFile df : currentState.files) {
                scheduleFileScan(acceptedFiles, target, df, error);
            }
        }
        project.getExecutor().waitForFinish(futures);
        futures.clear();
        currentState.files.removeAll(acceptedFiles);
        if (error.get() != null) {
            throw new RuntimeException(error.get());
        }
    }

    private void removeTargetFiles(final SmaliTarget target) {
        if (target.isClass()) {
            final SmaliFile[] smaliFilesArray;
            if (currentState.filesArray == null) {
                smaliFilesArray = currentState.files.toArray(new SmaliFile[0]);
            } else {
                smaliFilesArray = currentState.filesArray;
            }

            final Set<SmaliFile> synchronizedDeletedFiles = Collections.synchronizedSet(new HashSet<SmaliFile>());
            int chunkCount = Runtime.getRuntime().availableProcessors();
            final int chunkSize = smaliFilesArray.length / chunkCount + chunkCount * 2;

            for (int i = 0; i < chunkCount; i++) {
                final int finalI = i;
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        int start = finalI * chunkSize;
                        int end = (finalI + 1) * chunkSize;
                        acceptFilesPathRanged(synchronizedDeletedFiles, smaliFilesArray, target.getSkipPath(), start, end);
                    }
                };
                futures.add(project.getExecutor().submit(r));
            }
            project.getExecutor().waitForFinish(futures);
            futures.clear();

            deletedFiles.addAll(synchronizedDeletedFiles);
            boolean filesChanged = currentState.files.removeAll(deletedFiles);
            currentState.deletedFiles.addAll(deletedFiles);

            if (currentState.filesArray != null && filesChanged) {
                currentState.filesArray = currentState.files.toArray(new SmaliFile[0]);
            }
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
        project.getExecutor().waitForFinish(futures);
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

    private void acceptFilesPathRanged(Set<SmaliFile> set, SmaliFile[] smaliFilesArray, String target, int start, int end) {
        end = Math.min(end, currentState.files.size());
        for (int i = start; i < end; i++) {
            SmaliFile file = smaliFilesArray[i];
            if (acceptPath(file.getPath(), target)) {
                set.add(file);
            }
        }
    }

    private boolean acceptPath(String path, String target) {
        int pos = path.indexOf('/') + 1;
        return path.startsWith(target, pos);
    }

    private SmaliClass scanFile(SmaliFile file, SmaliTarget target) {
        String body = file.getBody();
        if (!body.contains(target.getRef())) {
            return null;
        }

        if (body.charAt(body.indexOf('\n') - 1) == '\r') {    //get rid of windows \r
            body = body.replace("\r", "");
        }

        if (acceptStringBody(body, target)) {
            return new SmaliClass(project, file, body);
        }
        return null;
    }
}
