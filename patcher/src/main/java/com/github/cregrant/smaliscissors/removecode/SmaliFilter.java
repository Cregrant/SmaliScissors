package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;
import com.github.cregrant.smaliscissors.removecode.classparts.ClassPart;
import com.github.cregrant.smaliscissors.util.ArraySplitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class SmaliFilter {

    private static final Logger logger = LoggerFactory.getLogger(SmaliFilter.class);
    private final Project project;
    private final ClassesPool pool;
    private final State currentState;
    private final List<Future<?>> futures;
    private final Set<SmaliFile> deletedFiles = new HashSet<>();
    private final Set<SmaliClass> result = Collections.synchronizedSet(new HashSet<SmaliClass>());
    private boolean allowStateChange;

    public SmaliFilter(Project project, ClassesPool pool, State currentState) {
        this.project = project;
        this.pool = pool;
        this.currentState = currentState;
        futures = new ArrayList<>();
    }

    Set<SmaliClass> separate(final SmaliTarget target) {
        allowStateChange = true;
        Set<SmaliFile> possibleTargetFiles = getPossibleTargetFiles(target);
        if (possibleTargetFiles.isEmpty()) {
            return Collections.emptySet();
        }

        filterPatchedClasses(target, possibleTargetFiles);
        removeTargetFiles(target, possibleTargetFiles);
        scanAllFilesPooled(target, possibleTargetFiles);
        logger.debug("Filtered {} files", currentState.files.size());
        return result;
    }

    Set<SmaliClass> separateDoNotChangeState(final SmaliTarget target) {
        allowStateChange = false;
        Set<SmaliFile> possibleTargetFiles = getPossibleTargetFiles(target);
        if (possibleTargetFiles.isEmpty()) {
            return Collections.emptySet();
        }

        filterPatchedClasses(target, possibleTargetFiles);
        removeTargetFiles(target, possibleTargetFiles);
        scanAllFilesPooled(target, possibleTargetFiles);
        logger.debug("Filtered {} files, no write", currentState.files.size());
        return result;
    }

    public Set<SmaliFile> getPossibleTargetFiles(final SmaliTarget target) {
        String ref = target.getRef();
        if (!target.isClass()) {
            String parentClassRef = target.getRef().substring(0, target.getRef().indexOf(';') + 1);
            if (currentState.removedTargets.contains(new SmaliTarget().setRef(parentClassRef))) {
                return Collections.emptySet();          //parent class already removed
            }
        } else if (currentState.removedTargets.contains(target)) {
            return Collections.emptySet();              //target already removed
        } else if (ref.contains("/")) {
            String rootFolder = ref.substring(1, ref.indexOf('/'));
            if (!pool.getRootFolders().contains(rootFolder)) {
                return Collections.emptySet();          //parent folder does not exist
            }
        }

        if (!target.isClass()) {
            String targetRef = target.getRef();
            ref = targetRef;
            int endPos = targetRef.indexOf(';');
            if (endPos != -1 && endPos != targetRef.length() - 1) {
                ref = targetRef.substring(0, targetRef.indexOf(';') + 1);
            }
        }
        boolean isSingleClass = target.getRef().endsWith(";");
        if (isSingleClass) {
            ArrayList<SmaliFile> files = pool.getMap().get(ref);    //probably null if this is an android framework field
            return files == null ? Collections.<SmaliFile>emptySet() : new HashSet<>(files);
        }

        final Set<SmaliFile> syncronizedSet = Collections.synchronizedSet(new HashSet<SmaliFile>());
        final Map.Entry<String, ArrayList<SmaliFile>>[] array = pool.getArray();
        final ArraySplitter splitter = new ArraySplitter(array, 2);
        final String finalClassRef = ref;

        while (splitter.hasNext()) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    int start = splitter.chunkStart();
                    int end = splitter.chunkEnd();
                    for (int i = start; i < end; i++) {
                        Map.Entry<String, ArrayList<SmaliFile>> entry = array[i];
                        if (entry.getKey().startsWith(finalClassRef)) {
                            syncronizedSet.addAll(entry.getValue());
                        }
                    }
                }
            };
            futures.add(project.getExecutor().submit(r));
        }
        project.getExecutor().waitForFinish(futures);
        futures.clear();
        if (syncronizedSet.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(syncronizedSet);
    }

    private void removeTargetFiles(final SmaliTarget target, Set<SmaliFile> possibleTargetFiles) {
        if (!target.isClass() || possibleTargetFiles.isEmpty()) {
            return;
        }

        deletedFiles.addAll(getTargetFiles(target, possibleTargetFiles));
        if (allowStateChange) {
            currentState.deletedFiles.addAll(deletedFiles);
            currentState.files.removeAll(deletedFiles);
        }
    }

    List<SmaliFile> getTargetFiles(final SmaliTarget target, Set<SmaliFile> possibleTargetFiles) {
        List<SmaliFile> filesFiltered = new ArrayList<>();
        for (SmaliFile file : possibleTargetFiles) {
            if (currentState.files.contains(file) && acceptPath(file.getPath(), target.getSkipPath())) {
                filesFiltered.add(file);
            }
        }
        return filesFiltered;
    }

    private void filterPatchedClasses(final SmaliTarget target, Set<SmaliFile> possibleTargetFiles) {
        HashSet<SmaliClass> acceptedClasses = new HashSet<>();
        for (SmaliFile possibleTargetFile : possibleTargetFiles) {
            final SmaliClass smaliClass = possibleTargetFile.getSmaliClass();
            if (smaliClass == null || !currentState.patchedClasses.contains(smaliClass)) {
                continue;
            }
            if (target.isClass()) {     //remove patched classes that are matching the target
                if (acceptPath(smaliClass.getFile().getPath(), target.getSkipPath())) {
                    acceptedClasses.add(smaliClass);
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
        if (allowStateChange) {
            currentState.patchedClasses.removeAll(acceptedClasses);
        }
        project.getExecutor().waitForFinish(futures);
        futures.clear();
    }

    private void scanAllFilesPooled(final SmaliTarget target, final Set<SmaliFile> possibleTargetFiles) {
        final Set<SmaliFile> acceptedFiles = Collections.synchronizedSet(new HashSet<SmaliFile>(100));
        final AtomicReference<Exception> error = new AtomicReference<>();
        if (!target.isClass() || !deletedFiles.isEmpty()) {
            for (final SmaliFile df : possibleTargetFiles) {
                if (currentState.files.contains(df) && (allowStateChange || !deletedFiles.contains(df))) {
                    scheduleFileScan(acceptedFiles, target, df, error);
                }
            }
            project.getExecutor().waitForFinish(futures);
            futures.clear();
            if (allowStateChange) {
                currentState.files.removeAll(acceptedFiles);
            }
        }

        if (allowStateChange) {
            currentState.patchedClasses.addAll(result);
        }
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

    private SmaliClass scanFile(SmaliFile file, SmaliTarget target) {
        String body = file.getBody();
        if (!body.contains(target.getRef())) {
            return null;
        }

        if (acceptStringBody(body, target)) {
            if (body.charAt(body.indexOf('\n') - 1) == '\r') {    //get rid of windows \r
                body = body.replace("\r", "");
            }
            return new SmaliClass(project, file, body);
        }
        return null;
    }
}
