package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.common.ApkLocator;
import com.github.cregrant.smaliscissors.common.BackgroundWorker;
import com.github.cregrant.smaliscissors.common.decompiledfiles.DecompiledFile;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;
import com.github.cregrant.smaliscissors.common.decompiledfiles.XmlFile;
import com.github.cregrant.smaliscissors.removecode.XmlParser;
import com.github.cregrant.smaliscissors.removecode.XmlParserRaw;
import com.github.cregrant.smaliscissors.rule.types.Rule;
import com.github.cregrant.smaliscissors.util.IO;
import com.github.cregrant.smaliscissors.util.Regex;
import com.github.cregrant.smaliscissors.util.Scanner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

public class Project {
    private final MemoryManager memoryManager;
    private final BackgroundWorker executor;
    private final String apkPath;
    private final String path;
    private final String name;
    private ArrayList<SmaliFile> smaliList = new ArrayList<>();
    private ArrayList<XmlFile> xmlList = new ArrayList<>();
    private HashSet<String> protectedClasses;
    private boolean smaliScanned;
    private boolean xmlScanned;
    private boolean smaliCacheEnabled;
    private boolean xmlCacheEnabled;

    public Project(String path, BackgroundWorker executor) {
        this.path = path;
        this.executor = executor;
        name = Regex.getFilename(path);
        memoryManager = new MemoryManager(this);
        apkPath = new ApkLocator().getApkPath(this);
        getProtectedClasses();
    }

    void scan(boolean scanSmali, boolean scanXml) throws FileNotFoundException {
        Scanner scanner = new Scanner(this);
        if (scanSmali && !isSmaliScanned()) {
            smaliList = scanner.scanSmali();
            smaliScanned = true;
        }
        if (scanXml && !isXmlScanned()) {
            xmlList = scanner.scanXml();
            xmlScanned = true;
        }
        getMemoryManager().tryEnableCache();
    }

    public void scan(ArrayList<String> files) {
        Scanner scanner = new Scanner(this);
        ArrayList<DecompiledFile> decompiledFiles = scanner.scanFiles(files);
        for (DecompiledFile df : decompiledFiles) {
            if (df instanceof SmaliFile) {
                smaliList.add(((SmaliFile) df));
            } else {
                xmlList.add(((XmlFile) df));
            }
        }
    }

    void applyPatch(Patch patch) {
        try {
            while (true) {
                Rule rule = patch.getNextRule();
                if (rule == null) {
                    break;
                }

                Main.out.println("\n" + rule);
                rule.apply(this, patch);
                if (rule.nextRuleName() != null) {
                    patch.jumpToRuleName(rule.nextRuleName());
                }
            }
            patch.reset();
        } catch (IOException e) {
            Main.out.println(patch.getName() + " failed. Reason: " + e.getMessage());
            e.printStackTrace();
        }
    }

    void writeChanges() {
        if (!isXmlCacheEnabled() && !isSmaliCacheEnabled()) {
            return;
        }
        if (Prefs.logLevel == Prefs.Log.DEBUG) {
            Main.out.println("Writing changes to disk...");
        }
        ArrayList<DecompiledFile> list = new ArrayList<DecompiledFile>(smaliList);
        list.addAll(xmlList);
        ArrayList<Future<?>> futures = new ArrayList<>(list.size());
        for (final DecompiledFile dFile : list) {
            futures.add(executor.submit(new Runnable() {
                @Override
                public void run() {
                    dFile.save();
                }
            }));
        }
        executor.compute(futures);
    }


    public List<String> removeLoadedFile(Project project, String shortPath) {
        boolean isXml = shortPath.startsWith("res");
        boolean isFile = shortPath.endsWith(".smali") || shortPath.endsWith(".xml");
        ArrayList<String> deleted = new ArrayList<>();
        List<? extends DecompiledFile> files;
        if (isXml) {
            files = project.getXmlList();
        } else {
            files = project.getSmaliList();
        }

        int size = files.size();
        if (shortPath.contains("*") || shortPath.contains("?") || shortPath.contains("{") || shortPath.contains("[")) {   //remove by regex
            Pattern pattern = Pattern.compile(Regex.globToRegex(shortPath));
            for (int i = 0; i < size; i++) {
                if (pattern.matcher(files.get(i).getPath()).find()) {
                    deleted.add(files.get(i).getPath());
                    files.remove(i);
                    i--;
                    size--;
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                String filePath = files.get(i).getPath();
                if ((isFile && filePath.equals(shortPath)) || filePath.startsWith(shortPath)) {
                    deleted.add(filePath);
                    files.remove(i);
                    if (isFile) {
                        break;
                    }
                    i--;
                    size--;
                }
            }
        }
        return deleted;
    }

    public HashSet<String> getProtectedClasses() {
        if (protectedClasses == null) {
            protectedClasses = parseProtectedClasses();
        }
        return protectedClasses;
    }

    private HashSet<String> parseProtectedClasses() {
        File manifest = new File(path + File.separator + "AndroidManifest.xml");
        if (manifest.exists())
            return new XmlParser(IO.read(manifest.getPath())).parse();

        for (XmlFile file : xmlList) {
            if (file.getPath().equals("AndroidManifest.xml")) {
                return new XmlParser(file.getBody()).parse();
            }
        }

        if (apkPath != null) {
            return XmlParserRaw.parse(apkPath);
        }

        return new HashSet<>();
    }

    public String getApkPath() {
        return apkPath;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public List<SmaliFile> getSmaliList() {
        return smaliList;
    }

    public List<XmlFile> getXmlList() {
        return xmlList;
    }

    public boolean isSmaliCacheEnabled() {
        return smaliCacheEnabled;
    }

    public void setSmaliCacheEnabled(boolean smaliCacheEnabled) {
        this.smaliCacheEnabled = smaliCacheEnabled;
    }

    public boolean isXmlCacheEnabled() {
        return xmlCacheEnabled;
    }

    public void setXmlCacheEnabled(boolean xmlCacheEnabled) {
        this.xmlCacheEnabled = xmlCacheEnabled;
    }

    public boolean isSmaliScanned() {
        return smaliScanned;
    }

    public boolean isXmlScanned() {
        return xmlScanned;
    }

    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

    public BackgroundWorker getExecutor() {
        return executor;
    }
}
