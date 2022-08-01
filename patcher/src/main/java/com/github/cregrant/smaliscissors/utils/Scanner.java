package com.github.cregrant.smaliscissors.utils;

import com.github.cregrant.smaliscissors.BackgroundWorker;
import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.Prefs;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.structures.common.DecompiledFile;
import com.github.cregrant.smaliscissors.structures.common.SmaliFile;
import com.github.cregrant.smaliscissors.structures.common.XmlFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

public class Scanner {
    private final Project project;
    int pathOffset;

    public Scanner(Project project) {
        this.project = project;
        pathOffset = project.getPath().length() + 1;
    }

    public ArrayList<SmaliFile> scanSmali() throws FileNotFoundException {
        ArrayList<SmaliFile> smaliList = new ArrayList<>(1000);
        long startTime = System.currentTimeMillis();
        ArrayList<Future<ArrayList<DecompiledFile>>> tasks = new ArrayList<>();
        File[] list = new File(project.getPath()).listFiles();
        if (list == null)
            throw new FileNotFoundException("Error: incorrect path \"" + project.getPath());

        for (File file : list) {
            if (file.isDirectory() && file.getName().startsWith("smali")) {
                File[] subfolders = file.listFiles();
                if (subfolders == null)
                    continue;

                for (File subfolder : subfolders) {
                    if (Prefs.skipSmaliRootFolders && Prefs.smaliFoldersToSkip.contains(subfolder.getName()))   //skip some folders
                        continue;
                    tasks.add(BackgroundWorker.executor.submit(() -> scan(subfolder)));
                }
            }
        }
        if (tasks.isEmpty())
            throw new FileNotFoundException("Error: no smali folders found inside the project folder \"" + project.getName() + '\"');

        try {
            for (Future<ArrayList<DecompiledFile>> scanned : tasks) {
                for (DecompiledFile scannedFile : scanned.get()) {
                    if (scannedFile == null)
                        continue;
                    smaliList.add((SmaliFile) scannedFile);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new ArrayList<>(0);
        }
        Collections.sort(smaliList, Comparator.comparingInt(DecompiledFile::getSize));
        Collections.reverse(smaliList);
        Main.out.println(smaliList.size() + " smali files found in " + (System.currentTimeMillis() - startTime) + "ms.\n");
        return smaliList;
    }

    public ArrayList<XmlFile> scanSXml() throws FileNotFoundException {
        ArrayList<XmlFile> xmlList = new ArrayList<>(500);
        long startTime = System.currentTimeMillis();
        ArrayList<Future<ArrayList<DecompiledFile>>> tasks = new ArrayList<>();

        if (new File(project.getPath() + File.separator + "AndroidManifest.xml").exists()) {
            XmlFile manifest = new XmlFile(project, "AndroidManifest.xml");
            xmlList.add(manifest);
        } else
            throw new FileNotFoundException("Error: AndroidManifest.xml not found");

        File resFolder = new File(project.getPath() + File.separator + "res");
        if (resFolder.exists() && Objects.requireNonNull(resFolder.list()).length > 0) {
            tasks.add(BackgroundWorker.executor.submit(() -> scan(resFolder)));
        } else
            throw new FileNotFoundException("Error: no files found inside the res folder.");

        try {
            for (Future<ArrayList<DecompiledFile>> scanned : tasks) {
                for (DecompiledFile scannedFile : scanned.get()) {
                    if (scannedFile == null)
                        continue;
                    xmlList.add(((XmlFile) scannedFile));
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new ArrayList<>(0);
        }
        //noinspection Java8ListSort
        Collections.sort(xmlList, Comparator.comparingInt(DecompiledFile::getSize));
        Collections.reverse(xmlList);
        Main.out.println(xmlList.size() + " xml files found in " + (System.currentTimeMillis() - startTime) + "ms.\n");
        return xmlList;
    }

    ArrayList<DecompiledFile> scan(File parent) {
        ArrayList<DecompiledFile> decompiledFiles = new ArrayList<>();
        Stack<File> stack = new Stack<>();
        stack.add(parent);
        while (!stack.isEmpty()) {
            if (stack.peek().isDirectory()) {
                File[] files = stack.pop().listFiles();
                if (files == null)
                    continue;

                for (File file : files) {
                    if (file.isDirectory())
                        stack.push(file);
                    else {
                        DecompiledFile tmp = scanFile(file);
                        if (tmp != null)
                            decompiledFiles.add(tmp);
                    }
                }
            } else
                decompiledFiles.add(scanFile(stack.pop()));
        }
        return decompiledFiles;
    }

    public ArrayList<DecompiledFile> scanFiles(ArrayList<String> paths) {
        ArrayList<DecompiledFile> files = new ArrayList<>(paths.size());
        for (String s : paths) {
            DecompiledFile df = scanFile(new File(s));
            if (df != null)
                files.add(df);
        }
        return files;
    }

    private DecompiledFile scanFile(File file) {
        String origPath = file.getPath();
        String path = origPath.substring(pathOffset).replace('\\', '/');
        DecompiledFile df;

        if (path.endsWith(".smali"))
            df = new SmaliFile(project, path);
        else if (path.endsWith(".xml"))
            df = new XmlFile(project, path);
        else
            return null;

        df.setSize((int) file.length());
        return df;
    }

    public String getApkPath() {
        String apkPathSupplied = Main.dex.getApkPath();
        if (apkPathSupplied != null) {
            File file = new File(apkPathSupplied);
            if (file.exists())
                return apkPathSupplied;
        }

        File[] files = new File(project.getPath()).listFiles();
        if (files != null) {
            for (File str : files) {
                if (str.getName().startsWith("apktool."))
                    return parseApktoolConfig(str);
            }
        }
        return null;
    }

    private String parseApktoolConfig(File config) {
        Pattern pattern = Pattern.compile(".{0,5}apkFile.+?(?:\": \"|: )(.+?\\.apk)(?:\",)?");
        try {
            String scannedPath = Regex.matchSingleLine(IO.read(config.getPath()), pattern);
            if (scannedPath == null)
                return null;

            File parentFile = new File(project.getPath()).getParentFile();
            File apkFile = new File(parentFile + File.separator + scannedPath);
            if (apkFile.exists())
                return apkFile.getPath();
            apkFile = new File(scannedPath);
            if (apkFile.exists())
                return apkFile.getPath();
        } catch (Exception ignored) {
        }
        return null;
    }
}
