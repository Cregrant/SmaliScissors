package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.structures.DecompiledFile;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

public class Scanner {
    private final Project project;
    List<DecompiledFile> smaliList = new ArrayList<>(0);
    List<DecompiledFile> xmlList = new ArrayList<>(0);

    public Scanner(Project project) {
        this.project = project;
    }

    void scanProject(boolean smaliNeeded, boolean xmlNeeded) {
        long startTime = System.currentTimeMillis();
        ArrayList<Future<ArrayList<DecompiledFile>>> tasks = new ArrayList<>();

        if (smaliNeeded) {         //add smali folders
            smaliList = Collections.synchronizedList(new ArrayList<>(10000));

            File[] list = new File(project.getPath()).listFiles();
            if (Objects.requireNonNull(list).length == 0)
                Main.out.println("WARNING: no smali folders found inside the project folder \"" + project.getName() + '\"');
            for (File file : list) {
                if (file.isDirectory() && file.getName().startsWith("smali")) {
                    tasks.add(BackgroundWorker.executor.submit(() -> scanFolder(file)));
                }
            }
        }

        if (xmlNeeded) {        //add AndroidManifest.xml & res folder
            xmlList = Collections.synchronizedList(new ArrayList<>(1000));
            if (new File(project.getPath() + File.separator + "AndroidManifest.xml").exists()) {
                DecompiledFile manifest = new DecompiledFile(project.getPath(), "AndroidManifest.xml", true);
                xmlList.add(manifest);
            }
            File resFolder = new File(project.getPath() + File.separator + "res");
            if (resFolder.exists() && Objects.requireNonNull(resFolder.list()).length > 0) {
                tasks.add(BackgroundWorker.executor.submit(() -> scanFolder(resFolder)));
            }
            else
                Main.out.println("WARNING: no resources found inside the res folder.");
        }

        if (tasks.isEmpty())
            return;

        try {
            for (Future<ArrayList<DecompiledFile>> array : tasks) {
                for (DecompiledFile scannedFile : array.get()) {
                    if (scannedFile.isXML()) {
                        xmlList.add(scannedFile);
                    }
                    else {
                        smaliList.add(scannedFile);
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return;
        }
        smaliList.sort(Comparator.comparingInt(DecompiledFile::getSize));
        xmlList.sort(Comparator.comparingInt(DecompiledFile::getSize));
        Collections.reverse(smaliList);
        Collections.reverse(xmlList);

        ArrayList<Future<?>> futures = new ArrayList<>();
        if (Prefs.keepSmaliFilesInRAM)
            futures.addAll(loadToRam(smaliList));

        if (Prefs.keepXmlFilesInRAM)
            futures.addAll(loadToRam(xmlList));

        BackgroundWorker.compute(futures);
        Main.out.println(smaliList.size() + " smali & " + xmlList.size() + " xml files found in " + (System.currentTimeMillis() - startTime) + "ms.\n");
    }

    private ArrayList<Future<?>> loadToRam(List<DecompiledFile> array) {
        ArrayList<Future<?>> futures = new ArrayList<>(array.size());
        for (DecompiledFile dFile : array) {
            Runnable r = () -> dFile.setBody(IO.read(project.getPath() + File.separator + dFile.getPath()));
            futures.add(BackgroundWorker.executor.submit(r));
        }
        return futures;
    }

    ArrayList<DecompiledFile> scanFolder(File folder) {
        ArrayList<DecompiledFile> decompiledFiles = new ArrayList<>();
        Stack<File> stack = new Stack<>();
        stack.add(folder);
        while (!stack.isEmpty()) {
            if (stack.peek().isDirectory()) {
                for (File file : Objects.requireNonNull(stack.pop().listFiles())) {

                    if (file.isDirectory()) {
                        if (Prefs.skipSomeSmaliFiles) {                  //skip some folders
                            for (String str : Prefs.smaliFoldersToSkip) {
                                if (file.getPath().startsWith(str))
                                    break;
                            }
                        }
                        stack.push(file);
                    }

                    else {
                        DecompiledFile tmp = scanFile(file.getPath());
                        if (tmp != null) {
                            tmp.setSize((int) file.length());
                            decompiledFiles.add(tmp);
                        }
                    }
                }
            }
            else
                decompiledFiles.add(scanFile(stack.pop().getPath()));
        }
        return decompiledFiles;
    }

    public ArrayList<DecompiledFile> scanFiles(ArrayList<String> paths) {
        ArrayList<DecompiledFile> files = new ArrayList<>(paths.size());
        for (String s : paths) {
            files.add(scanFile(s));
        }
        return files;
    }

    private DecompiledFile scanFile(String file) {
        String path = file.replace(project.getPath() + File.separator, "");
        boolean isSmali = path.endsWith(".smali");
        boolean isXml = path.endsWith(".xml");

        if (!(isSmali || isXml))
            return null;

        return new DecompiledFile(project.getPath(), path.replace('\\', '/'), isXml);
    }

    public static ArrayList<String> removeLoadedFile(Project project, String shortPath, boolean returnDeleted) {
        boolean isXml = shortPath.startsWith("res");
        boolean isFile = shortPath.endsWith(".smali") || shortPath.endsWith(".xml");
        ArrayList<String> deleted = new ArrayList<>();
        List<DecompiledFile> files;
        if (isXml)
            files = project.getXmlList();
        else
            files = project.getSmaliList();

        int size = files.size();

        if (shortPath.contains("*") || shortPath.contains("?")) {   //remove by regex
            shortPath = Regex.globToRegex(shortPath);
            for (int i = 0; i < size; i++) {
                if (files.get(i).getPath().matches(shortPath)) {
                    if (returnDeleted)
                        deleted.add(files.get(i).getPath());
                    files.remove(i);
                    i--;
                    size--;
                }
            }
        }
        else {
            for (int i = 0; i < size; i++) {
                if (files.get(i).getPath().contains(shortPath)) {
                    if (returnDeleted)
                        deleted.add(files.get(i).getPath());
                    files.remove(i);
                    if (isFile)
                        break;
                    i--;
                    size--;
                }
            }
        }
        if (Prefs.verbose_level == 0)
            Main.out.println(shortPath + " removed.");
        return deleted;
    }

    public String getApkPath() {
        String apkPathSupplied = Main.dex.getApkPath();
        if (apkPathSupplied != null) {
            File file = new File(apkPathSupplied);
            if (file.exists())
                return apkPathSupplied;
        }

        File[] files = new File("").listFiles();
        if (files != null) {
            for (File str : files) {
                if (str.getName().startsWith("apktool."))
                    return parseConfig(str);
            }
        }

        return null;
    }

    private String parseConfig(File config) {
        Pattern pattern = Pattern.compile(".{0,5}apkFile.+?(?:\": \"|: )(.+?\\.apk)(?:\",)?");
        String scannedPath = Regex.matchSingleLine(IO.read(config.getPath()), pattern);
        if (scannedPath != null) {
            File parentFile = new File(project.getPath()).getParentFile();
            File apkFile = new File(parentFile + File.separator + scannedPath);
            if (apkFile.exists())
                return apkFile.getPath();
            apkFile = new File(scannedPath);
            if (apkFile.exists())
                return apkFile.getPath();
        }
        return null;
    }

    public List<DecompiledFile> getSmaliList() {
        return smaliList;
    }

    public List<DecompiledFile> getXmlList() {
        return xmlList;
    }
}
