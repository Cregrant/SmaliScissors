package com.github.cregrant;

import com.github.cregrant.smaliscissors.DexExecutor;
import com.github.cregrant.smaliscissors.OutStream;
import com.github.cregrant.smaliscissors.Prefs;
import com.googlecode.dex2jar.tools.Dex2jarCmd;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Main {
    public static final OutStream out = System.out::println;

    public static void main(String[] args) {
        File patchesDir = new File(System.getProperty("user.dir") + File.separator + "patches");
        File projectsDir = new File("C:/BAT/_INPUT_APK");
        Prefs.loadConf();

        while (true) {
            Scanner scanner = new Scanner(projectsDir, patchesDir);
            if (scanner.scanFailed())
                break;

            String msg = "\nPicker project. Enter = all. X - cancel. Example: 0 or 0 1 2 (means 0 and 1 and 2).";
            Picker projectsPicker = new Picker(scanner.getScannedProjects(), msg);
            ArrayList<String> selectedProjects = projectsPicker.getChoice();
            if (selectedProjects.isEmpty())
                break;

            String msg2 = "\nNow select patch:";
            Picker patchesPicker = new Picker(scanner.getScannedPatches(), msg2);
            ArrayList<String> selectedPatches = patchesPicker.getChoice();
            if (selectedPatches.isEmpty())
                break;

            //new Frame().MainFrame();
            ArrayList<String> strings = new ArrayList<>(selectedProjects);
            strings.addAll(selectedPatches);
            com.github.cregrant.smaliscissors.Main.main(strings.toArray(new String[0]), out, new DexExecutorWindows());
        }
    }

    static class DexExecutorWindows implements DexExecutor {

        @Override
        public void runDex(String dexPath, String entrance, String mainClass, String apkPath, String zipPath, String projectPath, String param, String tempDir) {
            new Dex2jarCmd().doMain(dexPath);
            try {
                URL jarUrl = Paths.get(dexPath.replace(".dex", ".jar")).toUri().toURL();
                URLClassLoader cl = new URLClassLoader(new URL[] {jarUrl}, Main.class.getClassLoader());
                Class<?> loadedClass = cl.loadClass(mainClass);
                Object instance = loadedClass.getDeclaredConstructor().newInstance();
                Method method = loadedClass.getMethod(entrance, String.class, String.class, String.class, String.class);
                method.invoke(instance, apkPath, zipPath, projectPath, param);
            } catch (Exception e) {
                Main.out.println(e.getMessage());
            }
        }

        @Override
        public String getApkPath() {
            return null;
        }
    }
}
