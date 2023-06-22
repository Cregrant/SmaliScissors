package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.common.BackgroundWorker;
import com.github.cregrant.smaliscissors.common.outer.DexExecutor;
import com.github.cregrant.smaliscissors.common.outer.PatcherTask;
import com.github.cregrant.smaliscissors.common.outer.SmaliGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;

import static java.lang.System.currentTimeMillis;

public class Patcher {

    private static final Logger logger = LoggerFactory.getLogger(Patcher.class);
    private final BackgroundWorker executor = new BackgroundWorker();
    private final DexExecutor dexExecutor;
    private final SmaliGenerator smaliGenerator;
    private final ArrayList<PatcherTask> tasks;

    public Patcher(DexExecutor dexExecutor, SmaliGenerator smaliGenerator, Collection<PatcherTask> tasks) {
        this.dexExecutor = dexExecutor;
        this.smaliGenerator = smaliGenerator;
        this.tasks = new ArrayList<>(tasks);
    }

    public void run() throws Exception {
        long globalStartTime = currentTimeMillis();
        try {
            for (PatcherTask task : tasks) {
                long projectStartTime = currentTimeMillis();
                Project project;
                try {
                    project = new Project(task.getProjectPath(), task.getApkPath(), this);
                } catch (Exception e) {
                    logger.error("Skipping project \"" + task.getProjectPath() + "\"! (" + e.getClass() + " - " + e.getMessage() + ")");
                    continue;
                }

                logger.info("Project - " + project.getName());
                for (Patch patch : task.getPatches()) {
                    project.scan(patch.isSmaliNeeded(), patch.isXmlNeeded());

                    long patchStartTime = currentTimeMillis();
                    logger.info("Patch - " + patch.getName() + "\n");
                    project.applyPatch(patch);
                    logger.info(patch.getName() + " applied in " + (currentTimeMillis() - patchStartTime) + "ms.");
                }
                project.writeChanges();
                logger.info(project.getName() + " finished in " + (currentTimeMillis() - projectStartTime) + "ms." + "\n------------------");
            }

            logger.info("Tasks completed in " + (currentTimeMillis() - globalStartTime) + "ms." + "\n------------------");
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
            logger.error("Probably patch require some files that haven't been decompiled yet.");
            throw e;
        } catch (Throwable e) {
            logger.error("Unexpected error", e);
            throw e;
        } finally {
            executor.stop();
        }
    }

    public BackgroundWorker getExecutor() {
        return executor;
    }

    public DexExecutor getDexExecutor() {
        return dexExecutor;
    }

    public SmaliGenerator getSmaliGenerator() {
        return smaliGenerator;
    }
}
