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
import java.util.Objects;

public class Main {
    public static final OutStream out = System.out::println;
    public static final DexExecutor dex = (dexPath, entrance, mainClass, apkPath, zipPath, projectPath, param, tempDir) -> {
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
    };

    public static void main(String[] args) {
        File patchesDir = new File(System.getProperty("user.dir") + File.separator + "patches");
        File projectsHome = new File("C:/BAT/_INPUT_APK");
        Prefs.loadConf();
        while (true) {
            if (!projectsHome.isDirectory()) {
                out.println("Error loading projects folder\n" + projectsHome);
                System.exit(1);
            }
            File[] projectsArr = null;
            try {
                projectsArr = projectsHome.listFiles();
            } catch (NullPointerException e) {
                out.println("Projects folder is empty\n" + projectsHome);
                System.exit(1);
            }
            ArrayList<String> projectsList = new ArrayList<>();
            for (File project : Objects.requireNonNull(projectsArr)) {
                if (project.isDirectory())
                    projectsList.add(project.toString());
            }

            ArrayList<String> projectsToPatch;
            if (projectsList.size()>1) {
                String msg = "\ncom.github.cregrant.Select project. Enter = all. X - cancel. Example: 0 or 0 1 2 (means 0 and 1 and 2).";
                projectsToPatch = Select.select(projectsList, msg, "No decompiled projects found");
            }
            else
                projectsToPatch = projectsList;

            if (projectsToPatch.get(0).equals("cancel")) break;
            ArrayList<String> zipFilesArr = new ArrayList<>();
            for (File zip : Objects.requireNonNull(patchesDir.listFiles())) {
                if (zip.toString().endsWith(".zip"))
                    zipFilesArr.add(zip.getName());
            }
            ArrayList<String> zipArr = Select.select(zipFilesArr, "\nNow select patch:", "No patches detected");
            ArrayList<String> fullZipArr = new ArrayList<>(zipArr.size());
            if (zipArr.get(0).equals("cancel")) break;
            for (String sh : zipArr)
                fullZipArr.add(patchesDir + File.separator + sh);
            projectsToPatch.addAll(fullZipArr);
            //new Frame().MainFrame();
            com.github.cregrant.smaliscissors.Main.main(projectsToPatch.toArray(new String[0]), out, dex);
        }
    }
}
