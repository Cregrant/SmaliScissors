import com.github.cregrant.smaliscissors.engine.DexExecutor;
import com.github.cregrant.smaliscissors.engine.OutStream;
import com.github.cregrant.smaliscissors.engine.Prefs;
import com.googlecode.dex2jar.tools.Dex2jarCmd;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

public class Main {
    public static OutStream out = System.out::println;
    public static DexExecutor dex = (dexPath, entrance, mainClass, apkPath, zipPath, projectPath, param) -> {
        new Dex2jarCmd().doMain(dexPath);
        try {
            URL jarUrl = Paths.get(dexPath.replace(".dex", ".jar")).toUri().toURL();
            URLClassLoader cl = new URLClassLoader(new URL[] {jarUrl}, Main.class.getClassLoader());
            Class<?> loadedClass = cl.loadClass(mainClass);
            Object instance = loadedClass.getDeclaredConstructor().newInstance();
            Method method = loadedClass.getMethod(entrance, String.class, String.class, String.class, String.class);
            method.invoke(instance, apkPath, zipPath, projectPath, param);
        } catch (IOException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
    };

    public static void main(String[] args) {
        Prefs.run_type = "pc";
        File patchesDir = new File(System.getProperty("user.dir") + File.separator + "patches");
        File projectsHome = new File("C:\\BAT\\_INPUT_APK");
        new Prefs().loadConf();
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
            String msg = "\nSelect project. Enter = all. X - cancel. Example: 0 or 0 1 2 (means 0 and 1 and 2).";
            ArrayList<String> projectsToPatch = Select.select(projectsList, msg, "No decompiled projects found");
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
            com.github.cregrant.smaliscissors.engine.Main.main(projectsToPatch.toArray(new String[0]), out, dex);
        }
    }
}
