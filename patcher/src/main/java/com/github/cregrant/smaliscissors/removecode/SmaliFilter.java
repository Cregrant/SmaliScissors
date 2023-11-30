package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.DecompiledFile;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;
import com.github.cregrant.smaliscissors.removecode.classparts.ClassPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmaliFilter {

    private static final Logger logger = LoggerFactory.getLogger(SmaliFilter.class);
    private final Project project;
    private final ClassesPool pool;
    private final State currentState;
    private final List<Future<?>> futures;
    private final Set<SmaliFile> deletedFiles = new HashSet<>(100);
    private final Set<SmaliClass> result = Collections.synchronizedSet(new HashSet<SmaliClass>(100));

    public SmaliFilter(Project project, ClassesPool pool, State currentState) {
        this.project = project;
        this.pool = pool;
        this.currentState = currentState;
        futures = new ArrayList<>(currentState.files.size());
    }

    Set<SmaliClass> separate(final SmaliTarget target) {
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

    public Set<SmaliFile> getPossibleTargetFiles(final SmaliTarget target) {
        if (!target.isClass()) {
            String parentClassRef = target.getRef().substring(0, target.getRef().indexOf(';') + 1);
            if (currentState.removedTargets.contains(new SmaliTarget().setRef(parentClassRef))) {
                return Collections.emptySet();          //parent class already removed
            }
        } else if (currentState.removedTargets.contains(target)) {
            return Collections.emptySet();              //target already removed
        }

        String classRef = target.getRef();
        if (!target.isClass()) {
            String targetRef = target.getRef();
            classRef = targetRef;
            int endPos = targetRef.indexOf(';');
            if (endPos != -1 && endPos != targetRef.length() - 1) {
                classRef = targetRef.substring(0, targetRef.indexOf(';') + 1);
            }
        }
        HashSet<SmaliFile> filesToScan = new HashSet<>(100);
        for (Map.Entry<String, ArrayList<SmaliFile>> entry : pool.getArray()) {
            if (entry.getKey().startsWith(classRef)) {
                filesToScan.addAll(entry.getValue());
            }
        }
        return filesToScan;
    }

    private void removeTargetFiles(final SmaliTarget target, Set<SmaliFile> possibleTargetFiles) {
        if (!target.isClass() || possibleTargetFiles.isEmpty()) {
            return;
        }

        deletedFiles.addAll(getTargetFiles(target, possibleTargetFiles));
        currentState.deletedFiles.addAll(deletedFiles);
        currentState.files.removeAll(deletedFiles);
    }

    List<SmaliFile> getTargetFiles(final SmaliTarget target, Set<SmaliFile> possibleTargetFiles) {
        List<SmaliFile> filesFiltered = new ArrayList<>();
        for (SmaliFile file : currentState.files) {
            if (possibleTargetFiles.contains(file) && acceptPath(file.getPath(), target.getSkipPath())) {
                filesFiltered.add(file);
            }
        }
        return filesFiltered;
    }

    private void filterPatchedClasses(final SmaliTarget target, Set<SmaliFile> possibleTargetFiles) {
        for (Iterator<SmaliClass> iterator = currentState.patchedClasses.iterator(); iterator.hasNext(); ) {
            final SmaliClass smaliClass = iterator.next();
            if (!possibleTargetFiles.contains(smaliClass.getFile())) {
                continue;
            }

            if (target.isClass()) {     //remove patched classes that are matching the target
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

    private void scanAllFilesPooled(final SmaliTarget target, final Set<SmaliFile> possibleTargetFiles) {
        final Set<SmaliFile> acceptedFiles = Collections.synchronizedSet(new HashSet<SmaliFile>(100));
        final AtomicReference<Exception> error = new AtomicReference<>();
        if (!target.isClass() || !deletedFiles.isEmpty()) {
            for (final SmaliFile df : possibleTargetFiles) {
                if (currentState.files.contains(df)) {
                    scheduleFileScan(acceptedFiles, target, df, error);
                }
            }
            project.getExecutor().waitForFinish(futures);
            futures.clear();
            currentState.files.removeAll(acceptedFiles);
        }

        currentState.patchedClasses.addAll(result);
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

    static SmaliTarget getClassRef(DecompiledFile dFile, Pattern match) {
        String smaliBody = dFile.getBody();
        Matcher matcher = match.matcher(smaliBody);
        if (matcher.find()) {
            logger.debug("Found REMOVE_CODE match {} in {}", matcher.group(0), dFile.getPath());
            int start = matcher.start(0);
            if (start == -1) {
                return null;
            }
            return extractClassRef(smaliBody, start);
        }
        return null;
    }

    private static SmaliTarget extractClassRef(String body, int start) {
        String lineSeparator = body.charAt(body.indexOf('\n') - 1) == '\r' ? "\r\n" : "\n";
        int startMethod = body.lastIndexOf("\n.method ", start);
        int endMethod = body.indexOf("\n.end method" + lineSeparator, startMethod);
        if (startMethod == -1 || endMethod == -1 || endMethod < startMethod) {
            return null;
        }
        String classPath = body.substring(body.indexOf(" L") + 1, body.indexOf(";") + 1);
        return new SmaliTarget().setRef(classPath);
    }

}
