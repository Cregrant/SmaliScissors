package com.github.cregrant.smaliscissors.console;

import com.github.cregrant.smaliscissors.Patcher;
import com.github.cregrant.smaliscissors.common.outer.DexExecutor;
import com.github.cregrant.smaliscissors.common.outer.PatcherTask;
import com.googlecode.dex2jar.tools.Dex2jarCmd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Args parsedArgs = Args.parseArgs(args);
        if (parsedArgs == null) {
            return;
        }
        LogbackConfig.useArgs(parsedArgs);

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

        ArrayList<PatcherTask> tasks = new ArrayList<>();
        for (String projectPath : projects) {
            PatcherTask task = new PatcherTask(projectPath)
                    .addPatchPaths(patches)
                    .addSmaliPaths(smaliPaths);
            task.validate();
            tasks.add(task);
        }
        try {
            new Patcher(new Dex2JarExecutor(), null, tasks).run();
        } catch (Exception e) {
            //error already logged
        }
    }

    static class Dex2JarExecutor implements DexExecutor {

        @Override
        public void runDex(String dexPath, String entrance, String mainClass,
                           String apkPath, String zipPath, String projectPath, String param, String tempDir) throws Exception {
            String jarPath = dexPath.replace(".dex", ".jar");
            new Dex2jarCmd().doMain(dexPath, "-o", jarPath, "--force");
            URL jarUrl = Paths.get(jarPath).toUri().toURL();
            try (URLClassLoader cl = new URLClassLoader(new URL[]{jarUrl}, Main.class.getClassLoader())) {
                Class<?> loadedClass = cl.loadClass(mainClass);
                Object instance = loadedClass.getDeclaredConstructor().newInstance();
                Method method = loadedClass.getMethod(entrance, String.class, String.class, String.class, String.class);
                method.invoke(instance, apkPath, zipPath, projectPath, param);
            }
        }

    }
}
