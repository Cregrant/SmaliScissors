package com.github.cregrant.smaliscissors.console;

import com.github.cregrant.smaliscissors.common.outer.DexExecutor;
import com.googlecode.dex2jar.tools.Dex2jarCmd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        Args parsedArgs = Args.parseArgs(args);
        if (parsedArgs == null) {
            return;
        }

        List<String> projects;
        List<String> patches;
        List<String> smaliPaths;
        if (!parsedArgs.isInteractiveSelectMode()) {
            projects = parsedArgs.getProjects();
            patches = parsedArgs.getPatches();
            smaliPaths = parsedArgs.getSmaliPaths();
        } else {
            InteractiveChoice choice = new InteractiveChoice(parsedArgs);
            choice.showSelection();
            if (choice.isChoiceFailed()) {
                return;
            }

            projects = choice.getProjectsList();
            patches = choice.getPatchesList();
            smaliPaths = Collections.emptyList();
        }

        com.github.cregrant.smaliscissors.Main.runPatcher(new DexExecutorWindows(), projects, patches, smaliPaths);
    }

    static class DexExecutorWindows implements DexExecutor {

        @Override
        public void runDex(String dexPath, String entrance, String mainClass,
                           String apkPath, String zipPath, String projectPath, String param, String tempDir) {
            new Dex2jarCmd().doMain(dexPath);
            try {
                URL jarUrl = Paths.get(dexPath.replace(".dex", ".jar")).toUri().toURL();
                try (URLClassLoader cl = new URLClassLoader(new URL[]{jarUrl}, Main.class.getClassLoader())) {
                    Class<?> loadedClass = cl.loadClass(mainClass);
                    Object instance = loadedClass.getDeclaredConstructor().newInstance();
                    Method method = loadedClass
                            .getMethod(entrance, String.class, String.class, String.class, String.class);
                    method.invoke(instance, apkPath, zipPath, projectPath, param);
                }
            } catch (Exception e) {
                logger.error("Error executing dex script", e);
            }
        }

        @Override
        public String getApkPath() {
            return null;
        }
    }
}
