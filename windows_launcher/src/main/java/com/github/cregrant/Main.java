package com.github.cregrant;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import com.github.cregrant.smaliscissors.common.outer.DexExecutor;
import com.googlecode.dex2jar.tools.Dex2jarCmd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        File patchesDir = new File(System.getProperty("user.dir") + File.separator + "patches");
        File projectsDir = new File("C:/BAT/_INPUT_APK");

        while (true) {
            Scanner scanner = new Scanner(projectsDir, patchesDir);
            if (scanner.scanFailed()) {
                break;
            }

            String msg = "\nSelect a project. Enter = all. x - exit. Example: 0 or 0 1 2 (means 0 and 1 and 2).";
            Picker projectsPicker = new Picker(scanner.getScannedProjects(), msg);
            ArrayList<String> selectedProjects = new ArrayList<>();
            for (String project : projectsPicker.getChoice()) {
                selectedProjects.add("-i");
                selectedProjects.add(project);
            }
            if (selectedProjects.isEmpty()) {
                break;
            }

            String msg2 = "\nNow select a patch:";
            Picker patchesPicker = new Picker(scanner.getScannedPatches(), msg2);
            ArrayList<String> selectedPatches = new ArrayList<>();
            for (String patch : patchesPicker.getChoice()) {
                selectedPatches.add("-p");
                selectedPatches.add(patch);
            }
            if (selectedPatches.isEmpty()) {
                break;
            }

            ArrayList<String> strings = new ArrayList<>(selectedProjects);
            strings.addAll(selectedPatches);
            com.github.cregrant.smaliscissors.Main.mainAsModule(strings.toArray(new String[0]), new DexExecutorWindows());
            Thread.sleep(100);
        }
    }

    private static void setLogLevel(String logLevel) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = loggerContext.getLogger("root");
        logger.setLevel(Level.toLevel(logLevel));

        FileAppender<ILoggingEvent> fileAppender = (FileAppender<ILoggingEvent>) logger.getAppender("FILE");
        if (fileAppender != null) {
            fileAppender.stop();
            fileAppender.setFile("Logs/log.txt");
            fileAppender.start();
        } else {
            System.err.println("FILE appender is not defined");
        }
    }

    static class DexExecutorWindows implements DexExecutor {

        @Override
        public void runDex(String dexPath, String entrance, String mainClass, String apkPath, String zipPath, String projectPath, String param, String tempDir) {
            new Dex2jarCmd().doMain(dexPath);
            try {
                URL jarUrl = Paths.get(dexPath.replace(".dex", ".jar")).toUri().toURL();
                try (URLClassLoader cl = new URLClassLoader(new URL[]{jarUrl}, Main.class.getClassLoader())) {
                    Class<?> loadedClass = cl.loadClass(mainClass);
                    Object instance = loadedClass.getDeclaredConstructor().newInstance();
                    Method method = loadedClass.getMethod(entrance, String.class, String.class, String.class, String.class);
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
