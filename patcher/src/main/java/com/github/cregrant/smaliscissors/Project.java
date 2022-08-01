package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.structures.common.DecompiledFile;
import com.github.cregrant.smaliscissors.structures.common.SmaliFile;
import com.github.cregrant.smaliscissors.structures.common.XmlFile;
import com.github.cregrant.smaliscissors.structures.rules.IRule;
import com.github.cregrant.smaliscissors.utils.Regex;
import com.github.cregrant.smaliscissors.utils.Scanner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

public class Project {
    private final String path;
    private final String name;
    private ArrayList<SmaliFile> smaliList = new ArrayList<>(0);
    private ArrayList<XmlFile> xmlList = new ArrayList<>(0);
    private boolean smaliScanned;
    private boolean xmlScanned;
    private boolean smaliCacheEnabled;
    private boolean xmlCacheEnabled;

    public Project(String path) {
        this.path = path;
        name = Regex.getFilename(path);
    }

    void scan(boolean scanSmali, boolean scanXml) throws FileNotFoundException {
        Scanner scanner = new Scanner(this);
        if (scanSmali && !smaliScanned) {
            smaliList = scanner.scanSmali();
            smaliScanned = true;
        }
        if (scanXml && !xmlScanned) {
            xmlList = scanner.scanSXml();
            xmlScanned = true;
        }
        tryEnableCache();
    }

    public void scan(ArrayList<String> files) {
        Scanner scanner = new Scanner(this);
        ArrayList<DecompiledFile> decompiledFiles = scanner.scanFiles(files);
        for (DecompiledFile df : decompiledFiles) {
            if (df instanceof SmaliFile)
                smaliList.add(((SmaliFile) df));
            else
                xmlList.add(((XmlFile) df));
        }
    }

    void applyPatch(Patch patch) {
        try {
            while (true) {
                IRule rule = patch.getNextRule();
                if (rule == null)
                    break;
                Main.out.println(rule.toString());

                rule.apply(this, patch);

                Main.out.println("");
                if (rule.nextRuleName() != null)
                    patch.jumpToRuleName(rule.nextRuleName());
            }
            patch.reset();
        } catch (IOException e) {
            Main.out.println(patch.getName() + " failed. Reason: " + e.getMessage());
            e.printStackTrace();
        }
    }

    void writeChanges() {
        if (!xmlCacheEnabled && !smaliCacheEnabled)
            return;
        if (Prefs.logLevel == Prefs.Log.DEBUG)
            Main.out.println("Writing changes to disk...");
        ArrayList<DecompiledFile> list = new ArrayList<>(smaliList);
        list.addAll(xmlList);
        ArrayList<Future<?>> futures = new ArrayList<>(list.size());
        for (DecompiledFile dFile : list) {
            futures.add(BackgroundWorker.executor.submit(dFile::save));
        }
        BackgroundWorker.compute(futures);
    }


    public List<String> removeLoadedFile(Project project, String shortPath) {
        boolean isXml = shortPath.startsWith("res");
        boolean isFile = shortPath.endsWith(".smali") || shortPath.endsWith(".xml");
        ArrayList<String> deleted = new ArrayList<>();
        List<? extends DecompiledFile> files;
        if (isXml)
            files = project.getXmlList();
        else
            files = project.getSmaliList();

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
                if (isFile && filePath.equals(shortPath) || filePath.startsWith(shortPath)) {
                    deleted.add(filePath);
                    files.remove(i);
                    if (isFile)
                        break;
                    i--;
                    size--;
                }
            }
        }
        return deleted;
    }

    private void tryEnableCache() {
        long max = Runtime.getRuntime().maxMemory() - 30000000;     //max heap size - 30MB
        if (smaliScanned) {
            long smaliSize = 0;
            for (DecompiledFile df : smaliList)
                smaliSize += df.getSize();
            if ((float) smaliSize / max < 0.7f) {
                smaliCacheEnabled = true;
                max -= smaliSize;
            }
        }
        if (xmlScanned) {
            long xmlSize = 0;
            for (DecompiledFile df : xmlList)
                xmlSize += df.getSize();
            if ((float) xmlSize / max < 0.7f)
                xmlCacheEnabled = true;
        }
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

    public boolean isXmlCacheEnabled() {
        return xmlCacheEnabled;
    }
}
