package com.github.cregrant.smaliscissors.util;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.Prefs;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.DecompiledFile;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;
import com.github.cregrant.smaliscissors.common.decompiledfiles.XmlFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Scanner {
    private final Project project;

    public Scanner(Project project) {
        this.project = project;
    }

    public ArrayList<SmaliFile> scanSmali() throws FileNotFoundException {
        ArrayList<SmaliFile> smaliList = new ArrayList<>(1000);
        long startTime = System.currentTimeMillis();
        ArrayList<Future<ArrayList<DecompiledFile>>> tasks = new ArrayList<>();
        File[] list = new File(project.getPath()).listFiles();
        if (list == null) {
            throw new FileNotFoundException("Error: incorrect path - " + project.getPath());
        }

        for (File file : list) {
            if (file.isDirectory() && file.getName().startsWith("smali")) {
                File[] subfolders = file.listFiles();
                if (subfolders == null) {
                    continue;
                }

                for (final File subfolder : subfolders) {   //skip some folders
                    if (Prefs.skipSmaliRootFolders && Prefs.smaliFoldersToSkip.contains(subfolder.getName())) {
                        continue;
                    }
                    Callable<ArrayList<DecompiledFile>> callable = new Callable<ArrayList<DecompiledFile>>() {
                        @Override
                        public ArrayList<DecompiledFile> call() {
                            return scan(subfolder);
                        }
                    };
                    tasks.add(project.getExecutor().submit(callable));
                }
            }
        }
        if (tasks.isEmpty()) {
            throw new FileNotFoundException("Error: no smali folders found inside the project folder \"" + project.getName() + '\"');
        }

        try {
            for (Future<ArrayList<DecompiledFile>> scanned : tasks) {
                for (DecompiledFile scannedFile : scanned.get()) {
                    if (scannedFile != null) {
                        smaliList.add((SmaliFile) scannedFile);
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new ArrayList<>(0);
        }
        Collections.sort(smaliList);
        Collections.reverse(smaliList);
        Main.out.println(smaliList.size() + " smali files found in " + (System.currentTimeMillis() - startTime) + "ms.\n");
        return smaliList;
    }

    public ArrayList<XmlFile> scanXml() throws FileNotFoundException {
        ArrayList<XmlFile> xmlList = new ArrayList<>(500);
        long startTime = System.currentTimeMillis();
        ArrayList<Future<ArrayList<DecompiledFile>>> tasks = new ArrayList<>();

        if (new File(project.getPath() + File.separator + "AndroidManifest.xml").exists()) {
            XmlFile manifest = new XmlFile(project, "AndroidManifest.xml");
            xmlList.add(manifest);
        } else {
            throw new FileNotFoundException("Error: AndroidManifest.xml not found");
        }

        final File resFolder = new File(project.getPath() + File.separator + "res");
        if (resFolder.exists() && Objects.requireNonNull(resFolder.list()).length > 0) {
            Callable<ArrayList<DecompiledFile>> callable = new Callable<ArrayList<DecompiledFile>>() {
                @Override
                public ArrayList<DecompiledFile> call() {
                    return scan(resFolder);
                }
            };
            tasks.add(project.getExecutor().submit(callable));
        } else {
            throw new FileNotFoundException("Error: no files found inside the res folder.");
        }

        try {
            for (Future<ArrayList<DecompiledFile>> scanned : tasks) {
                for (DecompiledFile scannedFile : scanned.get()) {
                    if (scannedFile != null) {
                        xmlList.add(((XmlFile) scannedFile));
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new ArrayList<>(0);
        }
        Collections.sort(xmlList);
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
                if (files == null) {
                    continue;
                }

                for (File file : files) {
                    if (file.isDirectory()) {
                        stack.push(file);
                    } else {
                        decompiledFiles.add(scanFile(file));
                    }
                }
            } else {
                decompiledFiles.add(scanFile(stack.pop()));
            }
        }
        return decompiledFiles;
    }

    public ArrayList<DecompiledFile> scanFiles(ArrayList<String> paths) {
        ArrayList<DecompiledFile> files = new ArrayList<>(paths.size());
        for (String s : paths) {
            DecompiledFile df = scanFile(new File(s));
            if (df != null) {
                files.add(df);
            }
        }
        return files;
    }

    private DecompiledFile scanFile(File file) {
        String origPath = file.getPath();
        String path = origPath.substring(project.getPath().length() + 1).replace('\\', '/');
        DecompiledFile df;

        if (path.endsWith(".smali")) {
            df = new SmaliFile(project, path);
        } else if (path.endsWith(".xml")) {
            df = new XmlFile(project, path);
        } else {
            return null;
        }

        df.setSize((int) file.length());
        return df;
    }
}
