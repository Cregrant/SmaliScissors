package com.github.cregrant.smaliscissors.smali;

import com.github.cregrant.smaliscissors.BackgroundWorker;
import com.github.cregrant.smaliscissors.structures.common.SmaliFile;
import com.github.cregrant.smaliscissors.structures.smali.SmaliClass;
import com.github.cregrant.smaliscissors.structures.smali.SmaliTarget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class SmaliFilter {

    List<SmaliClass> separate(SmaliTarget target, List<SmaliFile> smaliFiles, List<SmaliClass> smaliClasses) {
        List<SmaliClass> filtered = Collections.synchronizedList(new ArrayList<>(100));
        List<SmaliFile> removedFiles = Collections.synchronizedList(new ArrayList<>(100));
        List<SmaliClass> removedClasses = Collections.synchronizedList(new ArrayList<>(100));
        ArrayList<Future<?>> futures = new ArrayList<>(smaliFiles.size());

        for (SmaliClass smaliClass : smaliClasses) {
            Runnable r = () -> {
                if (!acceptPath(target.getSkipPath(), smaliClass.getFile().getPath())) {
                    removedClasses.add(smaliClass);
                    return;
                }

                if (acceptBody(target, smaliClass.getNewBody())) {
                    removedClasses.add(smaliClass);
                    filtered.add(new SmaliClass(smaliClass.getFile(), smaliClass.getNewBody()));       //fixme ???
                }
            };
            futures.add(BackgroundWorker.submit(r));
        }

        AtomicReference<Exception> error = new AtomicReference<>();
        for (SmaliFile df : smaliFiles) {
            Runnable r = () -> {
                if (!acceptPath(target.getSkipPath(), df.getPath())) {
                    removedFiles.add(df);
                    return;
                }

                String body = df.getBody();
                if (!body.contains(target.getRef()) || error.get() != null)
                    return;

                if (body.charAt(body.indexOf('\n') - 1) == '\r')    //get rid of windows \r
                    body = body.replace("\r", "");

                if (acceptBody(target, body)) {
                    removedFiles.add(df);
                    try {
                        filtered.add(new SmaliClass(df, body));
                    } catch (Exception e) {
                        error.set(e);
                    }
                }
            };
            futures.add(BackgroundWorker.submit(r));
        }
        BackgroundWorker.compute(futures);
        if (error.get() != null)
            throw new RuntimeException(error.get());
        smaliFiles.removeAll(removedFiles);
        smaliClasses.removeAll(removedClasses);
        return filtered;
    }

    private boolean acceptBody(SmaliTarget target, String body) {
        return target.containsInside(body);
    }

    private boolean acceptPath(String target, String path) {
        int pos = path.indexOf('/') + 1;
        return !path.startsWith(target, pos);
    }
}
