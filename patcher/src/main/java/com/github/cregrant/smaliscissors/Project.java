package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.common.ApkLocator;
import com.github.cregrant.smaliscissors.common.BackgroundWorker;
import com.github.cregrant.smaliscissors.common.ProjectProperties;
import com.github.cregrant.smaliscissors.common.decompiledfiles.DecompiledFile;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;
import com.github.cregrant.smaliscissors.common.decompiledfiles.XmlFile;
import com.github.cregrant.smaliscissors.common.outer.DexExecutor;
import com.github.cregrant.smaliscissors.removecode.SmaliKeeper;
import com.github.cregrant.smaliscissors.removecode.manifestparsers.BinaryParser;
import com.github.cregrant.smaliscissors.removecode.manifestparsers.DecompiledParser;
import com.github.cregrant.smaliscissors.rule.types.Rule;
import com.github.cregrant.smaliscissors.util.Regex;
import com.github.cregrant.smaliscissors.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

public class Project {

    private static final Logger logger = LoggerFactory.getLogger(Project.class);
    private final MemoryManager memoryManager;
    private final BackgroundWorker executor;
    private final DexExecutor dexExecutor;
    private final SmaliKeeper smaliKeeper;
    private final ProjectProperties properties;
    private final String apkPath;
    private final String path;
    private final String name;
    private XmlFile manifest;
    private ArrayList<SmaliFile> smaliList = new ArrayList<>();
    private ArrayList<XmlFile> xmlList = new ArrayList<>();
    private HashSet<String> protectedClasses;
    private boolean smaliScanned;
    private boolean xmlScanned;
    private boolean smaliCacheEnabled;
    private boolean xmlCacheEnabled;

    public Project(String path, BackgroundWorker executor, DexExecutor dexExecutor) {
        this.path = path;
        this.executor = executor;
        this.dexExecutor = dexExecutor;
        name = Regex.getFilename(path);
        memoryManager = new MemoryManager(this);
        apkPath = new ApkLocator().getApkPath(this);
        smaliKeeper = new SmaliKeeper(this);
        properties = new ProjectProperties(this);
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

                logger.info("\n" + rule);
                rule.apply(this, patch);
                if (rule.nextRuleName() != null) {
                    patch.jumpToRuleName(rule.nextRuleName());
                }
            }
            patch.reset();
        } catch (IOException e) {
            logger.error(patch.getName() + " failed", e);
        }
    }

    void writeChanges() {
        logger.debug("Writing changes...");
        properties.save();
        if (manifest != null) {
            manifest.save();
        }
        if (!isXmlCacheEnabled() && !isSmaliCacheEnabled()) {
            return;
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
        executor.waitForFinish(futures);
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
        if (getManifest() != null) {
            return new DecompiledParser(manifest.getBody()).parse();
        }
        if (apkPath != null) {
            BinaryParser parser = new BinaryParser(apkPath);
            return parser.getStrings();
        }
        return new HashSet<>();
    }

    public XmlFile getManifest() {
        if (manifest == null) {
            for (XmlFile file : xmlList) {
                if (file.getPath().equals("AndroidManifest.xml")) {
                    manifest = file;
                    return manifest;
                }
            }
            File manifestFile = new File(path + File.separator + "AndroidManifest.xml");
            if (manifestFile.exists()) {
                manifest = new XmlFile(this, "AndroidManifest.xml");
            }
        }
        return manifest;
    }

    public DexExecutor getDexExecutor() {
        return dexExecutor;
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

    public SmaliKeeper getSmaliKeeper() {
        return smaliKeeper;
    }

    public ProjectProperties getProperties() {
        return properties;
    }
}
