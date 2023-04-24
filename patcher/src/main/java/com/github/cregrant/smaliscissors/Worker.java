package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.common.BackgroundWorker;
import com.github.cregrant.smaliscissors.common.outer.DexExecutor;
import com.github.cregrant.smaliscissors.rule.types.RemoveCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.currentTimeMillis;

public class Worker {

    private static final Logger logger = LoggerFactory.getLogger(Worker.class);
    private final BackgroundWorker executor = new BackgroundWorker();
    private final DexExecutor dexExecutor;
    private final ArrayList<Project> projects = new ArrayList<>(5);
    private final ArrayList<Patch> patches = new ArrayList<>(5);

    public Worker(DexExecutor dexExecutor, List<String> projectsList) {
        this.dexExecutor = dexExecutor;
        setProjects(projectsList);
    }

    void setProjects(List<String> projectsList) {
        for (String path : projectsList) {
            try {
                Project project = new Project(path, executor, dexExecutor);
                projects.add(project);
            } catch (Exception e) {
                logger.error("Error: skipping project \"" + path + "\"! (" + e.getMessage() + ")");
            }
        }
    }

    void addPatches(List<String> patchesList) {
        for (String patchString : patchesList) {
            patches.add(new Patch(patchString));
        }
    }

    void addSingleRemoveCodeRules(List<String> smaliPaths) {
        if (smaliPaths == null || smaliPaths.isEmpty()) {
            return;
        }

        ArrayList<String> targetsList = new ArrayList<>(smaliPaths.size());
        for (String s : smaliPaths) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                targetsList.add(trimmed);
            }
        }
        RemoveCode rule = new RemoveCode(targetsList);
        patches.add(new Patch(rule));
    }

    void run() {
        try {
            long globalStartTime = currentTimeMillis();
            for (Project project : projects) {
                long projectStartTime = currentTimeMillis();
                logger.info("Project - " + project.getName());
                for (Patch patch : patches) {
                    project.scan(patch.isSmaliNeeded(), patch.isXmlNeeded());
                    logger.info("Patch - " + patch.getName() + "\n");
                    long patchStartTime = currentTimeMillis();
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
        } catch (Exception e) {
            logger.error("Unexpected error", e);
        } finally {
            executor.stop();
        }
    }
}
