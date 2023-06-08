package com.github.cregrant.smaliscissors;

import com.github.cregrant.smaliscissors.common.ApkLocator;
import com.github.cregrant.smaliscissors.common.BackgroundWorker;
import com.github.cregrant.smaliscissors.common.ProjectProperties;
import com.github.cregrant.smaliscissors.common.decompiledfiles.DecompiledFile;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;
import com.github.cregrant.smaliscissors.common.decompiledfiles.XmlFile;
import com.github.cregrant.smaliscissors.common.outer.DexExecutor;
import com.github.cregrant.smaliscissors.common.outer.SmaliGenerator;
import com.github.cregrant.smaliscissors.manifest.Manifest;
import com.github.cregrant.smaliscissors.removecode.SmaliKeeper;
import com.github.cregrant.smaliscissors.rule.types.Rule;
import com.github.cregrant.smaliscissors.util.Regex;
import com.github.cregrant.smaliscissors.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

public class Project {

    private static final Logger logger = LoggerFactory.getLogger(Project.class);
    private final Patcher patcher;
    private final MemoryManager memoryManager;
    private final SmaliKeeper smaliKeeper;
    private final ProjectProperties properties;
    private final String apkPath;
    private final String path;
    private final String name;
    private final Manifest manifest;
    private final ProjectState state = new ProjectState();
    private ArrayList<SmaliFile> smaliList = new ArrayList<>();
    private ArrayList<XmlFile> xmlList = new ArrayList<>();

    public Project(String projectPath, String apkPath, Patcher patcher) {
        this.path = projectPath;
        this.patcher = patcher;
        name = Regex.getFilename(projectPath);
        manifest = new Manifest(this);
        memoryManager = new MemoryManager(this);
        this.apkPath = apkPath != null ? apkPath : new ApkLocator().getApkPath(this);
        smaliKeeper = new SmaliKeeper(this);
        properties = new ProjectProperties(this);
    }

    void scan(boolean scanSmali, boolean scanXml) throws FileNotFoundException {
        Scanner scanner = new Scanner(this);
        if (scanSmali && !isSmaliScanned()) {
            smaliList = scanner.scanSmali();
            state.smaliScanned = true;
        }
        if (scanXml && !isXmlScanned()) {
            xmlList = scanner.scanXml();
            state.xmlScanned = true;
        }
        getMemoryManager().tryEnableCache();
    }

    public void rescanSmali() throws FileNotFoundException {
        state.smaliScanned = false;
        scan(true, false);
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
                patch.jumpToRuleName(rule.nextRuleName());  //if a next rule name is not null
            }
            patch.reset();
        } catch (IOException e) {
            logger.error(patch.getName() + " failed", e);
        }
    }

    void writeChanges() {
        logger.debug("Writing changes...");
        properties.save();
        if (!isXmlCacheEnabled() && !isSmaliCacheEnabled()) {
            return;
        }
        ArrayList<DecompiledFile> list = new ArrayList<DecompiledFile>(smaliList);
        list.addAll(xmlList);
        ArrayList<Future<?>> futures = new ArrayList<>(list.size());
        for (final DecompiledFile dFile : list) {
            futures.add(patcher.getExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    dFile.save();
                }
            }));
        }
        patcher.getExecutor().waitForFinish(futures);
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

    public List<DecompiledFile> applyTargetAssignments(String target) {
        switch (target) {
            case "[APPLICATION]":
                return Collections.singletonList(manifest.getApplicationFile());
            case "[ACTIVITIES]":
                return manifest.getActivityFiles();
            case "[LAUNCHER_ACTIVITIES]":
                return manifest.getLauncherActivityFiles();
            default:
                return new ArrayList<>();
        }
    }

    public HashSet<String> getProtectedClasses() {
        return manifest.getProtectedClasses();
    }

    public DexExecutor getDexExecutor() {
        return patcher.getDexExecutor();
    }

    public SmaliGenerator getSmaliGenerator() {
        return patcher.getSmaliGenerator();
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
        return state.smaliCacheEnabled;
    }

    public void setSmaliCacheEnabled(boolean smaliCacheEnabled) {
        state.smaliCacheEnabled = smaliCacheEnabled;
    }

    public boolean isXmlCacheEnabled() {
        return state.xmlCacheEnabled;
    }

    public void setXmlCacheEnabled(boolean xmlCacheEnabled) {
        state.xmlCacheEnabled = xmlCacheEnabled;
    }

    public boolean isSmaliScanned() {
        return state.smaliScanned;
    }

    public boolean isXmlScanned() {
        return state.xmlScanned;
    }

    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

    public BackgroundWorker getExecutor() {
        return patcher.getExecutor();
    }

    public SmaliKeeper getSmaliKeeper() {
        return smaliKeeper;
    }

    public ProjectProperties getProperties() {
        return properties;
    }

    private static class ProjectState {
        private boolean smaliScanned;
        private boolean xmlScanned;
        private boolean smaliCacheEnabled;
        private boolean xmlCacheEnabled;
    }
}
