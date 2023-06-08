package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.common.outer.DexExecutor;
import com.github.cregrant.smaliscissors.common.outer.PatcherTask;
import com.github.cregrant.smaliscissors.common.outer.SmaliGenerator;

import java.util.Collection;

public class Main {

    public static void runPatcher(DexExecutor dexExecutor, SmaliGenerator smaliGenerator,
                                  Collection<PatcherTask> tasks) throws Exception {
        Patcher worker = new Patcher(dexExecutor, smaliGenerator, tasks);
        worker.run();
    }
}