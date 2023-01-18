package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.common.outer.DexExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void runPatcher(DexExecutor dexExecutor, List<String> projects,
                                  List<String> patches, List<String> smaliPaths) throws Exception {
        try {
            Worker worker = new Worker(dexExecutor, projects);
            if (!smaliPaths.isEmpty()) {
                worker.addSingleRemoveCodeRules(smaliPaths);
            }
            worker.addPatches(patches);
            worker.run();
        } catch (Exception e) {
            logger.error("Execution interrupted:", e);
            throw e;
        }
    }
}